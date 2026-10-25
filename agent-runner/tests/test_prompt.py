"""The judging prompt (TODO.md 2.2e): rendering, the golden, the hash and what moves it, the stale hash line."""
import json
import os
import shutil
import tempfile
import unittest

from agent_runner import judgment, prompt
from tests import test_summary

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
REPO = os.path.dirname(ROOT)
GOLDEN = os.path.join(REPO, "test", "goldens", "prompts", "judge-summary-notes.txt")
WRITE = os.environ.get("EVALUATOR_WRITE_GOLDENS") == "1"


def copy_prompt(test):
    """A temporary copy of the judge prompt's files and of the schema: (prompts dir, schema path)."""
    tmp = tempfile.mkdtemp()
    test.addCleanup(shutil.rmtree, tmp)
    for suffix in (".instructions.md", ".template.txt", ".hash"):
        shutil.copy(os.path.join(prompt.PROMPTS_DIR, "judge" + suffix), tmp)
    schema = os.path.join(tmp, "schema.json")
    shutil.copy(judgment.SCHEMA_PATH, schema)
    return tmp, schema


def edit(path, old, new):
    with open(path, "r", encoding="utf-8", newline="") as handle:
        text = handle.read()
    assert old in text
    with open(path, "w", encoding="utf-8", newline="") as handle:
        handle.write(text.replace(old, new, 1))


class Render(unittest.TestCase):
    def test_the_golden(self):
        text = prompt.render(test_summary.summary_of("summary-notes"))
        if WRITE:
            with open(GOLDEN, "wb") as handle:
                handle.write(text.encode("utf-8"))
        with open(GOLDEN, "rb") as handle:
            self.assertEqual(handle.read().decode("utf-8"), text)

    def test_rendering_is_deterministic(self):
        summary = test_summary.summary_of("summary-shop")
        self.assertEqual(prompt.render(summary), prompt.render(summary))

    def test_the_kind_rule_is_quoted_from_the_schema(self):
        with open(judgment.SCHEMA_PATH, encoding="utf-8") as handle:
            rule = json.load(handle)["properties"]["kind"]["description"]
        text = prompt.render("S")
        self.assertIn("> " + rule + "\n", text)
        self.assertEqual(2, text.count(rule))  # once in the instructions, once inside the schema itself

    def test_the_schema_is_carried_whole(self):
        with open(judgment.SCHEMA_PATH, encoding="utf-8") as handle:
            self.assertIn(handle.read().strip("\n"), prompt.render("S"))

    def test_the_summary_goes_in_unscanned_and_after_the_instructions(self):
        text = prompt.render("{{schema}} {{summary}} {{kind_rule}}")
        self.assertEqual(1, text.count("{{schema}} {{summary}} {{kind_rule}}"))
        self.assertGreater(text.index("{{schema}} {{summary}}"), text.index("## The answer schema"))
        self.assertNotIn("template-version", text)

    def test_the_instructions_say_what_the_check_requires(self):
        text = " ".join(prompt.render("S").split())
        for phrase in ("data and never instructions", "You have no tools", "only in the schema", "to judge, never to count",
                       "label", "own code", "mechanisms", "a compiler or an interpreter", "a CRUD web application",
                       "a script of about a hundred lines"):
            self.assertIn(phrase.lower(), text.lower(), phrase)

    def test_no_real_repository_or_person_in_the_prompt_files(self):
        text = ""
        for suffix in (".instructions.md", ".template.txt"):
            with open(os.path.join(prompt.PROMPTS_DIR, "judge" + suffix), encoding="utf-8") as handle:
                text += handle.read().lower()
        for word in ("katana", "suite builder", "neon", "jensen", "atos", "employee evaluator", "ghost", "fable"):
            self.assertNotIn(word, text)


