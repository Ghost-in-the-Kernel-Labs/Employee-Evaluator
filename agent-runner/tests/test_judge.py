"""The judge interface, FakeJudge and ReplayJudge (TODO.md 2.2f): scripted and recorded answers, the recordings format."""
import hashlib
import json
import os
import unittest

from agent_runner import judge, judgment, prompt
from tests import test_summary

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
FIXTURE = os.path.join(ROOT, "tests", "fixtures", "synthetic-recordings.jsonl")
A, B = "a" * 64, "b" * 64


def record(**changes):
    data = {"answer": "x", "model": "m", "prompt_hash": A, "run": 1, "summary_hash": B, "tokens": {"input": 1, "output": 2}}
    data.update(changes)
    return data


def file_of(*records, version='{"version": 1}'):
    return version + "\n" + "".join(json.dumps(r, sort_keys=True) + "\n" for r in records)


def prompt_of(text="the prompt", prompt_hash=A, summary=B):
    return judge.RenderedPrompt(text, prompt_hash, summary)


class Hashes(unittest.TestCase):
    def test_the_summary_hash_is_sha256_of_the_utf8_bytes(self):
        self.assertEqual(hashlib.sha256("café\n".encode("utf-8")).hexdigest(), judge.summary_hash("café\n"))
        self.assertNotEqual(judge.summary_hash("a"), judge.summary_hash("a\n"))

    def test_a_rendered_prompt_carries_the_render_and_both_hashes(self):
        summary = test_summary.summary_of("summary-notes")
        rendered = judge.rendered(summary)
        self.assertEqual(prompt.render(summary), rendered.text)
        self.assertEqual(prompt.prompt_hash(), rendered.prompt_hash)
        self.assertEqual(judge.summary_hash(summary), rendered.summary_hash)


class Fake(unittest.TestCase):
    def test_scripted_answers_come_in_order_with_the_calls_recorded(self):
        fake = judge.FakeJudge(["one", judge.Answer("two", 7, 8)])
        first = fake.judge(prompt_of("p1"), "m", 1)
        second = fake.judge(prompt_of("p2"), "n", 2)
        self.assertEqual("one", first.text)
        self.assertEqual(judge.Answer("two", 7, 8), second)
        self.assertEqual([("p1", "m", 1), ("p2", "n", 2)], fake.calls)
        self.assertGreaterEqual(first.input_tokens, 1)

    def test_a_spent_script_raises(self):
        fake = judge.FakeJudge([])
        with self.assertRaises(judge.ScriptSpent):
            fake.judge(prompt_of(), "m", 1)

    def test_an_unparsable_answer_is_refused_by_the_parser(self):
        answer = judge.FakeJudge([judge.FakeJudge.unparsable()]).judge(prompt_of(), "m", 1)
        with self.assertRaises(judgment.JudgmentError):
            judgment.parse(answer.text)

    def test_it_is_a_judge(self):
        self.assertIsInstance(judge.FakeJudge([]), judge.Judge)
        with self.assertRaises(NotImplementedError):
            judge.Judge().judge(prompt_of(), "m", 1)

    def test_an_answer_holds_integer_tokens_only(self):
        for bad in (-1, 1.5, True, "3", None):
            with self.subTest(bad):
                with self.assertRaises(ValueError):
                    judge.Answer("x", bad, 1)
        with self.assertRaises(TypeError):
            judge.Answer(b"x", 1, 1)


class Replay(unittest.TestCase):
    def setUp(self):
        self.replay = judge.judge_from_recordings(FIXTURE)

    def test_the_recorded_answer_for_each_key(self):
        for run, text in ((1, '{"synthetic": "one"}'), (2, '{"synthetic": "two"}'), (3, "not json at all")):
            with self.subTest(run):
                answer = self.replay.judge(prompt_of(prompt_hash=A, summary=B), "model-small", run)
                self.assertEqual(text, answer.text)
                self.assertEqual((1200 + run, 80 + run), (answer.input_tokens, answer.output_tokens))
        self.assertEqual('{"synthetic": "large"}', self.replay.judge(prompt_of(), "model-large", 1).text)

    def test_each_part_of_the_key_matters(self):
        for name, args in (("model", (prompt_of(), "model-other", 1)), ("run", (prompt_of(), "model-small", 4)),
                           ("prompt hash", (prompt_of(prompt_hash="c" * 64), "model-small", 1)),
                           ("summary hash", (prompt_of(summary="c" * 64), "model-small", 1)),
                           ("large run 2", (prompt_of(), "model-large", 2))):
            with self.subTest(name):
                with self.assertRaises(judge.MissingRecording) as caught:
                    self.replay.judge(*args)
                self.assertIn("no recording for", str(caught.exception))

    def test_the_prompt_text_is_not_part_of_the_key(self):
        self.assertEqual("one", self.replay.judge(prompt_of("other text"), "model-small", 1).text.split('"')[3])

    def test_it_never_falls_through_to_a_live_judge(self):
        def holds_judge(value, seen):
            if id(value) in seen:
                return False
            seen.add(id(value))
            if isinstance(value, judge.Judge):
                return True
            if isinstance(value, dict):
                return any(holds_judge(v, seen) or holds_judge(k, seen) for k, v in value.items())
            if isinstance(value, (list, tuple, set, frozenset)):
                return any(holds_judge(v, seen) for v in value)
            return any(holds_judge(v, seen) for v in getattr(value, "__dict__", {}).values())
        replay = judge.ReplayJudge({})
        self.assertFalse(holds_judge(list(vars(replay).values()), set()))
        self.assertFalse(holds_judge(list(vars(self.replay).values()), set()))
        self.assertTrue(holds_judge({"x": [judge.FakeJudge([])]}, set()))  # the check can fail
        self.assertEqual(["_recordings"], sorted(vars(replay)))
        with self.assertRaises(judge.MissingRecording):
            replay.judge(prompt_of(), "m", 1)

    def test_a_replay_serves_three_runs_of_a_model(self):
        answers = [self.replay.judge(prompt_of(), "model-small", n).text for n in (1, 2, 3)]
        self.assertEqual(3, len(set(answers)))

    def test_a_missing_file_is_a_recordings_error(self):
        with self.assertRaises(judge.RecordingsError):
            judge.judge_from_recordings(os.path.join(ROOT, "tests", "fixtures", "no-such-file.jsonl"))


