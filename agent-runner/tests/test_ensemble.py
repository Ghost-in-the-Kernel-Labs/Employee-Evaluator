"""Three runs, the mean, the vote and Opus on a split (TODO.md 2.2h), with FakeJudge and ReplayJudge; synthetic answers only (E-014)."""
import json
import unittest

from agent_runner import ensemble, judge
from agent_runner.budget import Budget, BudgetedJudge
from agent_runner.ensemble import HAIKU, OPUS, judge_repository
from agent_runner.judge import Answer, FakeJudge, RenderedPrompt

A, B = "a" * 64, "b" * 64
PROMPT = RenderedPrompt("x" * 30, A, B)
FOLDERS = {".": "own tooling", "src": "own code"}


def ans(kind="own-work", folders=None, scores=(70, 55, 80), quality=4, tier="utility", confidence=0.6, evidence="e", drop=None):
    """One synthetic answer as text; `drop` removes a scores metric to make the answer unparsable."""
    def metric(score):
        return {"score": score, "confidence": confidence, "evidence": [evidence]}
    data = {"kind": kind, "folders": [{"path": p, "class": c} for p, c in (FOLDERS if folders is None else folders).items()],
            "scores": {"quality": metric(scores[0]), "complexity": metric(scores[1]), "navigability": metric(scores[2])},
            "ambition": {"tier": tier, "confidence": confidence, "evidence": [evidence]}, "quality": quality, "verdict": "ok"}
    if drop:
        del data["scores"][drop]
    return json.dumps(data)


def run(script, **kwargs):
    fake = FakeJudge(script)
    return judge_repository(fake, PROMPT, **kwargs), fake


def values(result, key):
    record = result.fields[key]
    return record["value"], record["method"], [(e["model"], e["run"], e["value"]) for e in record["runs"]]


class Scores(unittest.TestCase):
    def test_the_mean_when_the_spread_is_15_and_opus_when_it_is_16(self):
        cases = [("15 apart", (50, 65, 60), False, 58.333), ("16 apart", (50, 66, 60), True, 90), ("14 apart", (50, 64, 60), False, 58),
                 ("same", (60, 60, 60), False, 60), ("17 apart the other way", (80, 63, 70), True, 90)]
        for name, scores, opus, value in cases:
            with self.subTest(name):
                script = [ans(scores=(s, 55, 80)) for s in scores] + ([ans(scores=(90, 55, 80))] if opus else [])
                result, fake = run(script)
                self.assertEqual(4 if opus else 3, len(fake.calls))
                got, method, runs = values(result, "scores.quality.score")
                self.assertEqual((value, "opus" if opus else "mean"), (got, method))
                self.assertEqual(ensemble.OK, result.status)
                self.assertEqual(3 + opus, len(runs))
                if opus:
                    self.assertEqual((OPUS, 1, 90), runs[-1])
                    self.assertIn("spread 16 over 15" if name == "16 apart" else "spread 17 over 15", result.fields["scores.quality.score"]["why"])
                else:
                    self.assertIsNone(result.fields["scores.quality.score"]["why"])

    def test_only_the_split_metric_goes_to_opus_and_its_confidence_and_evidence_come_with_it(self):
        script = [ans(scores=(50, 55, 80), confidence=0.5, evidence="one"), ans(scores=(70, 55, 80), confidence=0.7, evidence="two"),
                  ans(scores=(60, 55, 80), confidence=0.9, evidence="one"), ans(scores=(95, 1, 1), confidence=0.2, evidence="deep")]
        result, fake = run(script)
        self.assertEqual([HAIKU, HAIKU, HAIKU, OPUS], [c[1] for c in fake.calls])
        self.assertEqual((95, "opus"), values(result, "scores.quality.score")[:2])
        self.assertEqual((0.2, "opus"), values(result, "scores.quality.confidence")[:2])
        self.assertEqual((["deep"], "opus"), values(result, "scores.quality.evidence")[:2])
        self.assertEqual((55, "mean"), values(result, "scores.complexity.score")[:2])
        self.assertEqual((0.7, "mean"), values(result, "scores.complexity.confidence")[:2])
        self.assertEqual((["one", "two"], "union"), values(result, "scores.complexity.evidence")[:2])

    def test_the_mean_keeps_every_run_and_the_model(self):
        result, _ = run([ans(scores=(60, 55, 80)), ans(scores=(70, 55, 80)), ans(scores=(65, 55, 80))])
        record = result.fields["scores.quality.score"]
        self.assertEqual((65, "mean", [HAIKU]), (record["value"], record["method"], record["models"]))
        self.assertEqual([(HAIKU, 1, 60), (HAIKU, 2, 70), (HAIKU, 3, 65)], values(result, "scores.quality.score")[2])

    def test_each_metric_is_judged_apart(self):
        script = [ans(scores=(60, 10, 80)), ans(scores=(60, 40, 80)), ans(scores=(60, 20, 80)), ans(scores=(1, 33, 2))]
        result, _ = run(script)
        self.assertEqual("mean", result.fields["scores.quality.score"]["method"])
        self.assertEqual("opus", result.fields["scores.complexity.score"]["method"])
        self.assertEqual(33, result.fields["scores.complexity.score"]["value"])
        self.assertEqual("mean", result.fields["scores.navigability.score"]["method"])


