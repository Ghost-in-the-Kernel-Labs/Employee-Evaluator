"""The calibration driver (TODO.md 2.2m): checkouts in, the agreement record out. No decision of its own; it chains the parts.

    python3 -B -m agent_runner.calibrate --corpus DIR --out BUILD_SUBDIR (--live | --replay RECORDINGS) [--ids c001,c002] [--date YYYY-MM-DD]

For each label id (`agreement.load_labels()`, or `--ids`), in id order: `evaluator brief DIR/<id>` to `OUT/briefs/<id>.json`,
`summary.build` to `OUT/summaries/<id>.txt`, `judge.rendered`, then `agreement.judge_all` (the three runs and Opus of
`ensemble`) under one `Budget` made from `caps.json` and `prices/2026-10-26.json`, `begin_trial()` once and `begin_evaluation()`
before each id, so the per-evaluation dollar cap holds per repository. Then `OUT/costs.json` (every charged call),
`OUT/results.json` (id -> status, kind, ambition, quality, nothing else) and the agreement record.

  --corpus DIR    a folder OUTSIDE this repository with one git checkout per id, named by the id alone (`DIR/c001`). A corpus
                  or a checkout (symbolic links followed) inside the repository is refused (E-014). An id with no checkout, a
                  failed brief or summary, or a judging that raised a budget or recording error is reported by id and counted
                  missing, a miss on every field as `agreement` counts it.
  --out SUBDIR    must lie under this repository's `build/` (refused elsewhere, E-014); briefs, summaries, recordings,
                  costs, results and, when it is not committed, the record are written there.
  --live          the live judge, `transport.live_judge` under the budget; its recordings go to `OUT/recordings.jsonl`. The
                  key is read by `transport.py` alone, from `ANTHROPIC_KEY`. Exactly one of --live and --replay is required.
  --replay FILE   `judge.judge_from_recordings(FILE)` under the same budget; it never reaches the network.
  --ids a,b       only these ids are judged; the record is then over all the labels with the rest missing.
  --date          the record's date, default today (a record is dated by the caller, `agreement.build_record`).

The record goes to `agent-runner/records/<prompt hash>.json` only when the run was --live, covered every label (no --ids) and no
id is missing; otherwise it is `OUT/record.json`, never in records/, so a replay, a partial run or a run the trial cap stopped
cannot make a prompt "not live" by a short count. A refusal by the TRIAL cap stops the run at that id and the rest is missing;
everything above is still written. Printed on stdout: ids and numbers only (per field matched, needed and live; the results by
status; dollars; refusals by cap; overruns); never a folder, a path inside a checkout or any text from a summary or an answer
(E-014). An error says what failed by id and exception class name.

The brief step runs the built CLI: `gradle compileJava --offline` once in the repository, then (from `cli_brief`)
`java -cp "build/classes/java/main:third_party/tree-sitter/*" evaluator.cli.Main brief DIR/<id>`, with the key variable
removed from the child's environment (`env -u`). A test, or a different build, passes its own `brief(checkout) -> JSON text`.
"""
import argparse
import datetime
import json
import os
import re
import subprocess
import sys
from decimal import Decimal

from agent_runner import agreement, caps as caps_files, ensemble, judge as judges, prompt as prompt_files, summary, transport
from agent_runner import budget as budgets

# Decision: E-003
# Decision: E-013
# Decision: E-014
# Decision: E-017
# Decision: E-084
# Decision: E-087
AGENT_RUNNER = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
REPO_ROOT = os.path.dirname(AGENT_RUNNER)
BUILD_ROOT = os.path.join(REPO_ROOT, "build")
PRICES_PATH = os.path.join(AGENT_RUNNER, "prices", "2026-10-26.json")
CLASSES = os.path.join(REPO_ROOT, "build", "classes", "java", "main")
TREE_SITTER = os.path.join(REPO_ROOT, "third_party", "tree-sitter", "*")
BRIEF_TIMEOUT = 900
_DATE = re.compile(r"[0-9]{4}-[0-9]{2}-[0-9]{2}")
USAGE = ("usage: calibrate --corpus DIR --out BUILD_SUBDIR (--live | --replay RECORDINGS) [--ids c001,c002] [--date YYYY-MM-DD]\n")


class CalibrateError(Exception):
    """A refused run or a failed step; the message holds ids and numbers only, never a path or repository text."""


