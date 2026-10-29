# Findings: what the project has proven about existing tools and practices

Every flaw this project's tests or runs prove in an existing tool, metric or practice, with the
evidence (E-022). Not a bug list for this codebase (that is `TODO.md` and the journal) and not an
opinion: a finding is something a run showed, that someone could reproduce from what is recorded.
A finding gets a number, F-001 onward, never reused. Newest last.

| Id | Date | Subject | Finding | Evidence |
| --- | --- | --- | --- | --- |
| F-001 | 2026-10-29 | The tree-sitter Java binding's string entry (`TSParser.parseStringEncoding`) | It passes the Java string through JNI as modified UTF-8, where a character beyond the Basic Multilingual Plane is 6 bytes, not 4, so every node byte offset after an emoji indexes bytes the file does not have: a 2-emoji Python file of 37 UTF-8 bytes reads as ending at byte 41. Slicing the file's real bytes with those offsets threw `StringIndexOutOfBoundsException` (5 of 30 real repositories in the first calibration run). Its `parse(byte[], ...)` entry indexes the real UTF-8 bytes, but hands the reader the very array it was given as the buffer, and a null reader crashes the JVM. | `test/evaluator/analyze/EmojiOffsetsTest.java` (J-131); fails on the string entry, passes on the byte entry. |

More are expected from phase 1 step 1.3 (static analysis adapters disagreeing
with each other on the same function) and 1.6 (what a history-shape analysis sees in repositories
known to be squashed or replayed).
