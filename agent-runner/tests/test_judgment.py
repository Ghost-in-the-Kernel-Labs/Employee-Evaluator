"""The judgment schema and its parser (TODO.md 2.2d): a valid answer round-trips, each refusal names its field, the schema stays in its subset."""
import copy
import json
import os
import unittest

from agent_runner import judgment
from agent_runner.judgment import JudgmentError

KINDS = ("generated", "clone", "plan only", "mixed", "own-work", "built")
CLASSES = ("own code", "own tests", "own tooling", "docs", "generated", "vendored", "reference", "fixture data")
TIERS = ("toy", "utility", "product", "research")


def metric(score=70):
    return {"score": score, "confidence": 0.6, "evidence": ["src/app.py: a synthetic quotation"]}


def valid():
    """A synthetic answer (E-014): no real repository, owner or person."""
    return {
        "kind": "own-work",
        "folders": [{"path": ".", "class": "own tooling"}, {"path": "src", "class": "own code"}, {"path": "tests", "class": "own tests"}],
        "scores": {"quality": metric(70), "complexity": metric(55), "navigability": metric(80)},
        "ambition": {"tier": "utility", "confidence": 0.7, "evidence": ["README.md: one useful job"]},
        "quality": 4,
        "verdict": "A small tool that does its one job and is tested.",
    }


def refused(case, text):
    """Assert `case` (a mutated answer) is refused with a message that contains `text`, and return the messages."""
    with case.assertRaises(JudgmentError) as caught:
        judgment.parse(json.dumps(case.answer))
    joined = " | ".join(caught.exception.messages)
    case.assertIn(text, joined)
    return caught.exception.messages


class Accepts(unittest.TestCase):
    def test_a_valid_answer_round_trips(self):
        answer = valid()
        self.assertEqual(judgment.parse(judgment.dump(answer)), answer)
        self.assertEqual(judgment.parse(json.dumps(answer)), answer)
        self.assertEqual(judgment.dump(judgment.parse(judgment.dump(answer))), judgment.dump(answer))

    def test_every_kind_is_accepted_own_work_and_clone_among_them(self):
        for kind in KINDS:
            answer = valid()
            answer["kind"] = kind
            self.assertEqual(judgment.parse(json.dumps(answer))["kind"], kind)
        self.assertEqual(sorted(judgment.load_schema()["properties"]["kind"]["enum"]), sorted(KINDS))

    def test_every_folder_class_and_tier_is_accepted(self):
        answer = valid()
        answer["folders"] = [{"path": "d%d" % i, "class": c} for i, c in enumerate(CLASSES)]
        self.assertEqual(len(judgment.parse(json.dumps(answer))["folders"]), 8)
        for tier in TIERS:
            answer = valid()
            answer["ambition"]["tier"] = tier
            judgment.parse(json.dumps(answer))

    def test_the_edges_of_every_range_are_accepted(self):
        for score in (0, 100):
            answer = valid()
            answer["scores"]["quality"]["score"] = score
            judgment.parse(json.dumps(answer))
        for quality in (1, 5):
            answer = valid()
            answer["quality"] = quality
            judgment.parse(json.dumps(answer))
        for confidence in (0, 1, 0.5):
            answer = valid()
            answer["ambition"]["confidence"] = confidence
            judgment.parse(json.dumps(answer))
        answer = valid()
        answer["folders"] = []
        answer["verdict"] = "x" * judgment.VERDICT_MAX
        judgment.parse(json.dumps(answer))

    def test_parsing_changes_nothing_it_was_given(self):
        answer = valid()
        before = copy.deepcopy(answer)
        self.assertEqual(judgment.validate(answer), [])
        self.assertEqual(answer, before)


