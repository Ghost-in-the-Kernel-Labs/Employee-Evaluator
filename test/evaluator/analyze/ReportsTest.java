package evaluator.analyze;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import evaluator.rubric.Fact;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ReportsTest {
    private static final Path ROOT = Path.of(System.getProperty("evaluator.root", "."));

    private static String report(String name) {
        try {
            return Files.readString(ROOT.resolve("test/fixtures/reports/" + name));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** pytest: 5 cases, 3 passed, 1 failed (rounding), 1 skipped; nothing reached for the network. */
    @Test
    void pytestsReportAsCountedByHand() throws Reports.Refused {
        assertEquals(new Reports.Outcome(5, 3, 1, 1, List.of()), Reports.junit(List.of(report("pytest.xml"))));
    }

    /** Jest and Surefire together: 2 + 3 cases, 4 passed, Surefire's third an UnknownHostException. */
    @Test
    void reportsAreSummedAndANetworkAttemptIsNamed() throws Reports.Refused {
        Reports.Outcome outcome = Reports.junit(List.of(report("jest.xml"), report("surefire.xml")));
        assertEquals(new Reports.Outcome(5, 4, 1, 0, List.of("shop.CartTest.fetchesExchangeRates")), outcome);
    }

    /** coverage.py: 30 of 40 lines; JaCoCo: the report's own LINE counter (15 of 20), not a package's. Neither DTD is fetched. */
    @Test
    void coverageFromCoberturaAndJacoco() throws Reports.Refused {
        assertEquals(Optional.of(new Reports.Lines(30, 40)), Reports.coverage(report("cobertura.xml")));
        assertEquals(Optional.of(new Reports.Lines(15, 20)), Reports.coverage(report("jacoco.xml")));
        assertEquals(Optional.empty(), Reports.coverage("<coverage/>"));
    }

    @Test
    void aHostileReportIsRefusedNotRead() {
        Reports.Refused xxe = assertThrows(Reports.Refused.class, () -> Reports.junit(List.of(report("xxe.xml"))));
        assertTrue(xxe.getMessage().contains("entity"));
        assertThrows(Reports.Refused.class, () -> Reports.junit(List.of(report("laughs.xml"))));
        assertThrows(Reports.Refused.class, () -> Reports.junit(List.of("<testsuite>" + "x".repeat(Reports.MAX_BYTES) + "</testsuite>")));
        assertThrows(Reports.Refused.class, () -> Reports.junit(List.of("not xml")));
    }

    /** Jest: 3 assertions, 1 passed, 1 failed on a name that would not resolve, 1 todo. */
    @Test
    void jestsJsonReport() throws Reports.Refused {
        assertEquals(new Reports.Outcome(3, 1, 1, 1, List.of("prices fetches rates")), Reports.jest(report("jest.json")));
        assertThrows(Reports.Refused.class, () -> Reports.jest("[".repeat(10_000)));
        assertThrows(Reports.Refused.class, () -> Reports.jest("{\"testResults\": [{\"assertionResults\": [\"unterminated"));
    }

    @Test
    void aRefusedLocalConnectionIsNotTheNetwork() {
        assertTrue(Reports.reachedNetwork("getaddrinfo ENOTFOUND registry.example"));
        assertEquals(false, Reports.reachedNetwork("connect ECONNREFUSED 127.0.0.1:5432"));
        assertEquals(false, Reports.reachedNetwork("java.net.ConnectException: Connection refused"));
    }

    @Test
    void theFactsOfARun() throws Reports.Refused {
        List<Fact> passing = TestRuns.facts(Reports.junit(List.of(report("pytest.xml"))), Reports.coverage(report("cobertura.xml")), "t", List.of("pytest.xml"));
        assertEquals(List.of("QUALITY.tests.pass_rate repository:.", "QUALITY.tests.coverage repository:."), passing.stream().map(Fact::key).toList());
        assertEquals(0.75, passing.get(0).value());
        assertEquals(0.75, passing.get(1).value());
        List<Fact> reaching = TestRuns.facts(Reports.junit(List.of(report("surefire.xml"))), Optional.empty(), "t", List.of("surefire.xml"));
        assertEquals(1, reaching.size());
        assertEquals("QUALITY.tests.network_attempts repository:.", reaching.get(0).key());
        assertEquals(List.of("shop.CartTest.fetchesExchangeRates"), reaching.get(0).evidence().sources());
        assertEquals(List.of(), TestRuns.facts(new Reports.Outcome(0, 0, 0, 0, List.of()), Optional.empty(), "t", List.of()));
    }

    /** JaCoCo's LINE counter: 3 missed and 3 covered is 3 of 6 (equal counts are not "counts nothing"); 2 missed, 0 covered is 0 of 2; 0 and 0 is nothing. */
    @Test
    void jacocosLineCounterIsMissedPlusCovered() throws Reports.Refused {
        assertEquals(Optional.of(new Reports.Lines(3, 6)), Reports.coverage("<report name=\"r\"><counter type=\"LINE\" missed=\"3\" covered=\"3\"/></report>"));
        assertEquals(Optional.of(new Reports.Lines(0, 2)), Reports.coverage("<report name=\"r\"><counter type=\"LINE\" missed=\"2\" covered=\"0\"/></report>"));
        assertEquals(Optional.empty(), Reports.coverage("<report name=\"r\"><counter type=\"LINE\" missed=\"0\" covered=\"0\"/></report>"));
    }

    /** The cap is on size, not on one byte under it: a report of exactly MAX_BYTES is read, one byte more is refused (each of the three readers). */
    @Test
    void aReportOfExactlyTheCapIsReadAndOneByteMoreIsNot() throws Reports.Refused {
        String xml = "<testsuite/>";
        assertEquals(new Reports.Outcome(0, 0, 0, 0, List.of()), Reports.junit(List.of(xml + " ".repeat(Reports.MAX_BYTES - xml.length()))));
        assertThrows(Reports.Refused.class, () -> Reports.junit(List.of(xml + " ".repeat(Reports.MAX_BYTES - xml.length() + 1))));
        String json = "{}";
        assertEquals(new Reports.Outcome(0, 0, 0, 0, List.of()), Reports.jest(json + " ".repeat(Reports.MAX_BYTES - json.length())));
        assertThrows(Reports.Refused.class, () -> Reports.jest(json + " ".repeat(Reports.MAX_BYTES - json.length() + 1)));
    }

    /** The JDK's secure processing limit is on: an element with 10,001 attributes (its limit is 10,000) is refused; with 10,000 it is read. */
    @Test
    void secureProcessingLimitsAnElementsAttributes() throws Reports.Refused {
        StringBuilder many = new StringBuilder("<testsuite><testcase name=\"t\"");
        for (int i = 0; i < 9_999; i++) many.append(" a").append(i).append("=\"1\"");
        assertEquals(new Reports.Outcome(1, 1, 0, 0, List.of()), Reports.junit(List.of(many + "/></testsuite>")));
        many.insert(many.length(), " b=\"1\"");
        assertThrows(Reports.Refused.class, () -> Reports.junit(List.of(many + "/></testsuite>")));
    }

    /** A run with only skipped tests has no pass rate (0 of 0), and a coverage of no lines is no fact. */
    @Test
    void noRunTestsAndNoLinesGiveNoFacts() {
        assertEquals(List.of(), TestRuns.facts(new Reports.Outcome(2, 0, 0, 2, List.of()), Optional.of(new Reports.Lines(0, 0)), "t", List.of("r")));
        List<Fact> facts = TestRuns.facts(new Reports.Outcome(5, 2, 1, 2, List.of()), Optional.of(new Reports.Lines(1, 3)), "t", List.of("r"));
        assertEquals(List.of(0.667, 0.333), facts.stream().map(Fact::value).toList());
    }
}