def inside(path, root):
    """True when `path` is `root` or lies under it, after links are followed."""
    path, root = os.path.realpath(path), os.path.realpath(root)
    return os.path.commonpath([path, root]) == root


def cli_brief(checkout):
    """The brief of `checkout` as the JSON text `evaluator brief` prints: the built CLI run as a child, the key removed from it."""
    if not os.path.isdir(CLASSES):
        raise CalibrateError("the CLI is not built: run `gradle compileJava --offline` in the repository")
    command = ["env", "-u", transport.KEY_VARIABLE, "java", "-cp", CLASSES + os.pathsep + TREE_SITTER,
               "evaluator.cli.Main", "brief", checkout]
    done = subprocess.run(command, capture_output=True, timeout=BRIEF_TIMEOUT, cwd=REPO_ROOT)
    if done.returncode != 0:
        raise CalibrateError("evaluator brief exited %d" % done.returncode)
    return done.stdout.decode("utf-8")


def _write(path, text):
    with open(path, "w", encoding="utf-8", newline="\n") as handle:
        handle.write(text)


def default_judge(live, replay, budget, output_limit, recordings_path):
    """The judge of a real run: live under `budget`, or the recordings at `replay` under the same budget."""
    if live:
        return transport.live_judge(budget, output_limit, recordings_path=recordings_path)
    return budgets.BudgetedJudge(judges.judge_from_recordings(replay), budget, output_limit)


def _check(corpus, out, ids, date, live, replay, labels, build_root):
    if live == bool(replay):
        raise CalibrateError("exactly one of --live and --replay is required")
    if not (isinstance(date, str) and _DATE.fullmatch(date)):
        raise CalibrateError("the date is YYYY-MM-DD")
    unknown = sorted(set(ids) - set(labels))
    if unknown:
        raise CalibrateError("not label ids: %s" % ", ".join(unknown))
    if not os.path.isdir(corpus):
        raise CalibrateError("the corpus is not a folder")
    if inside(corpus, REPO_ROOT):
        raise CalibrateError("the corpus lies inside this repository (E-014)")
    if not inside(out, build_root) or os.path.realpath(out) == os.path.realpath(build_root):
        raise CalibrateError("the output folder must lie under this repository's build/ (E-014)")
    if replay and not os.path.isfile(replay):
        raise CalibrateError("the recordings file is not a file")


def _prepare(label_id, corpus, out, brief, say):
    """The `RenderedPrompt` of one id, its brief and summary written under `out`; None (reported by id) when a step failed."""
    checkout = os.path.join(corpus, label_id)
    if not os.path.isdir(checkout):
        say("%s: no checkout" % label_id)
        return None
    if inside(checkout, REPO_ROOT):
        say("%s: the checkout lies inside this repository, refused" % label_id)
        return None
    try:
        text = brief(checkout)
        data = json.loads(text)
        _write(os.path.join(out, "briefs", label_id + ".json"), text)
        built = summary.build(data, checkout)
        _write(os.path.join(out, "summaries", label_id + ".txt"), built)
        return judges.rendered(built)
    except Exception as error:  # a hostile checkout may break any step; the class name is all that is said of it
        say("%s: brief or summary failed (%s)" % (label_id, type(error).__name__))
        return None


def _dollars(budget):
    return sum((Decimal(r["dollars"]) for r in budget.records if r["dollars"] is not None), Decimal(0))


