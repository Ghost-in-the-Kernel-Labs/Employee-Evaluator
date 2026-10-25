"""The judge interface, a fake judge and the recorded-response replay (TODO.md 2.2f).

A judge takes a rendered prompt, a model id and a run number and returns the raw answer text with the tokens used
(`Answer`). Parsing the text is `judgment.parse`'s work, not the judge's: a judge that returns text that does not parse is
a case the layers above must meet (2.2h retries an unparsable run once). There is no live judge in this file: the only one
will be `transport.py`'s (2.2l), the one door to the network and to a key (E-003, E-013), and it will implement `Judge`.
Everything above this slice is tested with the two judges here and never reaches a network.

  RenderedPrompt  the text sent (`prompt.render`), with the prompt hash and the summary hash that key a recording.
  summary_hash    SHA-256, as 64 lowercase hex digits, of the UTF-8 bytes of the summary text exactly as given to
                  `prompt.render` (not stripped, not fenced): the one definition of "the summary's hash".
  FakeJudge       scripted answers, one per call in order; `unparsable()` scripts an answer `judgment.parse` refuses.
  ReplayJudge     the recorded answer for the key (prompt hash, summary hash, model id, run number); a missing key raises
                  `MissingRecording`. It holds recordings and nothing else: no judge it could fall through to, so a run
                  of a replay can never become a live call. The run number is part of the key, so 2.2h's three runs of one
                  model are three recordings.

The recordings file is JSON lines, UTF-8, `\\n` line ends, the last line ended too:
  line 1      {"version": 1}
  line 2...   one recording per line, compact JSON with sorted keys (`", "` and `": "` separators), the lines sorted by
              (prompt_hash, summary_hash, model, run), so a file is byte-stable and a new recording is one added line:
              {"answer": "<raw text>", "model": "<id>", "prompt_hash": "<64 hex>", "run": N, "summary_hash": "<64 hex>",
               "tokens": {"input": N, "output": N}}
`parse_recordings(text)` returns {key: Answer} or raises `RecordingsError` naming each broken line: a missing or extra field,
a version other than 1, a key twice (also a JSON name twice in one object), a hash that is not 64 lowercase hex digits, a
model that is empty, a run that is not an integer of 1 or more, tokens that are not integers of 0 or more (a boolean is not
an integer), an answer that is not a string. `dumps(recordings)` writes the canonical form. Recordings of real runs would live
under `build/` and are never committed (E-014); the fixtures of the tests are hand-written and synthetic.
"""
import hashlib
import json
import re

from agent_runner import prompt as prompt_files

# Decision: E-003
# Decision: E-013
# Decision: E-014
# Decision: E-017
# Decision: E-055
VERSION = 1
UNPARSABLE_TEXT = "I cannot decide on a judgment for this repository."
_HEX = re.compile(r"[0-9a-f]{64}")
_FIELDS = ("answer", "model", "prompt_hash", "run", "summary_hash", "tokens")


class Answer:
    """What a judge returns: the raw text and the tokens used (two non-negative integers)."""
    __slots__ = ("text", "input_tokens", "output_tokens")

    def __init__(self, text, input_tokens, output_tokens):
        if not isinstance(text, str):
            raise TypeError("an answer's text is a string")
        for count in (input_tokens, output_tokens):
            if not _is_count(count):
                raise ValueError("tokens are integers of 0 or more")
        self.text, self.input_tokens, self.output_tokens = text, input_tokens, output_tokens

    def _tuple(self):
        return self.text, self.input_tokens, self.output_tokens

    def __eq__(self, other):
        return isinstance(other, Answer) and self._tuple() == other._tuple()

    def __hash__(self):
        return hash(self._tuple())

    def __repr__(self):
        return "Answer(%r, %d, %d)" % self._tuple()


def _is_count(value, least=0):
    return isinstance(value, int) and not isinstance(value, bool) and value >= least


def summary_hash(summary):
    """The hex SHA-256 of the UTF-8 bytes of `summary`, the text given to `prompt.render`."""
    return hashlib.sha256(summary.encode("utf-8")).hexdigest()


class RenderedPrompt:
    """The text sent to a judge, with the two hashes that key its recording."""
    __slots__ = ("text", "prompt_hash", "summary_hash")

    def __init__(self, text, prompt_hash, summary_hash):
        self.text, self.prompt_hash, self.summary_hash = text, prompt_hash, summary_hash


def rendered(summary, **prompt_args):
    """The `RenderedPrompt` for `summary`: `prompt.render`'s text, `prompt.prompt_hash` and `summary_hash(summary)`."""
    return RenderedPrompt(prompt_files.render(summary, **prompt_args), prompt_files.prompt_hash(**prompt_args), summary_hash(summary))


class Judge:
    """The interface: `judge(prompt, model, run)` returns an `Answer`. `prompt` is a `RenderedPrompt`, `run` counts from 1."""

    def judge(self, prompt, model, run):
        raise NotImplementedError("a Judge implements judge(prompt, model, run)")


