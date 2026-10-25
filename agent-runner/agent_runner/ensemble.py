"""Three runs, the mean, the vote and Opus on a split (TODO.md 2.2h, E-055).

`judge_repository(judge, prompt)` runs one rendered prompt three times on the Haiku model, reads each answer with
`judgment.parse`, and returns a `Result`: one record per judged field with the value that stands, how it was reached, and
every run behind it. It calls only the `Judge` it is given, so a `FakeJudge`, a `ReplayJudge` or a `BudgetedJudge` over
either runs unchanged; it imports no client and reaches no network (E-003, E-013). The model ids are parameters; the defaults
`claude-haiku-5-5` and `claude-opus-5-5` are placeholders until Ghost confirms the real ids (open question 3).

Run numbers (they key a recording, so every call of a model has its own). Haiku slot k (1, 2, 3) is run k; its one retry,
made only when the answer does not parse, is run 3 + k (4, 5, 6). Opus is run 1; its one retry is run 2. A slot that fails
twice is missing. Fewer than two usable Haiku runs is a REFUSED result (no fields, the reason, the calls made), never a score.

What stands, by field:
  scores.<metric>.score    quality, complexity, navigability, 0 to 100: the mean of the usable runs, unless max - min is more
                           than 15 (15 does not trigger, 16 does): then Opus judges the metric and its answer stands (E-055).
  scores.<metric>.confidence, .evidence
                           follow the score: the mean of the confidences and the runs' evidence in run order without
                           repeats ("union"); where Opus decided the metric, Opus's confidence and evidence.
  quality                  the 1 to 5 integer of the labels. The 15-point rule is for the 0 to 100 scale and does not carry
                           over (15 of 100 is 0.6 of a step on 1 to 5, so any difference would call Opus). It is an ordinal
                           label that 2.2i compares exactly and within one, so: Opus when max - min is more than 1 (the
                           "within one" tolerance), else the median of the usable runs (the lower of two when two runs differ
                           by one). A call for Ghost.
  kind, ambition.tier,     categories: the majority (more than half of the votes). No majority, which Ghost decided
  folders[path]            (2026-10-25) includes the three-way tie and two runs that differ, sends it to Opus, whose answer
                           stands even if no Haiku run gave it. ambition.confidence and .evidence follow the tier as above.
  folders                  voted per path. A path stands only when more than half of the usable runs list it; the rest go to
                           `dropped_folders` with their runs. The voters of a path are the runs that list it.
Opus is called once for everything that needs it, with the same prompt. Opus answer unparsable twice: those fields are
"unresolved" (value None), the result is PARTIAL. A `BudgetRefused` from the judge stops the calls: with two usable runs
the fields are computed from them and the result is PARTIAL (the fields Opus would have decided are "unresolved"); with fewer
it is REFUSED. Any other error from the judge, `MissingRecording` and `ScriptSpent` included, propagates.

A field record: {"value", "method" ("mean", "median", "majority", "union", "opus", "unresolved"), "runs": [{"model", "run",
"value"}] (Haiku runs, then Opus's), "models": [ids], "why": the reason Opus was called, or None}.
"""
import json
import statistics
from collections import Counter

from agent_runner import judgment
from agent_runner.budget import BudgetRefused

# Decision: E-003
# Decision: E-013
# Decision: E-014
# Decision: E-017
# Decision: E-055
HAIKU = "claude-haiku-5-5"
OPUS = "claude-opus-5-5"
RUNS = 3
MIN_USABLE = 2
SPREAD = 15
QUALITY_SPREAD = 1
DECIMALS = 3
METRICS = ("quality", "complexity", "navigability")
OK, PARTIAL, REFUSED = "ok", "partial", "refused"


class Result:
    """What `judge_repository` returns; `status` is OK, PARTIAL or REFUSED, `fields` and `folders` hold the records."""

    def __init__(self, status, reason, notes, models, calls, fields, folders, dropped):
        self.status, self.reason, self.notes, self.models = status, reason, notes, models
        self.calls, self.fields, self.folders, self.dropped_folders = calls, fields, folders, dropped

    def to_dict(self):
        return {"status": self.status, "reason": self.reason, "notes": self.notes, "models": self.models, "calls": self.calls,
                "fields": self.fields, "folders": self.folders, "dropped_folders": self.dropped_folders}

    def dumps(self):
        return json.dumps(self.to_dict(), indent=2, sort_keys=True, ensure_ascii=False) + "\n"


def _call(judge, prompt, model, runs, calls, slot=None):
    """Try the given run numbers in turn; (run, answer) of the first that parses, else None. Appends to `calls`."""
    for run in runs:
        text = judge.judge(prompt, model, run).text
        try:
            answer = judgment.parse(text)
        except judgment.JudgmentError as error:
            calls.append({"model": model, "run": run, "slot": slot, "usable": False, "error": str(error)[:200]})
            continue
        calls.append({"model": model, "run": run, "slot": slot, "usable": True, "error": None})
        return run, answer
    return None


def _vote(values):
    """The value with more than half of the votes, else None (no majority)."""
    value, count = Counter(json.dumps(v) for v in values).most_common(1)[0]
    return json.loads(value) if count * 2 > len(values) else None


def _union(lists):
    seen = []
    for items in lists:
        seen.extend(item for item in items if item not in seen)
    return seen


def _record(entries, value, method, why=None):
    return {"value": value, "method": method, "runs": entries, "models": sorted({e["model"] for e in entries}), "why": why}


