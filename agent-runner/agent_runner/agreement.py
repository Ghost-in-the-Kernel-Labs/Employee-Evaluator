"""Agreement with the calibration set, the baselines and the record under the prompt's hash (TODO.md 2.2i, E-084).

Given a judgment per calibration id (the 2.2h `ensemble.Result`, or its `to_dict()`), this reads the labels of `calibration/`
and computes, for four fields, how many labels the judgment matches, beside the baseline of always answering the same thing.
No model call, no network, no clock: the date of a record is passed in (E-003, E-013).

  ambition             the tier, exactly
  quality_exact        the 1 to 5 integer, exactly
  quality_within_one   the 1 to 5 integer, off by at most one
  kind                 exactly; the judge's six values include the labels' own-work, clone and generated (E-082). The set
                       holds no generated label, which the record says (`labels_absent`): a judge's "generated" is a miss.

The figure of a field is matched / labels, over ALL labels. An id whose result is partial or refused, or has no result, has no
judged value and is a miss on every field; the record counts them apart (`results`: ok, partial, refused, missing).

The baseline of a field is the constant answer that matches most labels, computed from the labels: the most common label
(ties: the first in sorted order), and for quality_within_one the integer 1 to 5 within one of most labels (ties: the lowest).

Live (E-084, Ghost's definition, 2026-10-25): a field is live only if BOTH hold,
  (1) the raw agreement is at least FLOOR (80 %), and
  (2) it beats the baseline by the margin: figure >= baseline + MARGIN * (1 - baseline), the figure closes at least MARGIN
      (default one half) of the gap between the baseline and 100 %.
The size of the margin is the agent's proposal for Ghost to confirm; it is a parameter (`margin`), never a number in the logic.
All of it is exact arithmetic (`fractions.Fraction`), so 24 of 30 is 80 % with no float rounding. `needed` is the least count of
labels that is live. `is_live(records_dir, prompt_hash, field)` answers from a record by its counts under the rule as it stands;
no record for the hash, or none for that field, is not live: no record means no fact (SC-05).

The record, `records/<prompt hash>.json`, sorted keys, two-space indent, one final newline (byte-stable):
  version, prompt {name, hash}, date, models, runs, labels (count), labels_absent {kind: [values the set does not hold]},
  margin {floor, fraction_of_gap} (as "a/b"), results {ok, partial, refused, missing},
  fields {<field>: {matched, total, figure, baseline {answer, matched, figure}, needed, live, why}},
  ids {<id>: {judged {status, kind, ambition, quality}, label {kind, ambition, quality}}}.
A figure is a decimal rounded to 3 places, for reading; `matched` and `total` are what is computed from.
Records of real runs are the question of 2.2m and are never written by a test; the tests write under a temporary directory
from synthetic labels and answers (E-014).
"""
import json
import math
import os
import re
from collections import Counter
from fractions import Fraction

from agent_runner import budget as budgets
from agent_runner import ensemble
from agent_runner import judge as judges

# Decision: E-003
# Decision: E-013
# Decision: E-014
# Decision: E-017
# Decision: E-055
# Decision: E-056
# Decision: E-082
# Decision: E-084
VERSION = 1
DEFAULT_CATCH = (budgets.BudgetError, judges.MissingRecording)
FLOOR = Fraction(4, 5)
MARGIN = Fraction(1, 2)  # Unsure: half the gap is the agent's proposal; Ghost confirms the size (E-084)
DECIMALS = 3
FIELDS = ("ambition", "quality_exact", "quality_within_one", "kind")
LABEL_KEYS = ("kind", "ambition", "quality")
KINDS = ("generated", "clone", "plan only", "mixed", "own-work", "built")
LABEL_KINDS = ("own-work", "clone", "generated")
TIERS = ("toy", "utility", "product", "research")
CALIBRATION_DIR = os.path.join(os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))), "calibration")
RECORDS_DIR = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "records")
_ID = re.compile(r"c[0-9]{3}")
_HEX = re.compile(r"[0-9a-f]{64}")
_DATE = re.compile(r"[0-9]{4}-[0-9]{2}-[0-9]{2}")


class AgreementError(ValueError):
    """A label, a judgment or a record that is not whole."""


def load_labels(directory=CALIBRATION_DIR):
    """{id: {"kind", "ambition", "quality"}} from the `c###.json` files of `directory`; AgreementError on a broken one."""
    labels = {}
    for name in sorted(os.listdir(directory)):
        if not (name.endswith(".json") and _ID.fullmatch(name[:-5])):
            continue
        with open(os.path.join(directory, name), encoding="utf-8") as handle:
            data = json.load(handle)
        label_id = name[:-5]
        if not isinstance(data, dict) or sorted(data) != sorted(("id",) + LABEL_KEYS) or data["id"] != label_id:
            raise AgreementError("%s: fields must be id (= the file name), kind, ambition, quality" % name)
        if data["kind"] not in LABEL_KINDS or data["ambition"] not in TIERS:
            raise AgreementError("%s: kind or ambition outside the vocabulary" % name)
        if not (isinstance(data["quality"], int) and not isinstance(data["quality"], bool) and 1 <= data["quality"] <= 5):
            raise AgreementError("%s: quality must be an integer 1 to 5" % name)
        labels[label_id] = {key: data[key] for key in LABEL_KEYS}
    if not labels:
        raise AgreementError("no labels in %s" % directory)
    return labels


