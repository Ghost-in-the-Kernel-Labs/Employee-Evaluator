"""The cost cap (TODO.md 2.2g): reserve, refuse, charge, record, with FakeJudge; the prices file."""
import json
import os
import tempfile
import unittest
from decimal import Decimal

from agent_runner import budget as budgets
from agent_runner.budget import Budget, BudgetedJudge, BudgetError, BudgetRefused, PriceMissing
from agent_runner.judge import Answer, FakeJudge, RenderedPrompt

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
PRICES = os.path.join(ROOT, "tests", "fixtures", "synthetic-prices.json")
A, B = "a" * 64, "b" * 64


def prompt_of(text="x" * 30):
    return RenderedPrompt(text, A, B)


def answer(i, o):
    return Answer("ok", i, o)


class Estimate(unittest.TestCase):
    def test_a_third_of_the_bytes_rounded_up(self):
        self.assertEqual(1, budgets.estimate_tokens(""))
        self.assertEqual(1, budgets.estimate_tokens("a"))
        self.assertEqual(1, budgets.estimate_tokens("abc"))
        self.assertEqual(2, budgets.estimate_tokens("abcd"))
        self.assertEqual(2, budgets.estimate_tokens("éé"))  # 4 bytes
        self.assertEqual(1, budgets.estimate_tokens("日"))  # 3 bytes
        self.assertEqual(4, budgets.estimate_tokens("日本語日"))  # 12 bytes

    def test_it_never_undercounts_the_fake_judges_scripted_usage(self):
        for text in ("", "a", "word " * 100, "日本語" * 50, "é" * 77, "x" * 1001, "\n".join(["line"] * 9)):
            fake = FakeJudge(["reply"])
            got = fake.judge(prompt_of(text), "m", 1)
            self.assertGreaterEqual(budgets.estimate_tokens(text), got.input_tokens, repr(text[:10]))


