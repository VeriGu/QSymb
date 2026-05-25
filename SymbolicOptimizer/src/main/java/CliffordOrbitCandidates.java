import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ast.BinOp;
import ast.Expr;
import ast.Real;
import ast.Symbol;
import ast.UnOp;

/**
 * Priority-pass candidate generator for the QSymb symbolic-rule synthesizer.
 *
 * Given a gateset, produces a small set of (L, R) circuit pairs whose
 * intertwiner equation `L · S = S · R` has guaranteed non-trivial solutions
 * by Sylvester's theorem (R is a Clifford-conjugate of L, so they share the
 * same eigenvalue multiset).
 *
 * Concretely: L is the gateset's parameterised native 2q gate, and R runs
 * over its Clifford orbit. The decomposition of R into the native gateset
 * is hardcoded per gateset (small, well-known table -- not a general 2q
 * synthesizer).
 *
 * The output pairs are appended to the existing pair pool in
 * {@code EnumeratorPrune.main} (the "completeness pass") and then routed
 * through the unchanged {@code infer_symb} -> intertwiner-solver pipeline.
 * That pipeline computes the basis from scratch for each pair, so this
 * class doesn't need to know about basis matrices.
 *
 * Mirrors the Python tables in `semantics.py` (CLIFFORD_ORBIT_SETS) -- both
 * sides should evolve together when extending to new gatesets.
 */
public final class CliffordOrbitCandidates {

    private CliffordOrbitCandidates() {}

    /**
     * Returns the orbit candidate pairs for a given gateset.
     * Returns an empty list for unknown gatesets (priority pass is a
     * strict augmentation -- never breaks the completeness pass).
     */
    public static List<SimpleEntry<EggGen.Circuit, EggGen.Circuit>> generateForGateset(String gateset) {
        if (gateset == null) return new ArrayList<>();
        switch (gateset) {
            case "ion":
                return generateForIon();
            default:
                return new ArrayList<>();
        }
    }

    /**
     * Ion-native orbit: L = RXX(θ), 4 (U_a, U_b) pairs producing
     *   RZZ(θ), RZZ(-θ), RYY(θ), RYY(-θ)
     * as the R-side, each decomposed into a 5-gate ion-native sequence.
     */
    static List<SimpleEntry<EggGen.Circuit, EggGen.Circuit>> generateForIon() {
        Expr theta1 = new Symbol("theta1");

        // QSymb canonical rule shape:  A; SYMB; B  ≡  C; SYMB; D
        // For L = RXX(θ), R = orbit_image(L), the canonical pair is
        //   left  = [RXX(θ), SYMB]                  (A = RXX, B = ε)
        //   right = [SYMB, ...R_decomp_gates...]     (C = ε, D = R_decomp)
        // compute_L_R then extracts:  L_mat = A · C† = RXX(θ),
        //                              R_mat = B† · D = R_decomp_matrix
        // which is exactly what we want. (Without the SYMB markers, the
        // solver's _circuit_parts raises "Circuit does not contain a 'symb'
        // gate" and returns no basis.)
        EggGen.Circuit L = new EggGen.Circuit(List.of(
                new EggGen.RXX("q0", "q1", theta1),
                new EggGen.SYMB(2)
        ));

        Expr piHalf = new BinOp(Expr.Op.DIV, new Symbol("pi"), new Real(2.0));
        Expr negPiHalf = new UnOp(Expr.Op.MINUS, piHalf);

        // R = RZZ(θ): conjugation by (Ry(π/2) ⊗ Ry(π/2)). The dagger of Ry(π/2)
        // is Ry(-π/2), placed BEFORE L in QASM order (left-to-right = applied
        // first), Ry(π/2) AFTER L.
        EggGen.Circuit R_RZZ_pos = ionRyDecomp(negPiHalf, negPiHalf, theta1, piHalf, piHalf);
        // R = RZZ(-θ): conjugation by (Ry(π/2) ⊗ Ry(-π/2)) -- mixed-sign pair.
        EggGen.Circuit R_RZZ_neg = ionRyDecomp(negPiHalf, piHalf,    theta1, piHalf,    negPiHalf);
        // R = RYY(θ): conjugation by (Rz(π/2) ⊗ Rz(π/2)).
        EggGen.Circuit R_RYY_pos = ionRzDecomp(negPiHalf, negPiHalf, theta1, piHalf, piHalf);
        // R = RYY(-θ): conjugation by (Rz(π/2) ⊗ Rz(-π/2)).
        EggGen.Circuit R_RYY_neg = ionRzDecomp(negPiHalf, piHalf,    theta1, piHalf,    negPiHalf);

        return new ArrayList<>(Arrays.asList(
                new SimpleEntry<>(L, R_RZZ_pos),
                new SimpleEntry<>(L, R_RZZ_neg),
                new SimpleEntry<>(L, R_RYY_pos),
                new SimpleEntry<>(L, R_RYY_neg)
        ));
    }

    /** SYMB; ry(a0)q0; ry(a1)q1; rxx(core); ry(b0)q0; ry(b1)q1 */
    private static EggGen.Circuit ionRyDecomp(Expr a0, Expr a1, Expr core, Expr b0, Expr b1) {
        return new EggGen.Circuit(List.of(
                new EggGen.SYMB(2),
                new EggGen.RY("q0", a0),
                new EggGen.RY("q1", a1),
                new EggGen.RXX("q0", "q1", core),
                new EggGen.RY("q0", b0),
                new EggGen.RY("q1", b1)
        ));
    }

    /** SYMB; rz(a0)q0; rz(a1)q1; rxx(core); rz(b0)q0; rz(b1)q1 */
    private static EggGen.Circuit ionRzDecomp(Expr a0, Expr a1, Expr core, Expr b0, Expr b1) {
        return new EggGen.Circuit(List.of(
                new EggGen.SYMB(2),
                new EggGen.RZ("q0", a0),
                new EggGen.RZ("q1", a1),
                new EggGen.RXX("q0", "q1", core),
                new EggGen.RZ("q0", b0),
                new EggGen.RZ("q1", b1)
        ));
    }
}
