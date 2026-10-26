"""Ghost's caps as data (TODO.md 2.2l, E-087): `agent-runner/caps.json`, read by `load_caps`, made into a `Budget` by `budget`.

Caps file, JSON: {"version": 1, "dollars_per_trial": N, "dollars_per_evaluation": N, "dollars_per_call": N or null,
"output_limit": N, "prompt_iterations": N}. The three dollar caps are a number of 0 or more or null (no cap); `output_limit` is
the most output tokens one call may use (the `max_tokens` sent, thinking included), an integer of 1 or more; `prompt_iterations`
is how many prompt versions 2.2n may try before it checks back with Ghost, an integer of 1 or more. Exactly these members.
The numbers are Ghost's; the code holds none.
"""
import json
import os

from agent_runner import budget as budgets

# Decision: E-087
VERSION = 1
CAPS_PATH = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "caps.json")
_FIELDS = ("dollars_per_call", "dollars_per_evaluation", "dollars_per_trial", "output_limit", "prompt_iterations", "version")


class Caps:
    """The caps of `caps.json`."""
    __slots__ = _FIELDS[:-1]

    def __init__(self, dollars_per_call, dollars_per_evaluation, dollars_per_trial, output_limit, prompt_iterations):
        self.dollars_per_call, self.dollars_per_evaluation = dollars_per_call, dollars_per_evaluation
        self.dollars_per_trial, self.output_limit, self.prompt_iterations = dollars_per_trial, output_limit, prompt_iterations

    def budget(self, prices):
        """A `Budget` under these dollar caps and `prices` (the dollar caps need them)."""
        return budgets.Budget(prices=prices, dollars_per_call=self.dollars_per_call,
                              dollars_per_evaluation=self.dollars_per_evaluation, dollars_per_trial=self.dollars_per_trial)


def _count(value):
    return isinstance(value, int) and not isinstance(value, bool) and value >= 1


def parse_caps(text):
    """`Caps` from the text of a caps file; `budgets.BudgetError` naming what is wrong."""
    try:
        data = json.loads(text)
    except ValueError as error:
        raise budgets.BudgetError("the caps file is not JSON: %s" % error)
    if not (isinstance(data, dict) and sorted(data) == list(_FIELDS)):
        raise budgets.BudgetError("the caps file holds exactly %s" % ", ".join(_FIELDS))
    if data["version"] != VERSION or isinstance(data["version"], bool):
        raise budgets.BudgetError("the caps file's version must be %d" % VERSION)
    for name in ("dollars_per_call", "dollars_per_evaluation", "dollars_per_trial"):
        budgets._cap(name, data[name])
    if not (_count(data["output_limit"]) and _count(data["prompt_iterations"])):
        raise budgets.BudgetError("output_limit and prompt_iterations are integers of 1 or more")
    return Caps(*(data[name] for name in _FIELDS[:-1]))


def load_caps(path=CAPS_PATH):
    try:
        with open(path, "rb") as handle:
            text = handle.read().decode("utf-8")
    except (OSError, UnicodeDecodeError) as error:
        raise budgets.BudgetError("cannot read %s: %s" % (path, getattr(error, "strerror", None) or error))
    return parse_caps(text)