class Caps(unittest.TestCase):
    def test_no_cap_by_default_and_any_number_is_a_cap(self):
        budget = Budget()
        self.assertEqual((None, None, None), (budget.per_call, budget.per_evaluation, budget.per_trial))
        budget.charge(budget.reserve(10**9, 10**9, "m"), 1, 1)
        for bad in (-1, True, float("nan"), float("inf"), "5"):
            with self.assertRaises(BudgetError):
                Budget(per_call=bad)
        Budget(per_call=0.5, per_evaluation=0, per_trial=7)

    def test_a_reserve_exactly_at_the_cap_passes_and_one_over_is_refused(self):
        budget = Budget(per_call=40)
        budget.release(budget.reserve(30, 10, "m"))
        with self.assertRaises(BudgetRefused) as caught:
            budget.reserve(30, 11, "m")
        self.assertEqual("call", caught.exception.cap)
        self.assertIn("call cap is 40", str(caught.exception))

    def test_the_call_that_would_pass_the_cap_is_not_made(self):
        fake = FakeJudge([answer(5, 5), answer(5, 5)])
        budget = Budget(per_call=10)
        judge = BudgetedJudge(fake, budget, output_limit=20)
        with self.assertRaises(BudgetRefused) as caught:
            judge.judge(prompt_of(), "m", 1)
        self.assertEqual("call", caught.exception.cap)
        self.assertEqual([], fake.calls)
        self.assertEqual([], budget.records)
        self.assertEqual(0, budget.spent("evaluation") + budget.spent("trial"))
        self.assertEqual(1, len(budget.refusals))

    def test_a_refused_call_charges_nothing_and_holds_nothing(self):
        fake = FakeJudge([answer(10, 10)] * 3)
        budget = Budget(per_evaluation=45)
        judge = BudgetedJudge(fake, budget, output_limit=20)  # reserve 10 + 20 = 30 for a 30-byte prompt
        judge.judge(prompt_of(), "m", 1)  # spent 20
        with self.assertRaises(BudgetRefused):
            judge.judge(prompt_of(), "m", 2)  # 20 + 30 > 45
        self.assertEqual(1, len(fake.calls))
        self.assertEqual(20, budget.spent("evaluation"))
        self.assertEqual(1, len(budget.records))
        budget.begin_evaluation()
        judge.judge(prompt_of(), "m", 2)  # the held reserve was not leaked
        self.assertEqual(2, len(fake.calls))

    def test_evaluation_and_trial_caps_are_independent(self):
        fake = FakeJudge([answer(10, 10)] * 10)
        budget = Budget(per_evaluation=50, per_trial=70)
        judge = BudgetedJudge(fake, budget, output_limit=20)  # each reserve 30, each charge 20
        judge.judge(prompt_of(), "m", 1)  # eval 20, trial 20
        with self.assertRaises(BudgetRefused) as caught:
            judge.judge(prompt_of(), "m", 2)
            judge.judge(prompt_of(), "m", 3)  # eval 40 + 30 > 50
        self.assertEqual("evaluation", caught.exception.cap)
        budget.begin_evaluation()  # the trial keeps its 40
        self.assertEqual((0, 40), (budget.spent("evaluation"), budget.spent("trial")))
        judge.judge(prompt_of(), "m", 1)  # trial 60
        with self.assertRaises(BudgetRefused) as caught:
            judge.judge(prompt_of(), "m", 2)  # trial 60 + 30 > 70, eval 20 + 30 = 50 fits
        self.assertEqual("trial", caught.exception.cap)
        budget.begin_trial()
        self.assertEqual((0, 0), (budget.spent("evaluation"), budget.spent("trial")))
        judge.judge(prompt_of(), "m", 1)

    def test_the_first_cap_passed_is_named_call_before_evaluation_before_trial(self):
        budget = Budget(per_call=1, per_evaluation=1, per_trial=1)
        with self.assertRaises(BudgetRefused) as caught:
            budget.reserve(5, 5, "m")
        self.assertEqual("call", caught.exception.cap)
        budget = Budget(per_evaluation=1, per_trial=1)
        with self.assertRaises(BudgetRefused) as caught:
            budget.reserve(5, 5, "m")
        self.assertEqual("evaluation", caught.exception.cap)

    def test_reserves_still_held_count_against_a_cap(self):
        budget = Budget(per_evaluation=50)
        held = budget.reserve(20, 10, "m")
        with self.assertRaises(BudgetRefused):
            budget.reserve(20, 10, "m")
        budget.release(held)
        budget.release(budget.reserve(20, 10, "m"))


class Charging(unittest.TestCase):
    def test_the_real_usage_is_charged_not_the_reserve(self):
        budget = Budget(per_evaluation=1000)
        judge = BudgetedJudge(FakeJudge([answer(7, 3)]), budget, output_limit=100)
        judge.judge(prompt_of(), "m", 2)
        self.assertEqual(10, budget.spent("evaluation"))
        record = budget.records[0]
        self.assertEqual((7, 3, 110, False, 2, A, B), tuple(record[k] for k in (
            "input_tokens", "output_tokens", "reserved", "over_reserve", "run", "prompt_hash", "summary_hash")))
        self.assertIsNone(record["dollars"])

    def test_usage_above_the_reserve_is_charged_recorded_and_flagged_without_a_crash(self):
        fake = FakeJudge([answer(500, 500), answer(1, 1)])
        budget = Budget(per_evaluation=1000)
        judge = BudgetedJudge(fake, budget, output_limit=10)
        judge.judge(prompt_of(), "m", 1)
        self.assertEqual(1000, budget.spent("evaluation"))
        self.assertTrue(budget.records[0]["over_reserve"])
        self.assertEqual(1, len(budget.overruns))
        with self.assertRaises(BudgetRefused):
            judge.judge(prompt_of(), "m", 2)  # the overrun is spent: the cap holds for the next

    def test_a_judge_that_raises_releases_its_reserve_and_records_nothing(self):
        budget = Budget(per_evaluation=40)
        judge = BudgetedJudge(FakeJudge([]), budget, output_limit=20)
        for run in (1, 2):  # ScriptSpent each time; a leaked reserve would refuse the second
            with self.assertRaises(Exception) as caught:
                judge.judge(prompt_of(), "m", run)
            self.assertNotIsInstance(caught.exception, BudgetRefused)
        self.assertEqual([], budget.records)

    def test_a_reservation_is_used_once(self):
        budget = Budget()
        held = budget.reserve(1, 1, "m")
        budget.charge(held, 1, 1)
        with self.assertRaises(BudgetError):
            budget.charge(held, 1, 1)
        with self.assertRaises(BudgetError):
            budget.release(held)

    def test_bad_counts_are_refused(self):
        budget = Budget()
        for args in ((-1, 1), (1, True), (1.5, 1)):
            with self.assertRaises(BudgetError):
                budget.reserve(*args, "m")
        with self.assertRaises(BudgetError):
            BudgetedJudge(FakeJudge([]), budget, output_limit=0)


