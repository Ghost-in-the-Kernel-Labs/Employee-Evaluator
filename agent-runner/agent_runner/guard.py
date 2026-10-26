"""The no-network guard (E-003, E-013, E-014): what a source file of agent-runner/ may not do.

The one door to the network is `transport.py` (TODO.md 2.2l), the counterpart of MOD-05 in the kernel. Every other
file may not import a network or model-client module, and may not read an environment variable whose name says it
holds a key. The files are parsed, never run, so a name inside a string or comment is not an import.

One more file is allowed one more module, and nothing else: the loopback test of the door, `tests/test_transport.py`, may
import `http` (a server on 127.0.0.1 is the only way to test the door without the internet). It may not read a key variable,
and may not import any other banned module; it sets the fake key by `patch.dict("os.environ", ...)`, a name in a string.
"""
import ast

# The five TODO.md 2.2a names, then the other network and model-client modules a file could reach for.
BANNED_MODULES = frozenset({
    "socket", "http", "urllib", "ssl", "anthropic",
    "socketserver", "ftplib", "smtplib", "poplib", "imaplib", "telnetlib", "xmlrpc",
    "urllib3", "requests", "httpx", "aiohttp", "websockets", "openai",
    "importlib",  # a way round this guard
})
KEY_WORDS = ("KEY", "TOKEN", "SECRET", "PASSWORD", "CREDENTIAL", "AUTH")
ENV_NAMES = frozenset({"environ", "environb", "getenv", "getenvb"})
DOOR = "agent_runner/transport.py"
LOOPBACK_TEST = "tests/test_transport.py"
LOOPBACK_MODULES = frozenset({"http"})


def _banned(module, allowed=frozenset()):
    root = module.split(".")[0]
    return root in BANNED_MODULES and root not in allowed


def _names_key(text):
    return any(word in text.upper() for word in KEY_WORDS)


def violations(source, allowed=frozenset()):
    """What `source` (the text of one Python file) does that agent-runner/ forbids, as a list of messages.

    `allowed` names banned modules the file may import (`LOOPBACK_MODULES` for the loopback test, nothing otherwise)."""
    tree = ast.parse(source)
    found = []
    consumed = set()  # environment accesses whose key is a constant that names no key
    for node in ast.walk(tree):
        if isinstance(node, ast.Import):
            found += ["line %d: imports %s" % (node.lineno, a.name) for a in node.names if _banned(a.name, allowed)]
        elif isinstance(node, ast.ImportFrom):
            if node.level == 0 and node.module and _banned(node.module, allowed):
                found.append("line %d: imports from %s" % (node.lineno, node.module))
        elif isinstance(node, ast.Call):
            callee = node.func.id if isinstance(node.func, ast.Name) else getattr(node.func, "attr", "")
            first = node.args[0] if node.args else None
            if callee in ("__import__", "import_module") and first is not None:
                if not (isinstance(first, ast.Constant) and isinstance(first.value, str) and not _banned(first.value, allowed)):
                    found.append("line %d: a dynamic import, a way round the import guard" % node.lineno)
            if callee in ENV_NAMES or (callee in ("get", "pop", "setdefault") and isinstance(node.func, ast.Attribute) and _is_env(node.func.value)):
                target = node.func if callee in ENV_NAMES else node.func.value
                _env_access(node, target, first, found, consumed)
        elif isinstance(node, ast.Subscript) and _is_env(node.value):
            _env_access(node, node.value, node.slice, found, consumed)
    for node in ast.walk(tree):
        if _env_ref(node) and id(node) not in consumed:
            found.append("line %d: uses the whole environment, not one named variable" % node.lineno)
    return sorted(set(found), key=lambda m: (int(m.split(":")[0].split()[1]), m))


def _env_ref(node):
    return (isinstance(node, ast.Attribute) and node.attr in ENV_NAMES) or (isinstance(node, ast.Name) and node.id in ENV_NAMES)


def _is_env(node):
    return _env_ref(node)


def _env_access(node, target, key, found, consumed):
    consumed.add(id(target))
    if not (isinstance(key, ast.Constant) and isinstance(key.value, str)):
        found.append("line %d: reads an environment variable by a name it cannot show" % node.lineno)
    elif _names_key(key.value):
        found.append("line %d: reads the environment variable %s, which names a key" % (node.lineno, key.value))


def is_door(relative_path):
    """True for the one file allowed the network, the path relative to agent-runner/."""
    return relative_path.replace("\\", "/") == DOOR


def is_loopback_test(relative_path):
    """True for the one test file allowed `LOOPBACK_MODULES`, the path relative to agent-runner/."""
    return relative_path.replace("\\", "/") == LOOPBACK_TEST
