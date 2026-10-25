"""The cost cap (TODO.md 2.2g): a token budget a judge is run under, and the record of what each judgment cost.

  estimate_tokens(text)  the prompt-token estimate that RESERVES, ceil(utf-8 bytes / 3), at least 1. There is no tokenizer here, so
                         the rule is deliberately high: English text and code run near four characters (and so near four
                         bytes) to a token, other scripts fewer, so a third of the bytes does not undercount them. The
                         estimate only reserves. The real counts come back from the API in 2.2l and are what is charged.
  Budget                 caps per call, per evaluation and per trial, each None (no cap) or a number of tokens.
  BudgetedJudge          wraps any `Judge` so 2.2h runs it under a `Budget` unchanged.
  load_prices            a data file of per-model prices with its date; dollars are off until a file is given.

A call costs input plus output tokens. Before a call `reserve` takes the worst case, the prompt's estimated tokens plus the
output limit, and raises `BudgetRefused` (naming `cap`: "call", "evaluation" or "trial") when the reserve would pass a cap
(per call: the reserve itself; per evaluation and per trial: what is already spent, plus reserves still held, plus this
reserve). A refused call is never made, charges nothing and adds no cost record (it is listed in `refusals`). After the call
`charge` replaces the reserve by the real usage. A real usage above its reserve should not happen; it is charged in full,
recorded with `over_reserve` true and added to `overruns`, and the run goes on. A judge that raises releases its reserve and
records nothing.

An evaluation is one repository's judging, a trial a whole run over many; `begin_evaluation` and `begin_trial` start fresh
counters (a trial start starts an evaluation too). The two counters are independent. The first evaluation and trial are open
from the start. `records` is one dict per charged call (the number that sets the unlock price floor, DESIGN open question);
`dumps_records` writes it as text and `write_records` writes it to a path under a `build/` directory only (E-014).

Ghost's caps and prices are Ghost's. The defaults here are None (no cap) and no prices; the synthetic numbers in the tests
mean nothing. The real prices file does not exist yet and is not guessed.

Prices file, JSON: {"version": 1, "date": "YYYY-MM-DD", "per_tokens": 1000000, "models": {"<model id>": {"input": N, "output": N}}}
with the price in dollars of `per_tokens` tokens. With prices given, a call on a model the file lacks is refused as `PriceMissing`.
"""
import json
import math
import os
import re
from decimal import Decimal

# Decision: E-003
# Decision: E-013
# Decision: E-014
# Decision: E-017
CAPS = ("call", "evaluation", "trial")
PRICES_VERSION = 1
_DATE = re.compile(r"\d{4}-\d{2}-\d{2}")


class BudgetError(ValueError):
    """A bad cap, price or reservation."""


class BudgetRefused(RuntimeError):
    """A call refused before it was made; `cap` names the cap ("call", "evaluation" or "trial")."""

    def __init__(self, cap, reserve, limit, used):
        self.cap, self.reserve, self.limit, self.used = cap, reserve, limit, used
        super().__init__("refused: the %s cap is %s tokens, %s already used or held, and the call would reserve %s"
                         % (cap, _show(limit), _show(used), _show(reserve)))


class PriceMissing(BudgetError):
    """Prices are on, and the model is not in the file."""


def _show(number):
    return str(int(number)) if float(number) == int(number) else str(number)


