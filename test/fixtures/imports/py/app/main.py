import os
import json
from app.util import total, unused_helper
from . import config


def main(xs):
    print(os.getcwd(), config.NAME)
    return total(xs)
