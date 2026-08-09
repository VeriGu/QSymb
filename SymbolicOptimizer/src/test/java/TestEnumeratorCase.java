import java.util.*;
import ast.*;

public class TestEnumeratorCase {
    static Circuit getStart(int n) {
        ArrayList<String> qubits = new ArrayList<>();
        TreeMap<String, Expr> f = new TreeMap<>();
        for (int i = 0; i < n; i++) {
            String name = "q" + i;
            qubits.add(name);
            f.put(name, new Var(name));
        }
        Symbolic s = new Symbolic(new Real(1), f);
        ArrayList<Symbolic> pathSum = new ArrayList<>(Arrays.asList(s));
        return new Circuit(qubits, pathSum, new ArrayList<>(), new ArrayList<>());
    }

    public static void main(String[] args) {
        Random rand = new Random(30);
        Verifier verifier = new Verifier(rand, 3);

        Map<String, Double> sm = new HashMap<>();
        sm.put(Symbolic.S_PHI, rand.nextDouble());
        for (String a : new String[]{"theta1", "theta2", "theta3"}) sm.put(a, rand.nextDouble());

        Circuit r = getStart(3);
        Circuit other = getStart(3);
        Symbolic.rz(other, "q2", new Symbol("theta1"));

        System.out.println("r.qasm = [" + r.getQasmString() + "]");
        System.out.println("other.qasm = [" + other.getQasmString() + "]");
        System.out.println("r.qubits = " + r.getQubits() + ", r.usedQubits = " + r.getUsedQubits());
        System.out.println("other.qubits = " + other.getQubits() + ", other.usedQubits = " + other.getUsedQubits());
        System.out.println("r.pathSum.size = " + r.getPathSum().size());
        System.out.println("other.pathSum.size = " + other.getPathSum().size());
        boolean v = verifier.verifyv2(r, other, sm);
        System.out.println("verifyv2(r, other) = " + v);

        Circuit other2 = getStart(3);
        Symbolic.rxx(other2, "q2", "q0", new UnOp(Expr.Op.MINUS, new Symbol("theta1")));
        System.out.println("\nother2.qasm = [" + other2.getQasmString() + "]");
        System.out.println("verifyv2(r, other2) = " + verifier.verifyv2(r, other2, sm));
    }
}
