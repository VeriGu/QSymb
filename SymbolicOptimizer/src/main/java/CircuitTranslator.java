import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import ast.Expr;
import ast.Real;
import ast.Var;

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
        if(constrainedCircuit.getCachedEgg() != null) {
            return constrainedCircuit.getCachedEgg();
        }
        EggGen.Circuit eggGenCircuit = translateCircuit(constrainedCircuit.getCircuit());
        EggGen.Permutation permutation = new EggGen.Permutation(constrainedCircuit.getConstraint());
        EggGen.ConstrainedCircuit eggConstrainedCircuit = new EggGen.ConstrainedCircuit(eggGenCircuit, permutation);
        constrainedCircuit.cacheEgg(eggConstrainedCircuit);
        cache.put(eggConstrainedCircuit.toEggString(), constrainedCircuit);
        return eggConstrainedCircuit;
    }

    public static ConstrainedCircuit translateBack(EggGen.ConstrainedCircuit eggCircuit, int maxQubits) {
        String eggString = eggCircuit.toEggString();
        if (cache.containsKey(eggString)) {
            return cache.get(eggString);
        }

        // If not in cache, it's a new circuit from egglog, so we construct it.
        Circuit circuit = translateCircuitBack(eggCircuit.circuit, maxQubits);
        List<Integer> permutation = eggCircuit.permutation.perm;
        ConstrainedCircuit newConstrainedCircuit = new ConstrainedCircuit(circuit, permutation);
        cache.put(eggString, newConstrainedCircuit);
        return newConstrainedCircuit;
    }

    private static Circuit translateCircuitBack(EggGen.Circuit eggCircuit, int maxQubits) {
        return reconstructCircuit(eggCircuit, maxQubits);
    }

    private static String getName(int qubit) {
        return String.format("q%s", qubit);
    }

    private static Circuit getStart(int maxQubits) {
        ArrayList<String> qubits = new ArrayList<>();
        TreeMap<String, Expr> f = new TreeMap<>();
    
        for (int i = 0; i < maxQubits; i++) {
            String name = getName(i);
            qubits.add(name);
            f.put(name, new Var(name));
        }
    
        Symbolic s = new Symbolic(new Real(1), f);
        ArrayList<Symbolic> pathSum = new ArrayList<>(Arrays.asList(s));
    
        return new Circuit(qubits, pathSum, new ArrayList<>(), new ArrayList<>());
        
    }

    private static Circuit reconstructCircuit(EggGen.Circuit eggCircuit, int maxQubits) {
        // Initialize an empty circuit
        Circuit circuit = getStart(maxQubits);

        // Apply each gate to the circuit
        for (EggGen.Gate gate : eggCircuit.gates) {
            if (gate instanceof EggGen.X x) {
                Symbolic.x(circuit, x.qubit);
            } else if (gate instanceof EggGen.CX cx) {
                Symbolic.cx(circuit, cx.control, cx.target);
            } else if (gate instanceof EggGen.RZ rz) {
                Symbolic.rz(circuit, rz.qubit, rz.angle);
            } else if (gate instanceof EggGen.H h) {
                Symbolic.h(circuit, h.qubit);
            } else if (gate instanceof EggGen.SYMB symb) {
                Symbolic.symb(circuit, EnumeratorPrune.MAX_QUBITS_SYMB);
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
        List<EggGen.Gate> gates = circuit.getGates();
        return new EggGen.Circuit(gates);
    }


    private static String parseQubitFromArg(String arg) {
        return arg.trim().replace("q[", "q").replace("]", "");
    }
}