def estimate_tokens(text):
    """The reserving estimate of the tokens in `text`: ceil(UTF-8 bytes / 3), at least 1. Never the real count."""
    return max(1, -(-len(text.encode("utf-8")) // 3))


def _number(value):
    return isinstance(value, (int, float)) and not isinstance(value, bool) and math.isfinite(value)


def _cap(name, value):
    if value is not None and not (_number(value) and value >= 0):
        raise BudgetError("the %s cap is None or a number of 0 or more, got %r" % (name, value))
    return value


class Prices:
    """Per-model prices with the date they were taken."""

    def __init__(self, date, per_tokens, models):
        self.date, self.per_tokens, self.models = date, per_tokens, models

    def dollars(self, model, input_tokens, output_tokens):
        """The cost as a Decimal; `PriceMissing` for a model the file lacks."""
        if model not in self.models:
            raise PriceMissing("no price for model %s in the prices file of %s" % (model, self.date))
        price = self.models[model]
        return (price["input"] * input_tokens + price["output"] * output_tokens) / self.per_tokens


def parse_prices(text):
    """`Prices` from the text of a prices file; `BudgetError` naming what is wrong."""
    try:
        data = json.loads(text)
    except ValueError as error:
        raise BudgetError("the prices file is not JSON: %s" % error)
    if not (isinstance(data, dict) and sorted(data) == ["date", "models", "per_tokens", "version"]):
        raise BudgetError('the prices file holds exactly "version", "date", "per_tokens" and "models"')
    if data["version"] != PRICES_VERSION or isinstance(data["version"], bool):
        raise BudgetError("the prices file's version must be %d" % PRICES_VERSION)
    if not (isinstance(data["date"], str) and _DATE.fullmatch(data["date"])):
        raise BudgetError("the prices file's date must be YYYY-MM-DD")
    per = data["per_tokens"]
    if not (isinstance(per, int) and not isinstance(per, bool) and per >= 1):
        raise BudgetError("per_tokens must be an integer of 1 or more")
    models, found = data["models"], {}
    if not (isinstance(models, dict) and models):
        raise BudgetError("models must be a non-empty object")
    for model, price in models.items():
        if not (model != "" and model == model.strip() and isinstance(price, dict) and sorted(price) == ["input", "output"]
                and all(_number(price[k]) and price[k] >= 0 for k in price)):
            raise BudgetError('the price of "%s" must be {"input": N, "output": N}, numbers of 0 or more' % model)
        found[model] = {k: Decimal(repr(price[k])) for k in price}
    return Prices(data["date"], per, found)


def load_prices(path):
    try:
        with open(path, "rb") as handle:
            text = handle.read().decode("utf-8")
    except (OSError, UnicodeDecodeError) as error:
        raise BudgetError("cannot read %s: %s" % (path, getattr(error, "strerror", None) or error))
    return parse_prices(text)


class Reservation:
    """The worst case held for one call."""
    __slots__ = ("tokens", "model", "run", "prompt_hash", "summary_hash", "evaluation", "trial", "open")

    def __init__(self, tokens, model, run, prompt_hash, summary_hash, evaluation, trial):
        self.tokens, self.model, self.run = tokens, model, run
        self.prompt_hash, self.summary_hash = prompt_hash, summary_hash
        self.evaluation, self.trial, self.open = evaluation, trial, True


class Budget:
    """Caps in tokens per call, per evaluation and per trial (None: no cap), with the record of every charged call."""

    def __init__(self, per_call=None, per_evaluation=None, per_trial=None, prices=None):
        self.per_call = _cap("per-call", per_call)
        self.per_evaluation = _cap("per-evaluation", per_evaluation)
        self.per_trial = _cap("per-trial", per_trial)
        self.prices = prices
        self.records, self.refusals, self.overruns = [], [], []
        self._spent = {"evaluation": 0, "trial": 0}
        self._held = {"evaluation": 0, "trial": 0}
        self._evaluation, self._trial = 1, 1

    def begin_evaluation(self):
        """Start a fresh evaluation counter; the trial's is untouched."""
        self._evaluation += 1
        self._spent["evaluation"] = self._held["evaluation"] = 0

    def begin_trial(self):
        """Start fresh trial and evaluation counters."""
        self._trial += 1
        self._spent["trial"] = self._held["trial"] = 0
        self.begin_evaluation()

    def spent(self, cap):
        """Tokens charged so far in the current "evaluation" or "trial"."""
        return self._spent[cap]

    def reserve(self, prompt_tokens, output_limit, model, run=None, prompt_hash=None, summary_hash=None):
        """Hold the worst case, prompt_tokens + output_limit; `BudgetRefused` (nothing held) when it passes a cap."""
        for count in (prompt_tokens, output_limit):
            if not (isinstance(count, int) and not isinstance(count, bool) and count >= 0):
                raise BudgetError("prompt tokens and the output limit are integers of 0 or more")
        if self.prices is not None:
            self.prices.dollars(model, 0, 0)
        tokens = prompt_tokens + output_limit
        checks = (("call", self.per_call, 0), ("evaluation", self.per_evaluation, self._spent["evaluation"] + self._held["evaluation"]),
                  ("trial", self.per_trial, self._spent["trial"] + self._held["trial"]))
        for cap, limit, used in checks:
            if limit is not None and used + tokens > limit:
                self.refusals.append({"cap": cap, "evaluation": self._evaluation, "trial": self._trial, "model": model,
                                      "run": run, "reserve": tokens, "limit": limit, "used": used})
                raise BudgetRefused(cap, tokens, limit, used)
        self._held["evaluation"] += tokens
        self._held["trial"] += tokens
        return Reservation(tokens, model, run, prompt_hash, summary_hash, self._evaluation, self._trial)

    def _release(self, reservation):
        if not reservation.open:
            raise BudgetError("this reservation is already charged or released")
        reservation.open = False
        if reservation.evaluation == self._evaluation:
            self._held["evaluation"] -= reservation.tokens
        if reservation.trial == self._trial:
            self._held["trial"] -= reservation.tokens

    def release(self, reservation):
        """Give a reserve back with nothing charged and nothing recorded (the call failed)."""
        self._release(reservation)

    def charge(self, reservation, input_tokens, output_tokens):
        """Replace the reserve by the real usage and record it; usage above the reserve is charged and flagged, never refused."""
        for count in (input_tokens, output_tokens):
            if not (isinstance(count, int) and not isinstance(count, bool) and count >= 0):
                raise BudgetError("tokens are integers of 0 or more")
        self._release(reservation)
        total = input_tokens + output_tokens
        if reservation.evaluation == self._evaluation:
            self._spent["evaluation"] += total
        if reservation.trial == self._trial:
            self._spent["trial"] += total
        dollars = None
        if self.prices is not None:
            dollars = str(self.prices.dollars(reservation.model, input_tokens, output_tokens))
        record = {"evaluation": reservation.evaluation, "trial": reservation.trial, "model": reservation.model,
                  "run": reservation.run, "prompt_hash": reservation.prompt_hash, "summary_hash": reservation.summary_hash,
                  "input_tokens": input_tokens, "output_tokens": output_tokens, "reserved": reservation.tokens,
                  "over_reserve": total > reservation.tokens, "dollars": dollars,
                  "prices_date": None if self.prices is None else self.prices.date}
        self.records.append(record)
        if record["over_reserve"]:
            self.overruns.append(record)
        return record

    def dumps_records(self):
        """The records as JSON text (sorted keys, one record per line inside a list, a final newline)."""
        body = ",\n".join("  " + json.dumps(r, sort_keys=True) for r in self.records)
        return "[\n" + body + "\n]\n" if body else "[]\n"

    def write_records(self, path):
        """Write `dumps_records` to `path`, which must lie under a directory named `build` (E-014); else `BudgetError`."""
        parts = os.path.abspath(path).split(os.sep)[:-1]
        if "build" not in parts:
            raise BudgetError("cost records are written under build/ only, not %s" % path)
        with open(path, "w", encoding="utf-8", newline="\n") as handle:
            handle.write(self.dumps_records())


class BudgetedJudge:
    """Runs any `Judge` under a `Budget`: reserve (prompt estimate + `output_limit`), call, charge the real usage.

    Same `judge(prompt, model, run)` as a `Judge`, so 2.2h takes it unchanged. A refused call never reaches the inner judge."""

    def __init__(self, judge, budget, output_limit):
        if not (isinstance(output_limit, int) and not isinstance(output_limit, bool) and output_limit >= 1):
            raise BudgetError("the output limit is an integer of 1 or more")
        self._judge, self.budget, self.output_limit = judge, budget, output_limit

    def judge(self, prompt, model, run):
        reservation = self.budget.reserve(estimate_tokens(prompt.text), self.output_limit, model, run,
                                          prompt.prompt_hash, prompt.summary_hash)
        try:
            answer = self._judge.judge(prompt, model, run)
        except BaseException:
            self.budget.release(reservation)
            raise
        self.budget.charge(reservation, answer.input_tokens, answer.output_tokens)
        return answer
