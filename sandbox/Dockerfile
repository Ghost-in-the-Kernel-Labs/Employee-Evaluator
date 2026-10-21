# The sandbox a submission's own tests run in (E-037, E-038): Python, Node and Java with their test
# and coverage tools, every piece pinned by digest or by hash, each public a week before it was pinned
# (E-033's cooldown). It builds with the network off (E-041): sandbox/prefetch.sh first fetches every
# piece into sandbox/cache/, and each is checked by its hash again here, before it is used:
#   sandbox/prefetch.sh && docker build --network none -f sandbox/Dockerfile -t evaluator-sandbox .
# The JDK is Temurin 21 from the kernel's own pinned image (Dockerfile).
FROM gradle:8.14.3-jdk21-noble@sha256:21bd311ed01360c189b8870c6b6e988199ff10f72d445d02fb39d3cff9da91d7 AS jdk

# python:3.12-slim-bookworm, built 2026-10-06.
FROM python:3.12-slim-bookworm@sha256:34386ef0cb081344d7ec1c103ba398e6e9f64e9ab3a1509accc92a4e24a07258

COPY --from=jdk /opt/java/openjdk /opt/java/openjdk
ENV JAVA_HOME=/opt/java/openjdk \
    PATH=/opt/evaluator/node/node_modules/.bin:/opt/node/bin:/opt/maven/bin:/opt/java/openjdk/bin:/usr/local/bin:/usr/bin:/bin

# Node v22.23.3 (2026-09-23) and Maven 3.9.16 (2026-05-13) by their projects' hashes; pytest, coverage.py
# and their dependencies by requirements.txt's; c8 and its packages by the lockfile's. Nothing is fetched.
RUN --mount=type=bind,source=sandbox,target=/sandbox,rw \
    python3 /sandbox/fetch.py unpack /sandbox/cache/node.tar.xz sha256 df450af89261115ef9f9e3830c3eeb2cc9213b63c720b1af623cb5dcbe2e02de /opt/node \
 && python3 /sandbox/fetch.py unpack /sandbox/cache/maven.tar.gz sha512 831a8591fe20c8243b1dbe7d71e3244f31d1665b0804b2e825e38cbbe5ce0cafb8338851f90780735568773e0a6cd07bbec107cda0b896b008b861075358b6f6 /opt/maven \
 && pip install --no-cache-dir --no-input --disable-pip-version-check --no-index --find-links /sandbox/cache/wheels --require-hashes --only-binary=:all: -r /sandbox/requirements.txt \
 && mkdir -p /opt/evaluator/node \
 && cp /sandbox/node/package.json /sandbox/node/package-lock.json /opt/evaluator/node/ \
 && cd /opt/evaluator/node \
 && npm ci --offline --ignore-scripts --no-audit --no-fund --cache /sandbox/cache/npm

COPY third_party/jacoco/org.jacoco.agent-0.8.15-runtime.jar /opt/evaluator/jacoco/jacocoagent.jar
COPY third_party/jacoco/org.jacoco.cli-0.8.15-nodeps.jar /opt/evaluator/jacoco/jacococli.jar

WORKDIR /work
