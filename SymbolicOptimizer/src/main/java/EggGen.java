import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;

import ast.Expr;




public class EggGen {

    private final StringBuilder content = new StringBuilder();
    private final Set<String> rules = new HashSet<>();
    private Integer numCircuits;
    private final Map<String, Circuit> circuits = new HashMap<>();

    private Process egglogProcess;
    private BufferedWriter processInput;
    private BufferedReader processOutput;
    private BufferedReader processError;
    public EggGen() {
        numCircuits = 0;
        // Add standard datatype and function definitions from qast.egg
        content.append("\n(datatype Op\n  (EXP) (SQRT) (MINUS) (COS) (SIN) (NOT) (PLUS) (SUBTRACT) (MULT) (DIV) (POWER) (XOR) (AND) (OR))\n");
        content.append("\n(datatype Expr\n  (Bool bool) (Real f64) (Symbol String) (Var String) (Fun String Expr) (UnOp Op Expr) (BinOp Op Expr Expr))\n");
        content.append("\n(datatype Qubit (Q String))\n");
        content.append("\n(datatype Gate\n  (X Qubit) (CX Qubit Qubit) (RZ Qubit Expr) (H Qubit) (SYMB i64) (U1 Qubit Expr) (U2 Qubit Expr Expr)\n  (U3 Qubit Expr Expr Expr) (RX Qubit Expr) (CZ Qubit Qubit) (RY Qubit Expr) (RXX Qubit Qubit Expr)\n  (GPI Qubit Expr) (GPI2 Qubit Expr) (VZ Qubit Expr) (MS Qubit Qubit Expr Expr) (SX Qubit))\n");
        content.append("\n(datatype Circuit (Nil) (Cons Gate Circuit))\n");
        content.append("\n(datatype Value (B bool) (R f64))\n");
        content.append("\n(datatype Permutation (PermNil) (PermCons i64 Permutation))\n");
        content.append("\n(datatype ConstrainedCircuit (CCircuit Circuit Permutation))\n");
        content.append("\n(function fingerprint (ConstrainedCircuit) i64 :merge new)\n");
        content.append("\n(function size (Circuit) i64 :merge (min old new))\n");
        content.append("(ruleset mergefinger)");
        content.append("(ruleset sizeanalysis)");
        content.append("(ruleset noteqfinger)");
        content.append("(ruleset opt)");
        content.append("(rule\n" + //
                        " ((= x (Nil)))\n" + //
                        " ((set (size x) 1))\n" + //
                        ":ruleset sizeanalysis)");
        content.append("(rule\n" + //
                        " ((= x (Cons y z))\n" + //
                        "  (= s (size z))\n" + //
                        " )\n" + //
                        " (\n" + //
                        "  (set (size x) (+ 1 s))\n" + //
                        " )\n" + //
                        ":ruleset sizeanalysis)");
        content.append("(relation notSameButEqfinger (ConstrainedCircuit ConstrainedCircuit))");
        content.append("(rule \n" + //
                        "(" + //
                        " (= x (CCircuit cx p1))\n" + //
                        " (= y (CCircuit cy p2))\n" + //
                        " (<= (size cx) (size cy))\n" + //
                        " (!= x y)\n" + //
                        " (= (fingerprint x) (fingerprint y))\n" + //
                        " (= p1 p2)\n" + //
                        ")\n" + //
                        "(" + //
                        " (notSameButEqfinger x y)\n" + //
                        ")\n" + //
                        ":ruleset noteqfinger)");
        content.append("(relation bad (ConstrainedCircuit ConstrainedCircuit))\n");
        try {
            startEgglogREPL();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(0);
        }
        String output = sendCommand(content.toString());
        System.out.println(output);
    }

