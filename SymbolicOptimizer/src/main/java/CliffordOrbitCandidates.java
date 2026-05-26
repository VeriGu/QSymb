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
 * For L = RXX(θ), generates 36 (U_a ⊗ U_b) Clifford-orbit pairs — the
 * complete axis-permutation orbit — plus 4 compound-Clifford (Ry·Rx,
 * Rz·Rx) pairs that catch the specific ion-native QASM compiler wrapper
 * structure. Total: 40 pairs.
 *
 * The 36 pairs cover: {I, Z, Rz(±π/2), Ry(±π/2)} × {same set}. Each U in
 * that set maps X to ±X / ±Y / ±Z. Their (U_a ⊗ U_b) conjugations of RXX
 * thus cover the full axis-permutation orbit of X⊗X — RXX(±θ), RYY(±θ),
 * RZZ(±θ), RXY(±θ), RXZ(±θ), RYX(±θ), RYZ(±θ), RZX(±θ), RZY(±θ).
 *
 * Why the 4 compound pairs are kept: ion-native QASM compilers emit
 * (Ry·Rx) and (Rz·Rx) wrappers around RXX, so the post-compiler middle
 * structures sit in subspaces those 4 specific compound pairs catch
 * directly. Replacing them with the 36 simple Cliffords loses that
 * coverage. Keeping both gives full axis orbit + compiler-specific.
 */
public final class CliffordOrbitCandidates {

    private CliffordOrbitCandidates() {}

    public static List<SimpleEntry<EggGen.Circuit, EggGen.Circuit>> generateForGateset(String gateset) {
        if (gateset == null) return new ArrayList<>();
        switch (gateset) {
            case "ion":
                return generateForIon();
            default:
                return new ArrayList<>();
        }
    }

    static List<SimpleEntry<EggGen.Circuit, EggGen.Circuit>> generateForIon() {
        Expr theta1 = new Symbol("theta1");
        Expr piHalf = new BinOp(Expr.Op.DIV, new Symbol("pi"), new Real(2.0));
        Expr negPiHalf = new UnOp(Expr.Op.MINUS, piHalf);
        Expr pi = new Symbol("pi");
        Expr negPi = new UnOp(Expr.Op.MINUS, pi);

        // L = RXX(θ); SYMB. Same for every pair.
        EggGen.Circuit L = new EggGen.Circuit(List.of(
                new EggGen.RXX("q0", "q1", theta1),
                new EggGen.SYMB(2)
        ));

        List<SimpleEntry<EggGen.Circuit, EggGen.Circuit>> pairs = new ArrayList<>();

        // ------------------------------------------------------------------
        // Pass 1: full 6×6 = 36 Pauli-axis orbit.
        // The 6 axis-permutation Cliffords (each maps X to one of ±X/±Y/±Z):
        //   AXIS_I       : I              -- X → +X
        //   AXIS_NEG_X   : RZ(π)  (= Z)   -- X → -X  (Z·X·Z† = -X)
        //   AXIS_POS_Y   : RZ(π/2)        -- X → +Y  (Rz(π/2)·X·Rz(-π/2) = +Y)
        //   AXIS_NEG_Y   : RZ(-π/2)       -- X → -Y
        //   AXIS_POS_Z   : RY(-π/2)       -- X → +Z  (Ry(-π/2)·X·Ry(π/2) = +Z)
        //   AXIS_NEG_Z   : RY(π/2)        -- X → -Z
        //
        // For each (axis_a, axis_b) ∈ choices², build the circuit:
        //     SYMB;  U_a†(q0); U_b†(q1);  RXX(θ);  U_a(q0); U_b(q1)
        // where U† is the same gate-type with negated angle (since R(θ)† = R(-θ)).
        // AXIS_I means "no gate" — skip both U† and U on that qubit.
        AxisClifford[] choices = {
                new AxisClifford("I",      null,  null),       // X → +X
                new AxisClifford("Z=-X",   "rz",  pi),         // X → -X
                new AxisClifford("Y=+Y",   "rz",  piHalf),     // X → +Y
                new AxisClifford("Y=-Y",   "rz",  negPiHalf),  // X → -Y
                new AxisClifford("Z=+Z",   "ry",  negPiHalf),  // X → +Z
                new AxisClifford("Z=-Z",   "ry",  piHalf),     // X → -Z
        };

        for (AxisClifford ua : choices) {
            for (AxisClifford ub : choices) {
                pairs.add(new SimpleEntry<>(L, simpleAxisDecomp(ua, ub, theta1)));
            }
        }

        // ------------------------------------------------------------------
        // Pass 2: 4 compound-Clifford pairs (Ry·Rx, Rz·Rx wrappers). These
        // catch the specific (Ry(π/2)·Rx(-π/2))⊗(Ry(-π/2)·Rx(-π/2))-style
        // middles produced by ion-native QASM compilers. Empirically:
        // pair "Y anti" matches the 4gt11_83 / ham3_102 / rd32-v0_66
        // gadget structures.
        pairs.add(new SimpleEntry<>(L, paired2GateDecomp(
                "ry", "rx", piHalf,    piHalf,      // U_a† = ry(π/2); rx(π/2)
                "ry", "rx", negPiHalf, piHalf,      // U_b† = ry(-π/2); rx(π/2)
                theta1,
                "rx", "ry", negPiHalf, negPiHalf,   // U_a  = rx(-π/2); ry(-π/2)
                "rx", "ry", negPiHalf, piHalf       // U_b  = rx(-π/2); ry(π/2)
        )));
        pairs.add(new SimpleEntry<>(L, paired2GateDecomp(
                "ry", "rx", negPiHalf, piHalf,
                "ry", "rx", negPiHalf, piHalf,
                theta1,
                "rx", "ry", negPiHalf, piHalf,
                "rx", "ry", negPiHalf, piHalf
        )));
        pairs.add(new SimpleEntry<>(L, paired2GateDecomp(
                "rz", "rx", negPiHalf, piHalf,
                "rz", "rx", piHalf,    piHalf,
                theta1,
                "rx", "rz", negPiHalf, piHalf,
                "rx", "rz", negPiHalf, negPiHalf
        )));
        pairs.add(new SimpleEntry<>(L, paired2GateDecomp(
                "rz", "rx", negPiHalf, piHalf,
                "rz", "rx", negPiHalf, piHalf,
                theta1,
                "rx", "rz", negPiHalf, piHalf,
                "rx", "rz", negPiHalf, piHalf
        )));

        return pairs;
    }

