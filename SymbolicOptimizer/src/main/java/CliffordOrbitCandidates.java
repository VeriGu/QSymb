import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ast.BinOp;
import ast.Expr;
import ast.Real;
import ast.Symbol;
import ast.UnOp;

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

        EggGen.Circuit L = new EggGen.Circuit(List.of(
                new EggGen.RXX("q0", "q1", theta1),
                new EggGen.SYMB(2)
        ));

        List<SimpleEntry<EggGen.Circuit, EggGen.Circuit>> pairs = new ArrayList<>();

        AxisClifford[] choices = {
                new AxisClifford("I",      null,  null),
                new AxisClifford("Z=-X",   "rz",  pi),
                new AxisClifford("Y=+Y",   "rz",  piHalf),
                new AxisClifford("Y=-Y",   "rz",  negPiHalf),
                new AxisClifford("Z=+Z",   "ry",  negPiHalf),
                new AxisClifford("Z=-Z",   "ry",  piHalf),
        };

        for (AxisClifford ua : choices) {
            for (AxisClifford ub : choices) {
                pairs.add(new SimpleEntry<>(L, simpleAxisDecomp(ua, ub, theta1)));
            }
        }

        pairs.add(new SimpleEntry<>(L, paired2GateDecomp(
                "ry", "rx", piHalf,    piHalf,
                "ry", "rx", negPiHalf, piHalf,
                theta1,
                "rx", "ry", negPiHalf, negPiHalf,
                "rx", "ry", negPiHalf, piHalf
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

    private static final class AxisClifford {
        final String name;
        final String gateName;
        final Expr   angle;
        AxisClifford(String name, String gateName, Expr angle) {
            this.name = name; this.gateName = gateName; this.angle = angle;
        }
        boolean isIdentity() { return gateName == null; }
    }

    private static EggGen.Circuit simpleAxisDecomp(AxisClifford ua, AxisClifford ub, Expr theta) {
        List<EggGen.Gate> gates = new ArrayList<>();
        gates.add(new EggGen.SYMB(2));
        if (!ua.isIdentity()) gates.add(mkGate(ua.gateName, "q0", negate(ua.angle)));
        if (!ub.isIdentity()) gates.add(mkGate(ub.gateName, "q1", negate(ub.angle)));
        gates.add(new EggGen.RXX("q0", "q1", theta));
        if (!ua.isIdentity()) gates.add(mkGate(ua.gateName, "q0", ua.angle));
        if (!ub.isIdentity()) gates.add(mkGate(ub.gateName, "q1", ub.angle));
        return new EggGen.Circuit(gates);
    }

    private static Expr negate(Expr e) {
        if (e instanceof UnOp uo && uo.getOp() == Expr.Op.MINUS) {
            return uo.getE();
        }
        return new UnOp(Expr.Op.MINUS, e);
    }

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