class Quality(unittest.TestCase):
    def test_the_one_to_five_integer_is_the_median_and_opus_when_more_than_one_apart(self):
        cases = [((3, 3, 3), None, 3, "median"), ((3, 4, 4), None, 4, "median"), ((2, 3, 3), None, 3, "median"), ((3, 4, 5), 1, 1, "opus"),
                 ((1, 1, 3), 5, 5, "opus"), ((4, 4, 2), 2, 2, "opus")]
        for runs, opus, value, method in cases:
            with self.subTest(runs):
                result, fake = run([ans(quality=q) for q in runs] + ([ans(quality=opus)] if opus else []))
                self.assertEqual((value, method), values(result, "quality")[:2])
                self.assertEqual(3 + bool(opus), len(fake.calls))

    def test_two_usable_runs_one_step_apart_take_the_lower(self):
        result, _ = run([ans(quality=4), ans(quality=3), FakeJudge.unparsable(), FakeJudge.unparsable()])
        self.assertEqual((3, "median"), values(result, "quality")[:2])


class Categories(unittest.TestCase):
    def test_two_of_three_is_a_majority_and_opus_is_not_called(self):
        result, fake = run([ans(kind="mixed"), ans(kind="own-work"), ans(kind="mixed")])
        self.assertEqual(3, len(fake.calls))
        self.assertEqual(("mixed", "majority"), values(result, "kind")[:2])
        self.assertEqual(["mixed", "own-work", "mixed"], [v for _, _, v in values(result, "kind")[2]])

    def test_a_three_way_tie_goes_to_opus_and_the_opus_answer_stands(self):
        result, fake = run([ans(kind="mixed"), ans(kind="clone"), ans(kind="built"), ans(kind="own-work")])
        self.assertEqual([HAIKU] * 3 + [OPUS], [c[1] for c in fake.calls])
        kind = result.fields["kind"]
        self.assertEqual(("own-work", "opus"), (kind["value"], kind["method"]))
        self.assertEqual("no majority among 3 votes (mixed, clone, built)", kind["why"])
        self.assertEqual([HAIKU, OPUS], kind["models"])
        self.assertEqual(4, len(kind["runs"]))

    def test_opus_may_name_a_value_no_haiku_run_gave(self):
        result, _ = run([ans(tier="toy"), ans(tier="utility"), ans(tier="product"), ans(tier="research", confidence=0.9, evidence="o")])
        self.assertEqual(("research", "opus"), values(result, "ambition.tier")[:2])
        self.assertEqual((0.9, "opus"), values(result, "ambition.confidence")[:2])
        self.assertEqual((["o"], "opus"), values(result, "ambition.evidence")[:2])

    def test_a_tier_majority_keeps_the_mean_confidence(self):
        result, fake = run([ans(tier="toy", confidence=0.2), ans(tier="toy", confidence=0.4), ans(tier="product", confidence=0.6)])
        self.assertEqual(3, len(fake.calls))
        self.assertEqual(("toy", "majority"), values(result, "ambition.tier")[:2])
        self.assertEqual((0.4, "mean"), values(result, "ambition.confidence")[:2])

    def test_one_opus_call_serves_every_split_at_once(self):
        script = [ans(kind="mixed", scores=(10, 55, 80)), ans(kind="clone", scores=(90, 55, 80)), ans(kind="built", scores=(50, 55, 80)),
                  ans(kind="generated", scores=(44, 55, 80))]
        result, fake = run(script)
        self.assertEqual(4, len(fake.calls))
        self.assertEqual(("generated", "opus"), values(result, "kind")[:2])
        self.assertEqual((44, "opus"), values(result, "scores.quality.score")[:2])

    def test_each_folder_is_voted_by_its_path(self):
        a = {".": "own tooling", "src": "own code", "docs": "docs"}
        b = {".": "own tooling", "src": "own code", "docs": "reference"}
        c = {".": "own tooling", "src": "vendored", "docs": "docs"}
        result, fake = run([ans(folders=a), ans(folders=b), ans(folders=c)])
        self.assertEqual(3, len(fake.calls))
        self.assertEqual(sorted(a), sorted(result.folders))
        self.assertEqual(("own code", "majority"), (result.folders["src"]["value"], result.folders["src"]["method"]))
        self.assertEqual("docs", result.folders["docs"]["value"])
        self.assertEqual("own tooling", result.folders["."]["value"])

    def test_a_folder_tie_goes_to_opus_and_the_others_stand(self):
        tie = [{"src": "own code"}, {"src": "vendored"}, {"src": "generated"}]
        result, fake = run([ans(folders=dict(f, **{".": "docs"})) for f in tie] + [ans(folders={"src": "own tests", ".": "own tooling"})])
        self.assertEqual(4, len(fake.calls))
        self.assertEqual(("own tests", "opus"), (result.folders["src"]["value"], result.folders["src"]["method"]))
        self.assertEqual(("docs", "majority"), (result.folders["."]["value"], result.folders["."]["method"]))

    def test_a_path_in_fewer_than_half_of_the_usable_runs_is_dropped_and_kept(self):
        extra = dict(FOLDERS, scratch="reference")
        result, fake = run([ans(folders=extra), ans(), ans()])
        self.assertEqual(3, len(fake.calls))
        self.assertEqual(["." , "src"], sorted(result.folders))
        self.assertEqual({"scratch": [{"model": HAIKU, "run": 1, "value": "reference"}]}, result.dropped_folders)

    def test_a_path_in_two_of_three_runs_stands_with_those_two_as_voters(self):
        result, _ = run([ans(folders=dict(FOLDERS, lib="vendored")), ans(folders=dict(FOLDERS, lib="vendored")), ans()])
        self.assertEqual(("vendored", "majority"), (result.folders["lib"]["value"], result.folders["lib"]["method"]))
        self.assertEqual([1, 2], [e["run"] for e in result.folders["lib"]["runs"]])

    def test_two_listers_that_differ_have_no_majority(self):
        result, _ = run([ans(folders=dict(FOLDERS, lib="vendored")), ans(folders=dict(FOLDERS, lib="own code")), ans(folders={}), ans(folders=dict(FOLDERS, lib="docs"))])
        self.assertEqual(("docs", "opus"), (result.folders["lib"]["value"], result.folders["lib"]["method"]))

    def test_an_opus_answer_that_lacks_the_folder_leaves_it_unresolved_and_partial(self):
        tie = [{"src": "own code"}, {"src": "vendored"}, {"src": "generated"}]
        result, _ = run([ans(folders=f) for f in tie] + [ans(folders={})])
        self.assertEqual((None, "unresolved"), (result.folders["src"]["value"], result.folders["src"]["method"]))
        self.assertEqual(ensemble.PARTIAL, result.status)
        self.assertIn("Opus did not list the folder src", result.notes)


