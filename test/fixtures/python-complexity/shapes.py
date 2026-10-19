def sum_of_primes(limit):
    total = 0
    for i in range(1, limit + 1):
        for j in range(2, i):
            if i % j == 0:
                break
        else:
            total += i
    return total


def get_words(number):
    match number:
        case 1:
            return "one"
        case 2:
            return "a couple"
        case 3:
            return "a few"
        case _:
            return "lots"


def grade(score):
    if score > 90:
        return "A"
    elif score > 80:
        return "B"
    else:
        return "C"


def logic(a, b, c, d):
    return a and b and c or d and not (a or b)


def nested(xs):
    n = 0
    for x in xs:
        if x > 0:
            n += 2 if x > 10 else 1
    return n


def try_it():
    try:
        run()
    except ValueError as e:
        if e.args is None:
            return


def run():
    pass


def later(x):
    return lambda: x if x > 0 else 0


def fact(n):
    return 1 if n <= 1 else n * fact(n - 1)


def evens(xs):
    return [x for x in xs if x % 2 == 0]


def outer(xs):
    def inner(x):
        if x:
            return 1
        return 0

    return [inner(x) for x in xs]


class Shape:
    def area(self):
        while self.dirty:
            self.dirty = False
        return 0

    def area(self, scale):
        return scale