def judged_value(result):
    """{"status", "kind", "ambition", "quality"} of one result; the three values are None unless the status is ok.

    `result` is an `ensemble.Result`, its `to_dict()`, or None (no result for the id: status "missing"). A partial or
    refused result is not read: it counts as a miss (Ghost's rule for 2.2i)."""
    if result is None:
        return {"status": "missing", "kind": None, "ambition": None, "quality": None}
    data = result.to_dict() if isinstance(result, ensemble.Result) else result
    if not isinstance(data, dict) or data.get("status") not in (ensemble.OK, ensemble.PARTIAL, ensemble.REFUSED):
        raise AgreementError("a result is an ensemble Result or its dict")
    if data["status"] != ensemble.OK:
        return {"status": data["status"], "kind": None, "ambition": None, "quality": None}
    fields = data["fields"]
    return {"status": ensemble.OK, "kind": fields["kind"]["value"], "ambition": fields["ambition.tier"]["value"],
            "quality": fields["quality"]["value"]}


def judge_all(judge, prompts, haiku=ensemble.HAIKU, opus=ensemble.OPUS, budget=None, errors=None, catch=DEFAULT_CATCH):
    """{id: Result}: `ensemble.judge_repository` over `prompts` ({id: RenderedPrompt}), in id order, one id after another.

    `budget` is the `Budget` the `judge` runs under, if any: an evaluation is one repository, so `begin_evaluation()` is called
    before each id and the per-evaluation cap holds per repository, not over the whole run (2.2m). Once an id's judging has
    been refused by the TRIAL cap the loop stops (every later id would be refused unmade); the ids it did not reach have no
    result, so `compute` counts them missing. An id whose judging raises one of the `catch` errors (a `BudgetError` such as a
    prompt too long, a `MissingRecording`) gets no result either, and its exception's class name goes in `errors` ({id: name})
    when a dict is passed; with no dict such an error propagates. Any other error propagates."""
    results = {}
    for label_id in sorted(prompts):
        if budget is not None:
            budget.begin_evaluation()
        seen = len(budget.refusals) if budget is not None else 0
        try:
            results[label_id] = ensemble.judge_repository(judge, prompts[label_id], haiku, opus)
        except catch as error:
            if errors is None:
                raise
            errors[label_id] = type(error).__name__
        if budget is not None and any(r["cap"] == "trial" for r in budget.refusals[seen:]):
            break
    return results


def _constant(labels, field):
    """(answer, matches) of the best constant answer for `field`, from the labels."""
    if field == "quality_within_one":
        counts = {c: sum(1 for l in labels.values() if abs(l["quality"] - c) <= 1) for c in range(1, 6)}
        answer = min(counts, key=lambda c: (-counts[c], c))
        return answer, counts[answer]
    key = _label_key(field)
    counts = Counter(l[key] for l in labels.values())
    answer = min(counts, key=lambda a: (-counts[a], str(a)))
    return answer, counts[answer]


def _label_key(field):
    return "quality" if field.startswith("quality") else field


def _hit(field, judged, label):
    if judged is None:
        return False
    if field == "quality_within_one":
        return abs(judged - label) <= 1
    return judged == label


def needed(total, baseline, margin=MARGIN, floor=FLOOR):
    """The least count of `total` labels that is live: at the floor, and baseline + margin of the gap to 100 %."""
    margin, floor, baseline = Fraction(margin), Fraction(floor), Fraction(baseline)
    return math.ceil(max(floor, baseline + margin * (1 - baseline)) * total)


def _verdict(matched, total, baseline_matched, margin, floor):
    """(live, why): `why` names each test that fails, in words with the counts."""
    base = Fraction(baseline_matched, total)
    least = needed(total, base, margin, floor)
    why = []
    if Fraction(matched, total) < Fraction(floor):
        why.append("under the floor: %d of %d, %d needed for %s" % (matched, total, math.ceil(Fraction(floor) * total), _pct(floor)))
    if matched < needed(total, base, margin, 0):
        why.append("does not beat the constant answer by the margin: %d of %d, baseline %d, %d needed"
                   % (matched, total, baseline_matched, needed(total, base, margin, 0)))
    return (not why), "; ".join(why) if why else None


def _pct(fraction):
    return "%s %%" % round(float(Fraction(fraction) * 100), 1)


def _figure(matched, total):
    return round(matched / total, DECIMALS)