    public void startEgglogREPL() throws IOException {
        ProcessBuilder pb = new ProcessBuilder("egglog");
        pb.environment().put("RUST_LOG", "ERROR");
        pb.redirectErrorStream(true);
        this.egglogProcess = pb.start();
        this.processInput = new BufferedWriter(new OutputStreamWriter(egglogProcess.getOutputStream()));
        this.processError = new BufferedReader(new InputStreamReader(egglogProcess.getErrorStream()));
        this.processOutput = new BufferedReader(new InputStreamReader(egglogProcess.getInputStream()));
    }

    public void stopEgglogREPL() {
        if (egglogProcess != null) {
            egglogProcess.destroy();
        }
    }

    public String sendCommand(String command) {
        return sendCommand(command, false);
    }

    public String sendCommand(String command, boolean wait) {
        if (processInput == null || processOutput == null) {
            System.out.println("REPL not started. Call startEgglogREPL() first.");
            return null;
        }
        try {
            processInput.write(command);
            processInput.newLine();
            processInput.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }

        String err = readError();
        if(!err.equals(""))
            System.err.print(err);
        if(wait) {
            return readOutput();
        } else {
            return "";
        }
    }

    public void setFingerprint(ConstrainedCircuit c, Integer fingerprint) {
        sendCommand(String.format("(set (fingerprint %s) %s)", c.toEggString(), fingerprint.toString()));
    }

    public void insertBad(ConstrainedCircuit c1, ConstrainedCircuit c2) {
        String relation = String.format("(bad %s %s)", c1.toEggString(), c2.toEggString());
        sendCommand(relation);
    }

    public void mergeFingerPrintsEQ() {
        sendCommand("(rule ((= (fingerprint x) (fingerprint y))) ((union x y)) :ruleset mergefinger)");
        runSaturation();
    }