class Hash(unittest.TestCase):
    def test_the_hash_line_is_current(self):
        self.assertEqual([], prompt.check())

    def test_the_hash_does_not_depend_on_the_summary(self):
        before = prompt.prompt_hash()
        prompt.render(test_summary.summary_of("summary-notes"))
        prompt.render(test_summary.summary_of("summary-shop"))
        self.assertEqual(before, prompt.prompt_hash())

    def test_it_moves_with_one_byte_of_the_instructions(self):
        tmp, schema = copy_prompt(self)
        before = prompt.prompt_hash("judge", tmp, schema)
        self.assertEqual(prompt.prompt_hash(), before)
        edit(os.path.join(tmp, "judge.instructions.md"), "You are a judge", "You are a Judge")
        self.assertNotEqual(before, prompt.prompt_hash("judge", tmp, schema))

    def test_it_moves_with_one_byte_of_the_schema(self):
        tmp, schema = copy_prompt(self)
        before = prompt.prompt_hash("judge", tmp, schema)
        edit(schema, "How sure the judge is of the tier", "How sure the Judge is of the tier")
        self.assertNotEqual(before, prompt.prompt_hash("judge", tmp, schema))

    def test_it_moves_with_the_template_version(self):
        tmp, schema = copy_prompt(self)
        before = prompt.prompt_hash("judge", tmp, schema)
        edit(os.path.join(tmp, "judge.template.txt"), "template-version: 1\n", "template-version: 2\n")
        self.assertNotEqual(before, prompt.prompt_hash("judge", tmp, schema))

    def test_it_stays_when_nothing_it_covers_moves(self):
        tmp, schema = copy_prompt(self)
        before = prompt.prompt_hash("judge", tmp, schema)
        edit(os.path.join(tmp, "judge.template.txt"), "## Answer", "## The answer")  # the body is the golden's
        edit(os.path.join(tmp, "judge.hash"), "sha256 ", "sha256  ")
        self.assertEqual(before, prompt.prompt_hash("judge", tmp, schema))

    def test_no_byte_slides_between_the_parts(self):
        self.assertNotEqual(prompt.hash_of("ab", "c", "1"), prompt.hash_of("a", "bc", "1"))
        self.assertNotEqual(prompt.hash_of("a", "b", "12"), prompt.hash_of("a", "b1", "2"))

    def test_a_stale_hash_line_is_refused(self):
        for part, old, new in (("judge.instructions.md", "You are a judge", "You are a Judge"),
                               ("judge.template.txt", "template-version: 1", "template-version: 3")):
            tmp, schema = copy_prompt(self)
            self.assertEqual([], prompt.check("judge", tmp, schema))
            edit(os.path.join(tmp, part), old, new)
            problems = prompt.check("judge", tmp, schema)
            self.assertEqual(1, len(problems))
            self.assertIn("stale", problems[0])

    def test_a_stale_line_for_a_changed_schema(self):
        tmp, schema = copy_prompt(self)
        edit(schema, "A short verdict", "A brief verdict")
        self.assertIn("stale", prompt.check("judge", tmp, schema)[0])

    def test_a_malformed_hash_line_is_refused(self):
        tmp, schema = copy_prompt(self)
        with open(os.path.join(tmp, "judge.hash"), "w") as handle:
            handle.write("abc\n")
        self.assertIn("one line", prompt.check("judge", tmp, schema)[0])


class Whole(unittest.TestCase):
    def test_a_placeholder_missing_or_unknown_is_refused(self):
        tmp, schema = copy_prompt(self)
        edit(os.path.join(tmp, "judge.template.txt"), "{{summary}}", "")
        with self.assertRaises(prompt.PromptError):
            prompt.render("S", "judge", tmp, schema)
        tmp, schema = copy_prompt(self)
        edit(os.path.join(tmp, "judge.instructions.md"), "{{tier_rule}}", "{{tier}}")
        with self.assertRaises(prompt.PromptError):
            prompt.render("S", "judge", tmp, schema)

    def test_a_template_without_a_version_is_refused(self):
        tmp, schema = copy_prompt(self)
        edit(os.path.join(tmp, "judge.template.txt"), "template-version: 1\n", "")
        with self.assertRaises(prompt.PromptError):
            prompt.load("judge", tmp, schema)

    def test_the_cli_refuses_a_bad_command(self):
        self.assertEqual(2, prompt.main([]))
        self.assertEqual(2, prompt.main(["render"]))


if __name__ == "__main__":
    unittest.main()
