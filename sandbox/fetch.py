"""Fetches a release tarball and unpacks it, each step refusing a file whose hash is not the one given.

Two steps, so the sandbox image builds with the network off (E-041):
  python3 fetch.py get <url> <sha256|sha512> <hex> <file>      before the build (sandbox/prefetch.sh)
  python3 fetch.py unpack <file> <sha256|sha512> <hex> <dir>   inside the build, from the cache
A tarball member that would land outside <dir> (an absolute path, a .., a link) is refused.
"""
import hashlib
import os
import sys
import tarfile
import urllib.request


def digest_of(path, algorithm):
    digest = hashlib.new(algorithm)
    with open(path, "rb") as f:
        for chunk in iter(lambda: f.read(1 << 20), b""):
            digest.update(chunk)
    return digest.hexdigest()


def check(path, algorithm, expected):
    actual = digest_of(path, algorithm)
    if actual != expected:
        sys.exit(f"{path}: {algorithm} {actual}, expected {expected}; refused")


def get(url, algorithm, expected, file):
    if os.path.exists(file) and digest_of(file, algorithm) == expected:
        return
    partial = file + ".part"
    with urllib.request.urlopen(url, timeout=120) as response, open(partial, "wb") as out:
        for chunk in iter(lambda: response.read(1 << 20), b""):
            out.write(chunk)
    try:
        check(partial, algorithm, expected)
    except SystemExit:
        os.remove(partial)
        raise
    os.replace(partial, file)


def unpack(file, algorithm, expected, target):
    check(file, algorithm, expected)
    with tarfile.open(file) as archive:
        members = []
        for member in archive.getmembers():
            parts = member.name.split("/", 1)
            if len(parts) < 2 or not parts[1]:
                continue
            member.name = parts[1]
            members.append(member)
        archive.extractall(target, members=members, filter="data")


if __name__ == "__main__":
    {"get": get, "unpack": unpack}[sys.argv[1]](*sys.argv[2:6])
