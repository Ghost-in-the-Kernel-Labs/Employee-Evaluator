from src import calc


def test_add():
    assert calc.add(1, 2) == 4


def test_classify():
    assert calc.classify(-1) == "neg"
