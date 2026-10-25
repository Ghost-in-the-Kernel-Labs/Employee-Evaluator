"""The judgment schema and its parser (TODO.md 2.2d): the judge's answer, held to one JSON schema file, strictly.

The schema is `agent-runner/schema/judgment.schema.json`, the file the API is later given (2.2l) and the prompt quotes
(2.2e). There is no `jsonschema` library here (standard library only, E-081), so this file holds a small strict checker of
that one schema and of an answer against it. The schema is kept to the subset a structured-output enforcement accepts:

  - an object has `properties`, `required` naming every one of them, and `additionalProperties` false;
  - a string may have an `enum` of strings; an integer or number may have `minimum` and `maximum`;
  - an array has `items` (an object or a string, any depth) and may have `minItems` and `maxItems`;
  - every node has a `type` and a non-empty `description`; nothing else (no `$ref`, `oneOf`, `pattern`, `format`,
    string lengths, `default`): `check_schema` refuses any other keyword, so the file cannot drift out of the subset.

The one rule the subset cannot say is checked after the schema, in `_rules`: the verdict's length, a folder path that is
absolute or climbs out with `..`, and one folder named twice. The answer is the judge's text and is data, never
instructions (E-013): it is parsed, never run, and a message quotes a refused value cut short.

Which metrics are asked (Decision: E-057, docs/SCORE-CARD.md "Where the facts come from"): the card's `agent-runner/` row is
AMBITION.*, QUALITY.agent_review, COMPLEXITY.agent_assessment and NAVIGABILITY.task, so the answer has a 0 to 100 score with a
confidence and evidence (SC-02) for quality, complexity and navigability, and Ambition as a tier with a confidence and
evidence. Size, Maintainability's parts and Provenance are the kernel's (E-057, E-003). `quality` at the top is the 1 to 5
number the calibration labels carry. The kind's six values are E-082 (E-057's four, plus own-work and clone).
"""
import json
import math
import os

# Decision: E-013
# Decision: E-017
# Decision: E-057
# Decision: E-082
SCHEMA_PATH = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "schema", "judgment.schema.json")
VERDICT_MAX = 400
SHOWN = 60
_TYPES = ("object", "string", "integer", "number", "array")
_ALLOWED = {
    "object": {"type", "description", "properties", "required", "additionalProperties"},
    "string": {"type", "description", "enum"},
    "integer": {"type", "description", "minimum", "maximum"},
    "number": {"type", "description", "minimum", "maximum"},
    "array": {"type", "description", "items", "minItems", "maxItems"},
}


class JudgmentError(ValueError):
    """An answer or a schema that does not hold; `messages` names each field that broke the rules."""

    def __init__(self, messages):
        self.messages = list(messages)
        super().__init__("; ".join(self.messages))


def load_schema(path=SCHEMA_PATH):
    """The schema file, parsed and checked against the subset; raises JudgmentError when it leaves it."""
    with open(path, encoding="utf-8") as handle:
        schema = json.load(handle)
    problems = check_schema(schema)
    if problems:
        raise JudgmentError(problems)
    return schema


def check_schema(schema, name="schema"):
    """The ways `schema` leaves the supported subset, as messages (empty when it is inside it)."""
    problems = []
    if not isinstance(schema, dict):
        return ["%s: a schema node must be an object" % name]
    kind = schema.get("type")
    if kind not in _TYPES:
        return ["%s: type must be one of %s" % (name, ", ".join(_TYPES))]
    for key in sorted(set(schema) - _ALLOWED[kind]):
        problems.append("%s: the keyword '%s' is not in the supported subset for %s" % (name, key, kind))
    description = schema.get("description")
    if not isinstance(description, str) or not description.strip():
        problems.append("%s: a description is required" % name)
    if kind == "object":
        props = schema.get("properties")
        if not isinstance(props, dict) or not props:
            problems.append("%s: properties must be a non-empty object" % name)
            props = {}
        if schema.get("additionalProperties") is not False:
            problems.append("%s: additionalProperties must be false" % name)
        required = schema.get("required")
        if not isinstance(required, list) or sorted(required) != sorted(props) or len(set(required)) != len(required):
            problems.append("%s: required must name every property exactly once" % name)
        for key in props:
            problems += check_schema(props[key], "%s.%s" % (name, key))
    elif kind == "string":
        if "enum" in schema:
            values = schema["enum"]
            if not (isinstance(values, list) and values and all(isinstance(v, str) for v in values) and len(set(values)) == len(values)):
                problems.append("%s: enum must be a non-empty list of distinct strings" % name)
    elif kind == "array":
        if "items" not in schema:
            problems.append("%s: items is required" % name)
        else:
            problems += check_schema(schema["items"], name + "[]")
            if isinstance(schema["items"], dict) and schema["items"].get("type") == "array":
                problems.append("%s: an array of arrays is not in the supported subset" % name)
        for key in ("minItems", "maxItems"):
            if key in schema and not _is_int(schema[key]):
                problems.append("%s: %s must be a whole number" % (name, key))
    else:
        for key in ("minimum", "maximum"):
            if key in schema and not _is_number(schema[key]):
                problems.append("%s: %s must be a number" % (name, key))
        if kind == "integer":
            for key in ("minimum", "maximum"):
                if key in schema and not _is_int(schema[key]):
                    problems.append("%s: %s must be a whole number" % (name, key))
    return problems


