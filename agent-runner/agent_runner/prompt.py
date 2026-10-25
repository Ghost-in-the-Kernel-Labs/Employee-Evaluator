"""The judging prompt, its rendering and its hash (TODO.md 2.2e): what the judge is told, as files, and the text sent.

A prompt is three things under `agent-runner/prompts/`, named `<name>` (today only `judge`):
  <name>.instructions.md  what the judge is told: its rules, the archetype anchors, navigability by its mechanisms;
  <name>.template.txt     the frame: a first line `template-version: N`, then the text with {{instructions}}, {{schema}}
                          and {{summary}} where those go;
  <name>.hash             one line, `sha256 <hex>`: the prompt's hash.
and the answer schema, `agent-runner/schema/judgment.schema.json`, the one file 2.2d's parser holds the answer to.

`render(summary)` is the exact text sent: the instructions (their {{kind_rule}}, {{folder_classes}} and {{tier_rule}} filled
with the schema's own descriptions, so the E-082 precedence rule is quoted from the schema and not restated), the schema's
bytes, and the summary. Substitution is one pass over the template: a value put in is never scanned again, so a summary that
says `{{schema}}` stays as written. The same inputs give the same bytes.

The hash is the SHA-256 of the instructions' bytes, the schema's bytes and the template version, each with its name and
length ahead of it so no byte can slide from one part to the next. It is not a hash of the summary or of the template's
body: the summary changes on every repository and must not move it, and the body is held by the golden (a change to it is
read in the golden's diff and carries a new `template-version`). It moves when any one byte of the instructions or the
schema moves, or the version does. 2.2f's recordings are keyed by it and 2.2i's record is kept under it.

Where each part of the instructions comes from (docs/judges/): the "kind" and "folders" sections ask first for the kind and
the classes, which trial 4's round 1 (v02, v03, v06) found Haiku does well when asked, and quote E-082's rule and E-057's
classes from the schema; "what you score" keeps the judge from scoring what is not the author's (trial 4 round 1: Haiku
called the generated suite generated and still scored it; E-057's division of work); the facts "to judge, never to count"
are E-055 and trial 2 (Haiku's Provenance error was a miscounted history); "a scale" is round 2's v22 archetype anchors (a
100-line script, a CRUD app, a compiler), described generally; "navigability" is round 2's v08 (by its mechanisms, paths
checked) with trial 3's note to name the mechanism and check it is enforced; "mistakes" is `fable-pitfalls.md` made general,
with no repository named (E-014); "evidence" is E-017. The calibration labels are not material for it (they are the test set).
"""
import hashlib
import json
import os
import re
import sys

from agent_runner import judgment

# Decision: E-003
# Decision: E-013
# Decision: E-014
# Decision: E-017
# Decision: E-055
# Decision: E-056
# Decision: E-057
# Decision: E-082
PROMPTS_DIR = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "prompts")
NAME = "judge"
HASH_PREFIX = "sha256 "
_PLACEHOLDER = re.compile(r"\{\{([a-z_]+)\}\}")
_VERSION = re.compile(r"template-version: ([0-9]+)\n")
INSTRUCTION_FIELDS = ("kind_rule", "folder_classes", "tier_rule")
TEMPLATE_FIELDS = ("instructions", "schema", "summary")


class PromptError(ValueError):
    """A prompt that is not whole: a missing file, a bad version line, a placeholder unknown or missing or used twice."""


def _paths(name, prompts_dir):
    base = os.path.join(prompts_dir, name)
    return base + ".instructions.md", base + ".template.txt", base + ".hash"


def _read(path):
    try:
        with open(path, "rb") as handle:
            return handle.read().decode("utf-8")
    except OSError as error:
        raise PromptError("cannot read %s: %s" % (path, error.strerror))


def load(name=NAME, prompts_dir=PROMPTS_DIR, schema_path=judgment.SCHEMA_PATH):
    """The prompt's parts: {instructions, template, version, schema} as text, version a string of digits."""
    instructions_path, template_path, _ = _paths(name, prompts_dir)
    template = _read(template_path)
    match = _VERSION.match(template)
    if not match:
        raise PromptError("%s: the first line must be `template-version: N`" % template_path)
    return {"instructions": _read(instructions_path), "template": template[match.end():], "version": match.group(1),
            "schema": _read(schema_path)}


