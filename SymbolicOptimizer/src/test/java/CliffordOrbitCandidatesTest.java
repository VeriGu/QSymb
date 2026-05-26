import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.AbstractMap.SimpleEntry;
import java.util.List;

/**
 * Unit tests for the {@link CliffordOrbitCandidates} generator.
 *
 * Structural tests live here. The semantic correctness check (each (L, R)
 * pair has an 8-dim intertwiner basis) lives in the Python test suite
 * `test_clifford_orbit.py` -- those tests verify the math via direct matrix
 * evaluation. Here we verify only that the Java generator emits the
 * expected number of pairs with the expected gate shape, so the pair list
 * threads through to `infer_symb` cleanly.
 *
 * JUnit 5 assertion order: (condition, message). Distinct from JUnit 4.
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
    public void ionGeneratesFourCandidates() {
        List<SimpleEntry<EggGen.Circuit, EggGen.Circuit>> cands =
                CliffordOrbitCandidates.generateForGateset("ion");
        assertEquals(4, cands.size());
    }

    @Test
    public void ionEveryLHSIsRXXThenSYMB() {
        // LHS shape: [RXX(θ) on q0,q1, SYMB] -- this is the canonical
        // "A; SYMB" form QSymb's compute_L_R expects (A = RXX, B = ε).
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
    public void ionEveryRHSIsSYMBThenNineGates() {
        // RHS shape: [SYMB, ...9-gate compound-Clifford decomp...] -- the
        // U pair is a 2-gate Clifford (e.g. Ry(π/2)·Rx(-π/2)), so each
        // side of the RXX gets 2 prep and 2 unprep gates -> 4 + 1 + 4 = 9.
        List<SimpleEntry<EggGen.Circuit, EggGen.Circuit>> cands =
                CliffordOrbitCandidates.generateForGateset("ion");
        for (SimpleEntry<EggGen.Circuit, EggGen.Circuit> p : cands) {
            EggGen.Circuit R = p.getValue();
            assertEquals(10, R.gates.size(),
                    "RHS must be SYMB + 9-gate compound-Clifford decomp = 10 gates");
            assertTrue(R.gates.get(0) instanceof EggGen.SYMB,
                    "RHS[0] must be SYMB");
            // Middle RXX is at index 5 (SYMB + 4 wrappers + RXX).
            assertTrue(R.gates.get(5) instanceof EggGen.RXX,
                    "RHS[5] must be RXX, got " + R.gates.get(5).getClass().getSimpleName());
            EggGen.RXX rxx = (EggGen.RXX) R.gates.get(5);
            assertEquals("q0", rxx.qubit1);
            assertEquals("q1", rxx.qubit2);
        }
    }

    @Test
    public void ionFirstTwoCandidatesAreYWrapped() {
        // First two pairs use Ry-Rx Clifford -- the inner wrapper on the
        // outside is RY, the inner is RX.
        List<SimpleEntry<EggGen.Circuit, EggGen.Circuit>> cands =
                CliffordOrbitCandidates.generateForGateset("ion");
        for (int i = 0; i < 2; i++) {
            EggGen.Circuit R = cands.get(i).getValue();
            assertTrue(R.gates.get(1) instanceof EggGen.RY, "R[" + i + "].1 must be RY (q0 prep)");
            assertTrue(R.gates.get(2) instanceof EggGen.RX, "R[" + i + "].2 must be RX (q0 prep)");
            assertTrue(R.gates.get(3) instanceof EggGen.RY, "R[" + i + "].3 must be RY (q1 prep)");
            assertTrue(R.gates.get(4) instanceof EggGen.RX, "R[" + i + "].4 must be RX (q1 prep)");
        }
    }

    @Test
    public void ionLastTwoCandidatesAreZWrapped() {
        // Last two pairs use Rz-Rx Clifford.
        List<SimpleEntry<EggGen.Circuit, EggGen.Circuit>> cands =
                CliffordOrbitCandidates.generateForGateset("ion");
        for (int i = 2; i < 4; i++) {
            EggGen.Circuit R = cands.get(i).getValue();
            assertTrue(R.gates.get(1) instanceof EggGen.RZ, "R[" + i + "].1 must be RZ (q0 prep)");
            assertTrue(R.gates.get(2) instanceof EggGen.RX, "R[" + i + "].2 must be RX (q0 prep)");
            assertTrue(R.gates.get(3) instanceof EggGen.RZ, "R[" + i + "].3 must be RZ (q1 prep)");
            assertTrue(R.gates.get(4) instanceof EggGen.RX, "R[" + i + "].4 must be RX (q1 prep)");
        }
    }
}
