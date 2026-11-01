"""The rule-based scan of a dependency update (E-091, docs/how/update-a-dependency.md, Step 2).

    python3 -I -B dependency-review/dependency_scan.py OLD NEW [--out DIR]

OLD and NEW are two unpacked artifacts (an npm package, a wheel, a jar and its sources jar, a tarball), each in a folder
outside the repository. The scan pairs the hashed chunks of a bundle across their renames, diffs the two sides, and
runs fixed rules over the lines the new side adds, the install scripts and the new files. It prints one line per hit,
`HIT <file>:<line> [<rule>] <text>`, then a summary, and exits 0 with no hit, 1 with hits, 2 when it refuses to run.
With --out it also writes package.diff, scripts.diff, files.diff, pairs.txt and scan.txt into DIR (outside the repository).

A hit is not proof of malice: it names a hunk the reviewer must explain. No rule judges; nothing is run.

What the folders hold is a stranger's data. This file only reads bytes (standard library: os, re, difflib,
json, sys, pathlib), imports no process, network or loader module, executes nothing it reads, opens every file with
O_NOFOLLOW, and never follows a symlink: a symlink is reported as a hit, and is neither read nor walked into.

Rules over added lines (the posture's Step 2, written for npm and extended to the JVM and Python):
  install-script   a preinstall, install, postinstall or prepare script added or changed in a package.json
  python-hook      a setup.py or a .pth file added or changed (a .pth line starting with import runs at start-up)
  big-file         a new file over 100 KB
  minified         a new js, mjs, cjs or css file with a line over 1000 characters, or lines averaging over 200
  token            NPM_TOKEN, NODE_AUTH_TOKEN, GITHUB_TOKEN, GH_TOKEN, ANTHROPIC_KEY, AWS_*KEY, AZURE_*SECRET,
                   GOOGLE_APPLICATION_CREDENTIALS, .npmrc, id_rsa
  secret-scanner   trufflehog, gitleaks
  ci-foothold      .github/workflows, actions/checkout, git push
  self-publish     npm publish, npm whoami, registry.npmjs.org/-/
  second-stage     curl, wget, powershell, Invoke-WebRequest, bash -c
  exfiltration     webhook.site, pastebin, ngrok, a Discord webhook, a raw IP address
  hidden-code      eval(, new Function(, atob(, Buffer.from(..., 'base64')
  encoded-string   200 or more base64 characters in a row, or 40 or more \\x escapes
  env-dump         JSON.stringify(process.env), Object.keys(process.env)
  jvm-process      Runtime.getRuntime().exec, ProcessBuilder, URLClassLoader
  symlink          a symlink in either folder (not followed)
  unreadable       a file over 20 MB, or one that cannot be opened (not scanned)

A binary file (a NUL in its first 8 KB, such as a class file) is read as the printable strings in it, of 8 characters or
more; a string the new side has and the old side does not is an added line, its number the byte offset.
"""
import difflib
import json
import os
import re
import sys
from pathlib import Path

REPO = Path(__file__).resolve().parent.parent
MAX_READ = 20 * 1024 * 1024
BIG_FILE = 100 * 1024
DIFF_CONTENT_LIMIT = 1024 * 1024
LIFECYCLE = ("preinstall", "install", "postinstall", "prepare")
SNIPPET = 140