class Retry(unittest.TestCase):
    def test_one_unparsable_run_is_retried_once_and_the_retry_is_used(self):
        result, fake = run([ans(scores=(60, 55, 80)), FakeJudge.unparsable(), ans(scores=(70, 55, 80)), ans(scores=(65, 55, 80))])
        self.assertEqual([(HAIKU, 1), (HAIKU, 2), (HAIKU, 5), (HAIKU, 3)], [(c[1], c[2]) for c in fake.calls])
        self.assertEqual(ensemble.OK, result.status)
        self.assertEqual([(HAIKU, 1, 60), (HAIKU, 5, 70), (HAIKU, 3, 65)], values(result, "scores.quality.score")[2])
        self.assertEqual([False, True], [c["usable"] for c in result.calls if c["slot"] == 2])

    def test_a_run_unparsable_twice_is_missing_and_the_other_two_are_used(self):
        result, fake = run([ans(scores=(60, 55, 80)), FakeJudge.unparsable(), FakeJudge.unparsable(), ans(scores=(70, 55, 80))])
        self.assertEqual(4, len(fake.calls))
        self.assertEqual(ensemble.OK, result.status)
        self.assertEqual((65, "mean", [(HAIKU, 1, 60), (HAIKU, 3, 70)]), values(result, "scores.quality.score"))

    def test_two_runs_that_split_still_call_opus(self):
        result, fake = run([ans(scores=(40, 55, 80)), FakeJudge.unparsable(), FakeJudge.unparsable(), ans(scores=(60, 55, 80)), ans(scores=(50, 55, 80))])
        self.assertEqual(5, len(fake.calls))
        self.assertEqual((50, "opus"), values(result, "scores.quality.score")[:2])

    def test_a_run_with_a_metric_missing_is_unparsable_and_counts_as_missing(self):
        result, fake = run([ans(), ans(drop="complexity"), ans(drop="complexity"), ans()])
        self.assertEqual(4, len(fake.calls))
        self.assertEqual(2, len(result.fields["scores.complexity.score"]["runs"]))
        self.assertIn("complexity", [c for c in result.calls if not c["usable"]][0]["error"])

    def test_fewer_than_two_usable_runs_is_a_refusal_not_a_score(self):
        for name, script in (("two missing", [ans(), FakeJudge.unparsable(), FakeJudge.unparsable(), FakeJudge.unparsable(), FakeJudge.unparsable()]),
                             ("all missing", [FakeJudge.unparsable()] * 6)):
            with self.subTest(name):
                result, fake = run(script)
                self.assertEqual(ensemble.REFUSED, result.status)
                self.assertEqual({}, result.fields)
                self.assertEqual({}, result.folders)
                self.assertIn("usable runs of 3, at least 2 needed", result.reason)
                self.assertEqual(len(script), len(fake.calls))

    def test_opus_unparsable_is_retried_once_as_run_two(self):
        result, fake = run([ans(kind="mixed"), ans(kind="clone"), ans(kind="built"), FakeJudge.unparsable(), ans(kind="own-work")])
        self.assertEqual([(OPUS, 1), (OPUS, 2)], [(c[1], c[2]) for c in fake.calls[3:]])
        self.assertEqual(("own-work", "opus"), values(result, "kind")[:2])
        self.assertEqual((OPUS, 2, "own-work"), values(result, "kind")[2][-1])

    def test_opus_unparsable_twice_leaves_the_split_unresolved_and_partial(self):
        result, _ = run([ans(kind="mixed"), ans(kind="clone"), ans(kind="built"), FakeJudge.unparsable(), FakeJudge.unparsable()])
        self.assertEqual((None, "unresolved"), values(result, "kind")[:2])
        self.assertEqual(ensemble.PARTIAL, result.status)
        self.assertEqual("mean", result.fields["scores.quality.score"]["method"])
        self.assertEqual(3, len(result.fields["kind"]["runs"]))


