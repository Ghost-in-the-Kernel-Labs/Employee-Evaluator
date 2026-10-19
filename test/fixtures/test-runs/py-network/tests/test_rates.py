import socket


def test_offline():
    assert 1 + 1 == 2


def test_fetches_rates():
    socket.getaddrinfo("registry.evaluator.invalid", 443)
