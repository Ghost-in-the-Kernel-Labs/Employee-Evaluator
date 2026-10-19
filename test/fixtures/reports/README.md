# reports

Test and coverage reports as the sandbox's runners write them, written by hand for `analyze/`'s
readers (TODO.md 1.5, E-037): pytest's JUnit XML, Jest's (jest-junit), Maven Surefire's with a test
that reached for the network, coverage.py's Cobertura and JaCoCo's XML (each with the DOCTYPE the real
tool writes, which must not be fetched), and two hostile reports that must be refused. Expected values
in `test/evaluator/analyze/ReportsTest.java`.