    public List<SimpleEntry<EggGen.ConstrainedCircuit, EggGen.ConstrainedCircuit>> parseRelation(String rel) {
        rel = rel.replaceAll("\\n+$", "");
        List<SimpleEntry<EggGen.ConstrainedCircuit, EggGen.ConstrainedCircuit>> list = new ArrayList<>();
        try (CSVReader reader = new CSVReader(new StringReader(rel))) {
            String[] nextLine;
            while ((nextLine = reader.readNext()) != null) {
                String elem1 = nextLine[1];
                String elem2 = nextLine[2];
                EggGen.ConstrainedCircuit cc1 = EggAstBuilder.parse(elem1);
                EggGen.ConstrainedCircuit cc2 = EggAstBuilder.parse(elem2);
                list.add(new SimpleEntry(cc1, cc2));
            }
        } catch (CsvValidationException | IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
        return list;
    }

    public Map<String, List<EggGen.ConstrainedCircuit>> parseEnodes(String nodes) {
        nodes = nodes.replaceAll("\\n+$", "");
        if(nodes.equals("")) {
            return new HashMap<>();
        }
        Map<String, List<EggGen.ConstrainedCircuit>> map = new HashMap<>();
        try (CSVReader reader = new CSVReader(new StringReader(nodes))) {
            String[] nextLine;
            while ((nextLine = reader.readNext()) != null) {
                String elem1 = nextLine[1];
                String elem2 = nextLine[2];
                String eid = nextLine[nextLine.length-1];
                
                EggGen.Circuit c = EggAstBuilder.parseCircuit(elem1);
                EggGen.Permutation perm = EggAstBuilder.parsePerm(elem2);
                EggGen.ConstrainedCircuit cc = new ConstrainedCircuit(c, perm);
                // EggGen.ConstrainedCircuit cc2 = EggAstBuilder.parse(elem2);
                if(map.containsKey(eid)) {
                    map.get(eid).add(cc);
                } else {
                    map.put(eid, new ArrayList<>());
                }
            }
        } catch (CsvValidationException | IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
        return map;
    }

    private String readOutput() {
        StringBuilder output = new StringBuilder();
        // A short sleep to allow the process to start writing output
        try {
            int waited = 0;
            int timeout = 20000;
            while (!processOutput.ready()) {
                Thread.sleep(10);
                waited += 10;
                if (waited > timeout) 
                    break;
            }
            while (processOutput.ready()) {
                output.append((char) processOutput.read());
            }
            
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return output.toString();
    }

    private String readError() {
        StringBuilder output = new StringBuilder();
        // A short sleep to allow the process to start writing output
        try {
            while (processError.ready()) {
                output.append((char) processError.read());
            }
            
        } catch (IOException e) {
            e.printStackTrace();
        }
        return output.toString();
    }

    public void push() {
        sendCommand("(push)");
    }

    public void pop() {
        sendCommand("(pop)");
    }

    public String addCircuit(Circuit circuit) {
        Permutation emptyPermutation = new Permutation(new ArrayList<>());
        ConstrainedCircuit constrainedCircuit = new ConstrainedCircuit(circuit, emptyPermutation);
        return addConstrainedCircuit(constrainedCircuit);
    }

    public String addConstrainedCircuit(ConstrainedCircuit constrainedCircuit) {
        String name = "cc_" + numCircuits++;
        String output = sendCommand(String.format("(let %s %s)\n", name, constrainedCircuit.toEggString()));
        System.err.println(output);
        return name;
    }

    public void addRewrite(String rule) {
        if(!rules.contains(rule)) {
            System.out.println(rule);
            rules.add(rule);
            sendCommand(rule);
        }
    }

    public void addRewriteRule(SimpleEntry<ConstrainedCircuit, ConstrainedCircuit> ruleEntry) {
        String rule = String.format("(birewrite %s %s :ruleset opt)", ruleEntry.getKey().toCongruenceString(), ruleEntry.getValue().toCongruenceString());
        addRewrite(rule);
    }

    public void merge(ConstrainedCircuit eclass1, ConstrainedCircuit eclass2) {
        sendCommand(String.format("(union %s %s)\n", eclass1.toEggString(), eclass2.toEggString()));
    }

    public void getSmallestRep(String eclass) {
        sendCommand(String.format("(extract %s)\n", eclass), true);
    }

    public void runN(String ruleset, int n) {
        sendCommand(String.format("(run %s %d)\n", ruleset, n));
    }

    public void runSaturation() {
        sendCommand("(run-schedule (saturate (run)))\n");
    }

    public void runSaturation(String ruleSet) {
        sendCommand(String.format("(run-schedule (saturate (run %s)))\n", ruleSet));
    }

    public String printFunctionCSV(String name) {
        String output = sendCommand(String.format("(print-function %s :mode csv)", name), true);
        return output;
    }

 
    public String printFunctionListCSV(List<String> list) {             
        String output = sendCommand(String.format("(print-function %s :mode csv)", list.toArray()), true);
        return output; 
    }

   
    public void toFile(String path) throws IOException {
        FileWriter writer = new FileWriter(path);
        writer.write(content.toString());
        writer.close();
    }

    @Override
    public String toString() {
        return content.toString();
    }

    public static void main(String[] args) {
        EggGen eggGen = new EggGen();
        try {
            eggGen.startEgglogREPL();

            // Send the datatype definitions
            String datatypes = eggGen.content.toString();
            String output1 = eggGen.sendCommand(datatypes);
            System.out.println("Datatype definitions loaded:");
            System.out.println(output1);

            // Define a circuit
            List<Gate> gates = new ArrayList<>();
            gates.add(new X("q0"));
            gates.add(new X("q0"));
            Circuit circuit = new Circuit(gates);
            String circuitName = eggGen.addCircuit(circuit);
            System.out.println("Circuit defined: " + circuitName);

            // Add a rewrite rule
            String rule = "(rewrite (CCircuit (Cons (X q) (Cons (X q) (Nil))) (PermNil)) (CCircuit (Nil) (PermNil))) ";
            String output3 = eggGen.sendCommand(rule);
            System.out.println("Rule added:");
            System.out.println(output3);

            // Run saturation
            String output4 = eggGen.sendCommand("(run-schedule (saturate (run)))");
            System.out.println("Saturation complete:");
            System.out.println(output4);

            // Extract representative
            String output5 = eggGen.sendCommand("(extract " + circuitName + ")");
            System.out.println("Extracted representative:");
            System.out.println(output5);

            //parse
            EggGen.ConstrainedCircuit c = EggAstBuilder.parse(output5);


        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            eggGen.stopEgglogREPL();
        }
    }

    // Inner classes for Expr, Op, Value
    public static enum Op {
        EXP, SQRT, MINUS, COS, SIN, NOT, PLUS, SUBTRACT, MULT, DIV, POWER, XOR, AND, OR;

        @Override
        public String toString() {
            return super.toString();
        }
    }


    // Inner classes for Circuit and Gates
    public static class Gate {
        public String toEggString() {
            return "Gate";
        }
    }

    public static class Circuit implements EggExpr {
        public final List<Gate> gates;
        public Circuit(List<Gate> gates) {
            this.gates = gates;
        }


        public String toCongruenceString() {
            return toCongruenceStringRecursive(0, "circuit");
        }


        private String toCongruenceStringRecursive(int index, String varName) {
            if (index >= gates.size()) {
                return varName;
            }
            String currentGate = gates.get(index).toEggString();
            return String.format("(Cons %s %s)", gates.get(index).toEggString(), toCongruenceStringRecursive(index + 1, varName));
        }

        public String toEggString() {
            return toEggStringRecursive(0);
        }

        private String toEggStringRecursive(int index) {
            if (index >= gates.size()) {
                return "(Nil)";
            }
            return String.format("(Cons %s %s)", gates.get(index).toEggString(), toEggStringRecursive(index + 1));
        }
    }

    public static class Permutation implements EggExpr {
        public final List<Integer> perm;

        public Permutation(List<Integer> perm) {
            this.perm = perm;
        }

        public String toEggString() {
            return toEggStringRecursive(0);
        }

        private String toEggStringRecursive(int index) {
            if (index >= perm.size()) {
                return "(PermNil)";
            }
            return String.format("(PermCons %d %s)", perm.get(index), toEggStringRecursive(index + 1));
        }
    }

    public static interface EggExpr {
        public String toEggString();
    }

    public static class ConstrainedCircuit implements EggExpr {
        public final Circuit circuit;
        public final Permutation permutation;

        public ConstrainedCircuit(Circuit circuit, Permutation permutation) {
            this.circuit = circuit;
            this.permutation = permutation;
        }

        public String toEggString() {
            return String.format("(CCircuit %s %s)", circuit.toEggString(), permutation.toEggString());
        }

        public String toCongruenceString() {
            return String.format("(CCircuit %s %s)", circuit.toCongruenceString(), permutation.toEggString());
        }
    }

    public static class X extends Gate {
        public final String qubit;
        public X(String qubit) { this.qubit = qubit; }

        
        public String toEggString() { return String.format("(X (Q \"%s\"))", qubit); }
    }

    public static class CX extends Gate {
        public final String control;
        public final String target;
        public CX(String control, String target) { this.control = control; this.target = target; }
        public String toEggString() { return String.format("(CX (Q \"%s\") (Q \"%s\"))", control, target); }
    }
    
    public static class RZ extends Gate {
        public final String qubit;
        public final Expr angle;
        public RZ(String qubit, Expr angle) { this.qubit = qubit; this.angle = angle; }
        public String toEggString() { return String.format("(RZ (Q \"%s\") %s)", qubit, angle.toEggString()); }
    }
    
    public static class H extends Gate {
        public final String qubit;
        public H(String qubit) { this.qubit = qubit; }
        public String toEggString() { return String.format("(H (Q \"%s\"))", qubit); }
    }

    public static class SYMB extends Gate {
        public final int maxQubits;
        public SYMB(int maxQubits) { this.maxQubits = maxQubits; }
        public String toEggString() { return String.format("(SYMB %d)", maxQubits); }
    }

    public static class U1 extends Gate {
        public final String qubit;
        public final Expr lambda;
        public U1(String qubit, Expr lambda) { this.qubit = qubit; this.lambda = lambda; }
        public String toEggString() { return String.format("(U1 (Q \"%s\") %s)", qubit, lambda.toEggString()); }
    }

    public static class U2 extends Gate {
        public final String qubit;
        public final Expr phi;
        public final Expr lambda;
        public U2(String qubit, Expr phi, Expr lambda) { this.qubit = qubit; this.phi = phi; this.lambda = lambda; }
        public String toEggString() { return String.format("(U2 (Q \"%s\") %s %s)", qubit, phi.toEggString(), lambda.toEggString()); }
    }

    public static class U3 extends Gate {
        public final String qubit;
        public final Expr theta;
        public final Expr phi;
        public final Expr lambda;
        public U3(String qubit, Expr theta, Expr phi, Expr lambda) { this.qubit = qubit; this.theta = theta; this.phi = phi; this.lambda = lambda; }
        public String toEggString() { return String.format("(U3 (Q \"%s\") %s %s %s)", qubit, theta.toEggString(), phi.toEggString(), lambda.toEggString()); }
    }

    public static class RX extends Gate {
        public final String qubit;
        public final Expr angle;
        public RX(String qubit, Expr angle) { this.qubit = qubit; this.angle = angle; }
        public String toEggString() { return String.format("(RX (Q \"%s\") %s)", qubit, angle.toEggString()); }
    }

    public static class CZ extends Gate {
        public final String control;
        public final String target;
        public CZ(String control, String target) { this.control = control; this.target = target; }
        public String toEggString() { return String.format("(CZ (Q \"%s\") (Q \"%s\"))", control, target); }
    }

    public static class RY extends Gate {
        public final String qubit;
        public final Expr angle;
        public RY(String qubit, Expr angle) { this.qubit = qubit; this.angle = angle; }
        public String toEggString() { return String.format("(RY (Q \"%s\") %s)", qubit, angle.toEggString()); }
    }

    public static class RXX extends Gate {
        public final String qubit1;
        public final String qubit2;
        public final Expr angle;
        public RXX(String qubit1, String qubit2, Expr angle) { this.qubit1 = qubit1; this.qubit2 = qubit2; this.angle = angle; }
        public String toEggString() { return String.format("(RXX (Q \"%s\") (Q \"%s\") %s)", qubit1, qubit2, angle.toEggString()); }
    }

    public static class GPI extends Gate {
        public final String qubit;
        public final Expr phi;
        public GPI(String qubit, Expr phi) { this.qubit = qubit; this.phi = phi; }
        public String toEggString() { return String.format("(GPI (Q \"%s\") %s)", qubit, phi.toEggString()); }
    }

    public static class GPI2 extends Gate {
        public final String qubit;
        public final Expr phi;
        public GPI2(String qubit, Expr phi) { this.qubit = qubit; this.phi = phi; }
        public String toEggString() { return String.format("(GPI2 (Q \"%s\") %s)", qubit, phi.toString()); }
    }

    public static class VZ extends Gate {
        public final String qubit;
        public final Expr theta;
        public VZ(String qubit, Expr theta) { this.qubit = qubit; this.theta = theta; }
        public String toEggString() { return String.format("(VZ (Q \"%s\") %s)", qubit, theta.toString()); }
    }

    public static class MS extends Gate {
        public final String qubit1;
        public final String qubit2;
        public final Expr phi1;
        public final Expr phi2;
        public MS(String qubit1, String qubit2, Expr phi1, Expr phi2) { this.qubit1 = qubit1; this.qubit2 = qubit2; this.phi1 = phi1; this.phi2 = phi2; }
        public String toEggString() { return String.format("(MS (Q \"%s\") (Q \"%s\") %s %s)", qubit1, qubit2, phi1.toString(), phi2.toString()); }
    }

    public static class SX extends Gate {
        public final String qubit;
        public SX(String qubit) { this.qubit = qubit; }
        public String toEggString() { return String.format("(SX (Q \"%s\"))", qubit); }
    }
}