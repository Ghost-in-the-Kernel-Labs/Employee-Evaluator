from src import calc


def label(n):
    if n > 100:
        return "big"
    return calc.classify(n)