class Records(unittest.TestCase):
    def test_records_are_written_under_build_only(self):
        budget = Budget()
        BudgetedJudge(FakeJudge([answer(2, 3)]), budget, 10).judge(prompt_of(), "m", 1)
        with self.assertRaises(BudgetError):
            budget.write_records(os.path.join(tempfile.gettempdir(), "costs.json"))
        with tempfile.TemporaryDirectory() as tmp:
            target = os.path.join(tmp, "build")
            os.mkdir(target)
            path = os.path.join(target, "costs.json")
            budget.write_records(path)
            with open(path, encoding="utf-8") as handle:
                text = handle.read()
        self.assertEqual(budget.dumps_records(), text)
        self.assertEqual(budget.records, json.loads(text))
        self.assertEqual("[]\n", Budget().dumps_records())


class PricesFile(unittest.TestCase):
    def test_the_synthetic_file_loads_with_its_date(self):
        prices = budgets.load_prices(PRICES)
        self.assertEqual("2000-01-01", prices.date)
        self.assertEqual(Decimal("4"), prices.dollars("synthetic-small", 2000000, 1000000))
        self.assertEqual(Decimal("0.00004"), prices.dollars("synthetic-large", 2, 1))

    def test_dollars_are_recorded_with_the_prices_date_and_a_missing_model_is_refused_unmade(self):
        fake = FakeJudge([answer(1000000, 500000)])
        budget = Budget(prices=budgets.load_prices(PRICES))
        BudgetedJudge(fake, budget, 10).judge(prompt_of(), "synthetic-small", 1)
        self.assertEqual(("2", "2000-01-01"), (budget.records[0]["dollars"], budget.records[0]["prices_date"]))
        with self.assertRaises(PriceMissing):
            BudgetedJudge(fake, budget, 10).judge(prompt_of(), "unlisted", 1)
        self.assertEqual(1, len(fake.calls))

    def test_the_real_prices_file_is_absent_so_dollars_are_off(self):
        self.assertFalse(os.path.exists(os.path.join(ROOT, "prices.json")))

    def test_a_broken_file_is_refused(self):
        with open(PRICES, encoding="utf-8") as handle:
            good = json.load(handle)
        def broken(**change):
            data = dict(good)
            data.update(change)
            return json.dumps(data)
        for text in ("not json", "[]", broken(version=2), broken(date="1/1/2000"), broken(per_tokens=0), broken(models={}),
                     broken(models={"m": {"input": 1}}), broken(models={"m": {"input": -1, "output": 1}}),
                     broken(models={"m": {"input": True, "output": 1}}), broken(extra=1)):
            with self.assertRaises(BudgetError, msg=text[:40]):
                budgets.parse_prices(text)
        with self.assertRaises(BudgetError):
            budgets.load_prices(os.path.join(ROOT, "no-such-file.json"))


if __name__ == "__main__":
    unittest.main()
