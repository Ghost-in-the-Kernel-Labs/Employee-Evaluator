# The reviewed-versions record: what each pin is, and the last version this repository read

Every artifact the repository pins from a public registry has one row below (E-091): a vendored jar
(`third_party/MANIFEST.md`), an npm package of `sandbox/node/package-lock.json`, a Python wheel of
`sandbox/requirements.txt`, a tarball of `sandbox/prefetch.sh`, a base image or a GitHub Action, each
by the version or the first twelve hex digits of the hash or digest it is pinned by. The next update of
an artifact reads from its "Last reviewed" cell (`docs/how/update-a-dependency.md`): the old side of
its diff is that version, never the last one installed, so a payload spread over several small releases
is read as one change. `test/DependencyReviewTest.java` fails on a pin with no row, a row for something
no longer pinned, and a row that says nothing of its review. A pin changes its row in the same commit.

"Last reviewed" holds the version (or hash prefix) last read and how: `read` (the whole range, diffed),
`large bundle` (compiled or bundled code that no reader can follow, reviewed the large-bundle way) or
`whole package` (a new dependency, read in full). Every row starts as "pinned before E-091, not yet
reviewed by this posture": the pin is taken as the baseline of the next review, not as something read.

## Pins

| Registry | Artifact | Pinned | Last reviewed |
| --- | --- | --- | --- |
| maven | jacoco/org.jacoco.agent-0.8.15-runtime.jar | fb5b0036a089 | pinned before E-091, not yet reviewed by this posture |
| maven | jacoco/org.jacoco.cli-0.8.15-nodeps.jar | d2b74b20b415 | pinned before E-091, not yet reviewed by this posture |
| maven | junit/apiguardian-api-1.1.2.jar | b509448ac506 | pinned before E-091, not yet reviewed by this posture |
| maven | junit/junit-jupiter-api-5.11.4.jar | ab83ef9e51ac | pinned before E-091, not yet reviewed by this posture |
| maven | junit/junit-jupiter-engine-5.11.4.jar | cdf8ac59f3fa | pinned before E-091, not yet reviewed by this posture |
| maven | junit/junit-platform-commons-1.11.4.jar | 9edd969b0d06 | pinned before E-091, not yet reviewed by this posture |
| maven | junit/junit-platform-engine-1.11.4.jar | b1dd998f64f9 | pinned before E-091, not yet reviewed by this posture |
| maven | junit/junit-platform-launcher-1.11.4.jar | d7430bd029e7 | pinned before E-091, not yet reviewed by this posture |
| maven | junit/opentest4j-1.3.0.jar | 48e2df636cab | pinned before E-091, not yet reviewed by this posture |
| maven | pitest/asm-9.11.jar | 5fe3b469b864 | pinned before E-091, not yet reviewed by this posture |
| maven | pitest/asm-analysis-9.11.jar | a1421c20978c | pinned before E-091, not yet reviewed by this posture |
| maven | pitest/asm-commons-9.11.jar | fcff37e6a022 | pinned before E-091, not yet reviewed by this posture |
| maven | pitest/asm-tree-9.11.jar | da8ad24d0bf7 | pinned before E-091, not yet reviewed by this posture |
| maven | pitest/asm-util-9.11.jar | d5a23a677f31 | pinned before E-091, not yet reviewed by this posture |
| maven | pitest/commons-lang3-3.18.0.jar | 4eeeae8d20c0 | pinned before E-091, not yet reviewed by this posture |
| maven | pitest/commons-text-1.14.0.jar | 121fce228291 | pinned before E-091, not yet reviewed by this posture |
| maven | pitest/pitest-1.30.1.jar | 893af485bf0c | pinned before E-091, not yet reviewed by this posture |
| maven | pitest/pitest-command-line-1.30.1.jar | e7fe206c7c6e | pinned before E-091, not yet reviewed by this posture |
| maven | pitest/pitest-entry-1.30.1.jar | 3a05342c7efa | pinned before E-091, not yet reviewed by this posture |
| maven | pitest/pitest-junit5-plugin-1.2.3.jar | d3e2fcc8db5f | pinned before E-091, not yet reviewed by this posture |
| maven | tree-sitter/tree-sitter-0.26.6.jar | 81b113176c47 | pinned before E-091, not yet reviewed by this posture |
| maven | tree-sitter/tree-sitter-python-0.25.0.jar | 2cf56d951790 | pinned before E-091, not yet reviewed by this posture |
| maven | tree-sitter/tree-sitter-tsx-0.23.2.jar | a5eb98db7157 | pinned before E-091, not yet reviewed by this posture |
| maven | tree-sitter/tree-sitter-typescript-0.23.2.jar | 5c3d1716441f | pinned before E-091, not yet reviewed by this posture |
| npm | @bcoe/v8-coverage | 1.0.2 | pinned before E-091, not yet reviewed by this posture |
| npm | @istanbuljs/schema | 0.1.6 | pinned before E-091, not yet reviewed by this posture |
| npm | @jridgewell/resolve-uri | 3.1.2 | pinned before E-091, not yet reviewed by this posture |
| npm | @jridgewell/sourcemap-codec | 1.6.0 | pinned before E-091, not yet reviewed by this posture |
| npm | @jridgewell/trace-mapping | 0.3.31 | pinned before E-091, not yet reviewed by this posture |
| npm | @types/istanbul-lib-coverage | 2.0.6 | pinned before E-091, not yet reviewed by this posture |
| npm | ansi-regex | 6.4.0 | pinned before E-091, not yet reviewed by this posture |
| npm | ansi-styles | 6.2.3 | pinned before E-091, not yet reviewed by this posture |
| npm | balanced-match | 4.0.4 | pinned before E-091, not yet reviewed by this posture |
| npm | brace-expansion | 5.0.12 | pinned before E-091, not yet reviewed by this posture |
| npm | c8 | 12.0.0 | pinned before E-091, not yet reviewed by this posture |
| npm | cliui | 9.0.1 | pinned before E-091, not yet reviewed by this posture |
| npm | cliui/node_modules/string-width | 7.2.0 | pinned before E-091, not yet reviewed by this posture |
| npm | convert-source-map | 2.0.0 | pinned before E-091, not yet reviewed by this posture |
| npm | cross-spawn | 7.0.6 | pinned before E-091, not yet reviewed by this posture |
| npm | emoji-regex | 10.6.0 | pinned before E-091, not yet reviewed by this posture |
| npm | escalade | 3.2.0 | pinned before E-091, not yet reviewed by this posture |
| npm | find-up | 5.0.0 | pinned before E-091, not yet reviewed by this posture |
| npm | foreground-child | 3.3.1 | pinned before E-091, not yet reviewed by this posture |
| npm | get-caller-file | 2.0.5 | pinned before E-091, not yet reviewed by this posture |
| npm | get-east-asian-width | 1.7.0 | pinned before E-091, not yet reviewed by this posture |
| npm | glob | 13.0.6 | pinned before E-091, not yet reviewed by this posture |
| npm | has-flag | 4.0.0 | pinned before E-091, not yet reviewed by this posture |
| npm | html-escaper | 2.0.2 | pinned before E-091, not yet reviewed by this posture |
| npm | isexe | 2.0.0 | pinned before E-091, not yet reviewed by this posture |
| npm | istanbul-lib-coverage | 3.2.2 | pinned before E-091, not yet reviewed by this posture |
| npm | istanbul-lib-report | 3.0.1 | pinned before E-091, not yet reviewed by this posture |
| npm | istanbul-reports | 3.2.0 | pinned before E-091, not yet reviewed by this posture |
| npm | locate-path | 6.0.0 | pinned before E-091, not yet reviewed by this posture |
| npm | lru-cache | 11.5.3 | pinned before E-091, not yet reviewed by this posture |
| npm | make-dir | 4.0.0 | pinned before E-091, not yet reviewed by this posture |
| npm | minimatch | 10.2.6 | pinned before E-091, not yet reviewed by this posture |
| npm | minipass | 7.1.3 | pinned before E-091, not yet reviewed by this posture |
| npm | p-limit | 3.1.0 | pinned before E-091, not yet reviewed by this posture |
| npm | p-locate | 5.0.0 | pinned before E-091, not yet reviewed by this posture |
| npm | path-exists | 4.0.0 | pinned before E-091, not yet reviewed by this posture |
| npm | path-key | 3.1.1 | pinned before E-091, not yet reviewed by this posture |
| npm | path-scurry | 2.0.2 | pinned before E-091, not yet reviewed by this posture |
| npm | semver | 7.8.5 | pinned before E-091, not yet reviewed by this posture |
| npm | shebang-command | 2.0.0 | pinned before E-091, not yet reviewed by this posture |
| npm | shebang-regex | 3.0.0 | pinned before E-091, not yet reviewed by this posture |
| npm | signal-exit | 4.1.0 | pinned before E-091, not yet reviewed by this posture |
| npm | string-width | 8.3.0 | pinned before E-091, not yet reviewed by this posture |
| npm | strip-ansi | 7.2.0 | pinned before E-091, not yet reviewed by this posture |
| npm | supports-color | 7.2.0 | pinned before E-091, not yet reviewed by this posture |
| npm | test-exclude | 8.0.0 | pinned before E-091, not yet reviewed by this posture |
| npm | v8-to-istanbul | 9.3.0 | pinned before E-091, not yet reviewed by this posture |
| npm | which | 2.0.2 | pinned before E-091, not yet reviewed by this posture |
| npm | wrap-ansi | 9.0.2 | pinned before E-091, not yet reviewed by this posture |
| npm | wrap-ansi/node_modules/string-width | 7.2.0 | pinned before E-091, not yet reviewed by this posture |
| npm | y18n | 5.0.8 | pinned before E-091, not yet reviewed by this posture |
| npm | yargs | 18.2.0 | pinned before E-091, not yet reviewed by this posture |
| npm | yargs-parser | 21.1.1 | pinned before E-091, not yet reviewed by this posture |
| npm | yargs/node_modules/yargs-parser | 22.0.0 | pinned before E-091, not yet reviewed by this posture |
| npm | yocto-queue | 0.1.0 | pinned before E-091, not yet reviewed by this posture |
| pypi | coverage | 7.16.2 | pinned before E-091, not yet reviewed by this posture |
| pypi | iniconfig | 2.3.1 | pinned before E-091, not yet reviewed by this posture |
| pypi | packaging | 26.3 | pinned before E-091, not yet reviewed by this posture |
| pypi | pluggy | 1.6.0 | pinned before E-091, not yet reviewed by this posture |
| pypi | pygments | 2.21.0 | pinned before E-091, not yet reviewed by this posture |
| pypi | pytest | 9.1.1 | pinned before E-091, not yet reviewed by this posture |
| tarball | apache-maven-3.9.16-bin.tar.gz | 831a8591fe20 | pinned before E-091, not yet reviewed by this posture |
| tarball | node-v22.23.3-linux-x64.tar.xz | df450af89261 | pinned before E-091, not yet reviewed by this posture |
| image | gradle:8.14.3-jdk21-noble | 21bd311ed013 | pinned before E-091, not yet reviewed by this posture |
| image | python:3.12-slim-bookworm | 34386ef0cb08 | pinned before E-091, not yet reviewed by this posture |
| action | actions/checkout | 11d5960a3267 | pinned before E-091, not yet reviewed by this posture |

## Reviews

One line per update, newest last: the artifact, the range read (last reviewed to new), the reviewer's
verdict and where its evidence is (the commit), the reputation check's report, each layer's result, and
whether the update went in automatically or which question Ghost answered and how. None yet.

| Date | Artifact | Range read | Verdict | Reputation | Layers | Decision |
| --- | --- | --- | --- | --- | --- | --- |
