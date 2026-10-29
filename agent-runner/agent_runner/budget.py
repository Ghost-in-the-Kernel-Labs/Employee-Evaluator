"""The cost cap (TODO.md 2.2g): a token budget a judge is run under, and the record of what each judgment cost.

  estimate_tokens(text)  the prompt-token estimate that RESERVES, ceil(utf-8 bytes / 2), at least 1. There is no tokenizer here, so
                         the rule is deliberately high. Measured on the first live run (J-132): of 77 calls, 13 used more
                         than their reserve under the old rule (bytes / 3); the prompts are code-heavy summaries at about
                         2.1 bytes to a token, and the real input ran about 1.4 times the estimate (23588 real against 17118
                         estimated, 27806 against 17535). Prose runs near four bytes to a token, so half the bytes is high
                         for it, which is the side to err on. The estimate only reserves. The real counts come back from the
                         API (`transport.py`, 2.2l) and are what is charged.
  Budget                 caps per call, per evaluation and per trial, each None (no cap) or a number of tokens, and the
                         same three in dollars (they need a prices file).
  BudgetedJudge          wraps any `Judge` so 2.2h runs it under a `Budget` unchanged.
  load_prices            a data file of per-model prices with its date; dollars are off until a file is given.

A call costs input plus output tokens. Before a call `reserve` takes the worst case, the prompt's estimated tokens plus the
output limit, and raises `BudgetRefused` (naming `cap`: "call", "evaluation" or "trial") when the reserve would pass a cap
(per call: the reserve itself; per evaluation and per trial: what is already spent, plus reserves still held, plus this
reserve). The dollar caps work the same way on dollars: the reserve's dollars are `prices.dollars(model, prompt estimate,
output limit)`, what is spent is the real usage priced; the refusal names the unit ("tokens" or "dollars"). A dollar cap with no
prices file is a `BudgetError`. Per cap the tokens are checked before the dollars. A refused call is never made, charges nothing and adds no cost record (it is listed in `refusals`). After the call
`charge` replaces the reserve by the real usage. A real usage above its reserve should not happen; it is charged in full,
recorded with `over_reserve` true and added to `overruns`, and the run goes on. A judge that raises releases its reserve and
records nothing.

An evaluation is one repository's judging, a trial a whole run over many; `begin_evaluation` and `begin_trial` start fresh
counters (a trial start starts an evaluation too). The two counters are independent. The first evaluation and trial are open
from the start. `records` is one dict per charged call (the number that sets the unlock price floor, DESIGN open question);
`dumps_records` writes it as text and `write_records` writes it to a path under a `build/` directory only (E-014).

Ghost's caps and prices are Ghost's, and live in data: `agent-runner/caps.json` (`caps.py`) and `agent-runner/prices/<date>.json`.
The defaults here are None (no cap) and no prices; the synthetic numbers in the tests mean nothing.

Prices file, JSON: {"version": 1, "date": "YYYY-MM-DD", "per_tokens": 1000000, "source": "<url>", "models": {"<model id>":
{"input": N, "output": N, "max_prompt_tokens": N}}} with the price in dollars of `per_tokens` tokens; "source" and each model's
"max_prompt_tokens" are optional. With prices given, a call on a model the file lacks is refused as `PriceMissing`. A model's
`max_prompt_tokens` is the longest prompt the listed price covers (a longer one is billed at another price the file does not
hold): `reserve` refuses a prompt estimate above it as `PromptTooLong`, nothing held, listed in `refusals` as cap
"prompt_length", so the file's price is the price paid. A real `input_tokens` above it at charge time is charged (at the
file's price, so its dollars are a floor), recorded with `above_price_tier` true, never refused.
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
UNITS = ("tokens", "dollars")
PRICES_VERSION = 1
_DATE = re.compile(r"\d{4}-\d{2}-\d{2}")


class BudgetError(ValueError):
    """A bad cap, price or reservation."""


class BudgetRefused(RuntimeError):
    """A call refused before it was made; `cap` names the cap ("call", "evaluation" or "trial"), `unit` "tokens" or "dollars"."""

    def __init__(self, cap, reserve, limit, used, unit="tokens"):
        self.cap, self.reserve, self.limit, self.used, self.unit = cap, reserve, limit, used, unit
        super().__init__("refused: the %s cap is %s %s, %s already used or held, and the call would reserve %s"
                         % (cap, _show(limit), unit, _show(used), _show(reserve)))


class PriceMissing(BudgetError):
    """Prices are on, and the model is not in the file."""


class PromptTooLong(BudgetError):
    """A prompt estimate above the model's `max_prompt_tokens`, the longest the file's price covers; nothing was held."""

    def __init__(self, model, estimate, limit):
        self.model, self.estimate, self.limit = model, estimate, limit
        super().__init__("refused: the prompt is estimated at %s tokens, above the %s the price of %s covers"
                         % (_show(estimate), _show(limit), model))


def _plain(number):
    """A Decimal as a string (JSON has no Decimal); a count as it is."""
    return str(number) if isinstance(number, Decimal) else number


def _show(number):
    return str(int(number)) if float(number) == int(number) else str(number)


def estimate_tokens(text):
    """The reserving estimate of the tokens in `text`: ceil(UTF-8 bytes / 2), at least 1. Never the real count.

    Half the bytes, not a third: the first live run measured code-heavy prompts at about 2.1 bytes to a token, so a
    third of the bytes undercounted them by about 1.4 times and 13 of 77 calls used more than their reserve (J-132).
    """
    return max(1, -(-len(text.encode("utf-8")) // 2))


def _number(value):
    return isinstance(value, (int, float)) and not isinstance(value, bool) and math.isfinite(value)


def _cap(name, value):
    if value is not None and not (_number(value) and value >= 0):
        raise BudgetError("the %s cap is None or a number of 0 or more, got %r" % (name, value))
    return value


def _dollar_cap(name, value, prices):
    _cap(name, value)
    if value is None:
        return None
    if prices is None:
        raise BudgetError("the %s cap is in dollars and there is no prices file" % name)
    return Decimal(repr(value))


class Prices:
    """Per-model prices with the date they were taken."""

    def __init__(self, date, per_tokens, models, source=None):
        self.date, self.per_tokens, self.models, self.source = date, per_tokens, models, source

    def max_prompt_tokens(self, model):
        """The longest prompt, in tokens, the model's price covers, or None when the file sets none."""
        return self.models[model].get("max_prompt_tokens") if model in self.models else None

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
    if not (isinstance(data, dict) and sorted(k for k in data if k != "source") == ["date", "models", "per_tokens", "version"]):
        raise BudgetError('the prices file holds exactly "version", "date", "per_tokens" and "models", and may hold "source"')
    if "source" in data and not (isinstance(data["source"], str) and data["source"].strip() != ""):
        raise BudgetError("the prices file's source must be a non-empty string")
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
        if not (model != "" and model == model.strip() and isinstance(price, dict)
                and sorted(k for k in price if k != "max_prompt_tokens") == ["input", "output"]
                and all(_number(price[k]) and price[k] >= 0 for k in ("input", "output"))):
            raise BudgetError('the price of "%s" must be {"input": N, "output": N}, numbers of 0 or more, and may hold "max_prompt_tokens"'
                              % model)
        found[model] = {k: Decimal(repr(price[k])) for k in ("input", "output")}
        if "max_prompt_tokens" in price:
            limit = price["max_prompt_tokens"]
            if not (isinstance(limit, int) and not isinstance(limit, bool) and limit >= 1):
                raise BudgetError('the max_prompt_tokens of "%s" must be an integer of 1 or more' % model)
            found[model]["max_prompt_tokens"] = limit
    return Prices(data["date"], per, found, data.get("source"))


