package evaluator.analyze;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** The report files of a language's folder, found and read as hostile files: never through a link, under the size cap. */
class ReportFilesTest {
    private static final Path REPORTS = Path.of(System.getProperty("evaluator.root", ".")).resolve("test/fixtures/reports");

    private static void copy(Path dir, String to, String fixture) throws IOException {
        Files.createDirectories(dir.resolve(to).getParent());
        Files.copy(REPORTS.resolve(fixture), dir.resolve(to));
    }

    /**
     * junit.xml (jest.xml: 2 cases, 2 passed), surefire/a.xml (surefire.xml: 3 cases, 2 passed, 1 failed on a host that would not
     * resolve) and surefire/b.xml (1 skipped), then jest.json (3 assertions: 1 passed, 1 failed on the network, 1 todo):
     * 9 cases, 5 passed, 2 failed, 2 skipped. Not read: a .txt, a folder and a link named .xml in surefire, and a link out of it.
     */
    @Test
    void theReportsOfAFolderAreSummedInOrderAndLinksAndOtherFilesAreNotRead(@TempDir Path root, @TempDir Path outside) throws IOException {
        Path dir = root.resolve("java");
        copy(dir, "junit.xml", "jest.xml");
        copy(dir, "surefire/a.xml", "surefire.xml");
        Files.writeString(dir.resolve("surefire/b.xml"), "<testsuite><testcase name=\"t\"><skipped/></testcase></testsuite>");
        Files.writeString(dir.resolve("surefire/notes.txt"), "not xml at all");
        Files.createDirectories(dir.resolve("surefire/folder.xml"));
        Files.writeString(outside.resolve("secret.xml"), "not xml either");
        Files.createSymbolicLink(dir.resolve("surefire/link.xml"), outside.resolve("secret.xml"));
        copy(dir, "jest.json", "jest.json");
        List<String> reports = new ArrayList<>();
        Reports.Outcome outcome;
        try {
            outcome = ReportFiles.outcome(dir, reports);
        } catch (Reports.Refused e) {
            throw new AssertionError(e);
        }
        assertEquals(new Reports.Outcome(9, 5, 2, 2, List.of("shop.CartTest.fetchesExchangeRates", "prices fetches rates")), outcome);
        assertEquals(List.of("java/junit.xml", "java/surefire/a.xml", "java/surefire/b.xml", "java/jest.json"), reports);
    }

    /** A surefire folder that is a link, a jest.json that is a link and a junit.xml that is a link are not followed. */
    @Test
    void aLinkedFolderOrFileIsNotFollowed(@TempDir Path root, @TempDir Path outside) throws IOException, Reports.Refused {
        copy(outside, "folder/a.xml", "surefire.xml");
        copy(outside, "jest.json", "jest.json");
        copy(outside, "junit.xml", "jest.xml");
        Path dir = root.resolve("java");
        Files.createDirectories(dir);
        Files.createSymbolicLink(dir.resolve("surefire"), outside.resolve("folder"));
        Files.createSymbolicLink(dir.resolve("jest.json"), outside.resolve("jest.json"));
        Files.createSymbolicLink(dir.resolve("junit.xml"), outside.resolve("junit.xml"));
        List<String> reports = new ArrayList<>();
        assertEquals(new Reports.Outcome(0, 0, 0, 0, List.of()), ReportFiles.outcome(dir, reports));
        assertEquals(List.of(), reports);
        assertEquals(List.of(), ReportFiles.files(dir, "junit.xml", "surefire"));
    }

    /** A report of exactly the cap is read; one byte over is refused before it is read. */
    @Test
    void aFileOfExactlyTheCapIsReadAndOneByteMoreIsNot(@TempDir Path dir) throws IOException, Reports.Refused {
        Path file = dir.resolve("r.xml");
        Files.write(file, new byte[Reports.MAX_BYTES]);
        assertEquals(Reports.MAX_BYTES, ReportFiles.text(file).length());
        Files.write(file, new byte[Reports.MAX_BYTES + 1]);
        assertThrows(Reports.Refused.class, () -> ReportFiles.text(file));
    }

    /** Every count of two outcomes added, from values that differ, and their network attempts in order. */
    @Test
    void outcomesAreAddedFieldByField() {
        Reports.Outcome sum = ReportFiles.add(new Reports.Outcome(10, 6, 3, 1, List.of("a")), new Reports.Outcome(20, 12, 5, 3, List.of("b")));
        assertEquals(new Reports.Outcome(30, 18, 8, 4, List.of("a", "b")), sum);
    }
}