def hash_of(instructions, schema, version):
    """The hash of the three parts (text), as a hex digest: each part's name, length and bytes in turn."""
    digest = hashlib.sha256()
    for part, text in (("instructions", instructions), ("schema", schema), ("template-version", version)):
        data = text.encode("utf-8")
        digest.update(b"%s %d\n" % (part.encode("ascii"), len(data)))
        digest.update(data)
        digest.update(b"\n")
    return digest.hexdigest()


def prompt_hash(name=NAME, prompts_dir=PROMPTS_DIR, schema_path=judgment.SCHEMA_PATH):
    """The hex hash of the prompt `name` as its files stand now."""
    parts = load(name, prompts_dir, schema_path)
    return hash_of(parts["instructions"], parts["schema"], parts["version"])


def stored_hash(name=NAME, prompts_dir=PROMPTS_DIR):
    """The hex hash the prompt's .hash file holds; PromptError when the file is not one line `sha256 <64 hex>`."""
    path = _paths(name, prompts_dir)[2]
    text = _read(path)
    if not re.fullmatch(HASH_PREFIX + "[0-9a-f]{64}\n", text):
        raise PromptError("%s must be one line `sha256 <64 hex digits>`" % path)
    return text[len(HASH_PREFIX):-1]


def check(name=NAME, prompts_dir=PROMPTS_DIR, schema_path=judgment.SCHEMA_PATH):
    """Problems with the prompt, as messages: a stale hash line, or a prompt that does not render."""
    problems = []
    try:
        stored, now = stored_hash(name, prompts_dir), prompt_hash(name, prompts_dir, schema_path)
        if stored != now:
            problems.append("%s.hash is stale: it holds %s, the files give %s" % (name, stored, now))
        render("", name, prompts_dir, schema_path)
    except PromptError as error:
        problems.append(str(error))
    return problems


def _fill(text, values, expected, where):
    """`text` with each {{field}} replaced by its value in one pass; every expected field exactly once, no other."""
    found = _PLACEHOLDER.findall(text)
    if sorted(found) != sorted(expected):
        raise PromptError("%s must hold the placeholders %s once each, and has %s" % (where, list(expected), found))
    return _PLACEHOLDER.sub(lambda m: values[m.group(1)], text)


def render(summary, name=NAME, prompts_dir=PROMPTS_DIR, schema_path=judgment.SCHEMA_PATH):
    """The exact text sent to the judge for `summary` (the text of 2.2c's builder)."""
    parts = load(name, prompts_dir, schema_path)
    schema = json.loads(parts["schema"])
    values = {"kind_rule": schema["properties"]["kind"]["description"],
              "folder_classes": schema["properties"]["folders"]["items"]["properties"]["class"]["description"],
              "tier_rule": schema["properties"]["ambition"]["properties"]["tier"]["description"]}
    instructions = _fill(parts["instructions"], values, INSTRUCTION_FIELDS, name + ".instructions.md")
    return _fill(parts["template"], {"instructions": instructions.strip("\n"), "schema": parts["schema"].strip("\n"),
                                     "summary": summary.strip("\n")}, TEMPLATE_FIELDS, name + ".template.txt")


def main(argv):
    """`python3 -B -m agent_runner.prompt hash|check|write-hash [NAME]` or `render SUMMARY_FILE [NAME]`."""
    usage = "usage: prompt hash|check|write-hash [NAME] | render SUMMARY_FILE [NAME]\n"
    if not argv or argv[0] not in ("hash", "check", "write-hash", "render"):
        sys.stderr.write(usage)
        return 2
    command, rest = argv[0], argv[1:]
    if len(rest) > (2 if command == "render" else 1) or (command == "render" and not rest):
        sys.stderr.write(usage)
        return 2
    try:
        if command == "render":
            with open(rest[0], "r", encoding="utf-8") as handle:
                sys.stdout.write(render(handle.read(), *rest[1:]))
            return 0
        name = rest[0] if rest else NAME
        if command == "hash":
            sys.stdout.write(prompt_hash(name) + "\n")
        elif command == "write-hash":
            with open(_paths(name, PROMPTS_DIR)[2], "w", encoding="utf-8", newline="\n") as handle:
                handle.write(HASH_PREFIX + prompt_hash(name) + "\n")
        else:
            problems = check(name)
            sys.stdout.write("".join(p + "\n" for p in problems) or "ok\n")
            return 1 if problems else 0
    except (PromptError, OSError) as error:
        sys.stderr.write("%s\n" % error)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