    /** Single-qubit axis-permutation Clifford: one gate (or none for identity). */
    private static final class AxisClifford {
        final String name;
        final String gateName;   // "rx" / "ry" / "rz", or null for identity
        final Expr   angle;      // the angle, or null for identity
        AxisClifford(String name, String gateName, Expr angle) {
            this.name = name; this.gateName = gateName; this.angle = angle;
        }
        boolean isIdentity() { return gateName == null; }
    }

    /**
     * R-side decomposition for the simple (single-rotation-per-side) Clifford
     * orbit. The circuit is:
     *     SYMB;
     *     U_a†(q0);  U_b†(q1);
     *     RXX(θ);
     *     U_a(q0);   U_b(q1);
     * where a Clifford U with gate G(α) has U† = G(-α). Identity Cliffords
     * contribute no gate.
     */
    private static EggGen.Circuit simpleAxisDecomp(AxisClifford ua, AxisClifford ub, Expr theta) {
        List<EggGen.Gate> gates = new ArrayList<>();
        gates.add(new EggGen.SYMB(2));
        // Dagger (prefix) gates -- negate the angle.
        if (!ua.isIdentity()) gates.add(mkGate(ua.gateName, "q0", negate(ua.angle)));
        if (!ub.isIdentity()) gates.add(mkGate(ub.gateName, "q1", negate(ub.angle)));
        gates.add(new EggGen.RXX("q0", "q1", theta));
        // Forward (suffix) gates.
        if (!ua.isIdentity()) gates.add(mkGate(ua.gateName, "q0", ua.angle));
        if (!ub.isIdentity()) gates.add(mkGate(ub.gateName, "q1", ub.angle));
        return new EggGen.Circuit(gates);
    }

    /** Negate an angle expression. Constant-folds -(- x) to x. */
    private static Expr negate(Expr e) {
        if (e instanceof UnOp uo && uo.getOp() == Expr.Op.MINUS) {
            return uo.getE();
        }
        return new UnOp(Expr.Op.MINUS, e);
    }

    /**
     * Compound-Clifford decomposition (2-gate U per qubit). Used for the
     * 4 ion-compiler-specific pairs. Circuit layout: 10 gates after SYMB
     * (4 prefix + RXX + 4 suffix).
     */
    private static EggGen.Circuit paired2GateDecomp(
            String aPre0, String aPre1, Expr aPre0Angle, Expr aPre1Angle,
            String bPre0, String bPre1, Expr bPre0Angle, Expr bPre1Angle,
            Expr theta,
            String aSuf0, String aSuf1, Expr aSuf0Angle, Expr aSuf1Angle,
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
