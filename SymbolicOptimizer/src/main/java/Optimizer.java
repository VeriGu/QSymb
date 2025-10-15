import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.AbstractMap.SimpleEntry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Map;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Triple;

public class Optimizer {
    private SymbolicSolve solver;
    public Optimizer() {
        solver = new SymbolicSolve(new Random());
    }

    public static boolean checkEquivalenceWithQiskit(String qasm1, String qasm2, int maxQubits) throws IOException, InterruptedException {
        // Create temporary files for the QASM strings
        String header = String.format("OPENQASM 2.0;\ninclude \"qelib1.inc\";\nqreg q[%s];\n", maxQubits);
        qasm1 = header + qasm1;
        qasm2 = header + qasm2;
        Path tempFile1 = Files.createTempFile("qasm1", ".qasm");
        Path tempFile2 = Files.createTempFile("qasm2", ".qasm");

        Files.writeString(tempFile1, qasm1);
        Files.writeString(tempFile2, qasm2);

        ProcessBuilder pb = new ProcessBuilder("python3", "/root/qiskit_equivalence_checker.py", tempFile1.toString(), tempFile2.toString());
        Process p = pb.start();

        BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
        BufferedReader err = new BufferedReader(new InputStreamReader(p.getErrorStream()));

        String line;
        StringBuilder output = new StringBuilder();
        while ((line = in.readLine()) != null) {
            output.append(line);
        }

        StringBuilder errorOutput = new StringBuilder();
        while ((line = err.readLine()) != null) {
            errorOutput.append(line);
        }

        int exitCode = p.waitFor();

        // Clean up temporary files
        Files.delete(tempFile1);
        Files.delete(tempFile2);

        if (exitCode != 0) {
            System.err.println("Qiskit equivalence checker script exited with error code: " + exitCode);
            System.err.println("Error output: " + errorOutput.toString());
            return false;
        }
        System.out.print("Output:" + output);

        return output.toString().trim().equals("true");
    }

    public void optimize(EggGen.ConstrainedCircuit circuit, EggGen egraph) {
         System.out.println("Original Gate Size: " + circuit.toEggString());
        System.out.println("Original Gate Size: " + circuit.circuit.gates.size());
        String name = egraph.addConstrainedCircuit(circuit);
        egraph.runSaturation("opt");
        EggGen.ConstrainedCircuit extracted = egraph.extract(name);
        System.out.println("Optimized Circuit: " + extracted.toEggString());
        System.out.println("Optimized gate size:" + extracted.circuit.gates.size());
        
        String originalQasm = CircuitTranslator.translateBack(circuit, circuit.circuit.getMaxQubits()+1).getCircuit().getQasmString();
        
        String optimizedQasm = CircuitTranslator.translateBack(extracted, extracted.circuit.getMaxQubits()+1).getCircuit().getQasmString();
        System.out.println(originalQasm);
        System.out.println(optimizedQasm);
        try {
            boolean equivalent = checkEquivalenceWithQiskit(originalQasm, optimizedQasm, circuit.circuit.getMaxQubits()+1);
            System.out.println("Circuits are equivalent (Qiskit): " + equivalent);
        } catch (IOException | InterruptedException e) {
            System.err.println("Error during Qiskit equivalence check: " + e.getMessage());
        }
    }


    // private String substitute(Circuit s, String rhs) {

    // }