def _is_int(value):
    return isinstance(value, int) and not isinstance(value, bool)


def _is_number(value):
    return isinstance(value, (int, float)) and not isinstance(value, bool) and math.isfinite(value)


def _shown(value):
    text = json.dumps(value, ensure_ascii=True) if not isinstance(value, str) else repr(value)
    return text if len(text) <= SHOWN else text[:SHOWN] + "..."


def validate(value, schema=None, name="answer"):
    """The ways `value` breaks `schema`, as messages that name the field (empty when it holds). Neither is changed."""
    if schema is None:
        schema = load_schema()
    problems = []
    _walk(value, schema, name, problems)
    return problems


def _walk(value, schema, name, problems):
    kind = schema["type"]
    if kind == "object":
        if not isinstance(value, dict):
            problems.append("%s: expected an object, got %s" % (name, _shown(value)))
            return
        for key in sorted(value):
            if key not in schema["properties"]:
                problems.append("%s.%s: unexpected field" % (name, key))
        for key in schema["required"]:
            if key not in value:
                problems.append("%s.%s: missing required field" % (name, key))
            else:
                _walk(value[key], schema["properties"][key], "%s.%s" % (name, key), problems)
    elif kind == "array":
        if not isinstance(value, list):
            problems.append("%s: expected a list, got %s" % (name, _shown(value)))
            return
        if "minItems" in schema and len(value) < schema["minItems"]:
            problems.append("%s: has %d items, at least %d required" % (name, len(value), schema["minItems"]))
        if "maxItems" in schema and len(value) > schema["maxItems"]:
            problems.append("%s: has %d items, at most %d allowed" % (name, len(value), schema["maxItems"]))
        for index, item in enumerate(value):
            _walk(item, schema["items"], "%s[%d]" % (name, index), problems)
    elif kind == "string":
        if not isinstance(value, str):
            problems.append("%s: expected a string, got %s" % (name, _shown(value)))
        elif "enum" in schema and value not in schema["enum"]:
            problems.append("%s: %s is not one of %s" % (name, _shown(value), ", ".join(schema["enum"])))
    else:
        ok = _is_int(value) if kind == "integer" else _is_number(value)
        if not ok:
            problems.append("%s: expected %s, got %s" % (name, "a whole number" if kind == "integer" else "a number", _shown(value)))
            return
        if "minimum" in schema and value < schema["minimum"]:
            problems.append("%s: %s is below the minimum %s" % (name, _shown(value), schema["minimum"]))
        if "maximum" in schema and value > schema["maximum"]:
            problems.append("%s: %s is above the maximum %s" % (name, _shown(value), schema["maximum"]))


def _rules(answer):
    """The rules the schema subset cannot say; `answer` already holds the schema."""
    problems = []
    if len(answer["verdict"]) > VERDICT_MAX:
        problems.append("answer.verdict: %d characters, at most %d allowed" % (len(answer["verdict"]), VERDICT_MAX))
    seen = set()
    for index, folder in enumerate(answer["folders"]):
        path = folder["path"]
        name = "answer.folders[%d].path" % index
        if not path or path.startswith("/") or ".." in path.split("/"):
            problems.append("%s: %s must be relative, non-empty and without '..'" % (name, _shown(path)))
        if path in seen:
            problems.append("%s: %s is listed twice" % (name, _shown(path)))
        seen.add(path)
    return problems


def _no_duplicates(pairs):
    keys = [k for k, _ in pairs]
    if len(set(keys)) != len(keys):
        raise JudgmentError(["answer: a field is given twice (%s)" % _shown(sorted(k for k in set(keys) if keys.count(k) > 1)[0])])
    return dict(pairs)


def _no_constant(text):
    raise JudgmentError(["answer: %s is not a JSON number" % text])


def parse(text, schema=None):
    """The judge's answer text as a dict, or JudgmentError naming every field that breaks the schema or its rules."""
    try:
        answer = json.loads(text, object_pairs_hook=_no_duplicates, parse_constant=_no_constant)
    except JudgmentError:
        raise
    except ValueError as error:
        raise JudgmentError(["answer: not valid JSON (%s)" % error])
    problems = validate(answer, schema)
    if not problems:
        problems = _rules(answer)
    if problems:
        raise JudgmentError(problems)
    return answer


def dump(answer):
    """The answer as canonical text: keys sorted, two-space indent, a final newline; `parse(dump(a)) == a`."""
    return json.dumps(answer, indent=2, sort_keys=True, ensure_ascii=False) + "\n"
