"""Agreement with the calibration set (TODO.md 2.2i): worked by hand on the real labels' counts, written only under a temporary directory.

The labels are read from calibration/ (ids and counts only are used here); every judged value is synthetic (E-014)."""
import json
import os
import tempfile
import unittest
from fractions import Fraction

from agent_runner import agreement, ensemble, judge
from agent_runner.agreement import AgreementError, FIELDS
from agent_runner.judge import FakeJudge, RenderedPrompt

H = "c" * 64
LABELS = agreement.load_labels()
IDS = sorted(LABELS)
WRONG_TIER = {"toy": "utility", "utility": "product", "product": "research", "research": "toy"}


def ok_result(kind, tier, quality):
    return {"status": "ok", "fields": {"kind": {"value": kind}, "ambition.tier": {"value": tier}, "quality": {"value": quality}}}


def results_for(kind=30, ambition=30, exact=30, within=30):
    """Results right on the first k ids of each field. `exact` and `within` both set quality: ids before `within` are within
    one; of those, the first `exact` are exact (so exact <= within is required) and the rest are one step off or two away."""
    assert exact <= within or within == 30
    out = {}
    for n, i in enumerate(IDS):
        label = LABELS[i]
        q = label["quality"]
        if n < exact:
            quality = q
        elif n < within:
            quality = q + 1 if q < 5 else q - 1  # one off: a miss exactly, a hit within one
        else:
            quality = 5 if q <= 3 else 1  # two or more away
        out[i] = ok_result(label["kind"] if n < kind else "plan only", label["ambition"] if n < ambition else WRONG_TIER[label["ambition"]], quality)
    return out


def live(results, **kw):
    record = agreement.build_record("judge", H, {"haiku": "h", "opus": "o"}, 3, "2026-10-25", LABELS, results, **kw)
    return {f: record["fields"][f]["live"] for f in FIELDS}, record


class Baselines(unittest.TestCase):
    def test_the_real_labels_baselines_are_computed_not_written(self):
        _, record = live(results_for())
        base = {f: (record["fields"][f]["baseline"]["answer"], record["fields"][f]["baseline"]["matched"]) for f in FIELDS}
        self.assertEqual({"kind": ("own-work", 23), "ambition": ("utility", 14), "quality_exact": (3, 17), "quality_within_one": (3, 27)}, base)
        self.assertEqual({"kind": 27, "ambition": 24, "quality_exact": 24, "quality_within_one": 29}, {f: record["fields"][f]["needed"] for f in FIELDS})
        self.assertEqual({"kind": ["generated", "plan only", "mixed", "built"]}, {"kind": [k for k in record["labels_absent"]["kind"] if k != "clone"]})

    def test_the_baselines_follow_the_labels_given(self):
        labels = {"c001": {"kind": "clone", "ambition": "toy", "quality": 1}, "c002": {"kind": "clone", "ambition": "toy", "quality": 5},
                  "c003": {"kind": "own-work", "ambition": "research", "quality": 5}, "c004": {"kind": "clone", "ambition": "toy", "quality": 4}}
        body = agreement.compute(labels, {})
        self.assertEqual(("clone", 3), (body["fields"]["kind"]["baseline"]["answer"], body["fields"]["kind"]["baseline"]["matched"]))
        self.assertEqual((4, 3), (body["fields"]["quality_within_one"]["baseline"]["answer"], body["fields"]["quality_within_one"]["baseline"]["matched"]))
        self.assertEqual((5, 2), (body["fields"]["quality_exact"]["baseline"]["answer"], body["fields"]["quality_exact"]["baseline"]["matched"]))

    def test_needed_is_the_floor_or_the_margin_whichever_is_higher(self):
        self.assertEqual(29, agreement.needed(30, Fraction(27, 30)))  # 95 % of 30 = 28.5
        self.assertEqual(27, agreement.needed(30, Fraction(23, 30)))  # 88.33 % of 30 = 26.5
        self.assertEqual(24, agreement.needed(30, Fraction(14, 30)))  # 73.3 % = 22 < the floor 24
        self.assertEqual(30, agreement.needed(30, Fraction(1)))
        self.assertEqual(27, agreement.needed(30, Fraction(23, 30), Fraction(1, 2), 0))
        self.assertEqual(29, agreement.needed(30, Fraction(14, 30), Fraction(9, 10), Fraction(1, 2)))  # margin 0.9 of the gap: 0.9467 * 30 = 28.4


class Figures(unittest.TestCase):
    def test_k_of_30_gives_k_over_30_for_each_field(self):
        for k in (0, 7, 23, 24, 29):
            with self.subTest(k):
                _, record = live(results_for(kind=k, ambition=k, exact=min(k, 24), within=k))
                self.assertEqual((k, 30, round(k / 30, 3)), tuple(record["fields"]["kind"][x] for x in ("matched", "total", "figure")))
                self.assertEqual(k, record["fields"]["ambition"]["matched"])
                self.assertEqual(k, record["fields"]["quality_within_one"]["matched"])

    def test_exact_and_within_one_differ_by_the_one_step_misses(self):
        _, record = live(results_for(exact=10, within=25))
        self.assertEqual((10, 25), (record["fields"]["quality_exact"]["matched"], record["fields"]["quality_within_one"]["matched"]))