    // Helper method to build qubit mapping from concrete circuit to pattern circuit
    Map<String, String> buildQubitMap(EggGen.Circuit concrete, EggGen.Circuit pattern) {
        Map<String, String> qubitMap = new HashMap<>();
        int n = Math.min(concrete.gates.size(), pattern.gates.size());
        for (int k = 0; k < n; k++) {
            EggGen.Gate cg = concrete.gates.get(k);
            EggGen.Gate pg = pattern.gates.get(k);
            // For 1-qubit gates
            if (cg instanceof EggGen.X && pg instanceof EggGen.X) {
                qubitMap.put(((EggGen.X) cg).qubit, ((EggGen.X) pg).qubit);
            } else if (cg instanceof EggGen.H && pg instanceof EggGen.H) {
                qubitMap.put(((EggGen.H) cg).qubit, ((EggGen.H) pg).qubit);
            } else if (cg instanceof EggGen.SX && pg instanceof EggGen.SX) {
                qubitMap.put(((EggGen.SX) cg).qubit, ((EggGen.SX) pg).qubit);
            } else if (cg instanceof EggGen.RZ && pg instanceof EggGen.RZ) {
                qubitMap.put(((EggGen.RZ) cg).qubit, ((EggGen.RZ) pg).qubit);
            } else if (cg instanceof EggGen.RX && pg instanceof EggGen.RX) {
                qubitMap.put(((EggGen.RX) cg).qubit, ((EggGen.RX) pg).qubit);
            } else if (cg instanceof EggGen.RY && pg instanceof EggGen.RY) {
                qubitMap.put(((EggGen.RY) cg).qubit, ((EggGen.RY) pg).qubit);
            } else if (cg instanceof EggGen.U1 && pg instanceof EggGen.U1) {
                qubitMap.put(((EggGen.U1) cg).qubit, ((EggGen.U1) pg).qubit);
            } else if (cg instanceof EggGen.U2 && pg instanceof EggGen.U2) {
                qubitMap.put(((EggGen.U2) cg).qubit, ((EggGen.U2) pg).qubit);
            } else if (cg instanceof EggGen.U3 && pg instanceof EggGen.U3) {
                qubitMap.put(((EggGen.U3) cg).qubit, ((EggGen.U3) pg).qubit);
            } else if (cg instanceof EggGen.GPI && pg instanceof EggGen.GPI) {
                qubitMap.put(((EggGen.GPI) cg).qubit, ((EggGen.GPI) pg).qubit);
            } else if (cg instanceof EggGen.GPI2 && pg instanceof EggGen.GPI2) {
                qubitMap.put(((EggGen.GPI2) cg).qubit, ((EggGen.GPI2) pg).qubit);
            } else if (cg instanceof EggGen.VZ && pg instanceof EggGen.VZ) {
                qubitMap.put(((EggGen.VZ) cg).qubit, ((EggGen.VZ) pg).qubit);
            }
            // For 2-qubit gates
            else if (cg instanceof EggGen.CX && pg instanceof EggGen.CX) {
                qubitMap.put(((EggGen.CX) cg).control, ((EggGen.CX) pg).control);
                qubitMap.put(((EggGen.CX) cg).target, ((EggGen.CX) pg).target);
            } else if (cg instanceof EggGen.CZ && pg instanceof EggGen.CZ) {
                qubitMap.put(((EggGen.CZ) cg).control, ((EggGen.CZ) pg).control);
                qubitMap.put(((EggGen.CZ) cg).target, ((EggGen.CZ) pg).target);
            } else if (cg instanceof EggGen.RXX && pg instanceof EggGen.RXX) {
                qubitMap.put(((EggGen.RXX) cg).qubit1, ((EggGen.RXX) pg).qubit1);
                qubitMap.put(((EggGen.RXX) cg).qubit2, ((EggGen.RXX) pg).qubit2);
            } else if (cg instanceof EggGen.MS && pg instanceof EggGen.MS) {
                qubitMap.put(((EggGen.MS) cg).qubit1, ((EggGen.MS) pg).qubit1);
                qubitMap.put(((EggGen.MS) cg).qubit2, ((EggGen.MS) pg).qubit2);
            }
            // Could add more gate types here as needed
        }
        return qubitMap;
    }
    
