import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.AbstractMap.SimpleEntry;
import java.util.List;

/**
 * Unit tests for the {@link CliffordOrbitCandidates} generator.
 *
 * Structural tests live here. The semantic correctness check (each (L, R)
 * pair has an 8-dim intertwiner basis) is verified end-to-end by the
 * Python orbit suite (`test_clifford_orbit.py`) and by the
 * CliffordOrbitEndToEndTest's infer_symb call.
 */
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
        // 36 simple Pauli-axis orbit pairs (6×6 over {I, Z, Rz(±π/2), Ry(±π/2)})
        // + 4 compound (Ry·Rx, Rz·Rx) compiler-specific pairs.
        List<SimpleEntry<EggGen.Circuit, EggGen.Circuit>> cands =
                CliffordOrbitCandidates.generateForGateset("ion");
        assertEquals(40, cands.size());
    }

    @Test
    public void ionEveryLHSIsRXXThenSYMB() {
        // LHS: [RXX(θ), SYMB] on (q0, q1). Identical for every pair.
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
        // RHS: [SYMB, ...decomp gates including one RXX...]
        // Simple-Clifford pairs produce 1-5 gates after SYMB (0-2 pairs of
        // dagger/forward wrappers, plus the inner RXX). Compound-Clifford
        // pairs produce 9 gates after SYMB. So sizes range from 1 (the
        // (I, I) trivial pair) to 10.
        List<SimpleEntry<EggGen.Circuit, EggGen.Circuit>> cands =
                CliffordOrbitCandidates.generateForGateset("ion");
        int seenRxxCount = 0;
        for (SimpleEntry<EggGen.Circuit, EggGen.Circuit> p : cands) {
            EggGen.Circuit R = p.getValue();
            assertTrue(R.gates.size() >= 2 && R.gates.size() <= 10,
                    "RHS gate count must be 2..10, got " + R.gates.size());
            assertTrue(R.gates.get(0) instanceof EggGen.SYMB,
                    "RHS[0] must be SYMB");
            // Exactly one RXX inside.
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
        // The first 36 pairs are the simple axis-orbit. Each has at most
        // 2 prefix + 1 RXX + 2 suffix = 5 single-gate wrappers (less when
        // a side is identity).
        List<SimpleEntry<EggGen.Circuit, EggGen.Circuit>> cands =
                CliffordOrbitCandidates.generateForGateset("ion");
        for (int i = 0; i < 36; i++) {
            EggGen.Circuit R = cands.get(i).getValue();
            // SYMB + (up to 5 single-gate-Clifford gates) = up to 6.
            assertTrue(R.gates.size() <= 6,
                    "Simple-Clifford pair " + i + " RHS has too many gates: " + R.gates.size());
        }
    }

    @Test
    public void ion4CompoundPairsHave10Gates() {
        // The last 4 pairs are compound Cliffords (2 gates per U), so RHS
        // size = SYMB + 4 prefix + RXX + 4 suffix = 10.
        List<SimpleEntry<EggGen.Circuit, EggGen.Circuit>> cands =
                CliffordOrbitCandidates.generateForGateset("ion");
        for (int i = 36; i < 40; i++) {
            EggGen.Circuit R = cands.get(i).getValue();
            assertEquals(10, R.gates.size(),
                    "Compound-Clifford pair " + i + " should be 10 gates");
        }
    }
}