RULES = [
    ("token", re.compile(r"\b(?:NPM_TOKEN|NODE_AUTH_TOKEN|GITHUB_TOKEN|GH_TOKEN|ANTHROPIC_KEY)\b|AWS_\w*KEY|AZURE_\w*SECRET|GOOGLE_APPLICATION_CREDENTIALS|\.npmrc|\bid_rsa\b")),
    ("secret-scanner", re.compile(r"trufflehog|gitleaks", re.I)),
    ("ci-foothold", re.compile(r"\.github/workflows|actions/checkout|\bgit push\b")),
    ("self-publish", re.compile(r"\bnpm publish\b|\bnpm whoami\b|registry\.npmjs\.org/-/")),
    ("second-stage", re.compile(r"\b(?:curl|wget|powershell|Invoke-WebRequest)\b|\bbash -c\b", re.I)),
    ("exfiltration", re.compile(r"webhook\.site|pastebin|ngrok|discord(?:app)?\.com/api/webhooks|(?<![\d.])(?:\d{1,3}\.){3}\d{1,3}(?![\d.])", re.I)),
    ("hidden-code", re.compile(r"\beval\(|\bnew Function\(|\batob\(|Buffer\.from\([^)]*['\"]base64['\"]")),
    ("encoded-string", re.compile(r"[A-Za-z0-9+/]{200,}={0,2}|(?:\\x[0-9a-fA-F]{2}){40,}")),
    ("env-dump", re.compile(r"JSON\.stringify\(\s*process\.env\s*\)|Object\.keys\(\s*process\.env\s*\)")),
    ("jvm-process", re.compile(r"Runtime\.getRuntime\(\)\.exec|\bProcessBuilder\b|\bURLClassLoader\b")),
]
STRINGS = re.compile(rb"[\x20-\x7e]{8,}")
HASHED = re.compile(r"(?<=[.\-_])([A-Za-z0-9_-]{8})(?=\.[A-Za-z0-9]+$)")


class Refused(Exception):
    """The scan will not run on these arguments."""


class Side:
    """One unpacked folder: its regular files by relative path, its symlinks (never followed), and what could not be read."""

    def __init__(self, root):
        self.root = os.path.realpath(root)
        self.files = {}
        self.symlinks = []
        self.unreadable = []
        self._walk()

    def _walk(self):
        for here, dirs, names in os.walk(self.root, followlinks=False):
            for name in list(dirs):
                full = os.path.join(here, name)
                if os.path.islink(full):
                    self.symlinks.append(self._rel(full))  # os.walk(followlinks=False) does not descend into it
            for name in names:
                full = os.path.join(here, name)
                if os.path.islink(full):
                    self.symlinks.append(self._rel(full))
                elif os.path.isfile(full):
                    self.files[self._rel(full)] = full
        self.symlinks.sort()

    def _rel(self, full):
        return os.path.relpath(full, self.root).replace(os.sep, "/")

    def read(self, rel):
        """The bytes of a file, or None when it is over the limit or cannot be opened without following a link."""
        try:
            fd = os.open(self.files[rel], os.O_RDONLY | getattr(os, "O_NOFOLLOW", 0))
        except OSError:
            self.unreadable.append(rel)
            return None
        with os.fdopen(fd, "rb") as handle:
            if os.fstat(handle.fileno()).st_size > MAX_READ:
                self.unreadable.append(rel)
                return None
            return handle.read()


def unhashed(path):
    """A chunk's path with the hash of its name taken out (`dist/run.Cx91aQ0e.js` is `dist/run.js`), or the path itself when its name holds none."""
    folder, _, name = path.rpartition("/")
    match = HASHED.search(name)
    while match and not _looks_hashed(match.group(1)):
        match = HASHED.search(name, match.end())
    if not match:
        return path
    stripped = name[:match.start() - 1] + name[match.end():]
    return folder + "/" + stripped if folder else stripped


def _looks_hashed(segment):
    return any(c.isdigit() for c in segment) or (any(c.isupper() for c in segment) and any(c.islower() for c in segment))


def pair_chunks(old_files, new_files):
    """Renamed bundle chunks: {old path: new path} for each stripped name that names one removed file and one added file."""
    removed = [p for p in old_files if p not in new_files]
    added = [p for p in new_files if p not in old_files]
    by_old, by_new = {}, {}
    for path in removed:
        by_old.setdefault(unhashed(path), []).append(path)
    for path in added:
        by_new.setdefault(unhashed(path), []).append(path)
    return {olds[0]: by_new[key][0] for key, olds in by_old.items()
            if key in by_new and len(olds) == 1 and len(by_new[key]) == 1}