class Budgeted(unittest.TestCase):
    def budgeted(self, script, **caps):
        fake = FakeJudge(script)
        return BudgetedJudge(fake, Budget(**caps), 100), fake

    def answers(self, *texts):
        return [Answer(t, 10, 20) for t in texts]  # each charged 30; every call reserves 10 + 100

    def test_a_refusal_midway_returns_what_was_judged_flagged_partial(self):
        wrapped, fake = self.budgeted(self.answers(ans(scores=(60, 55, 80)), ans(scores=(70, 55, 80)), ans()), per_evaluation=150)
        result = judge_repository(wrapped, PROMPT)
        self.assertEqual(2, len(fake.calls))  # the refused third call was never made
        self.assertEqual(ensemble.PARTIAL, result.status)
        self.assertIn("budget refused after 2 usable Haiku runs", result.notes[0])
        self.assertEqual((65, "mean", [(HAIKU, 1, 60), (HAIKU, 2, 70)]), values(result, "scores.quality.score"))
        self.assertEqual(1, len(wrapped.budget.refusals))
        self.assertEqual("evaluation", wrapped.budget.refusals[0]["cap"])

    def test_a_split_that_needs_opus_after_a_refusal_is_unresolved(self):
        wrapped, fake = self.budgeted(self.answers(ans(kind="mixed"), ans(kind="clone"), ans()), per_evaluation=150)
        result = judge_repository(wrapped, PROMPT)
        self.assertEqual(2, len(fake.calls))
        self.assertEqual((None, "unresolved"), values(result, "kind")[:2])
        self.assertIn("Opus not called", " ".join(result.notes))
        self.assertEqual(ensemble.PARTIAL, result.status)

    def test_a_refusal_of_the_opus_call_leaves_the_split_unresolved(self):
        wrapped, fake = self.budgeted(self.answers(ans(kind="mixed"), ans(kind="clone"), ans(kind="built"), ans(kind="own-work")), per_evaluation=190)
        result = judge_repository(wrapped, PROMPT)
        self.assertEqual(3, len(fake.calls))
        self.assertEqual((None, "unresolved"), values(result, "kind")[:2])
        self.assertEqual("budget refused the Opus call", result.notes[0][:28])
        self.assertEqual(ensemble.PARTIAL, result.status)

    def test_a_refusal_before_two_runs_is_a_refusal(self):
        wrapped, fake = self.budgeted(self.answers(ans(), ans(), ans()), per_call=100)
        result = judge_repository(wrapped, PROMPT)
        self.assertEqual([], fake.calls)
        self.assertEqual(ensemble.REFUSED, result.status)
        self.assertIn("budget refused", result.reason)

    def test_the_whole_run_within_a_cap_is_complete(self):
        wrapped, fake = self.budgeted(self.answers(ans(), ans(), ans()), per_evaluation=1000)
        result = judge_repository(wrapped, PROMPT)
        self.assertEqual((ensemble.OK, 3, 90), (result.status, len(fake.calls), wrapped.budget.spent("evaluation")))


