"""The calibration driver (TODO.md 2.2m): synthetic checkouts, a synthetic brief and a FakeJudge or hand-written recordings.

Nothing real is judged and no network is reached (E-003, E-014). The brief step is injected (the built CLI is not assumed)."""
import contextlib
import io
import json
import os
import shutil
import subprocess
import tempfile
import unittest
from decimal import Decimal

from agent_runner import agreement, budget as budgets, calibrate, ensemble, judge, prompt, summary
from agent_runner.judge import Answer, FakeJudge

SECRET = "SECRET-README-MARKER"
EVIDENCE = "EVIDENCE-MARKER"
BRIEF = {"inventory": {"source": [{"path": "src/a.py", "language": "Python", "lines": 2}], "setAside": []}, "facts": {"SIZE.files": 1},
         "commitsTotal": 1, "commits": [], "readme": ["README.md"], "agentInstructions": []}
LABELS = agreement.load_labels()
IDS = sorted(LABELS)


def synthetic_brief(checkout):
    return json.dumps(BRIEF)


def answer(kind="own-work", tier="utility", quality=3):
    metric = {"score": 60, "confidence": 0.5, "evidence": [EVIDENCE]}
    return json.dumps({"kind": kind, "folders": [{"path": ".", "class": "own code"}], "quality": quality, "verdict": "ok",
                       "scores": {"quality": metric, "complexity": metric, "navigability": metric},
                       "ambition": {"tier": tier, "confidence": 0.5, "evidence": [EVIDENCE]}})


def git(*args, cwd):
    done = subprocess.run(["env", "-u", "GIT_CONFIG_COUNT", "git", "-c", "user.name=T", "-c", "user.email=t@example.invalid", *args], cwd=cwd, capture_output=True, text=True)
    if done.returncode != 0:
        raise AssertionError("git %s: %s" % (args[0], done.stderr[:300]))