class FakeJudge(Judge):
    """Scripted answers, one per call in order; `calls` lists (prompt text, model, run) of every call made.

    A script item is an `Answer`, or a string (its tokens estimated from lengths, four characters to a token), or
    `FakeJudge.unparsable()`. A call after the script is spent raises `ScriptSpent`."""

    def __init__(self, script):
        self._script = list(script)
        self.calls = []

    @staticmethod
    def unparsable(input_tokens=1, output_tokens=1):
        """An answer `judgment.parse` refuses."""
        return Answer(UNPARSABLE_TEXT, input_tokens, output_tokens)

    def judge(self, prompt, model, run):
        self.calls.append((prompt.text, model, run))
        if len(self.calls) > len(self._script):
            raise ScriptSpent("call %d, but the script holds %d answers" % (len(self.calls), len(self._script)))
        item = self._script[len(self.calls) - 1]
        if isinstance(item, Answer):
            return item
        return Answer(item, max(1, len(prompt.text) // 4), max(1, len(item) // 4))


class ScriptSpent(RuntimeError):
    """A `FakeJudge` asked for more answers than it was given."""


class MissingRecording(KeyError):
    """A `ReplayJudge` asked for a key its recordings do not hold; it never falls through to a live call."""

    def __str__(self):
        return self.args[0]


class RecordingsError(ValueError):
    """A recordings file that is not whole: the message names each broken line."""


def key_of(prompt_hash, summary_hash, model, run):
    return (prompt_hash, summary_hash, model, run)


class ReplayJudge(Judge):
    """Returns the recorded answer for the key of the prompt, model and run; raises `MissingRecording` otherwise."""

    def __init__(self, recordings):
        self._recordings = dict(recordings)

    def judge(self, prompt, model, run):
        key = key_of(prompt.prompt_hash, prompt.summary_hash, model, run)
        try:
            return self._recordings[key]
        except KeyError:
            raise MissingRecording("no recording for prompt %s, summary %s, model %s, run %s" % (key[0][:12], key[1][:12], model, run))


def _pairs(pairs):
    names = [name for name, _ in pairs]
    duplicate = sorted({n for n in names if names.count(n) > 1})
    if duplicate:
        raise ValueError("the name %s is twice in one object" % ", ".join(duplicate))
    return dict(pairs)


def _record(line):
    """(key, Answer) of one recording line, or ValueError saying what is wrong."""
    data = json.loads(line, object_pairs_hook=_pairs)
    if not isinstance(data, dict):
        raise ValueError("a recording is a JSON object")
    if sorted(data) != list(_FIELDS):
        raise ValueError("fields must be exactly %s, got %s" % (list(_FIELDS), sorted(data)))
    for name in ("prompt_hash", "summary_hash"):
        if not (isinstance(data[name], str) and _HEX.fullmatch(data[name])):
            raise ValueError("%s must be 64 lowercase hex digits" % name)
    if not (isinstance(data["model"], str) and data["model"] != "" and data["model"] == data["model"].strip()):
        raise ValueError("model must be a non-empty string without edge spaces")
    if not _is_count(data["run"], 1):
        raise ValueError("run must be an integer of 1 or more")
    if not isinstance(data["answer"], str):
        raise ValueError("answer must be a string")
    tokens = data["tokens"]
    if not (isinstance(tokens, dict) and sorted(tokens) == ["input", "output"] and _is_count(tokens["input"]) and _is_count(tokens["output"])):
        raise ValueError('tokens must be {"input": N, "output": N}, integers of 0 or more')
    return (key_of(data["prompt_hash"], data["summary_hash"], data["model"], data["run"]),
            Answer(data["answer"], tokens["input"], tokens["output"]))


def parse_recordings(text):
    """{key: Answer} from the text of a recordings file; `RecordingsError` naming each broken line."""
    lines = text.split("\n")
    if lines and lines[-1] == "":
        lines.pop()
    problems, recordings = [], {}
    try:
        header = json.loads(lines[0], object_pairs_hook=_pairs) if lines else None
    except ValueError:
        header = None
    if not (isinstance(header, dict) and sorted(header) == ["version"]):
        raise RecordingsError('line 1 must be {"version": %d}' % VERSION)
    if header["version"] != VERSION or isinstance(header["version"], bool):
        raise RecordingsError("line 1: version must be %d, got %s" % (VERSION, json.dumps(header["version"])[:20]))
    for number, line in enumerate(lines[1:], 2):
        try:
            key, answer = _record(line)
        except ValueError as error:
            problems.append("line %d: %s" % (number, error))
            continue
        if key in recordings:
            problems.append("line %d: the key (prompt, summary, model %s, run %d) is twice" % (number, key[2], key[3]))
        recordings[key] = answer
    if problems:
        raise RecordingsError("; ".join(problems))
    return recordings


def dumps(recordings):
    """The canonical text of {key: Answer}: the header, then one line per recording in key order."""
    out = ['{"version": %d}' % VERSION]
    for key in sorted(recordings):
        answer = recordings[key]
        out.append(json.dumps({"answer": answer.text, "model": key[2], "prompt_hash": key[0], "run": key[3],
                               "summary_hash": key[1], "tokens": {"input": answer.input_tokens, "output": answer.output_tokens}},
                              sort_keys=True, ensure_ascii=False))
    return "\n".join(out) + "\n"


def load_recordings(path):
    try:
        with open(path, "rb") as handle:
            text = handle.read().decode("utf-8")
    except (OSError, UnicodeDecodeError) as error:
        raise RecordingsError("cannot read %s: %s" % (path, getattr(error, "strerror", None) or error))
    return parse_recordings(text)


def judge_from_recordings(path):
    """A `ReplayJudge` over the recordings file at `path`."""
    return ReplayJudge(load_recordings(path))