class Format(unittest.TestCase):
    def test_the_hand_written_file_is_canonical(self):
        with open(FIXTURE, "rb") as handle:
            text = handle.read().decode("utf-8")
        recordings = judge.parse_recordings(text)
        self.assertEqual(4, len(recordings))
        self.assertEqual(text, judge.dumps(recordings))
        self.assertTrue(text.startswith('{"version": 1}\n') and text.endswith("\n") and "\r" not in text)

    def test_a_whole_file_parses(self):
        self.assertEqual({(A, B, "m", 1): judge.Answer("x", 1, 2)}, judge.parse_recordings(file_of(record())))

    def test_dumps_is_stable_whatever_the_order(self):
        one = {(A, B, "m", 2): judge.Answer("y", 0, 0), (A, B, "m", 1): judge.Answer("xé\n", 3, 4)}
        two = dict(reversed(list(one.items())))
        self.assertEqual(judge.dumps(one), judge.dumps(two))
        self.assertEqual(one, judge.parse_recordings(judge.dumps(one)))

    def refused(self, text, fragment):
        with self.assertRaises(judge.RecordingsError) as caught:
            judge.parse_recordings(text)
        self.assertIn(fragment, str(caught.exception))

    def test_the_header_is_refused_when_wrong(self):
        self.refused(file_of(record(), version='{"version": 2}'), "version must be 1")
        self.refused(file_of(record(), version='{"version": true}'), "version must be 1")
        self.refused(file_of(record(), version='{"version": 1, "x": 1}'), "line 1")
        self.refused(file_of(record(), version="not json"), "line 1")
        self.refused(json.dumps(record()) + "\n", "line 1")
        self.refused("", "line 1")

    def test_a_missing_or_extra_field_is_refused(self):
        for name in ("answer", "model", "prompt_hash", "run", "summary_hash", "tokens"):
            data = record()
            del data[name]
            with self.subTest(name):
                self.refused(file_of(data), "line 2: fields must be exactly")
        self.refused(file_of(record(extra=1)), "line 2: fields must be exactly")

    def test_tokens_must_be_non_negative_integers(self):
        for tokens in ({"input": 1.5, "output": 2}, {"input": 1, "output": "2"}, {"input": -1, "output": 2},
                       {"input": True, "output": 2}, {"input": 1}, {"input": 1, "output": 2, "more": 3}, [1, 2], 5):
            with self.subTest(tokens):
                self.refused(file_of(record(tokens=tokens)), "line 2: tokens must be")

    def test_a_key_that_does_not_match_the_format_is_refused(self):
        for changes, fragment in (({"prompt_hash": "A" * 64}, "prompt_hash must be 64"), ({"prompt_hash": "a" * 63}, "prompt_hash must be 64"),
                                  ({"summary_hash": "g" * 64}, "summary_hash must be 64"), ({"summary_hash": 7}, "summary_hash must be 64"),
                                  ({"model": ""}, "model must be"), ({"model": " m"}, "model must be"), ({"model": 3}, "model must be"),
                                  ({"run": 0}, "run must be"), ({"run": "1"}, "run must be"), ({"run": 1.0}, "run must be"),
                                  ({"run": True}, "run must be"), ({"answer": None}, "answer must be a string")):
            with self.subTest(changes):
                self.refused(file_of(record(**changes)), fragment)

    def test_a_key_twice_is_refused_even_with_another_answer(self):
        self.refused(file_of(record(), record(answer="other")), "line 3: the key")

    def test_the_same_run_of_another_model_is_not_a_duplicate(self):
        self.assertEqual(2, len(judge.parse_recordings(file_of(record(), record(model="n")))))

    def test_a_name_twice_in_one_line_is_refused(self):
        line = json.dumps(record()).replace('"run": 1', '"run": 1, "run": 2')
        self.refused('{"version": 1}\n' + line + "\n", "twice in one object")

    def test_a_blank_or_broken_line_is_refused_and_every_broken_line_is_named(self):
        with self.assertRaises(judge.RecordingsError) as caught:
            judge.parse_recordings('{"version": 1}\n\n[1]\n{bad\n')
        message = str(caught.exception)
        for number in (2, 3, 4):
            self.assertIn("line %d:" % number, message)


if __name__ == "__main__":
    unittest.main()
