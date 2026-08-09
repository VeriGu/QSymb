import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.AbstractMap.SimpleEntry;
import java.util.List;

public class CliffordOrbitCandidatesTest {

    @Test
    public void emptyForUnknownGateset() {
        List<SimpleEntry<EggGen.Circuit, EggGen.Circuit>> cands =
                CliffordOrbitCandidates.generateForGateset("unknown");
        assertNotNull(cands);
        assertTrue(cands.isEmpty(),
                "unknown gateset should yield empty list, got " + cands.size());
    }

    @Test
    public void emptyForNullGateset() {
        assertTrue(CliffordOrbitCandidates.generateForGateset(null).isEmpty());
    }

    @Test
    public void ionGenerates40Candidates() {
        List<SimpleEntry<EggGen.Circuit, EggGen.Circuit>> cands =
                CliffordOrbitCandidates.generateForGateset("ion");
        assertEquals(40, cands.size());
    }

    @Test
    public void ionEveryLHSIsRXXThenSYMB() {
        List<SimpleEntry<EggGen.Circuit, EggGen.Circuit>> cands =
                CliffordOrbitCandidates.generateForGateset("ion");
        for (SimpleEntry<EggGen.Circuit, EggGen.Circuit> p : cands) {
            EggGen.Circuit L = p.getKey();
            assertEquals(2, L.gates.size(), "LHS must be RXX + SYMB");
            assertTrue(L.gates.get(0) instanceof EggGen.RXX,
                    "LHS[0] must be RXX, got " + L.gates.get(0).getClass().getSimpleName());
            assertTrue(L.gates.get(1) instanceof EggGen.SYMB,
                    "LHS[1] must be SYMB, got " + L.gates.get(1).getClass().getSimpleName());
            EggGen.RXX rxx = (EggGen.RXX) L.gates.get(0);
            assertEquals("q0", rxx.qubit1);
            assertEquals("q1", rxx.qubit2);
        }
    }

    @Test
    public void ionEveryRHSStartsWithSYMBAndContainsRXX() {
        List<SimpleEntry<EggGen.Circuit, EggGen.Circuit>> cands =
                CliffordOrbitCandidates.generateForGateset("ion");
        int seenRxxCount = 0;
        for (SimpleEntry<EggGen.Circuit, EggGen.Circuit> p : cands) {
            EggGen.Circuit R = p.getValue();
            assertTrue(R.gates.size() >= 2 && R.gates.size() <= 10,
                    "RHS gate count must be 2..10, got " + R.gates.size());
            assertTrue(R.gates.get(0) instanceof EggGen.SYMB,
                    "RHS[0] must be SYMB");
            int rxxCount = 0;
            for (EggGen.Gate g : R.gates) {
                if (g instanceof EggGen.RXX) rxxCount++;
            }
            assertEquals(1, rxxCount,
                    "RHS must contain exactly one RXX, got " + rxxCount);
            seenRxxCount++;
        }
        assertEquals(cands.size(), seenRxxCount);
    }

    @Test
    public void ion36AxisOrbitPairsHaveAtMost5GatesInRHS() {
        List<SimpleEntry<EggGen.Circuit, EggGen.Circuit>> cands =
                CliffordOrbitCandidates.generateForGateset("ion");
        for (int i = 0; i < 36; i++) {
            EggGen.Circuit R = cands.get(i).getValue();
            assertTrue(R.gates.size() <= 6,
                    "Simple-Clifford pair " + i + " RHS has too many gates: " + R.gates.size());
        }
    }

    @Test
    public void ion4CompoundPairsHave10Gates() {
        List<SimpleEntry<EggGen.Circuit, EggGen.Circuit>> cands =
                CliffordOrbitCandidates.generateForGateset("ion");
        for (int i = 36; i < 40; i++) {
            EggGen.Circuit R = cands.get(i).getValue();
            assertEquals(10, R.gates.size(),
                    "Compound-Clifford pair " + i + " should be 10 gates");
        }
    }
}
