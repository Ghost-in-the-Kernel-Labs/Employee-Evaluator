from src.notes import add, count


def test_add():
    assert count(add([], "a")) == 1
