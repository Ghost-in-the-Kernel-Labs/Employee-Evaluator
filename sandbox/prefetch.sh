#!/usr/bin/env sh
# Fetches every pinned piece of the sandbox image into sandbox/cache/ (git ignores it), each checked by
# its hash, so the image then builds with the network off (E-041): Node and Maven by the hashes their
# projects publish, the Python wheels by requirements.txt's, c8 and its packages by the lockfile's.
# Run by hand or by setup.sh; it runs nothing it fetches.
set -e
cd "$(dirname "$0")"
mkdir -p cache/wheels cache/npm
python3 fetch.py get https://nodejs.org/dist/v22.23.3/node-v22.23.3-linux-x64.tar.xz sha256 df450af89261115ef9f9e3830c3eeb2cc9213b63c720b1af623cb5dcbe2e02de cache/node.tar.xz
python3 fetch.py get https://archive.apache.org/dist/maven/maven-3/3.9.16/binaries/apache-maven-3.9.16-bin.tar.gz sha512 831a8591fe20c8243b1dbe7d71e3244f31d1665b0804b2e825e38cbbe5ce0cafb8338851f90780735568773e0a6cd07bbec107cda0b896b008b861075358b6f6 cache/maven.tar.gz
python3 -m pip download --no-input --disable-pip-version-check --require-hashes --only-binary=:all: --no-deps \
  --platform manylinux2014_x86_64 --platform manylinux_2_28_x86_64 --python-version 3.12 --implementation cp --abi cp312 \
  --dest cache/wheels -r requirements.txt
npm_root=$(mktemp -d)
cp node/package.json node/package-lock.json "$npm_root"/
npm ci --prefix "$npm_root" --ignore-scripts --no-audit --no-fund --cache "$PWD/cache/npm"
rm -rf "$npm_root"
echo "prefetch: sandbox/cache/ holds every pinned piece, each checked"
