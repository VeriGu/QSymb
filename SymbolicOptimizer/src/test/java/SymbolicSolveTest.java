import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import ast.Symbol;

public class SymbolicSolveTest {

    @Test
    public void checkBigConcreteEigen_identicalCircuitsPasses() {
        SymbolicSolve solver = new SymbolicSolve(new Random(0));
        List<EggGen.Gate> gates1 = new ArrayList<>();
        gates1.add(new EggGen.X("q0"));
        gates1.add(new EggGen.SYMB(1));
        gates1.add(new EggGen.X("q0"));
        EggGen.Circuit c1 = new EggGen.Circuit(gates1);

        List<EggGen.Gate> gates2 = new ArrayList<>();
        gates2.add(new EggGen.X("q0"));
        gates2.add(new EggGen.SYMB(1));
        gates2.add(new EggGen.X("q0"));
        EggGen.Circuit c2 = new EggGen.Circuit(gates2);

        boolean ok = solver.checkBig(c1, c2, 1, 42L, 5);
        assertTrue(ok, "Identical L=R=I circuits must pass the concrete eigenvalue check");
    }

    @Test
    public void checkBigConcreteEigen_differentEigenvaluesFails() {
        SymbolicSolve solver = new SymbolicSolve(new Random(0));
        List<EggGen.Gate> gates1 = new ArrayList<>();
        gates1.add(new EggGen.SYMB(1));
        gates1.add(new EggGen.X("q0"));
        EggGen.Circuit c1 = new EggGen.Circuit(gates1);

        List<EggGen.Gate> gates2 = new ArrayList<>();
        gates2.add(new EggGen.SYMB(1));
        gates2.add(new EggGen.RZ("q0", new Symbol("theta1")));
        EggGen.Circuit c2 = new EggGen.Circuit(gates2);

        boolean ok = solver.checkBig(c1, c2, 1, 42L, 5);
        assertFalse(ok, "L=I and R=X*RZ(theta1) have distinct eigenvalues -> must fail");
    }

    @Test
    public void checkBigConcreteEigen_seedReproducibility() {
        SymbolicSolve solver = new SymbolicSolve(new Random(0));
        List<EggGen.Gate> gates1 = new ArrayList<>();
        gates1.add(new EggGen.X("q0"));
        gates1.add(new EggGen.SYMB(1));
        gates1.add(new EggGen.X("q0"));
        EggGen.Circuit c1 = new EggGen.Circuit(gates1);
        EggGen.Circuit c2 = new EggGen.Circuit(new ArrayList<>(gates1));

        boolean a = solver.checkBig(c1, c2, 1, 7777L, 3);
        boolean b = solver.checkBig(c1, c2, 1, 7777L, 3);
        assertEquals(a, b, "Same seed must yield the same verdict");
    }

    @Test
    public void checkSymbolicEigen_identicalCircuitsPasses() {
        SymbolicSolve solver = new SymbolicSolve(new Random(0));
        List<EggGen.Gate> gates = new ArrayList<>();
        gates.add(new EggGen.X("q0"));
        gates.add(new EggGen.SYMB(1));
        gates.add(new EggGen.X("q0"));
        EggGen.Circuit c1 = new EggGen.Circuit(gates);
        EggGen.Circuit c2 = new EggGen.Circuit(new ArrayList<>(gates));

        assertTrue(solver.checkSymbolicEigen(c1, c2, 1));
    }

    @Test
    public void checkSymbolicEigen_differentCircuitsFails() {
        SymbolicSolve solver = new SymbolicSolve(new Random(0));
        List<EggGen.Gate> gates1 = new ArrayList<>();
        gates1.add(new EggGen.SYMB(1));
        gates1.add(new EggGen.X("q0"));
        EggGen.Circuit c1 = new EggGen.Circuit(gates1);

        List<EggGen.Gate> gates2 = new ArrayList<>();
        gates2.add(new EggGen.SYMB(1));
        gates2.add(new EggGen.RZ("q0", new Symbol("theta1")));
        EggGen.Circuit c2 = new EggGen.Circuit(gates2);

        assertFalse(solver.checkSymbolicEigen(c1, c2, 1));
    }

    @Test
    public void checkSymbolicEigen_parametricEqualPasses() {
        SymbolicSolve solver = new SymbolicSolve(new Random(0));
        List<EggGen.Gate> gates1 = new ArrayList<>();
        gates1.add(new EggGen.RZ("q0", new Symbol("theta1")));
        gates1.add(new EggGen.SYMB(1));
        EggGen.Circuit c1 = new EggGen.Circuit(gates1);

        List<EggGen.Gate> gates2 = new ArrayList<>();
        gates2.add(new EggGen.SYMB(1));
        gates2.add(new EggGen.RZ("q0", new Symbol("theta1")));
        EggGen.Circuit c2 = new EggGen.Circuit(gates2);

        assertTrue(solver.checkSymbolicEigen(c1, c2, 1));
    }

    @Test
    public void getTrace_seededMatchingCircuitsAgree() {
        SymbolicSolve solver = new SymbolicSolve(new Random(0));
        List<EggGen.Gate> gates1 = new ArrayList<>();
        gates1.add(new EggGen.X("q0"));
        gates1.add(new EggGen.X("q0"));
        List<EggGen.Gate> gates2 = new ArrayList<>();
        gates2.add(new EggGen.X("q0"));
        gates2.add(new EggGen.X("q0"));

        String t1 = solver.getTrace(gates1, 1, 99L, 4);
        String t2 = solver.getTrace(gates2, 1, 99L, 4);
        assertEquals(t1, t2, "Same matrix + same seed -> same multi-trace fingerprint");
        int commas = (int) t1.chars().filter(c -> c == ',').count();
        assertEquals(3, commas, "Expected 4 trace tuples separated by 3 commas, got: " + t1);
    }
}