class Refuses(unittest.TestCase):
    def setUp(self):
        self.answer = valid()

    def test_a_score_of_101(self):
        self.answer["scores"]["complexity"]["score"] = 101
        self.assertEqual(refused(self, "answer.scores.complexity.score: 101 is above the maximum 100"), ["answer.scores.complexity.score: 101 is above the maximum 100"])

    def test_a_negative_score_and_a_confidence_over_one(self):
        self.answer["scores"]["quality"]["score"] = -1
        self.answer["ambition"]["confidence"] = 1.5
        messages = refused(self, "answer.scores.quality.score: -1 is below the minimum 0")
        self.assertIn("answer.ambition.confidence: 1.5 is above the maximum 1", messages)

    def test_quality_outside_one_to_five(self):
        for bad in (0, 6):
            self.answer["quality"] = bad
            refused(self, "answer.quality: %d is" % bad)

    def test_an_unknown_kind(self):
        self.answer["kind"] = "handmade"
        refused(self, "answer.kind: 'handmade' is not one of generated, clone, plan only, mixed, own-work, built")

    def test_an_unknown_folder_class(self):
        self.answer["folders"][1]["class"] = "source"
        refused(self, "answer.folders[1].class: 'source' is not one of")

    def test_a_missing_evidence_list(self):
        del self.answer["scores"]["navigability"]["evidence"]
        refused(self, "answer.scores.navigability.evidence: missing required field")

    def test_an_empty_evidence_list(self):
        self.answer["ambition"]["evidence"] = []
        refused(self, "answer.ambition.evidence: has 0 items, at least 1 required")

    def test_an_evidence_item_that_is_not_text(self):
        self.answer["scores"]["quality"]["evidence"] = [3]
        refused(self, "answer.scores.quality.evidence[0]: expected a string")

    def test_an_extra_field_at_each_depth(self):
        self.answer["comment"] = "hello"
        refused(self, "answer.comment: unexpected field")
        self.answer = valid()
        self.answer["scores"]["quality"]["note"] = "x"
        refused(self, "answer.scores.quality.note: unexpected field")
        self.answer = valid()
        self.answer["folders"][0]["owner"] = "x"
        refused(self, "answer.folders[0].owner: unexpected field")
        self.answer = valid()
        self.answer["scores"]["maintainability"] = metric()
        refused(self, "answer.scores.maintainability: unexpected field")

    def test_free_text_where_an_enum_belongs(self):
        for path, value in ((("kind",), "this looks like a clone of something"), (("ambition", "tier"), "quite ambitious"),
                            (("folders", 0, "class"), "the source code folder")):
            self.answer = valid()
            target = self.answer
            for step in path[:-1]:
                target = target[step]
            target[path[-1]] = value
            refused(self, "is not one of")
        self.answer = valid()
        self.answer["kind"] = "x" * 500
        messages = refused(self, "answer.kind:")
        self.assertLess(len(messages[0]), 300)  # a long value is quoted cut short

    def test_wrong_types(self):
        self.answer["quality"] = True
        refused(self, "answer.quality: expected a whole number, got true")
        self.answer = valid()
        self.answer["quality"] = 4.0
        refused(self, "answer.quality: expected a whole number")
        self.answer = valid()
        self.answer["scores"]["quality"]["score"] = "70"
        refused(self, "answer.scores.quality.score: expected a whole number")
        self.answer = valid()
        self.answer["scores"]["quality"]["confidence"] = False
        refused(self, "answer.scores.quality.confidence: expected a number")
        self.answer = valid()
        self.answer["folders"] = {"src": "own code"}
        refused(self, "answer.folders: expected a list")
        self.answer = valid()
        self.answer["verdict"] = None
        refused(self, "answer.verdict: expected a string")

    def test_a_missing_top_level_field(self):
        del self.answer["verdict"]
        refused(self, "answer.verdict: missing required field")

    def test_a_verdict_over_the_cap(self):
        self.answer["verdict"] = "x" * (judgment.VERDICT_MAX + 1)
        refused(self, "answer.verdict: 401 characters, at most 400 allowed")

    def test_folder_paths_that_escape_or_repeat(self):
        for bad in ("/etc", "../up", "a/../b", ""):
            self.answer = valid()
            self.answer["folders"][1]["path"] = bad
            refused(self, "answer.folders[1].path:")
        self.answer = valid()
        self.answer["folders"][2]["path"] = "src"
        refused(self, "answer.folders[2].path: 'src' is listed twice")

    def test_every_broken_field_is_named_at_once(self):
        self.answer["kind"] = "x"
        self.answer["quality"] = 9
        self.answer["verdict"] = 3
        self.assertEqual(len(refused(self, "answer.kind")), 3)

    def test_text_that_is_not_an_answer(self):
        for text, expect in (("not json", "not valid JSON"), ("[1]", "expected an object"), ("", "not valid JSON"),
                             ('{"kind": "built", "kind": "clone"}', "a field is given twice"),
                             ('{"quality": NaN}', "NaN is not a JSON number"),
                             ('{"quality": 1e999}', "answer.quality")):
            with self.assertRaises(JudgmentError) as caught:
                judgment.parse(text)
            self.assertIn(expect, " | ".join(caught.exception.messages), text)


class TheSchemaFile(unittest.TestCase):
    def test_the_shipped_schema_is_inside_the_subset(self):
        with open(judgment.SCHEMA_PATH, encoding="utf-8") as handle:
            self.assertEqual(judgment.check_schema(json.load(handle)), [])

    def test_the_kind_description_gives_the_precedence_in_order(self):
        text = judgment.load_schema()["properties"]["kind"]["description"]
        at = [text.index(word + ":") for word in ("generated", "clone", "plan only", "mixed", "own-work", "built")]
        self.assertEqual(at, sorted(at))
        self.assertIn("FIRST", text)

    def test_leaving_the_subset_is_found(self):
        def mutated(change):
            schema = judgment.load_schema()
            change(schema)
            return " | ".join(judgment.check_schema(schema))

        self.assertIn("additionalProperties must be false", mutated(lambda s: s.update(additionalProperties=True)))
        self.assertIn("'pattern' is not in the supported subset", mutated(lambda s: s["properties"]["verdict"].update(pattern="x")))
        self.assertIn("'oneOf' is not in the supported subset", mutated(lambda s: s.update(oneOf=[])))
        self.assertIn("required must name every property", mutated(lambda s: s["required"].remove("verdict")))
        self.assertIn("schema.verdict: a description is required", mutated(lambda s: s["properties"]["verdict"].pop("description")))
        self.assertIn("type must be one of", mutated(lambda s: s["properties"]["verdict"].update(type="boolean")))
        self.assertIn("enum must be a non-empty list of distinct strings", mutated(lambda s: s["properties"]["kind"].update(enum=["a", "a"])))
        self.assertIn("minimum must be a whole number", mutated(lambda s: s["properties"]["quality"].update(minimum=1.5)))
        self.assertIn("items is required", mutated(lambda s: s["properties"]["folders"].pop("items")))
        self.assertIn("a schema node must be an object", mutated(lambda s: s["properties"].update(verdict=[])))

    def test_a_schema_file_outside_the_subset_is_not_loaded(self):
        import tempfile
        with tempfile.TemporaryDirectory() as folder:
            path = os.path.join(folder, "bad.json")
            with open(path, "w", encoding="utf-8") as handle:
                json.dump({"type": "string", "description": "d", "format": "date"}, handle)
            with self.assertRaises(JudgmentError):
                judgment.load_schema(path)


if __name__ == "__main__":
    unittest.main()
