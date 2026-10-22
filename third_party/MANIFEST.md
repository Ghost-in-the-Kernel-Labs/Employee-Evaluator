# Third-party code, vendored so a clean clone builds with the network off (E-021)

Each jar is the Maven Central artifact named, recorded here by SHA-256, with its license text beside
it. Nothing under a license that forbids redistribution, and no GPL code, enters this folder. "Ships"
says whether the jar reaches an applicant or an employer: nothing here does.

| Jar | License | Used by | Ships | SHA-256 |
| --- | --- | --- | --- | --- |
| `junit/apiguardian-api-1.1.2.jar` | Apache-2.0 (`junit/LICENSE-apiguardian`) | tests only | no | `b509448ac506d607319f182537f0b35d71007582ec741832a1f111e5b5b70b38` |
| `junit/junit-jupiter-api-5.11.4.jar` | EPL-2.0 (`junit/LICENSE-junit.md`) | tests only | no | `ab83ef9e51ac4597d59d26b4b58812129550e2f579a404c8af7d09f5ce5b4293` |
| `junit/junit-jupiter-engine-5.11.4.jar` | EPL-2.0 (`junit/LICENSE-junit.md`) | tests only | no | `cdf8ac59f3fad774ca738ad03890950eeb91833ef0e8908753177edd26f1581c` |
| `junit/junit-platform-commons-1.11.4.jar` | EPL-2.0 (`junit/LICENSE-junit.md`) | tests only | no | `9edd969b0d0670c54105bc91ae79bd1c6f503e12115faba82073b84c86bbc334` |
| `junit/junit-platform-engine-1.11.4.jar` | EPL-2.0 (`junit/LICENSE-junit.md`) | tests only | no | `b1dd998f64f9acadc15966d9cd3d08074662677b3e390f0a38fcbf0bb4c72330` |
| `junit/junit-platform-launcher-1.11.4.jar` | EPL-2.0 (`junit/LICENSE-junit.md`) | tests only | no | `d7430bd029e7fcced53ee445e4d2d1a8a1e043ea4c4df43b6335a857f79761ae` |
| `junit/opentest4j-1.3.0.jar` | Apache-2.0 (`junit/LICENSE-opentest4j`) | tests only | no | `48e2df636cab6563ced64dcdff8abb2355627cb236ef0bf37598682ddf742f1b` |
| `tree-sitter/tree-sitter-0.26.6.jar` | MIT (`tree-sitter/LICENSE-tree-sitter-ng`; the bundled tree-sitter library: `tree-sitter/LICENSE-tree-sitter`) | `analyze/` (E-028) | no | `81b113176c47c6adef9f6e3f7b8ca9f8141c4a8627f69d4423fc2e130a8ba74e` |
| `tree-sitter/tree-sitter-python-0.25.0.jar` | MIT (`tree-sitter/LICENSE-tree-sitter-python`) | `analyze/` (E-028) | no | `2cf56d951790ca5fbbd3fa64d11cca74e7d7bffc17d445050f9965c80875d640` |
| `tree-sitter/tree-sitter-typescript-0.23.2.jar` | MIT (`tree-sitter/LICENSE-tree-sitter-typescript`) | `analyze/` (E-028) | no | `5c3d1716441fbda55293b8632f9e691733bd3035a582da3936599d2309183bf0` |
| `jacoco/org.jacoco.agent-0.8.15-runtime.jar` | EPL-2.0 (`jacoco/LICENSE-jacoco.md`) | the sandbox image (E-037) | no | `fb5b0036a0899ea97edfa0fc2c7985b55f3f7c5695028163e5e60d4f3cf6075d` |
| `jacoco/org.jacoco.cli-0.8.15-nodeps.jar` | EPL-2.0 (`jacoco/LICENSE-jacoco.md`) | the sandbox image (E-037) | no | `d2b74b20b415163c1f53261e7c5ef4445b788327094208ff821e0c2baf9bc8f1` |
| `tree-sitter/tree-sitter-tsx-0.23.2.jar` | MIT (`tree-sitter/LICENSE-tree-sitter-typescript`: the same repository) | `analyze/` (E-028) | no | `a5eb98db7157e2693385f9e5af8b668a91112122d8c5a879b616206a841824d4` |
| `pitest/asm-9.11.jar` | BSD-3-Clause (`pitest/LICENSE-asm.txt`) | `gradle mutate` only (E-019) | no | `5fe3b469b8646e5bc53a897840952d63b712d4923d2a7a26aebc585f1fec7676` |
| `pitest/asm-analysis-9.11.jar` | BSD-3-Clause (`pitest/LICENSE-asm.txt`) | `gradle mutate` only (E-019) | no | `a1421c20978ca0a330f241b17923a751a23fb1c9423fda1aba7591bb086b01d8` |
| `pitest/asm-commons-9.11.jar` | BSD-3-Clause (`pitest/LICENSE-asm.txt`) | `gradle mutate` only (E-019) | no | `fcff37e6a0227a9f5ee81a7c07f51249307fa15889b56a359ae6f773c164c22b` |
| `pitest/asm-tree-9.11.jar` | BSD-3-Clause (`pitest/LICENSE-asm.txt`) | `gradle mutate` only (E-019) | no | `da8ad24d0bf7efb0674a906f59a7d441692b641718371f3bce6762b191a169fd` |
| `pitest/asm-util-9.11.jar` | BSD-3-Clause (`pitest/LICENSE-asm.txt`) | `gradle mutate` only (E-019) | no | `d5a23a677f31b8a7d2419895906e1fcbd9ac995beffb20944c8610c1bc377609` |
| `pitest/commons-lang3-3.18.0.jar` | Apache-2.0 (`pitest/LICENSE-apache-2.0.txt`, `pitest/NOTICE-commons-lang3.txt`) | `gradle mutate` only (E-019) | no | `4eeeae8d20c078abb64b015ec158add383ac581571cddc45c68f0c9ae0230720` |
| `pitest/commons-text-1.14.0.jar` | Apache-2.0 (`pitest/LICENSE-apache-2.0.txt`, `pitest/NOTICE-commons-text.txt`) | `gradle mutate` only (E-019) | no | `121fce2282910c8f0c3ba793a5436b31beb710423cbe2d574a3fb7a73c508e92` |
| `pitest/pitest-1.30.1.jar` | Apache-2.0 (`pitest/LICENSE-apache-2.0.txt`) | `gradle mutate` only (E-019) | no | `893af485bf0cb7ac2cb9c0a7da20a6213098586e6b768624d90b5b3e0d356616` |
| `pitest/pitest-command-line-1.30.1.jar` | Apache-2.0 (`pitest/LICENSE-apache-2.0.txt`); bundles StringTemplate and ANTLR 2 (BSD) and jopt-simple (MIT), `pitest/LICENSE-bundled-in-command-line.txt` | `gradle mutate` only (E-019) | no | `e7fe206c7c6ed8e4529ee9c44e6338df702e67326bf34702d1c57cfca6b31819` |
| `pitest/pitest-entry-1.30.1.jar` | Apache-2.0 (`pitest/LICENSE-apache-2.0.txt`) | `gradle mutate` only (E-019) | no | `3a05342c7efa4925fa3074337cc4491458a8487715b5401c84838309ea85f48f` |
| `pitest/pitest-junit5-plugin-1.2.3.jar` | Apache-2.0 (`pitest/LICENSE-apache-2.0.txt`) | `gradle mutate` only (E-019) | no | `d3e2fcc8db5fd52c0c4ac2fe0d937e64da3df39b657093a1581b5fd463f9be64` |