class Shape(unittest.TestCase):
    def test_model_ids_are_parameters_with_the_defaults(self):
        self.assertEqual(("claude-haiku-5-5", "claude-opus-5-5"), (HAIKU, OPUS))
        result, fake = run([ans(kind="mixed"), ans(kind="clone"), ans(kind="built"), ans()], haiku="h-model", opus="o-model")
        self.assertEqual(["h-model"] * 3 + ["o-model"], [c[1] for c in fake.calls])
        self.assertEqual({"haiku": "h-model", "opus": "o-model"}, result.models)
        self.assertEqual(["h-model", "o-model"], result.fields["kind"]["models"])

    def test_the_result_is_plain_data_that_round_trips_as_json(self):
        result, _ = run([ans(kind="mixed"), ans(kind="clone"), ans(kind="built"), ans()])
        data = json.loads(result.dumps())
        self.assertEqual(result.to_dict(), data)
        self.assertEqual({"status", "reason", "notes", "models", "calls", "fields", "folders", "dropped_folders"}, set(data))
        self.assertEqual({"value", "method", "runs", "models", "why"}, set(data["fields"]["kind"]))

    def test_a_refused_result_is_typed_and_has_no_fields(self):
        result, _ = run([FakeJudge.unparsable()] * 6)
        self.assertEqual((ensemble.REFUSED, {}), (result.status, result.fields))
        self.assertEqual(6, len(result.calls))

    def test_the_same_code_runs_from_recordings(self):
        answers = {(HAIKU, 1): ans(kind="mixed"), (HAIKU, 2): ans(kind="clone"), (HAIKU, 3): ans(kind="built"), (OPUS, 1): ans(kind="own-work")}
        recordings = {judge.key_of(A, B, model, run): Answer(text, 5, 6) for (model, run), text in answers.items()}
        replay = judge.ReplayJudge(judge.parse_recordings(judge.dumps(recordings)))  # through the file format
        result = judge_repository(replay, PROMPT)
        expected, _ = run(list(answers.values()))
        self.assertEqual(expected.to_dict(), result.to_dict())
        self.assertEqual(("own-work", "opus"), values(result, "kind")[:2])

    def test_a_replay_without_the_opus_recording_raises(self):
        recordings = {judge.key_of(A, B, HAIKU, n): Answer(ans(kind=k), 1, 1) for n, k in ((1, "mixed"), (2, "clone"), (3, "built"))}
        with self.assertRaises(judge.MissingRecording):
            judge_repository(judge.ReplayJudge(recordings), PROMPT)


if __name__ == "__main__":
    unittest.main()
