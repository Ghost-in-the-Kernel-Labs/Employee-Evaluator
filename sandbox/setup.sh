#!/usr/bin/env sh
# Sets up the sandbox on a machine with Docker, by hand (E-037); nothing runs it on its own. Builds the
# kernel's image and the sandbox's (its pieces prefetched and checked, its build offline, E-041), makes the registries network (internal: no route out), and starts the
# registries proxy on it, joined to the default network as its one way out. Run from the repository's root.
set -e
cd "$(dirname "$0")/.."
docker build -q -t evaluator .
sandbox/prefetch.sh
docker build -q --network none -f sandbox/Dockerfile -t evaluator-sandbox .
docker run --rm --network none -u "$(id -u)" -v "$PWD":/evaluator evaluator gradle classes --offline -q
docker network inspect evaluator-registries >/dev/null 2>&1 || docker network create --internal evaluator-registries
docker rm -f registries-proxy >/dev/null 2>&1 || true
docker run -d --name registries-proxy --network evaluator-registries --read-only --cap-drop ALL \
  --security-opt no-new-privileges --user 65534:65534 --memory 256m --pids-limit 128 \
  --mount "type=bind,source=$PWD/build/classes/java/main,target=/proxy,readonly" \
  evaluator java -cp /proxy evaluator.analyze.RegistriesProxy 3128
docker network connect bridge registries-proxy
echo "sandbox: ready (image evaluator-sandbox, network evaluator-registries, proxy registries-proxy:3128)"
