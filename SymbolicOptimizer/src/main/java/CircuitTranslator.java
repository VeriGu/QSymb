import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.HashMap;

import ast.Expr;
import ast.Real;
import ast.Symbol;
import ast.BinOp;
import ast.UnOp;

public class CircuitTranslator {

    private static final Map<String, ConstrainedCircuit> cache = new HashMap<>();

    public static EggGen.ConstrainedCircuit translate(Circuit circuit) {
        EggGen.Circuit eggGenCircuit = translateCircuit(circuit);
        EggGen.Permutation emptyPermutation = new EggGen.Permutation(new ArrayList<>());
        EggGen.ConstrainedCircuit eggConstrainedCircuit = new EggGen.ConstrainedCircuit(eggGenCircuit, emptyPermutation);
        cache.put(eggConstrainedCircuit.toEggString(), new ConstrainedCircuit(circuit, new ArrayList<>()));
        return eggConstrainedCircuit;
    }

    public static EggGen.ConstrainedCircuit translate(ConstrainedCircuit constrainedCircuit) {
        EggGen.Circuit eggGenCircuit = translateCircuit(constrainedCircuit.getCircuit());
        EggGen.Permutation permutation = new EggGen.Permutation(constrainedCircuit.getConstraint());
        EggGen.ConstrainedCircuit eggConstrainedCircuit = new EggGen.ConstrainedCircuit(eggGenCircuit, permutation);
        cache.put(eggConstrainedCircuit.toEggString(), constrainedCircuit);
        return eggConstrainedCircuit;
    }

    public static ConstrainedCircuit translateBack(EggGen.ConstrainedCircuit eggCircuit) {
        String eggString = eggCircuit.toEggString();
        if (cache.containsKey(eggString)) {
            return cache.get(eggString);
        }

        // If not in cache, it's a new circuit from egglog, so we construct it.
        Circuit circuit = translateCircuitBack(eggCircuit.circuit);
        List<Integer> permutation = eggCircuit.permutation.perm;
        ConstrainedCircuit newConstrainedCircuit = new ConstrainedCircuit(circuit, permutation);
        cache.put(eggString, newConstrainedCircuit);
        return newConstrainedCircuit;
    }

    private static Circuit translateCircuitBack(EggGen.Circuit eggCircuit) {
        return reconstructCircuit(eggCircuit.gates);
    }

    private static Circuit reconstructCircuit(List<EggGen.Gate> eggGates) {
        // Initialize an empty circuit
        Circuit circuit = new Circuit(new ArrayList<>(), new ArrayList<>(), new ArrayList<>());

        // Apply each gate to the circuit
        for (EggGen.Gate gate : eggGates) {
            if (gate instanceof EggGen.X x) {
                Symbolic.x(circuit, x.qubit);
            } else if (gate instanceof EggGen.CX cx) {
                Symbolic.cx(circuit, cx.control, cx.target);
            } else if (gate instanceof EggGen.RZ rz) {
                Symbolic.rz(circuit, rz.qubit, rz.angle);
            } else if (gate instanceof EggGen.H h) {
                Symbolic.h(circuit, h.qubit);
            } else if (gate instanceof EggGen.SYMB symb) {
                // Assuming SYMB gate doesn't directly affect qubits or pathSum in this context
                // If it does, we need to handle it appropriately.
                // For now, we'll just add its QASM string to the circuit.
                circuit.getQasm().add(gateToQasm(gate));
            } else if (gate instanceof EggGen.U1 u1) {
                Symbolic.u1(circuit, u1.qubit, u1.lambda);
            } else if (gate instanceof EggGen.U2 u2) {
                Symbolic.u2(circuit, u2.qubit, u2.phi, u2.lambda);
            } else if (gate instanceof EggGen.U3 u3) {
                Symbolic.u3(circuit, u3.qubit, u3.theta, u3.phi, u3.lambda);
            } else if (gate instanceof EggGen.RX rx) {
                Symbolic.rx(circuit, rx.qubit, rx.angle);
            } else if (gate instanceof EggGen.CZ cz) {
                Symbolic.cz(circuit, cz.control, cz.target);
            } else if (gate instanceof EggGen.RY ry) {
                Symbolic.ry(circuit, ry.qubit, ry.angle);
            } else if (gate instanceof EggGen.RXX rxx) {
                Symbolic.rxx(circuit, rxx.qubit1, rxx.qubit2, rxx.angle);
            } else if (gate instanceof EggGen.GPI gpi) {
                Symbolic.gpi(circuit, gpi.qubit, gpi.phi);
            } else if (gate instanceof EggGen.GPI2 gpi2) {
                Symbolic.gpi2(circuit, gpi2.qubit, gpi2.phi);
            } else if (gate instanceof EggGen.VZ vz) {
                Symbolic.vz(circuit, vz.qubit, vz.theta);
            } else if (gate instanceof EggGen.MS ms) {
                Symbolic.ms(circuit, ms.qubit1, ms.qubit2, ms.phi1, ms.phi2);
            } else if (gate instanceof EggGen.SX sx) {
                Symbolic.sx(circuit, sx.qubit);
            }
        }
        return circuit;
    }


