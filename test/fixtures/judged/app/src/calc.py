from src import util


def add(a, b):
    return util.double(a) + b


def classify(n):
    if n < 0:
        return "neg"
    return "pos"
