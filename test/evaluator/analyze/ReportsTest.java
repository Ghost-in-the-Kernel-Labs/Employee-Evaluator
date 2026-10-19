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
        assertEquals(Optional.of(0.75), Reports.coverage(report("cobertura.xml")));
        assertEquals(Optional.of(0.75), Reports.coverage(report("jacoco.xml")));
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
}
