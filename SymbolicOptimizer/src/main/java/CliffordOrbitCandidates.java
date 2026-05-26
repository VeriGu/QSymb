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
        //                              R_mat = B† · D = R_decomp_matrix.
        EggGen.Circuit L = new EggGen.Circuit(List.of(
                new EggGen.RXX("q0", "q1", theta1),
                new EggGen.SYMB(2)
        ));

        Expr piHalf = new BinOp(Expr.Op.DIV, new Symbol("pi"), new Real(2.0));
        Expr negPiHalf = new UnOp(Expr.Op.MINUS, piHalf);

        // Compound-Clifford pairs (U_a, U_b) with Rx(-π/2) prep -- these match
        // the wrapper structure produced by ion-native QASM compilers. The
        // simple Ry/Rz-only pairs (no Rx prep) yielded 0 matches across all
        // 10 benchmarks; the compound forms below are derived from the
        // 4gt11_83 manual fit and are designed to catch the same gadget
        // structure across the benchmark suite.
        //
        // Pair "Y anti": U_a (q0, high-order) = Ry(-π/2)·Rx(-π/2),
        //                U_b (q1, low-order)  = Ry(π/2)·Rx(-π/2).
        // (The qubit assignment matters: hand-built rule for 4gt11_83 had
        //  the high-order qubit carrying the negative-Y rotation. Swapping
        //  q0/q1 here yields a different basis that does NOT cover 4gt11_83.)
        // Conjugating X⊗X: each U sends X to ∓Z so X⊗X → -ZZ, giving R = RZZ(-θ).
        EggGen.Circuit R_YX_anti = paired2GateDecomp(
                "ry", "rx", piHalf,    piHalf,      // q0 prefix: U_a† = ry(π/2); rx(π/2)
                "ry", "rx", negPiHalf, piHalf,      // q1 prefix: U_b† = ry(-π/2); rx(π/2)
                theta1,
                "rx", "ry", negPiHalf, negPiHalf,   // q0 suffix: U_a = rx(-π/2); ry(-π/2)
                "rx", "ry", negPiHalf, piHalf);     // q1 suffix: U_b = rx(-π/2); ry(π/2)

        // Pair "Y same": U_a = U_b = Ry(π/2)·Rx(-π/2). Gives R = RZZ(+θ).
        EggGen.Circuit R_YX_same = paired2GateDecomp(
                "ry", "rx", negPiHalf, piHalf,
                "ry", "rx", negPiHalf, piHalf,
                theta1,
                "rx", "ry", negPiHalf, piHalf,
                "rx", "ry", negPiHalf, piHalf);

        // Pair "Z anti": U_a = Rz(π/2)·Rx(-π/2), U_b = Rz(-π/2)·Rx(-π/2).
        // X → Y on q_a, X → -Y on q_b, so X⊗X → Y⊗(-Y) = -YY, R = RYY(-θ).
        // Predicted to catch Toffoli-decomposition middles in tof_3.
        EggGen.Circuit R_ZX_anti = paired2GateDecomp(
                "rz", "rx", negPiHalf, piHalf,
                "rz", "rx", piHalf,    piHalf,
                theta1,
                "rx", "rz", negPiHalf, piHalf,
                "rx", "rz", negPiHalf, negPiHalf);

        // Pair "Z same": U_a = U_b = Rz(π/2)·Rx(-π/2). Gives R = RYY(+θ).
        EggGen.Circuit R_ZX_same = paired2GateDecomp(
                "rz", "rx", negPiHalf, piHalf,
                "rz", "rx", negPiHalf, piHalf,
                theta1,
                "rx", "rz", negPiHalf, piHalf,
                "rx", "rz", negPiHalf, piHalf);

        return new ArrayList<>(Arrays.asList(
                new SimpleEntry<>(L, R_YX_anti),
                new SimpleEntry<>(L, R_YX_same),
                new SimpleEntry<>(L, R_ZX_anti),
                new SimpleEntry<>(L, R_ZX_same)
        ));
    }

    /** Build R = (U_a ⊗ U_b) · RXX(θ) · (U_a ⊗ U_b)† where each U is a
     *  2-gate Clifford composition. The circuit layout is:
     *    SYMB;
     *    U_a† on q0 (2 gates);    U_b† on q1 (2 gates);
     *    RXX(θ);
     *    U_a  on q0 (2 gates);    U_b  on q1 (2 gates);
     *  -- so 9 gates total after SYMB.
     */
    private static EggGen.Circuit paired2GateDecomp(
            String aPre0, String aPre1, Expr aPre0Angle, Expr aPre1Angle,   // U_a† gates: gate names and angles
            String bPre0, String bPre1, Expr bPre0Angle, Expr bPre1Angle,
            Expr theta,
            String aSuf0, String aSuf1, Expr aSuf0Angle, Expr aSuf1Angle,   // U_a gates
            String bSuf0, String bSuf1, Expr bSuf0Angle, Expr bSuf1Angle) {
        return new EggGen.Circuit(List.of(
                new EggGen.SYMB(2),
                mkGate(aPre0, "q0", aPre0Angle),
                mkGate(aPre1, "q0", aPre1Angle),
                mkGate(bPre0, "q1", bPre0Angle),
                mkGate(bPre1, "q1", bPre1Angle),
                new EggGen.RXX("q0", "q1", theta),
                mkGate(aSuf0, "q0", aSuf0Angle),
                mkGate(aSuf1, "q0", aSuf1Angle),
                mkGate(bSuf0, "q1", bSuf0Angle),
                mkGate(bSuf1, "q1", bSuf1Angle)
        ));
    }

    private static EggGen.Gate mkGate(String name, String qubit, Expr angle) {
        switch (name) {
            case "rx": return new EggGen.RX(qubit, angle);
            case "ry": return new EggGen.RY(qubit, angle);
            case "rz": return new EggGen.RZ(qubit, angle);
            default: throw new IllegalArgumentException("unknown single-q gate: " + name);
        }
    }
}