def is_binary(data):
    return b"\x00" in data[:8192]


def lines_of(data):
    return data.decode("utf-8", "replace").splitlines()


def printable_strings(data):
    """The printable runs of a binary file with their byte offsets."""
    return [(m.start(), m.group().decode("ascii")) for m in STRINGS.finditer(data)]


def added_lines(old, new):
    """The lines of `new` the old side does not have, as (number, text): all of a new file, the inserted or replaced lines of a changed one."""
    if old is None:
        old = b""
    if is_binary(new) or is_binary(old):
        seen = {text for _, text in printable_strings(old)}
        return [(offset, text) for offset, text in printable_strings(new) if text not in seen]
    before, after = lines_of(old), lines_of(new)
    if len(before) * len(after) > 4 * 10 ** 8:
        return [(n + 1, t) for n, t in enumerate(after)]
    found = []
    for tag, _, _, j1, j2 in difflib.SequenceMatcher(None, before, after, autojunk=False).get_opcodes():
        if tag in ("insert", "replace"):
            found += [(j + 1, after[j]) for j in range(j1, j2)]
    return found


def unified(path_old, path_new, old, new):
    """One file's part of package.diff (the content of a file over 1 MB is left out)."""
    if (old and len(old) > DIFF_CONTENT_LIMIT) or (new and len(new) > DIFF_CONTENT_LIMIT):
        return "Large file %s -> %s (not diffed)\n" % (path_old, path_new)
    if (old and is_binary(old)) or (new and is_binary(new)):
        return "Binary file %s -> %s differs\n" % (path_old, path_new)
    diff = difflib.unified_diff(lines_of(old or b""), lines_of(new or b""), "old/" + path_old, "new/" + path_new, lineterm="", n=3)
    return "".join(line + "\n" for line in diff)


def scripts_of(data):
    """The `scripts` object of a package.json, or {} when it is not one."""
    try:
        value = json.loads(data.decode("utf-8", "replace")).get("scripts", {})
    except (ValueError, AttributeError):
        return {}
    return value if isinstance(value, dict) else {}


def key_line(data, key):
    """The line of a package.json that names the script `key` (1 when not found)."""
    for number, text in enumerate(lines_of(data), 1):
        if '"%s"' % key in text:
            return number
    return 1


def shape_hits(path, old, new):
    """The hits that come from what a file is, not from its lines."""
    hits = []
    name = path.rsplit("/", 1)[-1]
    if name == "package.json":
        before, after = scripts_of(old or b""), scripts_of(new)
        for key in LIFECYCLE:
            if key in after and after[key] != before.get(key):
                hits.append((path, key_line(new, key), "install-script", "%s: %s" % (key, str(after[key])[:SNIPPET])))
    if old is None or old != new:
        if name == "setup.py" or name.endswith(".pth"):
            hits.append((path, 1, "python-hook", "%s %s" % (name, "added" if old is None else "changed")))
    if old is None:
        if len(new) > BIG_FILE:
            hits.append((path, 1, "big-file", "new file of %d bytes" % len(new)))
        if path.rsplit(".", 1)[-1] in ("js", "mjs", "cjs", "css") and not is_binary(new):
            rows = lines_of(new)
            longest = max((len(r) for r in rows), default=0)
            if longest > 1000 or (rows and sum(len(r) for r in rows) / len(rows) > 200):
                hits.append((path, 1, "minified", "new file with a line of %d characters" % longest))
    return hits


def rule_hits(path, lines):
    hits = []
    for number, text in lines:
        for rule, pattern in RULES:
            match = pattern.search(text)
            if match:
                start = max(0, match.start() - 30)
                hits.append((path, number, rule, text[start:start + SNIPPET].strip()))
    return hits