class Base(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.root = self.tmp.name
        self.build = os.path.join(self.root, "repo", "build")
        self.out = os.path.join(self.build, "run")
        self.records = os.path.join(self.root, "records")
        self.corpus = os.path.join(self.root, "corpus")
        self.stdout, self.stderr = io.StringIO(), io.StringIO()

    def make_corpus(self, ids):
        os.makedirs(self.corpus, exist_ok=True)
        first = None
        for label_id in ids:
            path = os.path.join(self.corpus, label_id)
            if first is None:
                os.makedirs(os.path.join(path, "src"))
                with open(os.path.join(path, "README.md"), "w") as handle:
                    handle.write("# notes\n%s\n" % SECRET)
                with open(os.path.join(path, "src", "a.py"), "w") as handle:
                    handle.write("x = 1\ny = 2\n")
                git("init", "-q", cwd=path)
                git("add", ".", cwd=path)
                git("commit", "-q", "-m", "first", cwd=path)
                first = path
            else:
                shutil.copytree(first, path)

    def files(self, name, caps=None, prices=True):
        """A caps file and a prices file of the test's own (synthetic numbers)."""
        caps = {"version": 1, "dollars_per_trial": None, "dollars_per_evaluation": None, "dollars_per_call": None,
                "output_limit": 1, "prompt_iterations": 1, **(caps or {})}
        path = os.path.join(self.root, name + "-caps.json")
        with open(path, "w") as handle:
            json.dump(caps, handle)
        prices = os.path.join(self.root, name + "-prices.json")
        with open(prices, "w") as handle:
            json.dump({"version": 1, "date": "2000-01-01", "per_tokens": 1000000,
                       "models": {ensemble.HAIKU: {"input": 1, "output": 1}, ensemble.OPUS: {"input": 1, "output": 1}}}, handle)
        return path, prices

    def run_driver(self, ids=None, live=False, script=None, caps=None, brief=synthetic_brief, replay=None, **extra):
        caps_path, prices_path = self.files("t", caps)
        fake = FakeJudge(script if script is not None else [answer()] * 3 * 40)
        seams = dict(brief=brief, records_dir=self.records, build_root=self.build, caps_path=caps_path, prices_path=prices_path,
                     stdout=self.stdout, stderr=self.stderr)
        if replay is None:
            seams["judge_factory"] = lambda b, limit, path: budgets.BudgetedJudge(fake, b, limit)
        seams.update(extra)
        argv = ["--corpus", self.corpus, "--out", self.out, "--date", "2026-10-29"]
        if live:
            argv.append("--live")
        else:
            argv += ["--replay", replay or __file__]  # with a factory the file is only checked to exist
        if ids:
            argv += ["--ids", ",".join(ids)]
        return calibrate.main(argv, **seams), fake

    @staticmethod
    def slurp(path):
        with open(path, encoding="utf-8") as handle:
            return handle.read()

    def read(self, name):
        with open(os.path.join(self.out, name), encoding="utf-8") as handle:
            return handle.read()


class Chain(Base):
    def test_a_replayed_run_writes_the_chain_under_out_and_no_record_in_records(self):
        self.make_corpus(IDS[:2])
        status, _ = self.run_driver(IDS[:2])
        self.assertEqual(0, status, self.stderr.getvalue())
        for name in ("briefs/c001.json", "summaries/c001.txt", "summaries/c002.txt", "costs.json", "results.json", "record.json"):
            self.assertTrue(os.path.isfile(os.path.join(self.out, name)), name)
        self.assertIn(SECRET, self.read("summaries/c001.txt"))  # the summary is under build/ and may hold repository text
        results = json.loads(self.read("results.json"))
        self.assertEqual(["c001", "c002"], sorted(results))
        self.assertEqual({"status", "kind", "ambition", "quality"}, set(results["c001"]))
        self.assertFalse(os.path.exists(self.records))

    def test_a_partial_live_run_stays_out_of_records(self):
        self.make_corpus(IDS[:2])
        status, _ = self.run_driver(IDS[:2], live=True)
        self.assertEqual(0, status)
        self.assertFalse(os.path.exists(self.records))
        self.assertTrue(os.path.isfile(os.path.join(self.out, "record.json")))

    def test_a_live_run_over_all_labels_writes_the_record_under_the_prompt_hash(self):
        self.make_corpus(IDS)
        status, _ = self.run_driver(live=True)
        self.assertEqual(0, status, self.stderr.getvalue())
        path = os.path.join(self.records, prompt.prompt_hash() + ".json")
        self.assertTrue(os.path.isfile(path))
        self.assertFalse(os.path.exists(os.path.join(self.out, "record.json")))
        self.assertEqual(30, json.loads(self.slurp(path))["results"]["ok"])

    def test_a_missing_checkout_is_a_miss_reported_by_id_and_keeps_the_record_out_of_records(self):
        self.make_corpus(IDS[:-1])
        status, _ = self.run_driver(live=True)
        self.assertEqual(0, status)
        self.assertIn("%s: no checkout" % IDS[-1], self.stderr.getvalue())
        self.assertIn("missing 1", self.stdout.getvalue())
        self.assertFalse(os.path.exists(self.records))

    def test_a_failing_brief_is_a_miss_by_id_with_only_the_class_name(self):
        self.make_corpus(IDS[:2])

        def brief(checkout):
            if checkout.endswith("c002"):
                raise calibrate.CalibrateError("evaluator brief exited 3")
            return synthetic_brief(checkout)
        status, _ = self.run_driver(IDS[:2], brief=brief)
        self.assertEqual(0, status)
        self.assertIn("c002: brief or summary failed (CalibrateError)", self.stderr.getvalue())
        self.assertEqual("missing", json.loads(self.read("results.json"))["c002"]["status"])

    def test_the_replay_judge_through_main_and_a_missing_recording_is_one_missing_id(self):
        self.make_corpus(IDS[:2])
        built = summary.build(BRIEF, os.path.join(self.corpus, "c001"))
        rendered = judge.rendered(built)
        recordings = {judge.key_of(rendered.prompt_hash, rendered.summary_hash, ensemble.HAIKU, run): Answer(answer(), 100, 50)
                      for run in (1, 2, 3)}
        path = os.path.join(self.root, "recordings.jsonl")
        with open(path, "w") as handle:
            handle.write(judge.dumps(recordings))
        status, _ = self.run_driver(IDS[:2], replay=path)
        self.assertEqual(0, status, self.stderr.getvalue())
        self.assertEqual("ok", json.loads(self.read("results.json"))["c001"]["status"])

    def test_a_recording_missing_for_an_id_is_reported_and_the_run_goes_on(self):
        self.make_corpus(IDS[:2])
        with open(os.path.join(self.corpus, "c002", "README.md"), "a") as handle:
            handle.write("different\n")
        rendered = judge.rendered(summary.build(BRIEF, os.path.join(self.corpus, "c001")))
        recordings = {judge.key_of(rendered.prompt_hash, rendered.summary_hash, ensemble.HAIKU, run): Answer(answer(), 100, 50)
                      for run in (1, 2, 3)}
        path = os.path.join(self.root, "recordings.jsonl")
        with open(path, "w") as handle:
            handle.write(judge.dumps(recordings))
        status, _ = self.run_driver(IDS[:2], replay=path)
        self.assertEqual(0, status)
        self.assertIn("c002: not judged (MissingRecording)", self.stderr.getvalue())
        self.assertEqual("ok", json.loads(self.read("results.json"))["c001"]["status"])
        self.assertEqual("missing", json.loads(self.read("results.json"))["c002"]["status"])


class Budgets(Base):
    def costs(self):
        self.make_corpus(IDS[:2])
        self.run_driver(IDS[:2])
        records = json.loads(self.read("costs.json"))
        per_call = max(Decimal(r["dollars"]) for r in records)
        return per_call, records

    def reserve(self):
        caps_path, prices_path = self.files("r")
        prices = budgets.load_prices(prices_path)
        built = summary.build(BRIEF, os.path.join(self.corpus, "c001"))
        text = judge.rendered(built).text
        return prices.dollars(ensemble.HAIKU, budgets.estimate_tokens(text), 1)

    def test_the_evaluation_cap_is_per_repository_not_per_run(self):
        per_call, records = self.costs()
        cap = float(2 * per_call + self.reserve() + per_call / 2)  # three calls of one repository fit; a fourth does not
        self.assertEqual(6, len(records))
        self.setUp()
        self.make_corpus(IDS[:3])
        status, _ = self.run_driver(IDS[:3], caps={"dollars_per_evaluation": cap})
        self.assertEqual(0, status)
        statuses = {i: r["status"] for i, r in json.loads(self.read("results.json")).items()}
        self.assertEqual({"c001": "ok", "c002": "ok", "c003": "ok"}, statuses)
        self.assertIn("refusals: 0", self.stdout.getvalue())

    def test_without_begin_evaluation_the_same_cap_would_refuse_the_second_repository(self):
        per_call, _ = self.costs()
        cap = float(2 * per_call + self.reserve() + per_call / 2)
        budget = budgets.Budget(prices=budgets.load_prices(self.files("n")[1]), dollars_per_evaluation=cap)
        built = judge.rendered(summary.build(BRIEF, os.path.join(self.corpus, "c001")))
        wrapped = budgets.BudgetedJudge(FakeJudge([answer()] * 6), budget, 1)
        first = ensemble.judge_repository(wrapped, built)
        second = ensemble.judge_repository(wrapped, built)
        self.assertEqual((ensemble.OK, ensemble.REFUSED), (first.status, second.status))

    def test_a_trial_cap_refusal_ends_the_run_early_and_everything_is_still_written(self):
        per_call, _ = self.costs()
        cap = float(2 * per_call + self.reserve() + per_call / 2)
        self.setUp()
        self.make_corpus(IDS[:3])
        status, _ = self.run_driver(IDS[:3], caps={"dollars_per_trial": cap})
        self.assertEqual(0, status)
        statuses = {i: r["status"] for i, r in json.loads(self.read("results.json")).items()}
        self.assertEqual({"c001": "ok", "c002": "refused", "c003": "missing"}, statuses)
        self.assertIn("c003", self.stdout.getvalue())
        self.assertIn("trial 1", self.stdout.getvalue())
        for name in ("costs.json", "record.json"):
            self.assertTrue(os.path.isfile(os.path.join(self.out, name)))


class Refusals(Base):
    def test_a_corpus_inside_the_repository_is_refused_before_anything_is_written(self):
        self.corpus = os.path.join(calibrate.REPO_ROOT, "agent-runner", "tests")
        status, _ = self.run_driver()
        self.assertEqual(1, status)
        self.assertIn("inside this repository", self.stderr.getvalue())
        self.assertFalse(os.path.exists(self.out))

    def test_a_link_into_the_repository_is_refused_as_a_corpus_and_as_a_checkout(self):
        os.makedirs(self.corpus)
        link = os.path.join(self.root, "link")
        os.symlink(calibrate.REPO_ROOT, link)
        self.corpus = link
        self.assertEqual(1, self.run_driver()[0])
        self.corpus = os.path.join(self.root, "corpus")
        os.symlink(calibrate.REPO_ROOT, os.path.join(self.corpus, "c001"))
        status, _ = self.run_driver(["c001"])
        self.assertIn("c001: the checkout lies inside this repository", self.stderr.getvalue())
        self.assertEqual(0, status)

    def test_an_output_outside_build_is_refused(self):
        self.make_corpus(IDS[:1])
        elsewhere = os.path.join(self.root, "elsewhere")
        caps_path, prices_path = self.files("t")
        status = calibrate.main(["--corpus", self.corpus, "--out", elsewhere, "--replay", caps_path], brief=synthetic_brief,
                                caps_path=caps_path, prices_path=prices_path, stdout=self.stdout, stderr=self.stderr)
        self.assertEqual(1, status)
        self.assertIn("under this repository's build/", self.stderr.getvalue())
        self.assertFalse(os.path.exists(elsewhere))

    def test_the_real_build_folder_itself_and_a_dotdot_escape_are_refused(self):
        self.make_corpus(IDS[:1])
        for out in (calibrate.BUILD_ROOT, os.path.join(calibrate.BUILD_ROOT, "..", "escape")):
            status = calibrate.main(["--corpus", self.corpus, "--out", out, "--replay", __file__], stdout=self.stdout, stderr=self.stderr)
            self.assertEqual(1, status)
        self.assertFalse(os.path.exists(os.path.join(calibrate.REPO_ROOT, "escape")))

    def test_exactly_one_of_live_and_replay(self):
        self.make_corpus(IDS[:1])
        base = ["--corpus", self.corpus, "--out", self.out]
        for extra in ([], ["--live", "--replay", __file__]):
            self.assertEqual(1, calibrate.main(base + extra, build_root=self.build, stdout=self.stdout, stderr=self.stderr))

    def test_a_usage_error_does_not_echo_what_was_typed(self):
        with self.assertRaises(SystemExit) as caught:
            with contextlib.redirect_stderr(io.StringIO()) as shown:
                calibrate.main(["--corpus", "/secret/place", "--nonsense"])
        self.assertEqual(2, caught.exception.code)
        self.assertNotIn("secret", shown.getvalue())
        self.assertNotIn("nonsense", shown.getvalue())

    def test_an_unknown_id_is_refused(self):
        self.make_corpus(IDS[:1])
        status, _ = self.run_driver(["c999"])
        self.assertEqual(1, status)


class Output(Base):
    def test_stdout_and_stderr_hold_ids_and_numbers_only(self):
        self.make_corpus(IDS[:2])
        status, _ = self.run_driver(IDS[:2], script=[answer()] * 6)
        self.assertEqual(0, status)
        shown = self.stdout.getvalue() + self.stderr.getvalue()
        for text in (SECRET, EVIDENCE, self.root, self.corpus, "README", "src/a.py", os.path.basename(self.root)):
            self.assertNotIn(text, shown)
        for field in agreement.FIELDS:
            self.assertRegex(self.stdout.getvalue(), r"%s: matched \d+ of 30, needed \d+, baseline \d+, live (yes|no)" % field)
        for line in ("dollars: ", "refusals: 0", "overruns: 0", "results: ok 2, partial 0, refused 0, missing 28"):
            self.assertIn(line, self.stdout.getvalue())

    def test_the_results_file_holds_status_kind_ambition_and_quality_only(self):
        self.make_corpus(IDS[:1])
        self.run_driver(IDS[:1], script=[answer("clone", "toy", 2)] * 3)
        self.assertEqual({"c001": {"status": "ok", "kind": "clone", "ambition": "toy", "quality": 2}}, json.loads(self.read("results.json")))
        self.assertNotIn(EVIDENCE, self.read("results.json") + self.read("record.json"))


if __name__ == "__main__":
    unittest.main()