class Live(unittest.TestCase):
    def test_ambition_sits_on_the_floor_the_margin_is_met_below_it(self):
        for k, want in ((22, False), (23, False), (24, True), (25, True)):
            with self.subTest(k):
                flags, record = live(results_for(ambition=k))
                self.assertEqual(want, flags["ambition"])
                if k == 22:
                    self.assertIn("under the floor", record["fields"]["ambition"]["why"])
                    self.assertNotIn("margin", record["fields"]["ambition"]["why"])

    def test_quality_exact_binds_at_24(self):
        for k, want in ((23, False), (24, True)):
            with self.subTest(k):
                self.assertEqual(want, live(results_for(exact=k, within=30))[0]["quality_exact"])

    def test_quality_within_one_needs_29_the_floor_alone_would_pass_28(self):
        for k, want in ((24, False), (27, False), (28, False), (29, True), (30, True)):
            with self.subTest(k):
                flags, record = live(results_for(within=k, exact=0))
                self.assertEqual(want, flags["quality_within_one"])
        _, record = live(results_for(within=28, exact=0))
        self.assertIn("does not beat the constant answer by the margin", record["fields"]["quality_within_one"]["why"])
        self.assertNotIn("floor", record["fields"]["quality_within_one"]["why"])

    def test_kind_needs_27_26_clears_the_floor_but_not_the_margin(self):
        for k, want in ((23, False), (24, False), (26, False), (27, True), (30, True)):
            with self.subTest(k):
                self.assertEqual(want, live(results_for(kind=k))[0]["kind"])

    def test_the_margin_is_a_parameter(self):
        self.assertTrue(live(results_for(kind=26), margin=Fraction(1, 4))[0]["kind"])
        self.assertFalse(live(results_for(kind=27), margin=Fraction(3, 4))[0]["kind"])
        with self.assertRaises(AgreementError):
            live(results_for(), margin=2)

    def test_a_perfect_judge_is_live_on_every_field(self):
        flags, record = live(results_for())
        self.assertEqual({f: True for f in FIELDS}, flags)
        self.assertEqual({"ok": 30, "partial": 0, "refused": 0, "missing": 0}, record["results"])

    def test_a_constant_baseline_judge_is_live_on_none(self):
        results = {i: ok_result("own-work", "utility", 3) for i in IDS}
        flags, record = live(results)
        self.assertEqual({f: False for f in FIELDS}, flags)
        self.assertEqual((23, 14, 17, 27), tuple(record["fields"][f]["matched"] for f in ("kind", "ambition", "quality_exact", "quality_within_one")))

    def test_a_judge_refusing_every_id_is_live_on_none_and_counted(self):
        results = {i: {"status": "refused", "fields": {}} for i in IDS}
        flags, record = live(results)
        self.assertEqual({f: False for f in FIELDS}, flags)
        self.assertEqual({"ok": 0, "partial": 0, "refused": 30, "missing": 0}, record["results"])
        self.assertEqual(0, record["fields"]["kind"]["matched"])

    def test_partial_and_missing_results_are_misses_counted_apart(self):
        results = results_for()
        results[IDS[0]] = dict(results[IDS[0]], status="partial")
        results[IDS[1]] = {"status": "refused", "fields": {}}
        del results[IDS[2]]
        flags, record = live(results)
        self.assertEqual({"ok": 27, "partial": 1, "refused": 1, "missing": 1}, record["results"])
        self.assertEqual(27, record["fields"]["kind"]["matched"])
        self.assertEqual(27, record["fields"]["ambition"]["matched"])
        self.assertEqual((None, "partial"), (record["ids"][IDS[0]]["judged"]["kind"], record["ids"][IDS[0]]["judged"]["status"]))
        self.assertTrue(flags["kind"])
        self.assertFalse(flags["quality_within_one"])  # 27 of 30 is under the 29 the margin needs

    def test_a_result_for_an_unknown_id_is_refused(self):
        with self.assertRaises(AgreementError):
            agreement.compute(LABELS, {"c999": ok_result("own-work", "toy", 1)})


