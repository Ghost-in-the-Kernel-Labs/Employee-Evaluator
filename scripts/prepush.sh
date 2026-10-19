#!/usr/bin/env sh
# The pre-push check (E-020, E-021): fetch main so the push is a thin pack on top of it, refuse a
# stale kernel map, then the fast loop on a fresh clone of HEAD in the pinned image with the network
# off. What passes here is what CI runs.
set -e
cd "$(dirname "$0")/.."
git fetch origin main
if ! git merge-base --is-ancestor origin/main HEAD; then
  echo "prepush: origin/main is not behind HEAD; rebase onto it first (E-020)"; exit 1
fi
clone=$(mktemp -d)
git clone -q . "$clone"
if ! docker image inspect evaluator >/dev/null 2>&1; then docker build -q -t evaluator . >/dev/null; fi
docker run --rm --network none -u "$(id -u)" -v "$clone":/evaluator evaluator \
  sh -c 'gradle build --offline -q && gradle kernelMap --offline -q && git diff --quiet -- docs/KERNEL-MAP.md' \
  || { echo "prepush: the fast loop failed, or docs/KERNEL-MAP.md is stale (run gradle kernelMap and commit it)"; rm -rf "$clone"; exit 1; }
rm -rf "$clone"
echo "prepush: ok"
