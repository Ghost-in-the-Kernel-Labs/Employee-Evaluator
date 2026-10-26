"""Ghost's caps file (TODO.md 2.2l): it loads, it makes a Budget with the dollar caps, a broken file is refused."""
import json
import os
import unittest
from decimal import Decimal

from agent_runner import budget as budgets
from agent_runner import caps

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
PRICES = os.path.join(ROOT, "prices", "2026-10-26.json")


class CapsFile(unittest.TestCase):
    def test_the_real_file_holds_ghosts_numbers(self):
        got = caps.load_caps()
        self.assertEqual((None, 0.5, 5.0, 8000, 3), (got.dollars_per_call, got.dollars_per_evaluation, got.dollars_per_trial,
                                                    got.output_limit, got.prompt_iterations))

    def test_it_makes_a_budget_under_those_caps(self):
        budget = caps.load_caps().budget(budgets.load_prices(PRICES))
        self.assertEqual((None, Decimal("0.5"), Decimal("5.0")),
                         (budget.dollars_per_call, budget.dollars_per_evaluation, budget.dollars_per_trial))

    def test_two_opus_calls_fit_an_evaluation_and_a_third_does_not(self):
        got = caps.load_caps()
        budget = got.budget(budgets.load_prices(PRICES))
        for _ in range(2):  # a 10,000-token prompt and the output limit: 0.04 + 0.16 dollars
            budget.charge(budget.reserve(10000, got.output_limit, "claude-opus-5-5"), 10000, got.output_limit)
        with self.assertRaises(budgets.BudgetRefused) as caught:
            budget.reserve(10000, got.output_limit, "claude-opus-5-5")
        self.assertEqual(("evaluation", "dollars"), (caught.exception.cap, caught.exception.unit))

    def test_a_broken_file_is_refused(self):
        with open(caps.CAPS_PATH, encoding="utf-8") as handle:
            good = json.load(handle)

        def broken(**change):
            data = dict(good)
            data.update(change)
            return json.dumps(data)
        for text in ("not json", "[]", broken(version=2), broken(output_limit=0), broken(output_limit=True), broken(prompt_iterations=1.5),
                     broken(dollars_per_trial=-1), broken(dollars_per_trial="5"), broken(extra=1)):
            with self.assertRaises(budgets.BudgetError, msg=text[:40]):
                caps.parse_caps(text)
        data = dict(good)
        del data["output_limit"]
        with self.assertRaises(budgets.BudgetError):
            caps.parse_caps(json.dumps(data))
        with self.assertRaises(budgets.BudgetError):
            caps.load_caps(os.path.join(ROOT, "no-such-caps.json"))

    def test_a_dollar_cap_without_prices_is_refused(self):
        with self.assertRaises(budgets.BudgetError):
            caps.load_caps().budget(None)


if __name__ == "__main__":
    unittest.main()