    private static String gateToQasm(EggGen.Gate gate) {
        if (gate instanceof EggGen.X x) {
            return String.format("x %s", x.qubit);
        } else if (gate instanceof EggGen.H h) {
            return String.format("h %s", h.qubit);
        } else if (gate instanceof EggGen.SX sx) {
            return String.format("sx %s", sx.qubit);
        } else if (gate instanceof EggGen.CX cx) {
            return String.format("cx %s, %s", cx.control, cx.target);
        } else if (gate instanceof EggGen.CZ cz) {
            return String.format("cz %s, %s", cz.control, cz.target);
        } else if (gate instanceof EggGen.RZ rz) {
            return String.format("rz(%s) %s", rz.angle, rz.qubit);
        } else if (gate instanceof EggGen.RX rx) {
            return String.format("rx(%s) %s", rx.angle, rx.qubit);
        } else if (gate instanceof EggGen.RY ry) {
            return String.format("ry(%s) %s", ry.angle, ry.qubit);
        } else if (gate instanceof EggGen.U1 u1) {
            return String.format("u1(%s) %s", u1.lambda, u1.qubit);
        } else if (gate instanceof EggGen.U2 u2) {
            return String.format("u2(%s,%s) %s", u2.phi, u2.lambda, u2.qubit);
        } else if (gate instanceof EggGen.U3 u3) {
            return String.format("u3(%s,%s,%s) %s", u3.theta, u3.phi, u3.lambda, u3.qubit);
        } else if (gate instanceof EggGen.RXX rxx) {
            return String.format("rxx(%s) %s, %s", rxx.angle, rxx.qubit1, rxx.qubit2);
        } else if (gate instanceof EggGen.GPI gpi) {
            return String.format("gpi(%s) %s", gpi.phi, gpi.qubit);
        } else if (gate instanceof EggGen.GPI2 gpi2) {
            return String.format("gpi2(%s) %s", gpi2.phi, gpi2.qubit);
        } else if (gate instanceof EggGen.VZ vz) {
            return String.format("rz(%s) %s", vz.theta, vz.qubit); // Note: This seems to be a typo, should be "vz" not "rz"
        } else if (gate instanceof EggGen.MS ms) {
            return String.format("ms (%s,%s) %s, %s", ms.phi1, ms.phi2, ms.qubit1, ms.qubit2);
        } else if (gate instanceof EggGen.SYMB) {
            return "symb q";
        }
        return "";
    }

    private static EggGen.Circuit translateCircuit(Circuit circuit) {
        List<EggGen.Gate> gates = new ArrayList<>();
        for (String qasmString : circuit.getQasm()) {
            EggGen.Gate gate = parseQasmInstruction(qasmString);
            if (gate != null) {
                gates.add(gate);
            }
        }
        return new EggGen.Circuit(gates);
    }

    private static EggGen.Gate parseQasmInstruction(String qasmString) {
        qasmString = qasmString.trim();
        String[] parts = qasmString.split(" ", 2);
        String gateName = parts[0];

        if (gateName.equals("x")) {
            return new EggGen.X(parseQubitFromArg(parts[1]));
        } else if (gateName.equals("h")) {
            return new EggGen.H(parseQubitFromArg(parts[1]));
        } else if (gateName.equals("sx")) {
            return new EggGen.SX(parseQubitFromArg(parts[1]));
        } else if (gateName.equals("cx")) {
            String[] args = parts[1].split(",");
            return new EggGen.CX(parseQubitFromArg(args[0]), parseQubitFromArg(args[1]));
        } else if (gateName.startsWith("rz")) {
            return parse1Qubit1ParamGate(qasmString, "rz");
        } else if (gateName.startsWith("u1")) {
            return parse1Qubit1ParamGate(qasmString, "u1");
        } else if (gateName.startsWith("rx")) {
            return parse1Qubit1ParamGate(qasmString, "rx");
        } else if (gateName.startsWith("ry")) {
            return parse1Qubit1ParamGate(qasmString, "ry");
        } else if (gateName.startsWith("gpi")) {
            return parse1Qubit1ParamGate(qasmString, "gpi");
        } else if (gateName.startsWith("gpi2")) {
            return parse1Qubit1ParamGate(qasmString, "gpi2");
        } else if (gateName.startsWith("vz")) {
            return parse1Qubit1ParamGate(qasmString, "vz");
        } else if (gateName.startsWith("u2")) {
            return parse1Qubit2ParamGate(qasmString, "u2");
        } else if (gateName.startsWith("u3")) {
            return parse1Qubit3ParamGate(qasmString, "u3");
        } else if (gateName.equals("cz")) {
            String[] args = parts[1].split(",");
            return new EggGen.CZ(parseQubitFromArg(args[0]), parseQubitFromArg(args[1]));
        } else if (gateName.startsWith("rxx")) {
            return parse2Qubit1ParamGate(qasmString, "rxx");
        } else if (gateName.startsWith("ms")) {
            return parse2Qubit2ParamGate(qasmString, "ms");
        } else if (gateName.equals("symb q")) {
            // This is a special case, not standard QASM
            return new EggGen.SYMB(0); // The number of qubits is not in the string
        }

        return null;
    }