def load_prices(path):
    try:
        with open(path, "rb") as handle:
            text = handle.read().decode("utf-8")
    except (OSError, UnicodeDecodeError) as error:
        raise BudgetError("cannot read %s: %s" % (path, getattr(error, "strerror", None) or error))
    return parse_prices(text)


class Reservation:
    """The worst case held for one call."""
    __slots__ = ("tokens", "dollars", "model", "run", "prompt_hash", "summary_hash", "evaluation", "trial", "open")

    def __init__(self, tokens, model, run, prompt_hash, summary_hash, evaluation, trial, dollars=Decimal(0)):
        self.tokens, self.dollars, self.model, self.run = tokens, dollars, model, run
        self.prompt_hash, self.summary_hash = prompt_hash, summary_hash
        self.evaluation, self.trial, self.open = evaluation, trial, True


class Budget:
    """Caps in tokens and in dollars per call, per evaluation and per trial (None: no cap), with the record of every charged call."""

    def __init__(self, per_call=None, per_evaluation=None, per_trial=None, prices=None,
                 dollars_per_call=None, dollars_per_evaluation=None, dollars_per_trial=None):
        self.per_call = _cap("per-call", per_call)
        self.per_evaluation = _cap("per-evaluation", per_evaluation)
        self.per_trial = _cap("per-trial", per_trial)
        self.prices = prices
        self.dollars_per_call = _dollar_cap("per-call dollar", dollars_per_call, prices)
        self.dollars_per_evaluation = _dollar_cap("per-evaluation dollar", dollars_per_evaluation, prices)
        self.dollars_per_trial = _dollar_cap("per-trial dollar", dollars_per_trial, prices)
        self.records, self.refusals, self.overruns = [], [], []
        self._spent = {"evaluation": 0, "trial": 0}
        self._held = {"evaluation": 0, "trial": 0}
        self._spent_dollars = {"evaluation": Decimal(0), "trial": Decimal(0)}
        self._held_dollars = {"evaluation": Decimal(0), "trial": Decimal(0)}
        self._evaluation, self._trial = 1, 1

    def begin_evaluation(self):
        """Start a fresh evaluation counter; the trial's is untouched."""
        self._evaluation += 1
        self._spent["evaluation"] = self._held["evaluation"] = 0
        self._spent_dollars["evaluation"] = self._held_dollars["evaluation"] = Decimal(0)

    def begin_trial(self):
        """Start fresh trial and evaluation counters."""
        self._trial += 1
        self._spent["trial"] = self._held["trial"] = 0
        self._spent_dollars["trial"] = self._held_dollars["trial"] = Decimal(0)
        self.begin_evaluation()

    def spent(self, cap):
        """Tokens charged so far in the current "evaluation" or "trial"."""
        return self._spent[cap]

    def spent_dollars(self, cap):
        """Dollars charged so far in the current "evaluation" or "trial" (a Decimal; 0 with no prices)."""
        return self._spent_dollars[cap]

    def reserve(self, prompt_tokens, output_limit, model, run=None, prompt_hash=None, summary_hash=None):
        """Hold the worst case, prompt_tokens + output_limit (and its dollars); a refusal holds nothing.

        `BudgetRefused` when it passes a cap, `PromptTooLong` when the prompt estimate is above the model's `max_prompt_tokens`."""
        for count in (prompt_tokens, output_limit):
            if not (isinstance(count, int) and not isinstance(count, bool) and count >= 0):
                raise BudgetError("prompt tokens and the output limit are integers of 0 or more")
        dollars = Decimal(0)
        if self.prices is not None:
            self.prices.dollars(model, 0, 0)
            limit = self.prices.max_prompt_tokens(model)
            if limit is not None and prompt_tokens > limit:
                self._refuse("prompt_length", "tokens", model, run, prompt_tokens, limit, 0)
                raise PromptTooLong(model, prompt_tokens, limit)
            dollars = self.prices.dollars(model, prompt_tokens, output_limit)
        tokens = prompt_tokens + output_limit
        held, spent, hd, sd = self._held, self._spent, self._held_dollars, self._spent_dollars
        checks = (("call", "tokens", self.per_call, tokens, 0), ("call", "dollars", self.dollars_per_call, dollars, 0),
                  ("evaluation", "tokens", self.per_evaluation, tokens, spent["evaluation"] + held["evaluation"]),
                  ("evaluation", "dollars", self.dollars_per_evaluation, dollars, sd["evaluation"] + hd["evaluation"]),
                  ("trial", "tokens", self.per_trial, tokens, spent["trial"] + held["trial"]),
                  ("trial", "dollars", self.dollars_per_trial, dollars, sd["trial"] + hd["trial"]))
        for cap, unit, limit, amount, used in checks:
            if limit is not None and used + amount > limit:
                self._refuse(cap, unit, model, run, amount, limit, used)
                raise BudgetRefused(cap, amount, limit, used, unit)
        self._held["evaluation"] += tokens
        self._held["trial"] += tokens
        self._held_dollars["evaluation"] += dollars
        self._held_dollars["trial"] += dollars
        return Reservation(tokens, model, run, prompt_hash, summary_hash, self._evaluation, self._trial, dollars)

    def _refuse(self, cap, unit, model, run, reserve, limit, used):
        self.refusals.append({"cap": cap, "unit": unit, "evaluation": self._evaluation, "trial": self._trial, "model": model,
                              "run": run, "reserve": _plain(reserve), "limit": _plain(limit), "used": _plain(used)})

    def _release(self, reservation):
        if not reservation.open:
            raise BudgetError("this reservation is already charged or released")
        reservation.open = False
        if reservation.evaluation == self._evaluation:
            self._held["evaluation"] -= reservation.tokens
            self._held_dollars["evaluation"] -= reservation.dollars
        if reservation.trial == self._trial:
            self._held["trial"] -= reservation.tokens
            self._held_dollars["trial"] -= reservation.dollars

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
        dollars, above = None, False
        if self.prices is not None:
            cost = self.prices.dollars(reservation.model, input_tokens, output_tokens)
            dollars = str(cost)
            limit = self.prices.max_prompt_tokens(reservation.model)
            above = limit is not None and input_tokens > limit
        if reservation.evaluation == self._evaluation:
            self._spent["evaluation"] += total
            self._spent_dollars["evaluation"] += Decimal(dollars or 0)
        if reservation.trial == self._trial:
            self._spent["trial"] += total
            self._spent_dollars["trial"] += Decimal(dollars or 0)
        record = {"evaluation": reservation.evaluation, "trial": reservation.trial, "model": reservation.model,
                  "run": reservation.run, "prompt_hash": reservation.prompt_hash, "summary_hash": reservation.summary_hash,
                  "input_tokens": input_tokens, "output_tokens": output_tokens, "reserved": reservation.tokens,
                  "over_reserve": total > reservation.tokens, "above_price_tier": above, "dollars": dollars,
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