The tree-sitter jars are `io.github.bonede` on Maven Central (github.com/bonede/tree-sitter-ng): a JNI
binding built for Java 11, so it runs on the kernel's Java 21, with its native libraries inside each
jar for Linux, macOS and Windows (x86_64, and aarch64 for Linux and macOS). The official binding,
`io.github.tree-sitter:jtreesitter`, is built for Java 23 and ships no native library (J-010).

The pitest jars are PIT 1.30.1 (2026-10-14) with its JUnit 5 plugin 1.2.3 and the runtime dependencies its
poms declare: ASM 9.11 (2026-10-04), commons-text 1.14.0 and commons-lang3 3.18.0; each past the 7-day
cooldown, each matching Maven Central's SHA-1 when fetched. The JUnit 5 plugin runs on the JUnit platform
jars above. They are on no classpath of the build or the tests: only `gradle mutate` reads them (E-019).

## The sandbox image's other pins (sandbox/, E-037)

Not vendored here, but pinned where the sandbox image is built, each public a week before it was pinned
(E-033's cooldown), each checked by `test/SupplyChainTest.java`: the base images by digest
(`sandbox/Dockerfile`: the kernel's own `gradle:8.14.3-jdk21-noble` for Temurin 21, and
`python:3.12-slim-bookworm`, built 2026-10-06); Node v22.23.3 and Maven 3.9.16 by the SHA-256 and
SHA-512 their projects publish (`sandbox/fetch.py` refuses any other); pytest 9.1.1, coverage 7.16.2 and
pytest's four dependencies by their wheels' SHA-256 (`sandbox/requirements.txt`); c8 12.0.0 and its 54
dependencies by npm's SHA-512 integrity, resolved as of 2026-10-12, none with an install script
(`sandbox/node/package-lock.json`). JaCoCo 0.8.15 (June 2026) is above. Licenses: pytest, coverage,
iniconfig, pluggy, pygments MIT, BSD or Apache-2.0; packaging Apache-2.0 or BSD; c8 ISC; Node MIT;
Maven Apache-2.0; Temurin GPLv2 with the Classpath Exception. Nothing in the image ships to an applicant
or an employer.