def _scale(values, limit):
    """Why Opus is needed when the values are more than `limit` apart, else None."""
    if max(values) - min(values) > limit:
        return "spread %s over %s (%s)" % (_num(max(values) - min(values)), limit, ", ".join(_num(v) for v in values))
    return None


def _num(value):
    return str(int(value)) if float(value) == int(value) else str(value)


def judge_repository(judge, prompt, haiku=HAIKU, opus=OPUS):
    """Three runs of `prompt` on `haiku`, the mean and the vote, Opus once on a split; see the module docstring."""
    calls, usable, stop = [], [], None
    for slot in range(1, RUNS + 1):
        try:
            got = _call(judge, prompt, haiku, (slot, RUNS + slot), calls, slot)
        except BudgetRefused as error:
            stop = error
            break
        if got:
            usable.append(got)
    models = {"haiku": haiku, "opus": opus}
    if len(usable) < MIN_USABLE:
        reason = ("budget refused (%s) with %d usable runs of at least %d" % (stop, len(usable), MIN_USABLE) if stop
                  else "%d usable runs of %d, at least %d needed" % (len(usable), RUNS, MIN_USABLE))
        return Result(REFUSED, reason, [], models, calls, {}, {}, {})
    notes = ["budget refused after %d usable Haiku runs: %s" % (len(usable), stop)] if stop else []

    def runs_of(getter):
        return [{"model": haiku, "run": run, "value": getter(answer)} for run, answer in usable]

    pending, fields, folders, dropped = [], {}, {}, {}

    def group(target, items, why):
        """items: (key, entries, rule, getter). Settled now unless `why` asks for Opus."""
        if why:
            pending.extend((target, key, entries, getter, why) for key, entries, rule, getter in items)
            return
        for key, entries, rule, getter in items:
            values = [e["value"] for e in entries]
            if rule == "mean":
                target[key] = _record(entries, round(sum(values) / len(values), DECIMALS), "mean")
            elif rule == "union":
                target[key] = _record(entries, _union(values), "union")
            elif rule == "median":
                target[key] = _record(entries, statistics.median_low(values), "median")
            else:
                target[key] = _record(entries, _vote(values), "majority")

    for metric in METRICS:
        def get(part, metric=metric):
            return lambda a: a["scores"][metric][part]
        base = "scores.%s." % metric
        scores = runs_of(get("score"))
        group(fields, [(base + "score", scores, "mean", get("score")),
                       (base + "confidence", runs_of(get("confidence")), "mean", get("confidence")),
                       (base + "evidence", runs_of(get("evidence")), "union", get("evidence"))],
              _scale([e["value"] for e in scores], SPREAD))
    quality = runs_of(lambda a: a["quality"])
    group(fields, [("quality", quality, "median", lambda a: a["quality"])], _scale([e["value"] for e in quality], QUALITY_SPREAD))
    kind = runs_of(lambda a: a["kind"])
    group(fields, [("kind", kind, "vote", lambda a: a["kind"])], _tie([e["value"] for e in kind]))
    tier = runs_of(lambda a: a["ambition"]["tier"])
    group(fields, [("ambition.tier", tier, "vote", lambda a: a["ambition"]["tier"]),
                   ("ambition.confidence", runs_of(lambda a: a["ambition"]["confidence"]), "mean", lambda a: a["ambition"]["confidence"]),
                   ("ambition.evidence", runs_of(lambda a: a["ambition"]["evidence"]), "union", lambda a: a["ambition"]["evidence"])],
          _tie([e["value"] for e in tier]))
    listed = Counter(path for _, answer in usable for path in {f["path"] for f in answer["folders"]})
    for path in sorted(listed):
        def get_class(answer, path=path):
            return next((f["class"] for f in answer["folders"] if f["path"] == path), None)
        voters = [{"model": haiku, "run": run, "value": get_class(answer)} for run, answer in usable if get_class(answer) is not None]
        if listed[path] * 2 <= len(usable):
            dropped[path] = voters
            continue
        group(folders, [(path, voters, "vote", get_class)], _tie([e["value"] for e in voters]))

    if pending:
        notes.extend(_opus(judge, prompt, opus, pending, calls, stop))
    for target, key, entries, getter, why in pending:
        if key not in target:
            target[key] = _record(entries, None, "unresolved", why)
    status = PARTIAL if notes else OK
    return Result(status, None, notes, models, calls, fields, folders, dropped)


def _tie(votes):
    """Why Opus is needed when no value has more than half of the votes, else None."""
    if _vote(votes) is None:
        return "no majority among %d votes (%s)" % (len(votes), ", ".join(votes))
    return None


def _opus(judge, prompt, opus, pending, calls, stop):
    """Call Opus once (one retry); fill each pending record. Returns the notes for what could not be settled."""
    if stop:
        return ["Opus not called: the budget had refused a call (%s)" % stop]
    try:
        got = _call(judge, prompt, opus, (1, 2), calls)
    except BudgetRefused as error:
        return ["budget refused the Opus call: %s" % error]
    if got is None:
        return ["Opus answer unparsable twice: %d field(s) unresolved" % len(pending)]
    run, answer = got
    notes = []
    for target, key, entries, getter, why in pending:
        value = getter(answer)
        if value is None:
            notes.append("Opus did not list the folder %s" % key)
            continue
        target[key] = _record(entries + [{"model": opus, "run": run, "value": value}], value, "opus", why)
    return notes
