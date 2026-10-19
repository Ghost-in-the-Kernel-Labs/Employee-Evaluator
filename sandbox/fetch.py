"""Downloads one release tarball, refuses it unless its hash is the one given, and unpacks it.

Used only while the sandbox image is built (E-037): python3 fetch.py <url> <sha256|sha512> <hex> <dir>.
A tarball member that would land outside <dir> (an absolute path, a .., a link) is refused.
"""
import hashlib
import sys
import tarfile
import tempfile
import urllib.request


def main(url, algorithm, expected, target):
    digest = hashlib.new(algorithm)
    with tempfile.TemporaryFile() as download:
        with urllib.request.urlopen(url, timeout=120) as response:
            for chunk in iter(lambda: response.read(1 << 20), b""):
                digest.update(chunk)
                download.write(chunk)
        if digest.hexdigest() != expected:
            sys.exit(f"{url}: {algorithm} {digest.hexdigest()}, expected {expected}; refused")
        download.seek(0)
        with tarfile.open(fileobj=download) as archive:
            members = []
            for member in archive.getmembers():
                parts = member.name.split("/", 1)
                if len(parts) < 2 or not parts[1]:
                    continue
                member.name = parts[1]
                members.append(member)
            archive.extractall(target, members=members, filter="data")


if __name__ == "__main__":
    main(*sys.argv[1:5])
