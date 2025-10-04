import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import ast.Expr;
import ast.Real;

public class SymbolicSolve {

    public String circuitToJson(EggGen.Circuit circuit) {
        return "[" + circuit.gates.stream()
                .map(this::gateToJson)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.joining(",")) + "]";
    }

    private String gateToJson(EggGen.Gate gate) {
        String gateName;
        List<Integer> targets = new ArrayList<>();
        String paramsJson = null;

        if (gate instanceof EggGen.X) {
            gateName = "x";
            targets.add(parseQubit(((EggGen.X) gate).qubit));
        } else if (gate instanceof EggGen.H) {
            gateName = "h";
            targets.add(parseQubit(((EggGen.H) gate).qubit));
        } else if (gate instanceof EggGen.SX) {
            gateName = "sx";
            targets.add(parseQubit(((EggGen.SX) gate).qubit));
        } else if (gate instanceof EggGen.CX) {
            gateName = "cx";
            targets.add(parseQubit(((EggGen.CX) gate).control));
            targets.add(parseQubit(((EggGen.CX) gate).target));
        } else if (gate instanceof EggGen.CZ) {
            gateName = "cz";
            targets.add(parseQubit(((EggGen.CZ) gate).control));
            targets.add(parseQubit(((EggGen.CZ) gate).target));
        } else if (gate instanceof EggGen.RZ) {
            gateName = "rz";
            targets.add(parseQubit(((EggGen.RZ) gate).qubit));
            paramsJson = paramToJson("gamma", ((EggGen.RZ) gate).angle);
        } else if (gate instanceof EggGen.RX) {
            gateName = "rx";
            targets.add(parseQubit(((EggGen.RX) gate).qubit));
            paramsJson = paramToJson("theta", ((EggGen.RX) gate).angle);
        } else if (gate instanceof EggGen.RY) {
            gateName = "ry";
            targets.add(parseQubit(((EggGen.RY) gate).qubit));
            paramsJson = paramToJson("theta", ((EggGen.RY) gate).angle);
        } else if (gate instanceof EggGen.U1) {
            gateName = "u1";
            targets.add(parseQubit(((EggGen.U1) gate).qubit));
            paramsJson = paramToJson("lam", ((EggGen.U1) gate).lambda);
        } else if (gate instanceof EggGen.U2) {
            gateName = "u2";
            targets.add(parseQubit(((EggGen.U2) gate).qubit));
            paramsJson = paramsToJson(
                new String[]{"phi", "lam"},
                new Expr[]{((EggGen.U2) gate).phi, ((EggGen.U2) gate).lambda}
            );
        } else if (gate instanceof EggGen.U3) {
            gateName = "u3";
            targets.add(parseQubit(((EggGen.U3) gate).qubit));
            paramsJson = paramsToJson(
                new String[]{"theta", "phi", "lam"},
                new Expr[]{((EggGen.U3) gate).theta, ((EggGen.U3) gate).phi, ((EggGen.U3) gate).lambda}
            );
        } else if (gate instanceof EggGen.RXX) {
            gateName = "rxx";
            targets.add(parseQubit(((EggGen.RXX) gate).qubit1));
            targets.add(parseQubit(((EggGen.RXX) gate).qubit2));
            paramsJson = paramToJson("theta", ((EggGen.RXX) gate).angle);
        } else {
            return null;
        }

        if (paramsJson == null && (gate instanceof EggGen.RZ || gate instanceof EggGen.RX || gate instanceof EggGen.RY || gate instanceof EggGen.U1 || gate instanceof EggGen.U2 || gate instanceof EggGen.U3 || gate instanceof EggGen.RXX)) {
            return null;
        }

        String targetsJson = "[" + targets.stream().map(String::valueOf).collect(Collectors.joining(",")) + "]";
        
        if (paramsJson != null) {
            return String.format("{\"gate\":\"%s\",\"targets\":%s,\"params\":%s}", gateName, targetsJson, paramsJson);
        } else {
            return String.format("{\"gate\":\"%s\",\"targets\":%s}", gateName, targetsJson);
        }
    }

    private String paramToJson(String paramName, Expr expr) {
        if (expr instanceof Real) {
            double value = ((Real) expr).getNumber();
            return String.format("{\"" + paramName + "\":%f}", value);
        }
        return null;
    }

    private String paramsToJson(String[] paramNames, Expr[] exprs) {
        List<String> paramEntries = new ArrayList<>();
        for (int i = 0; i < paramNames.length; i++) {
            if (exprs[i] instanceof Real) {
                double value = ((Real) exprs[i]).getNumber();
                paramEntries.add(String.format("\"%s\":%f", paramNames[i], value));
            } else {
                return null;
            }
        }
        return "{" + String.join(",", paramEntries) + "}";
    }

    private int parseQubit(String qubitStr) {
        if (qubitStr.startsWith("q")) {
            return Integer.parseInt(qubitStr.substring(1));
        }
        return Integer.parseInt(qubitStr);
    }
    
    public static void main(String[] args) {
        // Test case 1: Bell state (non-parameterized)
        List<EggGen.Gate> bellGates = new ArrayList<>();
        bellGates.add(new EggGen.H("q0"));
        bellGates.add(new EggGen.CX("q0", "q1"));
        EggGen.Circuit bellStateCircuit = new EggGen.Circuit(bellGates);

        SymbolicSolve solver = new SymbolicSolve();
        String bellJson = solver.circuitToJson(bellStateCircuit);

        System.out.println("Generated JSON for Bell state circuit:");
        System.out.println(bellJson);
        System.out.println();

        // Test case 2: Parameterized circuit
        List<EggGen.Gate> paramGates = new ArrayList<>();
        paramGates.add(new EggGen.RZ("q0", new Real(1.57)));
        paramGates.add(new EggGen.RX("q1", new Real(3.14)));
        
        EggGen.Circuit paramCircuit = new EggGen.Circuit(paramGates);
        String paramJson = solver.circuitToJson(paramCircuit);
        
        System.out.println("Generated JSON for parameterized circuit:");
        System.out.println(paramJson);
    }
}