    private static String parseQubitFromArg(String arg) {
        return arg.trim().replace("q[", "q").replace("]", "");
    }

    private static EggGen.Gate parse1Qubit1ParamGate(String qasmString, String gateName) {
        Pattern pattern = Pattern.compile(gateName + "\\((.*?)\\) (.*)");
        Matcher matcher = pattern.matcher(qasmString);
        if (matcher.matches()) {
            String param = matcher.group(1);
            String qubit = parseQubitFromArg(matcher.group(2));
            switch (gateName) {
                case "rz": return new EggGen.RZ(qubit, param);
                case "u1": return new EggGen.U1(qubit, param);
                case "rx": return new EggGen.RX(qubit, param);
                case "ry": return new EggGen.RY(qubit, param);
                case "gpi": return new EggGen.GPI(qubit, param);
                case "gpi2": return new EggGen.GPI2(qubit, param);
                case "vz": return new EggGen.VZ(qubit, param);
            }
        }
        return null;
    }

    private static EggGen.Gate parse1Qubit2ParamGate(String qasmString, String gateName) {
        Pattern pattern = Pattern.compile(gateName + "\\((.*?),\\s*(.*?)\\) (.*)");
        Matcher matcher = pattern.matcher(qasmString);
        if (matcher.matches()) {
            String param1 = matcher.group(1);
            String param2 = matcher.group(2);
            String qubit = parseQubitFromArg(matcher.group(3));
            if ("u2".equals(gateName)) {
                return new EggGen.U2(qubit, param1, param2);
            }
        }
        return null;
    }

    private static EggGen.Gate parse1Qubit3ParamGate(String qasmString, String gateName) {
        Pattern pattern = Pattern.compile(gateName + "\\((.*?),\\s*(.*?),\\s*(.*?)\\) (.*)");
        Matcher matcher = pattern.matcher(qasmString);
        if (matcher.matches()) {
            String param1 = matcher.group(1);
            String param2 = matcher.group(2);
            String param3 = matcher.group(3);
            String qubit = parseQubitFromArg(matcher.group(4));
            if ("u3".equals(gateName)) {
                return new EggGen.U3(qubit, param1, param2, param3);
            }
        }
        return null;
    }

    private static EggGen.Gate parse2Qubit1ParamGate(String qasmString, String gateName) {
        Pattern pattern = Pattern.compile(gateName + "\\((.*?)\\) (.*), (.*)");
        Matcher matcher = pattern.matcher(qasmString);
        if (matcher.matches()) {
            String param = matcher.group(1);
            String qubit1 = parseQubitFromArg(matcher.group(2));
            String qubit2 = parseQubitFromArg(matcher.group(3));
            if ("rxx".equals(gateName)) {
                return new EggGen.RXX(qubit1, qubit2, param);
            }
        }
        return null;
    }

    private static EggGen.Gate parse2Qubit2ParamGate(String qasmString, String gateName) {
        Pattern pattern = Pattern.compile(gateName + "\\((.*?),\\s*(.*?)\\) (.*), (.*)");
        Matcher matcher = pattern.matcher(qasmString);
        if (matcher.matches()) {
            String param1 = matcher.group(1);
            String param2 = matcher.group(2);
            String qubit1 = parseQubitFromArg(matcher.group(3));
            String qubit2 = parseQubitFromArg(matcher.group(4));
            if ("ms".equals(gateName)) {
                return new EggGen.MS(qubit1, qubit2, param1, param2);
            }
        }
        return null;
    }
}