    public void optimize(EggGen.ConstrainedCircuit circuit, List<String> rules, List<MatrixConstrainedRule> symbRules, int egraph_rule_limit, int symb_rule_limit, int iterations) {
        EggGen egraph = new EggGen();
        EggGen.ConstrainedCircuit optimized = circuit;
        //We need to preprocess the symb rules to (rule .....).
        int j = 0;
        while(j < iterations) {
            egraph.push();
            String name = egraph.addConstrainedCircuit(optimized);
            // choose egraph_rule_limit different rules from rules
            Random random = new Random();
            List<String> copy = new ArrayList<>(rules);
            for(int i = 0; i < Integer.min(copy.size(), egraph_rule_limit); i++) {
                int index = random.nextInt(copy.size());
                egraph.addRewritev2(copy.get(index));
                copy.remove(index);
            }

            // do ematching for symbolic rule
            egraph.runN("opt", 1);
            String current_terms = egraph.printFunctionCSV("Cons");
            System.out.println("Current Terms: " + current_terms);
            List<MatrixConstrainedRule> copysymb = new ArrayList<>(symbRules);

            for (int i = 0; i < Integer.min(copysymb.size(), symb_rule_limit); i++){
                //int index = random.nextInt(copysymb.size());
                int index = i;
                List<Triple<EggGen.Circuit, EggGen.Circuit, EggGen.Circuit>> matches = egraph.ematching(copysymb.get(index).getLHS(), copysymb.get(index).getRHS(), 10);
                for(Triple<EggGen.Circuit, EggGen.Circuit, EggGen.Circuit> match : matches) {
                    //now, we need to check that s satisfy the constraints
                    EggGen.Circuit matchedLhs = match.getMiddle();
                    EggGen.Circuit matchedRhs = match.getRight();

                    System.out.println("Match: s: " + match.getLeft().toEggString() +  "\nlhs:" + match.getMiddle().toEggString() + "\nrhs:" + match.getRight().toEggString());

                    String lhs = copysymb.get(index).getLHS();
                    Pattern pattern = Pattern.compile("\\(Cons \\(SYMB \\d+\\)\\s+c\\)");
                    Matcher matcher = pattern.matcher(lhs);
                    String removedSymb = null;
                    if(matcher.find()) {
                        String matched = matcher.group();
                        removedSymb = lhs.replace(matched, "(Nil)");
                    } 

                    Map<String, String> qubitMap = null;
                    if(removedSymb != null) {
                        removedSymb = removedSymb.replaceAll("q\\d+", "(Q \"$0\")");
                        removedSymb = removedSymb.replaceAll("theta1|theta2|theta3", "(Symbol \"$0\")");
                        EggGen.Circuit symblhs = EggAstBuilder.parseCircuit(removedSymb);
                        qubitMap = buildQubitMap(matchedLhs, symblhs);
                    } else {
                        Pattern pattern2 = Pattern.compile("\\(Cons \\(SYMB \\d+\\)\\s+(.*)\\)");
                        Matcher matcher2 = pattern2.matcher(lhs);
                        if (matcher2.find()) {
                            removedSymb = matcher2.group(1).trim();
                        }
                        removedSymb = removedSymb.replaceAll("\\bc\\b", "(Nil)");
                        System.out.println("Current symb rule:" + removedSymb);
                        removedSymb = removedSymb.replaceAll("q\\d+", "(Q \"$0\")");
                        removedSymb = removedSymb.replaceAll("theta1|theta2|theta3", "(Symbol \"$0\")");
                        System.out.println("replaced symb rule:" + removedSymb);
                        List<EggGen.Gate> concretelhsgates = new ArrayList<>(matchedLhs.gates.subList(match.getLeft().gates.size(), matchedLhs.gates.size()));
                        EggGen.Circuit concretecircuit = new EggGen.Circuit(concretelhsgates);
                        EggGen.Circuit symbpattern = EggAstBuilder.parseCircuit(removedSymb);
                        qubitMap = buildQubitMap(concretecircuit, symbpattern);
                    }
                    
                    EggGen.Circuit s = match.getLeft();
                    EggGen.Circuit canonicalized = EggGen.canonicalizeCircuit(s, qubitMap);
                    System.out.println("Maxqubits:" + (canonicalized.getMaxQubits() + 1));
                    if((canonicalized.getMaxQubits()+1) <= EnumeratorPrune.MAX_QUBITS_SYMB) {
                        System.out.println("Canonicaled:" + canonicalized.toEggString());
                        if(matchedLhs.toEggString().equals(matchedRhs.toEggString()) && s.gates.isEmpty()) {
                            continue;
                        }
                        try{ 
                            if(checkLinearCombination(canonicalized, copysymb.get(index).getConstraint(), EnumeratorPrune.MAX_QUBITS_SYMB))  {
                                System.out.println("S satisfy the constraints!");
                                //substitube symb with matched s
                                
                                //egraph.sendCommand(String.format("(union %s %s)", match.getLeft().toEggString(), match.getRight().toEggString()));
                            }
                        } catch (IOException | InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
                //copysymb.remove(index);
            }

            optimized = egraph.extract(name);
            System.out.println("Current Gate Size:" + optimized.circuit.gates.size());
            egraph.rules.clear();
            egraph.optrules.clear();
            egraph.pop();
            j++;
        }
    }


    private boolean checkLinearCombination(EggGen.Circuit circuit, List<SymbolicSolve.SparseMatrix> basis, int maxQubits) throws IOException, InterruptedException {
        String jsonString = solver.circuitToJson(circuit.gates, maxQubits);
        String jsonM = "[";
        // Create a temporary file for the basis matrices
        List<String> basisStrList = new ArrayList<>();
        for(SymbolicSolve.SparseMatrix m : basis) {
            String basis_str = m.to_json_string();
            basisStrList.add(basis_str);
        }
        jsonM = jsonM + String.join(",", basisStrList) + "]";
        
        System.out.println("Json Matrix" + jsonM);

        ProcessBuilder pb = new ProcessBuilder("python3", "semantics.py", "-islinear", jsonString, jsonM);
        Process p = pb.start();

        BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
        
        String line;
        StringBuilder output = new StringBuilder();
        while ((line = in.readLine()) != null) {
            output.append(line);
        }

        BufferedReader ereader = new BufferedReader(new InputStreamReader(p.getErrorStream(),java.nio.charset.StandardCharsets.UTF_8));
        while ((line = ereader.readLine()) != null) {                                                  
            System.err.println(line);
        }

        int exitCode = p.waitFor();

        if (exitCode != 0) {
            System.err.println("Semantic check script exited with error code: " + exitCode);
            return false;
        }

        return output.toString().trim().contains("True");
    }


   
 
    public static void main(String[] args) throws IOException {
        List<MatrixConstrainedRule> symbRules = new ArrayList<>();
        Options options = new Options();

        Option benchmarkO = new Option("b", "benchmark", true, "benchmark file path");
        benchmarkO.setRequired(true);
        options.addOption(benchmarkO);

        Option rulesO = new Option("r", "rule", true, "ruleset file path");
        rulesO.setRequired(true);
        options.addOption(rulesO);

        Option symbRulesO = new Option("sr", "symbrule", true, "symb ruleset file path");
        symbRulesO.setRequired(false);
        options.addOption(symbRulesO);
        

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd;

        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("Optimizer", options);
            System.exit(1);
            return;
        }

        String benchmarkFile = cmd.getOptionValue("benchmark");
        System.out.println(benchmarkFile);
        String rulesFile = cmd.getOptionValue("rule");
        String symrulesFile = cmd.getOptionValue("symbrule");
        List<String> rules = new ArrayList<>();
        //EggGen egraph = new EggGen();

        try (BufferedReader br = new BufferedReader(new FileReader(rulesFile, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                //egraph.addRewritev2(line);
                rules.add(line);
            }
        }

        if(symrulesFile != null) {
            try (BufferedReader br = new BufferedReader(new FileReader(symrulesFile, StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                String[] comp = line.split("\\|");
                String lhs = comp[0];
                String rhs = comp[1];
                String type = comp[2];
                String matrix = comp[3];
                System.out.println("LHS" + lhs);
                System.out.println("RHS" + rhs);
                System.out.println("matrix:" + matrix);
                Pattern matrixP = Pattern.compile("\\[(.*)\\]");
                Matcher matcher = matrixP.matcher(matrix);
                matcher.matches();
                String matricesStr = matcher.group(1);
                String[] matrices = matricesStr.split("::");
                List<SymbolicSolve.SparseMatrix> matrixList = new ArrayList<>();
                //TODO: Construct Matrices
                for(String m: matrices) {
                        List<SymbolicSolve.SparseMatrix.MatrixEntry> sentries = new ArrayList<>();
                        if(m.startsWith("Matrix(")) {
                            String content = m.substring("Matrix(".length(), m.length() - 1);
                            System.out.println("content: " + content);
                            String[] entries = content.split(";");
                            int rows = Integer.valueOf(entries[0]);
                            int cols = Integer.valueOf(entries[1]);
                            for(int i = 2; i < entries.length; i++) {
                                String entry = entries[i].substring(1, entries[i].length() - 1);
                                String[] elems = entry.split(", ");
                                int row = Integer.valueOf(elems[0].trim());
                                int col = Integer.valueOf(elems[1].trim());
                                SymbolicSolve.Complex value = SymbolicSolve.parseComplex(elems[2]);
                                sentries.add(new SymbolicSolve.SparseMatrix.MatrixEntry(row, col, value));
                            }
                            matrixList.add(new SymbolicSolve.SparseMatrix(rows, cols, sentries));
                        }
                }
                symbRules.add(new MatrixConstrainedRule(lhs, rhs, matrixList, type));
                }
            }
        }

        String circuitString = new String(Files.readAllBytes(Paths.get(benchmarkFile)));
        System.out.println(circuitString);
        

        EggGen.Circuit circuit = QASMAstBuilder.parse(circuitString);
        System.out.println(circuit.toEggString());
        Optimizer optimizer = new Optimizer();
        //optimier.optimize(new EggGen.ConstrainedCircuit(circuit, new EggGen.Permutation(new ArrayList<>())), egraph);
        optimizer.optimize(new EggGen.ConstrainedCircuit(circuit, new EggGen.Permutation(new ArrayList<>())), rules, symbRules, 30, 150, 20);
        //egraph.stopEgglogREPL();
    }
}
