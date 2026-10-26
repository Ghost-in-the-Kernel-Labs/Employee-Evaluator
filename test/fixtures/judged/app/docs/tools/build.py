from src import util


def build(items):
    total = 0
    for item in items:
        total += util.double(item)
    return total
