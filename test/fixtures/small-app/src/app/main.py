"""The app's entry point: totals a list of prices."""

from app.util import total


def main(prices):
    if not prices:
        return 0

    return total(prices)