def scan(old_root, new_root):
    """The scan of two unpacked folders: a dict with the hits, the pairs, the diff texts and the counts."""
    old, new = Side(old_root), Side(new_root)
    pairs = pair_chunks(old.files, new.files)
    renamed = {v: k for k, v in pairs.items()}
    hits, diffs, listing = [], [], []
    for path in old.symlinks:
        hits.append(("old/" + path, 1, "symlink", "not followed"))
    for path in new.symlinks:
        hits.append((path, 1, "symlink", "not followed"))
    changed = 0
    for path in sorted(new.files):
        before_path = renamed.get(path, path)
        before = old.read(before_path) if before_path in old.files else None
        after = new.read(path)
        if after is None:
            hits.append((path, 1, "unreadable", "over %d MB or not openable, not scanned" % (MAX_READ // (1024 * 1024))))
            continue
        if before is not None and before == after:
            continue
        changed += 1
        listing.append(("R %s -> %s" % (before_path, path)) if before_path != path else ("M " + path if before is not None else "A " + path))
        diffs.append(unified(before_path, path, before, after))
        hits += shape_hits(path, before, after)
        hits += rule_hits(path, added_lines(before, after))
    gone = [p for p in sorted(old.files) if p not in new.files and p not in pairs]
    listing += ["D " + p for p in gone]
    scripts = []
    for path in sorted(new.files):
        if path.rsplit("/", 1)[-1] == "package.json":
            before = old.read(renamed.get(path, path)) if renamed.get(path, path) in old.files else b""
            scripts.append("%s: %s -> %s" % (path, json.dumps(scripts_of(before or b""), sort_keys=True), json.dumps(scripts_of(new.read(path) or b""), sort_keys=True)))
    return {"hits": sorted(set(hits), key=lambda h: (h[0], h[1], h[2], h[3])), "pairs": pairs, "package.diff": "".join(diffs),
            "files.diff": "\n".join(listing) + "\n", "scripts.diff": "\n".join(scripts) + "\n", "changed": changed, "removed": len(gone)}


def check_outside(path, what):
    real = Path(os.path.realpath(path))
    if real == REPO or REPO in real.parents:
        raise Refused("%s %s is inside the repository: unpack it in a new empty folder outside it" % (what, path))
    if not real.is_dir():
        raise Refused("%s %s is not a folder" % (what, path))


def report(result):
    lines = ["HIT %s:%d [%s] %s" % h for h in result["hits"]]
    lines.append("scan: %d hit(s), %d chunk pair(s), %d file(s) added or changed, %d removed" % (len(result["hits"]), len(result["pairs"]), result["changed"], result["removed"]))
    return "\n".join(lines) + "\n"


def main(argv):
    out = None
    args = list(argv)
    if "--out" in args:
        at = args.index("--out")
        if at + 1 >= len(args):
            raise Refused("--out needs a folder")
        out = args[at + 1]
        del args[at:at + 2]
    if len(args) != 2:
        raise Refused("usage: dependency_scan.py OLD NEW [--out DIR]")
    check_outside(args[0], "OLD")
    check_outside(args[1], "NEW")
    if out:
        check_outside_target(out)
    result = scan(args[0], args[1])
    text = report(result)
    sys.stdout.write(text)
    if out:
        os.makedirs(out, exist_ok=True)
        pairs = "".join("%s -> %s\n" % kv for kv in sorted(result["pairs"].items()))
        for name, body in (("package.diff", result["package.diff"]), ("scripts.diff", result["scripts.diff"]),
                           ("files.diff", result["files.diff"]), ("pairs.txt", pairs), ("scan.txt", text)):
            Path(out, name).write_text(body, encoding="utf-8")
    return 1 if result["hits"] else 0


def check_outside_target(path):
    real = Path(os.path.realpath(path))
    if real == REPO or REPO in real.parents:
        raise Refused("--out %s is inside the repository" % path)


if __name__ == "__main__":
    try:
        sys.exit(main(sys.argv[1:]))
    except Refused as refusal:
        sys.stderr.write("refused: %s\n" % refusal)
        sys.exit(2)
