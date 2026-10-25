"""The no-network guard (TODO.md 2.2a): it catches what it must, and holds every file of agent-runner/."""
import os
import unittest

from agent_runner import guard

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))


class GuardCatches(unittest.TestCase):
    def test_each_banned_import_is_refused(self):
        for line in ("import socket", "import http.client", "from urllib import request", "from urllib.request import urlopen",
                     "import ssl", "import anthropic", "from anthropic import Anthropic", "import os, socket",
                     "import requests", "import importlib"):
            with self.subTest(line):
                self.assertTrue(guard.violations(line + "\n"), line)

    def test_a_dynamic_import_is_refused(self):
        self.assertTrue(guard.violations("__import__('socket')\n"))
        self.assertTrue(guard.violations("name = 'x'\n__import__(name)\n"))

    def test_an_environment_variable_naming_a_key_is_refused(self):
        for text in ("import os\nos.environ['ANTHROPIC_API_KEY']\n", "import os\nos.environ.get('MY_TOKEN')\n",
                     "import os\nos.getenv('secret')\n", "from os import getenv\ngetenv('API_KEY')\n",
                     "import os\nos.environ.pop('PASSWORD')\n"):
            with self.subTest(text):
                self.assertTrue(guard.violations(text), text)

    def test_the_whole_environment_or_a_hidden_name_is_refused(self):
        self.assertTrue(guard.violations("import os\ndict(os.environ)\n"))
        self.assertTrue(guard.violations("import os\nname = 'x'\nos.getenv(name)\n"))

    def test_plain_standard_library_code_passes(self):
        self.assertEqual([], guard.violations("import json, hashlib, os\nos.environ.get('HOME')\nx = 'socket'  # import socket\n"))
        self.assertEqual([], guard.violations("from . import guard\nimport ast\n"))

    def test_a_violation_names_its_line(self):
        self.assertEqual(["line 2: imports socket"], guard.violations("import json\nimport socket\n"))

    def test_only_transport_is_the_door(self):
        self.assertTrue(guard.is_door("agent_runner/transport.py"))
        self.assertFalse(guard.is_door("agent_runner/other.py"))
        self.assertFalse(guard.is_door("transport.py"))


class EveryFileIsHeld(unittest.TestCase):
    def test_no_file_but_the_door_reaches_the_network_or_a_key(self):
        problems = []
        seen = 0
        for folder, dirs, files in os.walk(ROOT):
            dirs[:] = [d for d in dirs if d not in ("__pycache__", ".venv")]
            for name in files:
                if not name.endswith(".py"):
                    continue
                path = os.path.join(folder, name)
                relative = os.path.relpath(path, ROOT)
                seen += 1
                if guard.is_door(relative):
                    continue
                with open(path, encoding="utf-8") as handle:
                    problems += ["%s %s" % (relative, v) for v in guard.violations(handle.read())]
        self.assertGreater(seen, 0)
        self.assertEqual([], problems)


if __name__ == "__main__":
    unittest.main()
