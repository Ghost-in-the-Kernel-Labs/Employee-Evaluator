from src import calc


def table(n):
    if n > 0:
        if n > 10:
            return calc.classify(n)
    return None
