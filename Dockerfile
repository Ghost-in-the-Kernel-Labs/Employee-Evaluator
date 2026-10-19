# Employee Evaluator's environment (E-021, docs/AGENT.md AGT-07): one image, pinned by digest, in
# which a clean clone builds and tests with the network off. An agent never installs tooling.
#
# The base pins Ubuntu 24.04, JDK 21 (Temurin) and Gradle 8.14.3 in one digest, the same image
# Katana builds in. git is for history/ (the submission's history is read with git itself).
FROM gradle:8.14.3-jdk21-noble@sha256:21bd311ed01360c189b8870c6b6e988199ff10f72d445d02fb39d3cff9da91d7

RUN apt-get update \
 && apt-get install -y --no-install-recommends git \
 && rm -rf /var/lib/apt/lists/* \
 && git --version

# Any user may run the image over a mounted clone; Gradle keeps its state off the clone.
ENV GRADLE_USER_HOME=/tmp/gradle-home
WORKDIR /evaluator
CMD ["gradle", "build", "--offline"]