def run(corpus, out, live=False, replay=None, ids=None, date=None, brief=cli_brief, judge_factory=None, records_dir=None,
        build_root=BUILD_ROOT, caps_path=caps_files.CAPS_PATH, prices_path=PRICES_PATH, stdout=None, stderr=None):
    """One calibration run; returns the exit status (0 done, 1 refused or failed). The keyword arguments after `date` are the
    tests' seams: `brief(checkout) -> JSON text`, `judge_factory(budget, output_limit, recordings_path) -> Judge`, and the folders."""
    stdout, stderr = stdout or sys.stdout, stderr or sys.stderr

    def say(line):
        print(line, file=stderr)

    labels = agreement.load_labels()
    wanted = sorted(ids) if ids else sorted(labels)
    date = date or datetime.date.today().isoformat()
    try:
        _check(corpus, out, wanted, date, live, replay, labels, build_root)
        os.makedirs(os.path.join(out, "briefs"), exist_ok=True)
        os.makedirs(os.path.join(out, "summaries"), exist_ok=True)
        prices = budgets.load_prices(prices_path)
        limits = caps_files.load_caps(caps_path)
        budget = limits.budget(prices)
        budget.begin_trial()
        prompts = {}
        for label_id in wanted:
            rendered = _prepare(label_id, corpus, out, brief, say)
            if rendered is not None:
                prompts[label_id] = rendered
        recordings = os.path.join(out, "recordings.jsonl")
        factory = judge_factory or (lambda b, limit, path: default_judge(live, replay, b, limit, path))
        judge = factory(budget, limits.output_limit, recordings)
        errors = {}
        catch = agreement.DEFAULT_CATCH + (transport.TransportError,)
        results = agreement.judge_all(judge, prompts, budget=budget, errors=errors, catch=catch)
    except (CalibrateError, budgets.BudgetError, judges.RecordingsError, transport.TransportError, OSError) as error:
        say("calibrate: refused: %s" % (error if isinstance(error, (CalibrateError, budgets.BudgetError)) else type(error).__name__))
        return 1
    for label_id in sorted(errors):
        say("%s: not judged (%s)" % (label_id, errors[label_id]))
    models = {"haiku": ensemble.HAIKU, "opus": ensemble.OPUS}
    record = agreement.build_record(prompt_files.NAME, prompt_files.prompt_hash(), models, ensemble.RUNS, date, labels, results)
    budget.write_records(os.path.join(out, "costs.json"))
    per_id = {i: {k: v for k, v in record["ids"][i]["judged"].items()} for i in wanted}
    _write(os.path.join(out, "results.json"), json.dumps(per_id, indent=2, sort_keys=True) + "\n")
    committed = live and set(wanted) == set(labels) and record["results"]["missing"] == 0
    if committed:
        agreement.write_record(record, records_dir or agreement.RECORDS_DIR)
    else:
        _write(os.path.join(out, "record.json"), agreement.dumps(record))
    stdout.write(_report(record, budget, wanted, results, "live" if live else "replay", committed))
    return 0


def _report(record, budget, wanted, results, mode, committed):
    counts = record["results"]
    lines = ["calibrate: %s run, %d of %d labels attempted" % (mode, len(wanted), record["labels"]),
             "results: ok %d, partial %d, refused %d, missing %d" % tuple(counts[s] for s in ("ok", "partial", "refused", "missing"))]
    missing = [i for i in wanted if i not in results]
    if missing:
        lines.append("not judged: " + ", ".join(missing))
    for field in agreement.FIELDS:
        entry = record["fields"][field]
        lines.append("%s: matched %d of %d, needed %d, baseline %d, live %s"
                     % (field, entry["matched"], entry["total"], entry["needed"], entry["baseline"]["matched"], "yes" if entry["live"] else "no"))
    by_cap = {cap: sum(1 for r in budget.refusals if r["cap"] == cap) for cap in budgets.CAPS + ("prompt_length",)}
    lines.append("dollars: %s" % _dollars(budget))
    lines.append("refusals: %d (%s)" % (len(budget.refusals), ", ".join("%s %d" % (c, n) for c, n in by_cap.items())))
    lines.append("overruns: %d" % len(budget.overruns))
    lines.append("record: " + ("written to agent-runner/records/ under the prompt's hash" if committed else "kept under the output folder, not in records/"))
    return "\n".join(lines) + "\n"


class _Parser(argparse.ArgumentParser):
    def error(self, message):  # a usage error never echoes what was typed: a path may be a repository's
        sys.stderr.write(USAGE)
        raise SystemExit(2)


def main(argv=None, **seams):
    """The command line; `seams` are `run`'s keyword arguments after `date` (the tests'), nothing else."""
    parser = _Parser(prog="calibrate", add_help=False)
    parser.add_argument("--corpus", required=True)
    parser.add_argument("--out", required=True)
    parser.add_argument("--live", action="store_true")
    parser.add_argument("--replay")
    parser.add_argument("--ids")
    parser.add_argument("--date")
    args = parser.parse_args(sys.argv[1:] if argv is None else argv)
    ids = [part for part in args.ids.split(",")] if args.ids else None
    return run(args.corpus, args.out, args.live, args.replay, ids, args.date, **seams)


if __name__ == "__main__":
    sys.exit(main())
