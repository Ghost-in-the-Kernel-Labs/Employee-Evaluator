package evaluator.analyze;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MeasureTest {
    private static Measure at(int line, int endLine) {
        return new Measure("f", line, 1, 0, endLine, "");
    }

    /** A function is within another when its lines lie inside the other's and are not exactly the other's. */
    @Test
    void aFunctionIsWithinAnotherOnlyWhenItsLinesLieInsideTheOthers() {
        Measure outer = at(1, 9);
        assertTrue(at(3, 5).within(outer), "strictly inside");
        assertTrue(at(1, 5).within(outer), "starts on the outer's first line");
        assertTrue(at(5, 9).within(outer), "ends on the outer's last line");
        assertFalse(at(1, 9).within(outer), "the same lines are not within (another object, same range)");
        assertFalse(at(0, 5).within(outer), "starts before");
        assertFalse(at(5, 10).within(outer), "ends after");
        assertFalse(at(0, 10).within(outer), "wraps it");
        assertFalse(at(10, 12).within(outer), "after it");
        assertFalse(outer.within(outer), "never within itself");
        assertFalse(outer.within(at(3, 5)), "an outer is not within its inner");
    }
}
