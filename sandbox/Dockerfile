# The sandbox a submission's own tests run in (E-037, E-038): Python, Node and Java with their test
# and coverage tools, every piece pinned by digest or by hash, each public a week before it was pinned
# (E-033's cooldown). Built from the repository's root:
#   docker build -f sandbox/Dockerfile -t evaluator-sandbox .
# The JDK is Temurin 21 from the kernel's own pinned image (Dockerfile); Node and Maven are their
# release tarballs, checked by sandbox/fetch.py before they are unpacked.
FROM gradle:8.14.3-jdk21-noble@sha256:21bd311ed01360c189b8870c6b6e988199ff10f72d445d02fb39d3cff9da91d7 AS jdk

# python:3.12-slim-bookworm, built 2026-10-06.
FROM python:3.12-slim-bookworm@sha256:34386ef0cb081344d7ec1c103ba398e6e9f64e9ab3a1509accc92a4e24a07258

COPY --from=jdk /opt/java/openjdk /opt/java/openjdk
COPY sandbox/fetch.py /tmp/fetch.py
# Node v22.23.3 (2026-09-23) and Maven 3.9.16 (2026-05-13), by the hashes their projects publish.
RUN python3 /tmp/fetch.py https://nodejs.org/dist/v22.23.3/node-v22.23.3-linux-x64.tar.xz sha256 df450af89261115ef9f9e3830c3eeb2cc9213b63c720b1af623cb5dcbe2e02de /opt/node \
 && python3 /tmp/fetch.py https://archive.apache.org/dist/maven/maven-3/3.9.16/binaries/apache-maven-3.9.16-bin.tar.gz sha512 831a8591fe20c8243b1dbe7d71e3244f31d1665b0804b2e825e38cbbe5ce0cafb8338851f90780735568773e0a6cd07bbec107cda0b896b008b861075358b6f6 /opt/maven \
 && rm /tmp/fetch.py

COPY sandbox/requirements.txt /opt/evaluator/python/requirements.txt
RUN pip install --no-cache-dir --no-input --disable-pip-version-check --require-hashes --only-binary=:all: -r /opt/evaluator/python/requirements.txt

ENV JAVA_HOME=/opt/java/openjdk \
    PATH=/opt/evaluator/node/node_modules/.bin:/opt/node/bin:/opt/maven/bin:/opt/java/openjdk/bin:/usr/local/bin:/usr/bin:/bin
COPY sandbox/node/package.json sandbox/node/package-lock.json /opt/evaluator/node/
RUN cd /opt/evaluator/node && npm ci --ignore-scripts --no-audit --no-fund && npm cache clean --force

COPY third_party/jacoco/org.jacoco.agent-0.8.15-runtime.jar /opt/evaluator/jacoco/jacocoagent.jar
COPY third_party/jacoco/org.jacoco.cli-0.8.15-nodeps.jar /opt/evaluator/jacoco/jacococli.jar

WORKDIR /work