class Judging(unittest.TestCase):
    def test_fake_judge_results_through_the_ensemble(self):
        def text(kind, tier, quality):
            metric = {"score": 60, "confidence": 0.5, "evidence": ["e"]}
            return json.dumps({"kind": kind, "folders": [{"path": ".", "class": "own code"}], "quality": quality, "verdict": "ok",
                               "scores": {"quality": metric, "complexity": metric, "navigability": metric},
                               "ambition": {"tier": tier, "confidence": 0.5, "evidence": ["e"]}})
        prompts = {i: RenderedPrompt("p" * 30, "a" * 64, "%064x" % n) for n, i in enumerate(IDS[:2])}
        script = [text("own-work", "utility", 3)] * 3 + [text("clone", "toy", 5)] * 3
        results = agreement.judge_all(FakeJudge(script), prompts)
        self.assertEqual(["c001", "c002"], sorted(results))
        self.assertEqual("ok", results["c001"].status)
        body = agreement.compute(LABELS, results)
        self.assertEqual({"ok": 2, "partial": 0, "refused": 0, "missing": 28}, body["results"])
        self.assertEqual("clone", body["ids"]["c002"]["judged"]["kind"])
        self.assertEqual(5, body["ids"]["c002"]["judged"]["quality"])


class Records(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.dir = os.path.join(self.tmp.name, "records")
        self.addCleanup(self.tmp.cleanup)

    def write(self, results, hash_=H):
        record = agreement.build_record("judge", hash_, {"haiku": "h", "opus": "o"}, 3, "2026-10-25", LABELS, results)
        return record, agreement.write_record(record, self.dir)

    def test_the_file_is_named_by_the_hash_sorted_and_byte_stable(self):
        record, path = self.write(results_for(kind=27))
        self.assertEqual(os.path.join(self.dir, H + ".json"), path)
        with open(path, encoding="utf-8") as handle:
            text = handle.read()
        self.assertTrue(text.endswith("}\n"))
        self.assertEqual(text, agreement.dumps(json.loads(text)))
        self.assertEqual(record, json.loads(text))
        _, again = self.write(results_for(kind=27))
        with open(again, encoding="utf-8") as handle:
            self.assertEqual(text, handle.read())
        self.assertEqual(["date", "fields", "ids", "labels", "labels_absent", "margin", "models", "prompt", "results", "runs", "version"], sorted(record))
        self.assertEqual({"floor": "4/5", "fraction_of_gap": "1/2"}, record["margin"])
        self.assertEqual({"name": "judge", "hash": H}, record["prompt"])
        self.assertEqual(IDS, sorted(record["ids"]))
        self.assertEqual(["judged", "label"], sorted(record["ids"]["c001"]))

    def test_no_record_for_the_hash_means_not_live(self):
        self.assertFalse(agreement.is_live(self.dir, H, "kind"))  # no directory
        self.write(results_for())
        self.assertTrue(agreement.is_live(self.dir, H, "kind"))
        self.assertFalse(agreement.is_live(self.dir, "d" * 64, "kind"))  # another hash: a changed prompt has no fact

    def test_live_is_answered_per_field(self):
        self.write(results_for(kind=26))
        self.assertEqual([False, True, True, True], [agreement.is_live(self.dir, H, f) for f in ("kind", "ambition", "quality_exact", "quality_within_one")])

    def test_a_hand_edited_live_flag_changes_nothing_and_a_new_margin_applies(self):
        _, path = self.write(results_for(kind=26))
        with open(path, encoding="utf-8") as handle:
            data = json.load(handle)
        data["fields"]["kind"]["live"] = True
        with open(path, "w", encoding="utf-8") as handle:
            handle.write(agreement.dumps(data))
        self.assertFalse(agreement.is_live(self.dir, H, "kind"))
        self.assertTrue(agreement.is_live(self.dir, H, "kind", margin=Fraction(1, 4)))

    def test_a_broken_record_or_bad_arguments_raise(self):
        os.makedirs(self.dir)
        with open(os.path.join(self.dir, H + ".json"), "w", encoding="utf-8") as handle:
            handle.write("{}")
        with self.assertRaises(AgreementError):
            agreement.is_live(self.dir, H, "kind")
        with self.assertRaises(AgreementError):
            agreement.is_live(self.dir, H, "nonsense")
        with self.assertRaises(AgreementError):
            agreement.is_live(self.dir, "short", "kind")
        with self.assertRaises(AgreementError):
            agreement.build_record("judge", "short", {}, 3, "2026-10-25", LABELS, {})
        with self.assertRaises(AgreementError):
            agreement.build_record("judge", H, {}, 3, "today", LABELS, {})

    def test_the_default_records_directory_holds_no_record_of_synthetic_data(self):
        for name in os.listdir(agreement.RECORDS_DIR) if os.path.isdir(agreement.RECORDS_DIR) else []:
            self.assertFalse(name.startswith(H))


class Labels(unittest.TestCase):
    def test_the_real_set_loads_and_a_broken_label_is_refused(self):
        self.assertEqual(30, len(LABELS))
        with tempfile.TemporaryDirectory() as tmp:
            with open(os.path.join(tmp, "c001.json"), "w") as handle:
                handle.write('{"id": "c001", "kind": "own-work", "ambition": "toy", "quality": 9}')
            with self.assertRaises(AgreementError):
                agreement.load_labels(tmp)


if __name__ == "__main__":
    unittest.main()
