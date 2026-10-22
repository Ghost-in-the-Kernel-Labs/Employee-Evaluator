import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

/** The documents' and the kernel's tests under JUnit (E-009): each must report no problem. */
class DocTests {
    @Test
    void ghost() {
        assertEquals(List.of(), GhostTest.run().problems());
    }

    @Test
    void glossary() {
        assertEquals(List.of(), GlossaryTest.run().problems());
    }

    @Test
    void docs() {
        assertEquals(List.of(), DocsTest.run().problems());
    }

    @Test
    void modules() {
        assertEquals(List.of(), ModulesTest.run().problems());
    }

    @Test
    void journal() {
        assertEquals(List.of(), JournalTest.run().problems());
    }

    @Test
    void todo() {
        assertEquals(List.of(), TodoTest.run().problems());
    }

    @Test
    void scoreCard() {
        assertEquals(List.of(), ScoreCardDocTest.run().problems());
    }

    @Test
    void supplyChain() {
        assertEquals(List.of(), SupplyChainTest.run().problems());
    }

    @Test
    void metrics() {
        assertEquals(List.of(), MetricsTest.run().problems());
    }

    @Test
    void kernelMap() {
        assertEquals(List.of(), KernelMapTest.run().problems());
    }
}