def compute(labels, results, margin=MARGIN, floor=FLOOR):
    """The record's measurable part: {"results", "fields", "ids"} for `labels` and `results` ({id: Result, dict or None}).

    An id of `results` that is not a label is an AgreementError; an id of `labels` with no result is "missing"."""
    unknown = sorted(set(results) - set(labels))
    if unknown:
        raise AgreementError("results for ids that are not labels: %s" % ", ".join(unknown))
    margin, floor = Fraction(margin), Fraction(floor)
    if not (0 <= margin <= 1 and 0 <= floor <= 1):
        raise AgreementError("the margin and the floor are fractions between 0 and 1")
    judged = {i: judged_value(results.get(i)) for i in labels}
    total = len(labels)
    counts = Counter(j["status"] for j in judged.values())
    fields = {}
    for field in FIELDS:
        key = _label_key(field)
        matched = sum(1 for i in labels if _hit(field, judged[i][key], labels[i][key]))
        answer, base = _constant(labels, field)
        live, why = _verdict(matched, total, base, margin, floor)
        fields[field] = {"matched": matched, "total": total, "figure": _figure(matched, total),
                         "baseline": {"answer": answer, "matched": base, "figure": _figure(base, total)},
                         "needed": needed(total, Fraction(base, total), margin, floor), "live": live, "why": why}
    return {"results": {s: counts.get(s, 0) for s in (ensemble.OK, ensemble.PARTIAL, ensemble.REFUSED, "missing")},
            "fields": fields,
            "ids": {i: {"judged": judged[i], "label": dict(labels[i])} for i in sorted(labels)}}


def build_record(prompt_name, prompt_hash, models, runs, date, labels, results, margin=MARGIN, floor=FLOOR):
    """The whole record (a dict). `date` is "YYYY-MM-DD", passed in; `models` a dict or list of ids; `runs` the runs made."""
    if not (isinstance(prompt_hash, str) and _HEX.fullmatch(prompt_hash)):
        raise AgreementError("the prompt hash is 64 lowercase hex digits")
    if not (isinstance(date, str) and _DATE.fullmatch(date)):
        raise AgreementError("the date is YYYY-MM-DD, passed in")
    body = compute(labels, results, margin, floor)
    absent = {"kind": [k for k in KINDS if k not in {l["kind"] for l in labels.values()}]}
    return {"version": VERSION, "prompt": {"name": prompt_name, "hash": prompt_hash}, "date": date, "models": models, "runs": runs,
            "labels": len(labels), "labels_absent": absent,
            "margin": {"floor": str(Fraction(floor)), "fraction_of_gap": str(Fraction(margin))},
            "results": body["results"], "fields": body["fields"], "ids": body["ids"]}


def dumps(record):
    """The canonical text of a record: sorted keys, two-space indent, one final newline."""
    return json.dumps(record, indent=2, sort_keys=True, ensure_ascii=False) + "\n"


def write_record(record, records_dir=RECORDS_DIR):
    """Write `record` to `<records_dir>/<its prompt hash>.json` and return the path. Tests pass a temporary directory."""
    path = os.path.join(records_dir, record["prompt"]["hash"] + ".json")
    os.makedirs(records_dir, exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as handle:
        handle.write(dumps(record))
    return path


def read_record(records_dir, prompt_hash):
    """The record for `prompt_hash`, or None when there is none; AgreementError when the file is not whole or not the hash's."""
    if not (isinstance(prompt_hash, str) and _HEX.fullmatch(prompt_hash)):
        raise AgreementError("the prompt hash is 64 lowercase hex digits")
    path = os.path.join(records_dir, prompt_hash + ".json")
    if not os.path.isfile(path):
        return None
    try:
        with open(path, encoding="utf-8") as handle:
            record = json.load(handle)
        if record["prompt"]["hash"] != prompt_hash or record["version"] != VERSION:
            raise AgreementError("%s: not the record of that hash, or another version" % path)
        for field in FIELDS:
            entry = record["fields"][field]
            int(entry["matched"]), int(entry["total"]), int(entry["baseline"]["matched"])
        return record
    except (ValueError, KeyError, TypeError) as error:
        if isinstance(error, AgreementError):
            raise
        raise AgreementError("%s: %s" % (path, error))


def is_live(records_dir, prompt_hash, field, margin=MARGIN, floor=FLOOR):
    """Whether the prompt of `prompt_hash` is live for `field`: its record's counts under the rule as it now stands.

    No record for the hash is False, not a guess (SC-05). The record's own `live` flag is not read, so a changed margin
    or floor takes effect on old records and a hand-edited flag changes nothing."""
    if field not in FIELDS:
        raise AgreementError("field must be one of %s" % ", ".join(FIELDS))
    record = read_record(records_dir, prompt_hash)
    if record is None:
        return False
    entry = record["fields"][field]
    return _verdict(entry["matched"], entry["total"], entry["baseline"]["matched"], Fraction(margin), Fraction(floor))[0]
