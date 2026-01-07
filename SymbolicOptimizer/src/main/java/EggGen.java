import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringReader;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Triple;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;

import ast.Expr;


public class EggGen {

    private final StringBuilder content = new StringBuilder();
    public final Set<String> rules = new HashSet<>();
    public final Set<String> optrules = new HashSet<>();
    private final Set<String> canonicalRules = new HashSet<>();
    private Integer numCircuits;
    

    private long addNewCircuitTime;
    private long equalitySaturationTime;
    private long printFunctionTime;
    private long addRewriteRuleTime;
    private long checkEqualityTime;
    private long ematchingTime;
    private long ematchingSetupTime;
    private long ematchingSaturationTime;
    private long ematchingPrefixTime;
    private long ematchingSuffixTime;
    private long ematchingRuleApplicationTime;
    private long ematchingResultParsingTime;
    private FileWriter fileWriter;
    private PrintWriter printWriter;
    private Process egglogProcess;
    private BufferedWriter processInput;
    private BufferedReader processOutput;
    private BufferedReader processError;

    public static Expr replaceSymbolWithVar(Expr expr) {
        if (expr == null) {
            return null;
        }

        if (expr instanceof ast.Symbol) {
            ast.Symbol symbol = (ast.Symbol) expr;
            if (symbol.getSymbol().equals("theta") | symbol.getSymbol().equals("theta1") || symbol.getSymbol().equals("theta2") || symbol.getSymbol().equals("theta3")) {
                return new ast.Var(symbol.getSymbol());
            } else {
                return symbol;
            }
        } else if (expr instanceof ast.Real || expr instanceof ast.Bool || expr instanceof ast.Var) {
            return expr;
        } else if (expr instanceof ast.UnOp) {
            ast.UnOp unOp = (ast.UnOp) expr;
            return new ast.UnOp(unOp.getOp(), replaceSymbolWithVar(unOp.getE()));
        } else if (expr instanceof ast.BinOp) {
            ast.BinOp binOp = (ast.BinOp) expr;
            return new ast.BinOp(binOp.getOp(), replaceSymbolWithVar(binOp.getE1()), replaceSymbolWithVar(binOp.getE2()));
        } else if (expr instanceof ast.Fun) {
            ast.Fun fun = (ast.Fun) expr;
            return new ast.Fun(fun.getName(), replaceSymbolWithVar(fun.getArg()));
        }
        return expr;
    }

    public EggGen() {
        numCircuits = 0;
        addNewCircuitTime = 0;
        equalitySaturationTime = 0;
        printFunctionTime = 0;
        addRewriteRuleTime = 0;
        checkEqualityTime = 0;
        ematchingTime = 0;
        ematchingSetupTime = 0;
        ematchingSaturationTime = 0;
        ematchingPrefixTime = 0;
        ematchingSuffixTime = 0;
        ematchingRuleApplicationTime = 0;
        ematchingResultParsingTime = 0;
        // Add standard datatype and function definitions from qast.egg
        content.append("\n(datatype Op\n  (EXP :cost 0) (SQRT :cost 0) (MINUS :cost 0) (COS :cost 0) (SIN :cost 0) (NOT :cost 0) (PLUS :cost 0) (SUBTRACT :cost 0) (MULT :cost 0) (DIV :cost 0) (POWER :cost 0) (XOR :cost 0) (AND :cost 0) (OR :cost 0))\n");
        content.append("\n(datatype Expr\n  (Bool bool :cost 0) (Real f64 :cost 0) (Symbol String :cost 0) (Var String :cost 0) (Fun String Expr :cost 0) (UnOp Op Expr :cost 0) (BinOp Op Expr Expr :cost 0))\n");
        content.append("\n(datatype Qubit (Q String :cost 0))\n");
        content.append("\n(datatype Gate\n  (X Qubit :cost 3) (CX Qubit Qubit :cost 1000) (RZ Qubit Expr :cost 3) (H Qubit :cost 3) (SYMB i64 :cost 3) (U1 Qubit Expr :cost 3) (U2 Qubit Expr Expr :cost 3)\n  (U3 Qubit Expr Expr Expr :cost 3) (RX Qubit Expr :cost 3) (CZ Qubit Qubit :cost 10) (RY Qubit Expr :cost 3) (RXX Qubit Qubit Expr :cost 10)\n  (GPI Qubit Expr :cost 3) (GPI2 Qubit Expr :cost 3) (VZ Qubit Expr :cost 3) (MS Qubit Qubit Expr Expr :cost 10) (SX Qubit :cost 3))\n");
        content.append("\n(datatype Circuit (Nil :cost 0) (Cons Gate Circuit :cost 0))\n");
        content.append("\n(datatype Value (B bool :cost 0) (R f64 :cost 0))\n");
        content.append("\n(datatype Permutation (PermNil :cost 0) (PermCons i64 Permutation :cost 0))\n");
        content.append("\n(datatype ConstrainedCircuit (CCircuit Circuit Permutation :cost 0))\n");
        content.append("\n(function fingerprint (ConstrainedCircuit) i64 :merge new)\n");
        content.append("\n(function size (Circuit) i64 :merge (min old new))\n");
        content.append("(ruleset mergefinger)\n");
        content.append("(ruleset sizeanalysis)\n");
        content.append("(ruleset noteqfinger)\n");
        content.append("(ruleset opt)\n");
        content.append("(ruleset opt1)\n");
        content.append("(ruleset opt2)\n");
        content.append("(rule\n" + //
                        " ((= x (Nil)))\n" + //
                        " ((set (size x) 0))\n" + //
                        ":ruleset sizeanalysis)");
        content.append("(rule\n" + //
                        " ((= x (Cons y z))\n" + //
                        "  (= s (size z))\n" + //
                        " )\n" + //
                        " (\n" + //
                        "  (set (size x) (+ 1 s))\n" + //
                        " )\n" + //
                        ":ruleset sizeanalysis)\n");
        content.append("(relation notSameButEqfinger (ConstrainedCircuit ConstrainedCircuit))\n");
        content.append("(rule \n" + //
                        "(" + //
                        " (= x (CCircuit cx p))\n" + //
                        " (= y (CCircuit cy p))\n" + //
                        " (!= x y)\n" + //
                        " (= (fingerprint x) (fingerprint y))\n" + //
                        ")\n" + //
                        "(" + //
                        " (notSameButEqfinger x y)\n" + //
                        ")\n" + //
                        ":ruleset noteqfinger)\n");
        content.append("(relation bad (ConstrainedCircuit ConstrainedCircuit))\n");
        content.append("(relation done (String))\n");
        content.append("(done \"Done\")\n");
        content.append("(ruleset list-ruleset)\n" + 
        "(constructor list-append (Circuit Circuit) Circuit)\n" + 
        "(rewrite (list-append (Nil) list) list :ruleset list-ruleset)\n" + 
        "(rewrite (list-append (Cons head tail) list) (Cons head (list-append tail list)) :ruleset list-ruleset)\n");
        content.append("(ruleset const)\n");

        try {
            File file = new File("egg_run.egg");
            this.fileWriter = new FileWriter(file);
            this.printWriter = new PrintWriter(this.fileWriter);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(0);
        }
        content.append("(rewrite (BinOp (PLUS) (Real x) (Real y)) (Real (+ x y)) :ruleset const)\n");
        content.append("(rewrite (BinOp (SUBTRACT) (Real x) (Real y)) (Real (- x y)) :ruleset const)\n");
        // content.append("(rewrite (BinOp (DIV) (Real x) (Real y)) (Real (/ x y)) :ruleset const)\n");
        // content.append("(rewrite (BinOp (MULT) (Real x) (Real y)) (Real (* x y)) :ruleset const)\n");
        // content.append("(rewrite (BinOp (PLUS) (Real 0.0) x) x :ruleset const)\n");
        // content.append("(rewrite (BinOp (MULT) (Real 0.0) x) (Real 0.0) :ruleset const)\n");
        // content.append("(rewrite (BinOp (PLUS) x y) (BinOp (PLUS) y x) :ruleset const)\n");
        // content.append("(rewrite (BinOp (PLUS) (BinOp (MULT) (Real x) (Symbol z)) (BinOp (MULT) (Real y) (Symbol z))) (BinOp (MULT) (Real (+ x y)) (Symbol z)) :ruleset const)\n");
        // content.append("(rewrite (BinOp (MULT) x y) (BinOp (MULT) y x) :ruleset const)\n");
        // content.append("(rewrite (BinOp (DIV) (Symbol x) (Real y)) (BinOp (MULT) (Real (/ 1.0 y)) (Symbol x)) :ruleset const)\n");
        // content.append("(rewrite (BinOp (DIV) (UnOp (MINUS) (Symbol x)) (Real y)) (BinOp (MULT) (Real (/ -1.0 y)) (Symbol x)) :ruleset const)\n");
        content.append("(rewrite (UnOp (MINUS) (Real y)) (Real (- 0.0 y)) :ruleset const)\n");
        content.append("(rewrite (UnOp (MINUS) (UnOp (MINUS) y)) y :ruleset const)\n");
        content.append("(rewrite (BinOp (PLUS) x y) (BinOp (PLUS) y x) :ruleset const)\n");
        content.append("(ruleset wire)\n");
        // content.append("(union (Symbol \"pi\") (Real 3.141592653589793238))\n");
        // content.append("(union (BinOp (DIV) (Symbol \"pi\") (Real 2.0)) (Real 1.570796326794896619))\n");
        // content.append("(union (BinOp (DIV) (Symbol \"pi\") (Real 4.0)) (Real 0.7853981633974483096))\n");
        // content.append("(union (BinOp (DIV) (Symbol \"pi\") (Real 8.0)) (Real 0.3926990816987241548))\n");
        // content.append("(union (BinOp (MULT) (Symbol \"pi\") (Real 2.0)) (Real 6.283185307179586476))\n");
        // content.append("(union (BinOp (MULT) (Symbol \"pi\") (Real 4.0)) (Real 12.56637061435917295))\n");
        // content.append("(union (BinOp (MULT) (Symbol \"pi\") (Real 8.0)) (Real 25.13274122871834591))\n");
        System.out.println(content.toString());
        
        try {
            startEgglogREPL();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(0);
        }
        String output = sendCommand(content.toString());
        System.out.println(output);
    }

    public ConstrainedCircuit extract(String name) {
        String output = sendCommand(String.format("(extract %s)", name), true);
        output = processPrintedOutput(output);
        EggGen.ConstrainedCircuit c = EggAstBuilder.parse(output);
        return c;
    }

    public Circuit extractCircuit(String name) {
        String output = sendCommand(String.format("(extract %s)", name), true);
        output = processPrintedOutput(output);
        EggGen.Circuit c = EggAstBuilder.parseCircuit(output);
        return c;
    }

    public List<Circuit> extractCircuit(String name, int n) {
        String output = sendCommand(String.format("(extract %s %d)", name, n), true);
        output = processPrintedOutput(output);
        List<EggGen.Circuit> list = new ArrayList<>();
        String[] lines = output.substring(1, output.length()-1).trim().split("\\n");
        for(String line: lines) {
            list.add(EggAstBuilder.parseCircuit(line));
        }
        return list;
    }

    public List<ConstrainedCircuit> extract(String name, int n) {
        String output = sendCommand(String.format("(extract %s %d)", name, n), true);
        output = processPrintedOutput(output);
        //System.out.println(output);
        List<EggGen.ConstrainedCircuit> list = new ArrayList<>();
        String[] lines = output.substring(1, output.length()-1).trim().split("\\n");
        for(String line: lines) {
            //System.out.println(line);
            list.add(EggAstBuilder.parse(line));
        }
        return list;
    }

    public void startEgglogREPL() throws IOException {
        ProcessBuilder pb = new ProcessBuilder("egglog-experimental");
        pb.environment().put("RUST_LOG", "ERROR");
        pb.redirectErrorStream(true);
        //the program output should be redirected to my buffer reader
        this.egglogProcess = pb.start();
        this.processInput = new BufferedWriter(new OutputStreamWriter(egglogProcess.getOutputStream()));
        this.processError = new BufferedReader(new InputStreamReader(egglogProcess.getErrorStream()));
        this.processOutput = new BufferedReader(new InputStreamReader(egglogProcess.getInputStream()));
    }

    public void stopEgglogREPL() {
        printWriter.close();
        if (egglogProcess != null) {
            egglogProcess.destroy();
        }
    }

    public String sendCommand(String command) {
        return sendCommand(command, false);
    }

    public String sendCommand(String command, boolean wait) {
        printWriter.println(command);
        printWriter.flush();
        if (processInput == null || processOutput == null) {
            System.out.println("REPL not started. Call startEgglogREPL() first.");
            return null;
        }
        
        try {
            processInput.write(command);
            processInput.newLine();
            processInput.write("(print-function done :mode csv)");
            processInput.newLine();
            processInput.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        return readOutput();
    }

    public void setFingerprint(ConstrainedCircuit c, Integer fingerprint) {
        long time = System.nanoTime();
        sendCommand(String.format("(set (fingerprint %s) %s)", c.toEggString(), fingerprint.toString()));
        addNewCircuitTime += System.nanoTime() - time;
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
        rel = rel.replaceAll("\n+$", "");
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


    public List<EggGen.Circuit> parseSingletons(String rel) {
        rel = rel.replaceAll("\n+$", "");
        List<EggGen.Circuit> list = new ArrayList<>();
        try (CSVReader reader = new CSVReader(new StringReader(rel))) {
            String[] nextLine;
            while ((nextLine = reader.readNext()) != null) {
                String elem1 = nextLine[1];
                EggGen.Circuit c1 = EggAstBuilder.parseCircuit(elem1);
                list.add(c1);
            }
        } catch (CsvValidationException | IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
        return list;
    }


    public List<SimpleEntry<EggGen.Circuit, EggGen.Circuit>> parseCircuitTwoRelation(String rel) {
        rel = rel.replaceAll("\n+$", "");
        List<SimpleEntry<EggGen.Circuit, EggGen.Circuit>> list = new ArrayList<>();
        try (CSVReader reader = new CSVReader(new StringReader(rel))) {
            String[] nextLine;
            while ((nextLine = reader.readNext()) != null) {
                String elem1 = nextLine[1];
                String elem2 = nextLine[2];
                EggGen.Circuit cc1 = EggAstBuilder.parseCircuit(elem1);
                EggGen.Circuit cc2 = EggAstBuilder.parseCircuit(elem2);
                list.add(new SimpleEntry(cc1, cc2));
            }
        } catch (CsvValidationException | IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
        return list;
    }

    public List<Triple<EggGen.Circuit, EggGen.Circuit, EggGen.Circuit>> parseCircuitThreeRelation(String rel) {
        rel = rel.replaceAll("\n+$", "");
        List<Triple<EggGen.Circuit, EggGen.Circuit, EggGen.Circuit>> list = new ArrayList<>();
        try (CSVReader reader = new CSVReader(new StringReader(rel))) {
            String[] nextLine;
            while ((nextLine = reader.readNext()) != null) {
                String elem1 = nextLine[1];
                String elem2 = nextLine[2];
                String elem3 = nextLine[3];
                EggGen.Circuit cc1 = EggAstBuilder.parseCircuit(elem1);
                EggGen.Circuit cc2 = EggAstBuilder.parseCircuit(elem2);
                EggGen.Circuit cc3 = EggAstBuilder.parseCircuit(elem3);
                list.add(new ImmutableTriple(cc1, cc2, cc3));
            }
        } catch (CsvValidationException | IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
        return list;
    }

    public Map<String, List<EggGen.ConstrainedCircuit>> parseEnodes(String nodes) {
        nodes = nodes.replaceAll("\n+$", "");
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


    private String readError() {
        StringBuilder error = new StringBuilder();
        String line;
        
        try {
        while ((line = processError.readLine()) != null) {
            error.append(line).append('\n');
            System.out.println(line);
            
        }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return error.toString();
    }

    private String readOutput() {
        StringBuilder output = new StringBuilder();
        // A short sleep to allow the process to start writing output
        String line;
        try {
        while ((line = processOutput.readLine()) != null) {
            output.append(line).append('\n');
            if(line.contains("done")){
                break;
            }
        }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return output.toString();
    }


    public boolean check(String predicate) {
        long startTime = System.nanoTime();
        String output = sendCommand(String.format("(check %s)",predicate), true);
        System.out.println(output);
        //System.out.println(predicate);
        if(output.contains("failed")) {
            //System.out.println(output);
            //System.out.println("false");
            checkEqualityTime += System.nanoTime() - startTime;
            return false;
        }
        //System.out.println("true");
        checkEqualityTime += System.nanoTime() - startTime;
        return true;
    }

    public void push() {
        sendCommand("(push)");
    }

    public void pop() {
        sendCommand("(pop)");
    }


    public void clearRules() { 
        rules.clear();
        optrules.clear();
    }

    public String addCircuit(Circuit circuit) {
        long startTime = System.nanoTime();
        String name = "c_" + numCircuits++;
        String output = sendCommand(String.format("(let %s %s)", name, circuit.toEggString()));
        //System.err.println(output);
        addNewCircuitTime += System.nanoTime() - startTime;
        return name;
    }

    public String addConstrainedCircuit(ConstrainedCircuit constrainedCircuit) {
        long startTime = System.nanoTime();
        String name = "cc_" + numCircuits++;
        String output = sendCommand(String.format("(let %s %s)\n", name, constrainedCircuit.toEggString()));
        //System.err.println(output);
        addNewCircuitTime += System.nanoTime() - startTime;
        return name;
    }

    public void addRewrite(String rule){
        if(!rules.contains(rule)) {
            long startTime = System.nanoTime();
            
            //System.out.println("Egg: Add Rewrite Rule:" + rule);
            rules.add(rule);
            String output = sendCommand(rule);
            //System.out.println("Egg: Add Rewrite Rule Output:" + output);
            addRewriteRuleTime += System.nanoTime() - startTime;
        }
    }


    // public List<String> preprocessRule(String rule, String ruleset, boolean allBirewrite) {
    //     String[] compo = rule.split("\\|");
    //     String lhs = compo[0];
    //     String rhs = compo[1];
    //     List<String> processed = new ArrayList<>();
        
    //     String type = compo[2];
        
    //     if(lhs.contains("Q") && rhs.contains("Q")) {
    //         if(type.equals("rewrite")) {
    //             if(allBirewrite) {
    //                 processed.add(String.format("(birewrite %s %s :ruleset %s)", lhs, rhs, ruleset));
    //             } else {
    //                 processed.add(String.format("(rewrite %s %s :ruleset %s)", lhs, rhs, ruleset));
    //             }
    //         } else if(type.equals("birewrite")) {
    //             processed.add(String.format("(birewrite %s %s :ruleset %s)", lhs, rhs, ruleset));
    //         }
    //         return processed;
    //     }
    //     Set<String> qubitVars = new HashSet<>();
    //     Pattern qubitPattern = Pattern.compile("q\\d+");

    //     Matcher lhsMatcher = qubitPattern.matcher(lhs);
    //     while (lhsMatcher.find()) {
    //         qubitVars.add(lhsMatcher.group());
    //     }

    //     Matcher rhsMatcher = qubitPattern.matcher(rhs);
    //     while (rhsMatcher.find()) {
    //         qubitVars.add(rhsMatcher.group());
    //     }

    //     List<String> constraints = new ArrayList<>();
    //     List<String> sortedQubitVars = new ArrayList<>(qubitVars);
    //     sortedQubitVars.sort(null); // Sort to ensure consistent order of constraints

    //     for (int i = 0; i < sortedQubitVars.size(); i++) {
    //         for (int j = i + 1; j < sortedQubitVars.size(); j++) {
    //             constraints.add(String.format("(!= %s %s)", sortedQubitVars.get(i), sortedQubitVars.get(j)));
    //         }
    //     }

    //     String constraintString = String.join(" ", constraints);
        

    //     if (type.equals("rewrite")) {
    //         String newRule = String.format("(rewrite %s %s :when (%s) :ruleset %s)", lhs, rhs, constraintString, ruleset);
    //         processed.add(newRule);
    //         if(allBirewrite) {
    //             String newRule1 = String.format("(rewrite %s %s :when (%s) :ruleset %s)", rhs, lhs, constraintString, ruleset);
    //             processed.add(newRule1);
    //         } 
    //         //String newRule1 = String.format("(rewrite %s %s :when (%s) :ruleset %s)", rhs, lhs, constraintString, "opt");
    //         //processed.add(newRule1);
    //     } else if (type.equals("birewrite")) {
    //         // Generate two rewrite rules for birewrite
    //         String rule1 = String.format("(birewrite %s %s :when (%s) :ruleset %s)", lhs, rhs, constraintString, ruleset);
    //         processed.add(rule1);
    //     }

    //     return processed;
    // }

    // public void addRewritev2(String rule, String ruleset, boolean allBirewrite) {
    //     List<String> rs = preprocessRule(rule, ruleset, allBirewrite);
    //     for(String r: rs) {
    //         addRewrite(r);
    //     }
    // }

    // public void addRewritev2(String rule) {
    //     //System.out.println(rule);
    //     List<String> rs = preprocessRule(rule, "opt", false);
    //     for(String r: rs) {
    //         addRewrite(r);
    //     }
    // }

    public List<Rule> processRules(Set<String> rules) {
        List<Rule> processedRules = new ArrayList<>();
        for (String rule : rules) {
            Rule parsedRule = QASMAstBuilder.parseRule(rule);
            processedRules.add(parsedRule);
        }
        return processedRules;
    }

    public void addOptRule(Rule r, String ruleset, String type) {
        List<Rule.Equality> equalities = r.getEqualities();
        String egg_rule = String.format("(%s %s %s %s :ruleset %s)", type, EggGen.circuitToGeneralizedOnlyRemoveQ(r.lhs, "c"), EggGen.circuitToGeneralizedOnlyRemoveQ(r.rhs, "c"), ":when (" + equalities.stream().map(e -> String.format("(%s %s %s)",  e.isEqual ? "=" : "!=", e.qubit1, e.qubit2)).collect(Collectors.joining(" ")) + ")", ruleset);
        addRewrite(egg_rule);
    }

    public List<String> getAllRewriteRules() {
        return new ArrayList<>(rules);
    }

     public List<String> getAllRewriteRulesOpt() {
        return new ArrayList<>(optrules);
    }

    private Set<String> getQubitVars(Circuit circuit) {
        Set<String> vars = new HashSet<>();
        for (Gate g : circuit.gates) {
            if (g instanceof X) vars.add(((X) g).qubit);
            else if (g instanceof H) vars.add(((H) g).qubit);
            else if (g instanceof SX) vars.add(((SX) g).qubit);
            else if (g instanceof RZ) vars.add(((RZ) g).qubit);
            else if (g instanceof RX) vars.add(((RX) g).qubit);
            else if (g instanceof RY) vars.add(((RY) g).qubit);
            else if (g instanceof U1) vars.add(((U1) g).qubit);
            else if (g instanceof U2) vars.add(((U2) g).qubit);
            else if (g instanceof U3) vars.add(((U3) g).qubit);
            else if (g instanceof GPI) vars.add(((GPI) g).qubit);
            else if (g instanceof GPI2) vars.add(((GPI2) g).qubit);
            else if (g instanceof VZ) vars.add(((VZ) g).qubit);
            else if (g instanceof CX) {
                vars.add(((CX) g).control);
                vars.add(((CX) g).target);
            } else if (g instanceof CZ) {
                vars.add(((CZ) g).control);
                vars.add(((CZ) g).target);
            } else if (g instanceof RXX) {
                vars.add(((RXX) g).qubit1);
                vars.add(((RXX) g).qubit2);
            } else if (g instanceof MS) {
                vars.add(((MS) g).qubit1);
                vars.add(((MS) g).qubit2);
            }
        }
        return vars;
    }

    // private String circuitToAlphaEquivalentString(Circuit circuit, Map<String, String> qubitMap) {
    //     String current = "(Nil)";
    //     for (int i = circuit.gates.size() - 1; i >= 0; i--) {
    //         Gate g = circuit.gates.get(i);
    //         current = String.format("(Cons %s %s)", gateToAlphaEquivalentString(g, qubitMap), current);
    //     }
    //     return current;
    // }

    public static Circuit canonicalizeCircuit(Circuit circuit, Map<String, String> qubitMap) {
        return canonicalizeCircuit(circuit, qubitMap, false);
    }

    public static Circuit canonicalizeCircuit(Circuit circuit, Map<String, String> qubitMap, boolean replaceSymbol) {
        List<EggGen.Gate> canonicalGates = new ArrayList<>(circuit.gates.size());
        for (EggGen.Gate gate : circuit.gates) {
          canonicalGates.add(canonicalizeGate(gate, qubitMap, replaceSymbol));
        }
        EggGen.Circuit canonicalEggCircuit = new EggGen.Circuit(canonicalGates);
        return canonicalEggCircuit;
    }


    private static EggGen.Gate canonicalizeGate(EggGen.Gate gate, Map<String, String> qubitMap, boolean replaceSymbol) {
        if (gate instanceof EggGen.X x) {
          return new EggGen.X(canonicalizeQubit(x.qubit, qubitMap));
        }
        if (gate instanceof EggGen.H h) {
          return new EggGen.H(canonicalizeQubit(h.qubit, qubitMap));
        }
        if (gate instanceof EggGen.SX sx) {
          return new EggGen.SX(canonicalizeQubit(sx.qubit, qubitMap));
        }
        if (gate instanceof EggGen.RZ rz) {
          return new EggGen.RZ(canonicalizeQubit(rz.qubit, qubitMap), replaceSymbol ? replaceSymbolWithVar(rz.angle) : rz.angle);
        }
        if (gate instanceof EggGen.RX rx) {
          return new EggGen.RX(canonicalizeQubit(rx.qubit, qubitMap), replaceSymbol ? replaceSymbolWithVar(rx.angle) : rx.angle);
        }
        if (gate instanceof EggGen.RY ry) {
          return new EggGen.RY(canonicalizeQubit(ry.qubit, qubitMap), replaceSymbol ? replaceSymbolWithVar(ry.angle) : ry.angle);
        }
        if (gate instanceof EggGen.U1 u1) {
          return new EggGen.U1(canonicalizeQubit(u1.qubit, qubitMap), replaceSymbol ? replaceSymbolWithVar(u1.lambda) : u1.lambda);
        }
        if (gate instanceof EggGen.U2 u2) {
          return new EggGen.U2(canonicalizeQubit(u2.qubit, qubitMap), replaceSymbol ? replaceSymbolWithVar(u2.phi) : u2.phi, replaceSymbol ? replaceSymbolWithVar(u2.lambda) : u2.lambda);
        }
        if (gate instanceof EggGen.U3 u3) {
          return new EggGen.U3(canonicalizeQubit(u3.qubit, qubitMap), replaceSymbol ? replaceSymbolWithVar(u3.theta) : u3.theta, replaceSymbol ? replaceSymbolWithVar(u3.phi) : u3.phi, replaceSymbol ? replaceSymbolWithVar(u3.lambda) : u3.lambda);
        }
        if (gate instanceof EggGen.GPI gpi) {
          return new EggGen.GPI(canonicalizeQubit(gpi.qubit, qubitMap), replaceSymbol ? replaceSymbolWithVar(gpi.phi) : gpi.phi);
        }
        if (gate instanceof EggGen.GPI2 gpi2) {
          return new EggGen.GPI2(canonicalizeQubit(gpi2.qubit, qubitMap), replaceSymbol ? replaceSymbolWithVar(gpi2.phi) : gpi2.phi);
        }
        if (gate instanceof EggGen.VZ vz) {
          return new EggGen.VZ(canonicalizeQubit(vz.qubit, qubitMap), replaceSymbol ? replaceSymbolWithVar(vz.theta) : vz.theta);
        }
        if (gate instanceof EggGen.CX cx) {
          return new EggGen.CX(canonicalizeQubit(cx.control, qubitMap), canonicalizeQubit(cx.target, qubitMap));
        }
        if (gate instanceof EggGen.CZ cz) {
          return new EggGen.CZ(canonicalizeQubit(cz.control, qubitMap), canonicalizeQubit(cz.target, qubitMap));
        }
        if (gate instanceof EggGen.RXX rxx) {
          return new EggGen.RXX(canonicalizeQubit(rxx.qubit1, qubitMap), canonicalizeQubit(rxx.qubit2, qubitMap), rxx.angle);
        }
        if (gate instanceof EggGen.MS ms) {
          return new EggGen.MS(canonicalizeQubit(ms.qubit1, qubitMap), canonicalizeQubit(ms.qubit2, qubitMap), ms.phi1, ms.phi2);
        }
        if (gate instanceof EggGen.SYMB symb) {
          return new EggGen.SYMB(symb.maxQubits);
        }
        throw new IllegalArgumentException("Unsupported gate type: " + gate.getClass());
    }

    private static String canonicalizeQubit(String qubit, Map<String, String> qubitMap) {
        return qubitMap.computeIfAbsent(qubit, q -> "q" + qubitMap.size());
    }


    private void addListAppendViewForMatchPrefix(String matchExpr) {
        push();
        sendCommand("(ruleset prefixset)");
        // sendCommand("(relation prefix-split (Circuit Circuit))");
        sendCommand("(relation prefix-demand (Circuit Circuit))");
        // sendCommand("(rule ((prefix-demand (Cons x y))) ((prefix-demand y)) :ruleset prefixset)");
        // sendCommand("(rule ((prefix-demand (Nil)) (= pattern (Nil))) ((prefix-split candidate (Nil))) :ruleset prefixset)");
        // sendCommand("(rule ((prefix-demand pattern) (= pattern (Cons gate pattern-tail)) (= candidate (Cons gate candidate-tail)) (prefix-split candidate-tail pattern-tail)) ((prefix-split candidate pattern)) :ruleset prefixset)");
        sendCommand(String.format("(rule ((= e %s)) ((prefix-demand c %s)) :ruleset prefixset)", matchExpr, matchExpr));
        runSaturation("prefixset");
        String prefixCsv = printFunctionCSV("prefix-demand");
        pop();

        // System.out.print("expressions that have prefix " + matchExpr + ":\n");
        // System.out.println(prefixCsv);
        if (prefixCsv == null || prefixCsv.isEmpty()) {
            return;
        }

        List<SimpleEntry<EggGen.Circuit, EggGen.Circuit>> prefixCircuits = new ArrayList<>();
        try (CSVReader reader = new CSVReader(new StringReader(prefixCsv))) {
            String[] row;
            while ((row = reader.readNext()) != null) {
                if (row.length < 3) {
                    continue;
                }
                String matchedCircuit = row[1];
                String circuitExpr = row[2];
                try {
                    prefixCircuits.add(new SimpleEntry<>(EggAstBuilder.parseCircuit(matchedCircuit), EggAstBuilder.parseCircuit(circuitExpr)));
                } catch (RuntimeException ignored) {
                }
            }
        } catch (CsvValidationException | IOException e) {
            e.printStackTrace();
            return;
        }

        Set<String> emitted = new HashSet<>();
        for (SimpleEntry<EggGen.Circuit, EggGen.Circuit> candidate : prefixCircuits) {
            List<EggGen.Gate> gates = candidate.getKey().gates;
            int matchSize = candidate.getKey().gates.size();
            EggGen.Circuit matched = candidate.getKey();
            for (int offset = 1; offset <= matchSize; offset++) {
                List<EggGen.Gate> prefixList = new ArrayList<>(gates.subList(0, offset));
                EggGen.Circuit prefixCircuit = new EggGen.Circuit(prefixList);
                List<EggGen.Gate> suffixList = new ArrayList<>(gates.subList(offset, matchSize));
                EggGen.Circuit suffixCircuit = new EggGen.Circuit(suffixList);
                String candidateExpr = candidate.getKey().toEggString();
                String unionKey = prefixCircuit.toEggString() + "|" + suffixCircuit.toEggString();
                if (emitted.add(unionKey)) {
                    String unionCmd = String.format("(union %s (list-append %s %s))",
                        candidateExpr,
                        prefixCircuit.toEggString(),
                        suffixCircuit.toEggString());
                    
                    sendCommand(unionCmd);
                }
            }
        }
    }
  
    private void addListAppendViewsForMatch(String matchExpr) {

        push();
        String suffixCsv;
        sendCommand("(ruleset suffixset)");
        sendCommand("(relation suffix-of (Circuit Circuit))");
        String baseRule = String.format("(rule ((= e %s)) ((suffix-of %s %s)) :ruleset suffixset)", matchExpr, matchExpr, matchExpr);
        sendCommand(baseRule);
        String transRule = "(rule ((suffix-of m e) (= (Cons x e) z)) ((suffix-of m (Cons x e))) :ruleset suffixset)";
        sendCommand(transRule);
        runSaturation("suffixset");
        suffixCsv = printFunctionCSV("suffix-of");
        pop();
        

        //System.out.print("expressions that have suffix " + matchExpr + ":\n");
        //System.out.println(suffixCsv);
        if (suffixCsv == null || suffixCsv.isEmpty()) {
            return;
        }

        List<SimpleEntry<EggGen.Circuit, EggGen.Circuit>> suffixCircuits = new ArrayList<>();
        try (CSVReader reader = new CSVReader(new StringReader(suffixCsv))) {
            String[] row;
            while ((row = reader.readNext()) != null) {
                if (row.length < 3) {
                    continue;
                }
                String matchedCircuit = row[1];
                String circuitExpr = row[2];
                try {
                    suffixCircuits.add(new SimpleEntry<>(EggAstBuilder.parseCircuit(matchedCircuit), EggAstBuilder.parseCircuit(circuitExpr)));
                } catch (RuntimeException ignored) {
                }
            }
        } catch (CsvValidationException | IOException e) {
            e.printStackTrace();
            return;
        }

        Set<String> emitted = new HashSet<>();
        for (SimpleEntry<EggGen.Circuit, EggGen.Circuit> candidate : suffixCircuits) {
            List<EggGen.Gate> gates = candidate.getValue().gates;
            int matchSize = candidate.getKey().gates.size();
            EggGen.Circuit matched = candidate.getKey();
            for (int start = 0; start < gates.size() - matchSize; start++) {
                List<EggGen.Gate> prefixGateList = new ArrayList<>(gates.subList(start, gates.size() - matchSize));
                EggGen.Circuit prefixCircuit = new EggGen.Circuit(prefixGateList);
                String prefixExpr = prefixCircuit.toEggString();
                String candidateExpr = new EggGen.Circuit(new ArrayList<>(candidate.getValue().gates.subList(start, gates.size()))).toEggString();
                String unionKey = candidateExpr + "|" + prefixExpr;
                if (emitted.add(unionKey)) {
                    String unionCmd = String.format("(union %s (list-append %s %s))",
                        candidateExpr,
                        prefixExpr,
                        matched.toEggString());
                    
                    sendCommand(unionCmd);
                }
            }
        }
    }




    public static String replaceNilWithVar(Circuit circuit, String congruenceVar) {
        String current = congruenceVar;
        for (int i = circuit.gates.size() - 1; i >= 0; i--) {
            Gate g = circuit.gates.get(i);
            current = String.format("(Cons %s %s)", g.toEggString(), current);
        }
        return current;
    }

    public static String circuitToGeneralizedQASMString(Circuit circuit, Map<String, String> qubitMap, String congruenceVar) {
        List<Gate> alphaGates = new ArrayList<>();
        for (Gate g : circuit.gates) {
            Gate ng = gateToAlphaEquivalentString(g, qubitMap, false);
            alphaGates.add(ng);
        }
        Circuit alphaCircuit = new Circuit(alphaGates);
        return alphaCircuit.toQASM();
    }


    public static String circuitToGeneralizedOnlyRemoveQ(Circuit circuit, String congruenceVar) {
        String current = congruenceVar;
        for (int i = circuit.gates.size() - 1; i >= 0; i--) {
            Gate g = circuit.gates.get(i);
            current = String.format("(Cons %s %s)", gateRemoveQ(g), current);
        }
        return current;
    }

   public static String gateRemoveQ(Gate gate) {
    if(gate instanceof X) return String.format("(X %s)", ((X) gate).qubit);
    if(gate instanceof H) return String.format("(H %s)", ((H) gate).qubit);
    if(gate instanceof SX) return String.format("(SX %s)", ((SX) gate).qubit);
    if(gate instanceof RZ) return String.format("(RZ %s %s)", ((RZ) gate).qubit, replaceSymbolWithVar(((RZ) gate).angle).toEggString());
    if(gate instanceof RX) return String.format("(RX %s %s)", ((RX) gate).qubit, replaceSymbolWithVar(((RX) gate).angle).toEggString());
    if(gate instanceof RY) return String.format("(RY %s %s)", ((RY) gate).qubit, replaceSymbolWithVar(((RY) gate).angle).toEggString());
    if(gate instanceof U1) return String.format("(U1 %s %s)", ((U1) gate).qubit, replaceSymbolWithVar(((U1) gate).lambda).toEggString());
    if(gate instanceof U2) return String.format("(U2 %s %s %s)", ((U2) gate).qubit, replaceSymbolWithVar(((U2) gate).phi).toEggString(), replaceSymbolWithVar(((U2) gate).lambda).toEggString());
    if(gate instanceof U3) return String.format("(U3 %s %s %s %s)", ((U3) gate).qubit, replaceSymbolWithVar(((U3) gate).theta).toEggString(), replaceSymbolWithVar(((U3) gate).phi).toEggString(), replaceSymbolWithVar(((U3) gate).lambda).toEggString());
    if(gate instanceof GPI) return String.format("(GPI %s %s)", ((GPI) gate).qubit, replaceSymbolWithVar(((GPI) gate).phi).toEggString());
    if(gate instanceof GPI2) return String.format("(GPI2 %s %s)", ((GPI2) gate).qubit, replaceSymbolWithVar(((GPI2) gate).phi).toEggString());
    if(gate instanceof VZ) return String.format("(VZ %s %s)", ((VZ) gate).qubit, replaceSymbolWithVar(((VZ) gate).theta).toEggString());
    if(gate instanceof CX) return String.format("(CX %s %s)", ((CX) gate).control, ((CX) gate).target);
    if(gate instanceof CZ) return String.format("(CZ %s %s)", ((CZ) gate).control, ((CZ) gate).target);
    if(gate instanceof RXX) return String.format("(RXX %s %s %s)", ((RXX) gate).qubit1, ((RXX) gate).qubit2, replaceSymbolWithVar(((RXX) gate).angle).toEggString());
    if(gate instanceof MS) return String.format("(MS %s %s %s %s)", ((MS) gate).qubit1, ((MS) gate).qubit2, replaceSymbolWithVar(((MS) gate).phi1).toEggString(), replaceSymbolWithVar(((MS) gate).phi2).toEggString());
    return null;
   }


    public static Gate gateToAlphaEquivalentString(Gate gate, Map<String, String> qubitMap, boolean replaceSymbol) {
        if(replaceSymbol){
            if (gate instanceof X) return new X(qubitMap.computeIfAbsent(((X) gate).qubit, q -> "q" + qubitMap.size()));
            if (gate instanceof H) return new H(qubitMap.computeIfAbsent(((H) gate).qubit, q -> "q" + qubitMap.size()));
            if (gate instanceof SX) return new SX(qubitMap.computeIfAbsent(((SX) gate).qubit, q -> "q" + qubitMap.size()));
            if (gate instanceof RZ) return new RZ(qubitMap.computeIfAbsent(((RZ) gate).qubit, q -> "q" + qubitMap.size()), replaceSymbolWithVar(((RZ) gate).angle));
            if (gate instanceof RX) return new RX(qubitMap.computeIfAbsent(((RX) gate).qubit, q -> "q" + qubitMap.size()), replaceSymbolWithVar(((RX) gate).angle));
            if (gate instanceof RY) return new RY(qubitMap.computeIfAbsent(((RY) gate).qubit, q -> "q" + qubitMap.size()), replaceSymbolWithVar(((RY) gate).angle));
            if (gate instanceof U1) return new U1(qubitMap.computeIfAbsent(((U1) gate).qubit, q -> "q" + qubitMap.size()), replaceSymbolWithVar(((U1) gate).lambda));
            if (gate instanceof U2) return new U2(qubitMap.computeIfAbsent(((U2) gate).qubit, q -> "q" + qubitMap.size()), replaceSymbolWithVar(((U2) gate).phi), replaceSymbolWithVar(((U2) gate).lambda));
            if (gate instanceof U3) return new U3(qubitMap.computeIfAbsent(((U3) gate).qubit, q -> "q" + qubitMap.size()), replaceSymbolWithVar(((U3) gate).theta), replaceSymbolWithVar(((U3) gate).phi), replaceSymbolWithVar(((U3) gate).lambda));
            if (gate instanceof GPI) return new GPI(qubitMap.computeIfAbsent(((GPI) gate).qubit, q -> "q" + qubitMap.size()), replaceSymbolWithVar(((GPI) gate).phi));
            if (gate instanceof GPI2) return new GPI2(qubitMap.computeIfAbsent(((GPI2) gate).qubit, q -> "q" + qubitMap.size()), replaceSymbolWithVar(((GPI2) gate).phi));
            if (gate instanceof VZ) return new VZ(qubitMap.computeIfAbsent(((VZ) gate).qubit, q -> "q" + qubitMap.size()), replaceSymbolWithVar(((VZ) gate).theta));
            if (gate instanceof CX) return new CX(qubitMap.computeIfAbsent(((CX) gate).control, q -> "q" + qubitMap.size()), qubitMap.computeIfAbsent(((CX) gate).target, q -> "q" + qubitMap.size()));
            if (gate instanceof CZ) return new CZ(qubitMap.computeIfAbsent(((CZ) gate).control, q -> "q" + qubitMap.size()), qubitMap.computeIfAbsent(((CZ) gate).target, q -> "q" + qubitMap.size()));
            if (gate instanceof RXX) return new RXX(qubitMap.computeIfAbsent(((RXX) gate).qubit1, q -> "q" + qubitMap.size()), qubitMap.computeIfAbsent(((RXX) gate).qubit2, q -> "q" + qubitMap.size()), replaceSymbolWithVar(((RXX) gate).angle));
            if (gate instanceof MS) return new MS(qubitMap.computeIfAbsent(((MS) gate).qubit1, q -> "q" + qubitMap.size()), qubitMap.computeIfAbsent(((MS) gate).qubit2, q -> "q" + qubitMap.size()), replaceSymbolWithVar(((MS) gate).phi1), replaceSymbolWithVar(((MS) gate).phi2));
            if (gate instanceof SYMB) return new SYMB(((SYMB) gate).maxQubits);
        } else {
            if (gate instanceof X) return new X(qubitMap.computeIfAbsent(((X) gate).qubit, q -> "q" + qubitMap.size()));
            if (gate instanceof H) return new H(qubitMap.computeIfAbsent(((H) gate).qubit, q -> "q" + qubitMap.size()));
            if (gate instanceof SX) return new SX(qubitMap.computeIfAbsent(((SX) gate).qubit, q -> "q" + qubitMap.size()));
            if (gate instanceof RZ) return new RZ(qubitMap.computeIfAbsent(((RZ) gate).qubit, q -> "q" + qubitMap.size()), ((RZ) gate).angle);
            if (gate instanceof RX) return new RX(qubitMap.computeIfAbsent(((RX) gate).qubit, q -> "q" + qubitMap.size()), ((RX) gate).angle);
            if (gate instanceof RY) return new RY(qubitMap.computeIfAbsent(((RY) gate).qubit, q -> "q" + qubitMap.size()), ((RY) gate).angle);
            if (gate instanceof U1) return new U1(qubitMap.computeIfAbsent(((U1) gate).qubit, q -> "q" + qubitMap.size()), ((U1) gate).lambda);
            if (gate instanceof U2) return new U2(qubitMap.computeIfAbsent(((U2) gate).qubit, q -> "q" + qubitMap.size()), ((U2) gate).phi, ((U2) gate).lambda);
            if (gate instanceof U3) return new U3(qubitMap.computeIfAbsent(((U3) gate).qubit, q -> "q" + qubitMap.size()), ((U3) gate).theta, ((U3) gate).phi, ((U3) gate).lambda);
            if (gate instanceof GPI) return new GPI(qubitMap.computeIfAbsent(((GPI) gate).qubit, q -> "q" + qubitMap.size()), ((GPI) gate).phi);
            if (gate instanceof GPI2) return new GPI2(qubitMap.computeIfAbsent(((GPI2) gate).qubit, q -> "q" + qubitMap.size()), ((GPI2) gate).phi);
            if (gate instanceof VZ) return new VZ(qubitMap.computeIfAbsent(((VZ) gate).qubit, q -> "q" + qubitMap.size()), ((VZ) gate).theta);
            if (gate instanceof CX) return new CX(qubitMap.computeIfAbsent(((CX) gate).control, q -> "q" + qubitMap.size()), qubitMap.computeIfAbsent(((CX) gate).target, q -> "q" + qubitMap.size()));
            if (gate instanceof CZ) return new CZ(qubitMap.computeIfAbsent(((CZ) gate).control, q -> "q" + qubitMap.size()), qubitMap.computeIfAbsent(((CZ) gate).target, q -> "q" + qubitMap.size()));
            if (gate instanceof RXX) return new RXX(qubitMap.computeIfAbsent(((RXX) gate).qubit1, q -> "q" + qubitMap.size()), qubitMap.computeIfAbsent(((RXX) gate).qubit2, q -> "q" + qubitMap.size()), ((RXX) gate).angle);
            if (gate instanceof MS) return new MS(qubitMap.computeIfAbsent(((MS) gate).qubit1, q -> "q" + qubitMap.size()), qubitMap.computeIfAbsent(((MS) gate).qubit2, q -> "q" + qubitMap.size()), ((MS) gate).phi1, ((MS) gate).phi2);
            if (gate instanceof SYMB) return new SYMB(((SYMB) gate).maxQubits);
        }
        return null;
    }


    private void addOptRules(String rule) {
        if(!optrules.contains(rule)) {
            System.out.println("Adding optimization rule: " + rule);
            optrules.add(rule);
        }
    };

    public void addRewriteRule(SimpleEntry<ConstrainedCircuit, ConstrainedCircuit> ruleEntry, boolean isopt) {
        Circuit lhsCircuit = ruleEntry.getKey().circuit;
        Circuit rhsCircuit = ruleEntry.getValue().circuit;

        Set<String> lhsQubits = getQubitVars(lhsCircuit);
        Set<String> rhsQubits = getQubitVars(rhsCircuit);

        boolean rhsVarsAreSubsetOfLhs = lhsQubits.containsAll(rhsQubits);
        boolean lhsVarsAreSubsetOfRhs = rhsQubits.containsAll(lhsQubits);
        String congruenceVar = "c";
        Map<String, String> qubitToVar = new HashMap<>();
        Circuit lhsCanonicalCircuit = canonicalizeCircuit(lhsCircuit, qubitToVar, true);
        Circuit rhsCanonicalCircuit = canonicalizeCircuit(rhsCircuit, qubitToVar, true);
        
        String lhsCanonical = replaceNilWithVar(lhsCanonicalCircuit, congruenceVar);
        String rhsCanonical = replaceNilWithVar(rhsCanonicalCircuit, congruenceVar);
        String lhsCanonicalQASM = lhsCanonicalCircuit.toQASM(false);
        String rhsCanonicalQASM = rhsCanonicalCircuit.toQASM(false);

        Set<String> qubitVars = new HashSet<>();
        lhsCanonicalCircuit.getQubitVars(qubitVars);
        rhsCanonicalCircuit.getQubitVars(qubitVars);

        List<String> sortedQubitVars = new ArrayList<>(qubitVars);
        sortedQubitVars.sort(null); // Sort to ensure consistent order of constraints
        List<String> constraints = new ArrayList<>();
        for (int i = 0; i < sortedQubitVars.size(); i++) {
            for (int j = i + 1; j < sortedQubitVars.size(); j++) {
                constraints.add(String.format("%s != %s", sortedQubitVars.get(i), sortedQubitVars.get(j)));
            }
        }
        String constraintString = String.join(", ", constraints);

        if(ruleEntry.getKey().circuit.gates.size() > ruleEntry.getValue().circuit.gates.size() && rhsVarsAreSubsetOfLhs) {
            if(ruleEntry.getKey().permutation.perm.isEmpty() && ruleEntry.getValue().permutation.perm.isEmpty()) {
                if(isopt){
                    if(constraints.isEmpty()){
                        String rule = String.format("%s | %s",
                        lhsCanonicalQASM,
                        rhsCanonicalQASM);
                        addOptRules(rule);
                    } else {
                        String rule = String.format("%s | %s when %s",
                        lhsCanonicalQASM,
                        rhsCanonicalQASM,
                        constraintString);
                        addOptRules(rule);
                    }
                } else {
                    String rule = String.format("(rewrite %s %s :ruleset opt)",
                    lhsCanonical,
                    rhsCanonical);
                    addRewrite(rule);
                }
            } else {
                if(isopt){
                    //this should be deprecated
                    String rule = String.format("(CCircuit %s %s)|(CCircuit %s %s)|rewrite",
                    lhsCircuit.toCongruenceString("c"), ruleEntry.getKey().permutation.toEggString(),
                    rhsCircuit.toCongruenceString("c"), ruleEntry.getValue().permutation.toEggString());
                    addOptRules(rule);
                } else {
                     String rule = String.format("(rewrite (CCircuit %s %s) (CCircuit %s %s) :ruleset opt)",
                    lhsCircuit.toCongruenceString("c"), ruleEntry.getKey().permutation.toEggString(),
                    rhsCircuit.toCongruenceString("c"), ruleEntry.getValue().permutation.toEggString());
                    addRewrite(rule);
                }
            }
        } else if(ruleEntry.getKey().circuit.gates.size() < ruleEntry.getValue().circuit.gates.size() && lhsVarsAreSubsetOfRhs){
            if(ruleEntry.getKey().permutation.perm.isEmpty()) {
                if(isopt){
                    if(constraints.isEmpty()){
                        String rule = String.format("%s | %s",
                        rhsCanonicalQASM,
                        lhsCanonicalQASM);
                        addOptRules(rule);
                    } else {
                        String rule = String.format("%s | %s when %s",
                        rhsCanonicalQASM,
                        lhsCanonicalQASM,
                        constraintString);
                        addOptRules(rule);
                    }
                } else {
                    String rule = String.format("(rewrite %s %s :ruleset opt)",
                    rhsCanonical,
                    lhsCanonical);
                    addRewrite(rule);
                }
            } else {
                if(isopt){
                   String rule = String.format("(CCircuit %s %s)|(CCircuit %s %s)|rewrite",
                    rhsCircuit.toCongruenceString("c"), ruleEntry.getKey().permutation.toEggString(),
                    lhsCircuit.toCongruenceString("c"), ruleEntry.getValue().permutation.toEggString());
                    addOptRules(rule);
                } else {
                     String rule = String.format("(rewrite (CCircuit %s %s) (CCircuit %s %s) :ruleset opt)",
                    rhsCircuit.toCongruenceString("c"), ruleEntry.getKey().permutation.toEggString(),
                    lhsCircuit.toCongruenceString("c"), ruleEntry.getValue().permutation.toEggString());
                    addRewrite(rule);
                }
            }
        } else if (lhsVarsAreSubsetOfRhs && rhsVarsAreSubsetOfLhs && ruleEntry.getKey().circuit.gates.size() == ruleEntry.getValue().circuit.gates.size()) {
            if(ruleEntry.getKey().permutation.perm.isEmpty()) {
                if(isopt){
                    if(constraints.isEmpty()){
                        String rule = String.format("%s | %s",
                        rhsCanonicalQASM,
                        lhsCanonicalQASM);
                        addOptRules(rule);
                    } else {
                        String rule = String.format("%s | %s when %s",
                        rhsCanonicalQASM,
                        lhsCanonicalQASM,
                        constraintString);
                        addOptRules(rule);
                    }
                } else {
                    String rule = String.format("(birewrite %s %s :ruleset opt)",
                    rhsCanonical,
                    lhsCanonical);
                    addRewrite(rule);
                }
            } else {
                if(isopt){
                    String rule = String.format("(CCircuit %s %s)|(CCircuit %s %s)|birewrite",
                    lhsCircuit.toCongruenceString("c"), ruleEntry.getKey().permutation.toEggString(),
                    rhsCircuit.toCongruenceString("c"), ruleEntry.getValue().permutation.toEggString());
                    addOptRules(rule);
                } else {
                    String rule = String.format("(birewrite (CCircuit %s %s) (CCircuit %s %s) :ruleset opt)",
                    lhsCircuit.toCongruenceString("c"), ruleEntry.getKey().permutation.toEggString(),
                    rhsCircuit.toCongruenceString("c"), ruleEntry.getValue().permutation.toEggString());
                    addRewrite(rule);
                }
            }
        } else {
            if(ruleEntry.getKey().permutation.perm.isEmpty()) {
                if(isopt){
                    if(constraints.isEmpty()){
                        String rule = String.format("%s | %s",
                        lhsCanonicalQASM,
                        rhsCanonicalQASM);
                        addOptRules(rule);
                    } else {
                        String rule = String.format("%s | %s when %s",
                        lhsCanonicalQASM,
                        rhsCanonicalQASM,
                        constraintString);
                        addOptRules(rule);
                    }
                } else {
                    String rule = String.format("(rewrite %s %s :ruleset opt)",
                    lhsCircuit.toCongruenceString("c"),
                    rhsCircuit.toCongruenceString("c"));
                    addRewrite(rule);
                }
            } else {
                if(isopt){
                    String rule = String.format("(CCircuit %s %s)|(CCircuit %s %s)|rewrite",
                    lhsCircuit.toCongruenceString("c"), ruleEntry.getKey().permutation.toEggString(),
                    rhsCircuit.toCongruenceString("c"), ruleEntry.getValue().permutation.toEggString());
                    addOptRules(rule);
                } else {
                    String rule = String.format("(rewrite (CCircuit %s %s) (CCircuit %s %s) :ruleset opt)",
                    lhsCircuit.toCongruenceString("c"), ruleEntry.getKey().permutation.toEggString(),
                    rhsCircuit.toCongruenceString("c"), ruleEntry.getValue().permutation.toEggString());
                    addRewrite(rule);
                }
            }
        }
    }

    public void merge(ConstrainedCircuit eclass1, ConstrainedCircuit eclass2) {
        sendCommand(String.format("(union %s %s)\n", eclass1.toEggString(), eclass2.toEggString()));
    }

    public void getSmallestRep(String eclass) {
        sendCommand(String.format("(extract %s)\n", eclass), true);
    }

    public void runN(String ruleset, int n) {
        String output = sendCommand(String.format("(run-schedule (repeat %d (saturate (run const)) (run %s)))", n, ruleset));
        //System.out.println("Run N: " + output);
    }

    public void runSchedule(String ruleset1, String ruleset2, String ruleset3, String ruleset4, int rounds, int n1,int n2, int n3, int n4) {
        String output = sendCommand(String.format("(run-schedule (let-scheduler bo (back-off)) (repeat %d (repeat %d (run  %s)) (repeat %d (run %s)) (repeat %d (run %s)) (repeat %d (run %s))))", rounds, n1, ruleset1, n2, ruleset2, n3, ruleset3, n4, ruleset4));
        //System.out.println("Run Schedule: " + output);
    }

    public void runBackoff(String ruleset, int n) {
        String output = sendCommand(String.format("(run-schedule (let-scheduler bo (back-off)) (repeat %d (saturate (run const)) (run-with bo %s)))", n, ruleset));
        //System.out.println("Run Backoff Output: " + output);
    }
    public void runSaturation() {
        long startTime = System.nanoTime();
        sendCommand("(run-schedule (saturate (run)))\n");
        equalitySaturationTime += System.nanoTime() - startTime;
    }

    public void runSaturation(String ruleSet) {
        long startTime = System.nanoTime();
        sendCommand(String.format("(run-schedule (saturate (run %s)))\n", ruleSet));
        equalitySaturationTime += System.nanoTime() - startTime;
    }


    public String printSize(String name) {
        String output = sendCommand(String.format("(print-size %s)", name));
        System.out.println(output);
        int lastNewline = output.lastIndexOf('\n');
        if(lastNewline > 0) {
            output = output.substring(0, lastNewline).trim();
            lastNewline = output.lastIndexOf('\n');
        } else {
            return "";
        }
        if(lastNewline > 0) {
            output = output.substring(0, lastNewline).trim();
        }
        else{
            return "";
        }
        return output;
    }


    public String printFunctionCSV(String name) {
        long startTime = System.nanoTime();
        String output = sendCommand(String.format("(print-function %s :mode csv)", name), true);
        //System.out.println("original output:" +  output);
        int lastNewline = output.lastIndexOf('\n');
        if(lastNewline > 0) {
            output = output.substring(0, lastNewline).trim();
            lastNewline = output.lastIndexOf('\n');
        } else {
            return "";
        }
        if(lastNewline > 0) {
            output = output.substring(0, lastNewline).trim();
        }
        else{
            return "";
        }
        //System.out.println("truncated output:" +  output);
        printFunctionTime += System.nanoTime() - startTime;
        return output;
    }


    private String processPrintedOutput(String output) {
        int lastNewline = output.lastIndexOf('\n');
        if(lastNewline > 0) {
            output = output.substring(0, lastNewline).trim();
            lastNewline = output.lastIndexOf('\n');
        } else {
            return "";
        }

        if(lastNewline > 0) {
            output = output.substring(0, lastNewline).trim();
        }
        else{
            return "";
        }

        return output;
    }

    public void union(Circuit lhs, Circuit rhs) {
        sendCommand(String.format("(union %s %s)", lhs.toEggString(), rhs.toEggString()));
    }

    public String printFunctionCSVn(String name, int n) {
        long startTime = System.nanoTime();
        String output = sendCommand(String.format("(print-function %s %d :mode csv)", name, n), true);
        //System.out.println("original output:" +  output);
        output = processPrintedOutput(output);
        //System.out.println("truncated output:" +  output);
        printFunctionTime += System.nanoTime() - startTime;
        return output;
    }

 
    public String printFunctionListCSV(List<String> list) {             
        long startTime = System.nanoTime();
        String output = sendCommand(String.format("(print-function %s :mode csv)", list.toArray()), true);
        int lastNewline = output.lastIndexOf('\n');
        if(lastNewline > 0) {
            output = output.substring(0, lastNewline).trim();
            lastNewline = output.lastIndexOf('\n');
        } else {
            return "";
        }

        if(lastNewline > 0) {
            output = output.substring(0, lastNewline).trim();
        }
        else{
            return "";
        }
        System.out.println("truncated output:" +  output);
        printFunctionTime += System.nanoTime() - startTime;
        return output; 
    }

    public List<Triple<EggGen.Circuit, EggGen.Circuit, EggGen.Circuit>> ematching(String lhs, String rhs, int n) {
        long ematchingStartTime = System.nanoTime();
        System.out.println("LHS:" + lhs);
        System.out.println("RHS:" + rhs);
        // replace SYMB with a variable
        long setupStartTime = System.nanoTime();
        Set<String> qubitVars = new HashSet<>();
        Pattern qubitPattern = Pattern.compile("q\\d+");

        Matcher lhsMatcher = qubitPattern.matcher(lhs);
        while (lhsMatcher.find()) {
            qubitVars.add(lhsMatcher.group());
        }

        Matcher rhsMatcher = qubitPattern.matcher(rhs);
        while (rhsMatcher.find()) {
            qubitVars.add(rhsMatcher.group());
        }

        List<String> constraints = new ArrayList<>();
        List<String> sortedQubitVars = new ArrayList<>(qubitVars);
        sortedQubitVars.sort(null); // Sort to ensure consistent order of constraints

        for (int i = 0; i < sortedQubitVars.size(); i++) {
            for (int j = i + 1; j < sortedQubitVars.size(); j++) {
                constraints.add(String.format("(!= %s %s)", sortedQubitVars.get(i), sortedQubitVars.get(j)));
            }
        }

        String constraintString = String.join(" ", constraints);

        Pattern pattern = Pattern.compile("\\(Cons \\(SYMB \\d+\\)\\s+c\\)");
        Matcher matcher = pattern.matcher(lhs);
        String matchPrefix = null;
        Pattern pattern2 = Pattern.compile("\\(Cons \\(SYMB \\d+\\)\\s+(.*)\\)");
        Matcher matcher2 = pattern2.matcher(lhs);
        String matchExpr = null;
        if(matcher.find()) {
            String match = matcher.group();
            matchPrefix = lhs.replace(match, "c");
            lhs = lhs.replace(match, "(list-append s c)");
        } else {
            if (matcher2.find()) {
                matchExpr = matcher2.group(1).trim();
                lhs = "(list-append s " + matchExpr + ")";
            }
        }

        matcher = pattern.matcher(rhs);
        if(matcher.find()) {
            String match = matcher.group();
            rhs = rhs.replace(match, "(list-append s c)");
        } else {
            matcher2 = pattern2.matcher(rhs);
            if (matcher2.find()) {
                String rhsMatchExpr = matcher2.group(1).trim();
                rhs = "(list-append s " + rhsMatchExpr + ")";
            }
        }

        ematchingSetupTime += System.nanoTime() - setupStartTime;

        System.out.println("Replaced lhs:" + lhs);
        System.out.println("Replaced rhs:" + rhs);
        
        push();
        if (matchExpr != null) {
            long suffixTime = System.nanoTime();
            addListAppendViewsForMatch(matchExpr);
            ematchingSuffixTime += System.nanoTime() - suffixTime;
        }
        long saturationTime = System.nanoTime();
        runSaturation("list-ruleset");
        ematchingSaturationTime += System.nanoTime() - saturationTime;
        if(matchPrefix != null) {
            long prefixTime = System.nanoTime();
            addListAppendViewForMatchPrefix(matchPrefix);
            ematchingPrefixTime += System.nanoTime() - prefixTime;
        }
        saturationTime = System.nanoTime();
        runSaturation("list-ruleset");
        ematchingSaturationTime += System.nanoTime() - saturationTime;
        String list_append = printFunctionCSV("list-append");
        sendCommand("(ruleset ematchset)");
        sendCommand("(relation ematch (Circuit Circuit Circuit))");
        String rule = String.format("(rule (%s (= %s e)) ((ematch s %s %s)) :ruleset ematchset)", constraintString, lhs, lhs, rhs);
        System.out.println("Symb rule:" + rule);
        String out = sendCommand(rule);
        saturationTime = System.nanoTime();
        runN("ematchset", 10);
        runSaturation("list-ruleset");
        ematchingSaturationTime += System.nanoTime() - saturationTime;
        long resultParsingTime = System.nanoTime();
        String output = printFunctionCSVn("ematch", n);
        pop();

        List<Triple<EggGen.Circuit, EggGen.Circuit, EggGen.Circuit>> result = parseCircuitThreeRelation(output);
        ematchingResultParsingTime += System.nanoTime() - resultParsingTime;
        ematchingTime += System.nanoTime() - ematchingStartTime;
        return result;
    }

   
    public Map<String, Long> getProfilingData() {
        Map<String, Long> profilingData = new HashMap<>();
        profilingData.put("addNewCircuitTime", addNewCircuitTime);
        profilingData.put("equalitySaturationTime", equalitySaturationTime);
        profilingData.put("printFunctionTime", printFunctionTime);
        profilingData.put("addRewriteRuleTime", addRewriteRuleTime);
        profilingData.put("checkEqualityTime", checkEqualityTime);
        profilingData.put("ematchingTime", ematchingTime);
        profilingData.put("ematchingSetupTime", ematchingSetupTime);
        profilingData.put("ematchingSaturationTime", ematchingSaturationTime);
        profilingData.put("ematchingPrefixTime", ematchingPrefixTime);
        profilingData.put("ematchingSuffixTime", ematchingSuffixTime);
        profilingData.put("ematchingRuleApplicationTime", ematchingRuleApplicationTime);
        profilingData.put("ematchingResultParsingTime", ematchingResultParsingTime);
        return profilingData;
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

            System.out.println("Profiling data: " + eggGen.getProfilingData());


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

        public int getTwoQubitsCount() {
            return 0;
        }

        public void getQubitVars(Set<String> vars) {}

        public String toAlphaEquivalentString(Map<String, String> qubitMap) {
            return toEggString();
        }

        public int getMaxQubits(){
            return 0;
        }

        public void getAllSymbols(Set<String> vars){
    
        }

        public String toQASM() {
            return "";
        }

        public String toQASM(boolean linebreak) {
            return "";
        }

        public Gate instantiate(Map<String, Expr> angleMap) {
            return new Gate();
        }
    }

    public static class Circuit implements EggExpr {
        public final List<Gate> gates;
        private String qasm;
        public Circuit(List<Gate> gates) {
            this.gates = gates;
        }

        public int getTwoQubitsCount() {
            int count = 0;
            for(Gate g: gates) {
                count += g.getTwoQubitsCount();
            }
            return count;
        }


        public String toQASM() {
            return toQASM(true);
        }

        public String toQASM(boolean linebreak) {
            if(qasm != null) {
                return qasm;
            }

            if(gates.isEmpty()) {
                return ";";
            }

            qasm = "";
            for(Gate g: gates) {
                qasm += g.toQASM(linebreak);
            }
            return qasm;
        }


        public String toAlphaEquivalentString(Map<String, String> qubitMap) {
            return toAlphaEquivalentStringRecursive(0, qubitMap);
        }

        private String toAlphaEquivalentStringRecursive(int index, Map<String, String> qubitMap) {
            if (index >= gates.size()) {
                return "(Nil)";
            }
            return String.format("(Cons %s %s)", gates.get(index).toAlphaEquivalentString(qubitMap), toAlphaEquivalentStringRecursive(index + 1, qubitMap));
        }

        public String toCongruenceString(String varName) {
            return toCongruenceStringRecursive(0, varName);
        }

        private String toCongruenceStringRecursive(int index, String varName) {
            if (index >= gates.size()) {
                return varName;
            }
            return String.format("(Cons %s %s)", gates.get(index).toEggString(), toCongruenceStringRecursive(index + 1, varName));
        }

        public String toEggString() {
            return toEggStringRecursive(0);
        }

        public String toEggStringRecursive(int index) {
            if (index >= gates.size()) {
                return "(Nil)";
            }
            return String.format("(Cons %s %s)", gates.get(index).toEggString(), toEggStringRecursive(index + 1));
        }

        public void getQubitVars(Set<String> vars) {
            for (Gate g : gates) {
                g.getQubitVars(vars);
            }
        }

        public int getMaxQubits() {
            int max = 0;
            for(Gate g: gates) {
                max = Integer.max(g.getMaxQubits(), max);
            }
            return max;
        }


        public void getAllSymbols(Set<String> vars) {
            for (Gate g : gates) {
                g.getAllSymbols(vars);
            }
        }


        public Circuit instantiate(Map<String, Expr> angleMap) {
            List<Gate> gatesnew = new ArrayList<>();
            for(Gate g: gates) {
                gatesnew.add(g.instantiate(angleMap));
            }
            return new Circuit(gatesnew);
        }


        @Override
        public boolean equals(Object obj) {
            if(obj instanceof Circuit) {
                return toQASM().equals(((Circuit) obj).toQASM());
            }
            return false;
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

        public String toCongruenceString(String varName) {
            return String.format("(CCircuit %s %s)", circuit.toCongruenceString(varName), permutation.toEggString());
        }

        public String toAlphaEquivalentString(Map<String, String> qubitMap) {
            return String.format("(CCircuit %s %s)", circuit.toAlphaEquivalentString(qubitMap), permutation.toEggString());
        }

    }

    public static class X extends Gate {
        public final String qubit;
        public X(String qubit) { this.qubit = qubit; }

        

        public String toEggString() { return String.format("(X (Q \"%s\"))", qubit); }

        public int getTwoQubitsCount() {
            return 0;
        }

        @Override
        public void getQubitVars(Set<String> vars) {
            vars.add(qubit);
        }

        @Override
        public String toAlphaEquivalentString(Map<String, String> qubitMap) {
            String var = qubitMap.computeIfAbsent(qubit, q -> "q" + qubitMap.size());
            return String.format("(X %s)", var);
        }

        @Override
        public String toQASM() {
            return String.format("x %s;\n", qubit);
        }

        @Override
        public String toQASM(boolean linebreak) {
            if(linebreak){
                return String.format("x %s;\n", qubit);
            } else {
                return String.format("x %s;", qubit);
            }
        }

        @Override
        public int getMaxQubits(){
            return Integer.valueOf(qubit.replaceAll("q", ""));
        }

        @Override
        public Gate instantiate(Map<String, Expr> angleMap) {
            return new X(qubit);
        }
    }

    public static class CX extends Gate {
        public final String control;
        public final String target;
        public CX(String control, String target) { this.control = control; this.target = target; }
        public String toEggString() { return String.format("(CX (Q \"%s\") (Q \"%s\"))", control, target); }

        @Override
        public int getTwoQubitsCount() {
            return 1;
        }

        @Override
        public void getQubitVars(Set<String> vars) {
            vars.add(control);
            vars.add(target);
        }

        @Override
        public String toAlphaEquivalentString(Map<String, String> qubitMap) {
            String controlVar = qubitMap.computeIfAbsent(control, q -> "q" + qubitMap.size());
            String targetVar = qubitMap.computeIfAbsent(target, q -> "q" + qubitMap.size());
            return String.format("(CX %s %s)", controlVar, targetVar);
        }


        @Override
        public int getMaxQubits(){
            return Integer.max(Integer.valueOf(control.replaceAll("q", "")), Integer.valueOf(target.replaceAll("q", "")));
        }


        @Override
        public void getAllSymbols(Set<String> vars) {

        }

        @Override
        public String toQASM() {
            return String.format("cx %s, %s;\n", control, target);
        }

        @Override
        public String toQASM(boolean linebreak) {
            if(linebreak){
                return String.format("cx %s, %s;\n", control, target);
            } else {
                return String.format("cx %s, %s;", control, target);
            }
        }

        @Override
        public Gate instantiate(Map<String, Expr> angleMap) {
            return new CX(control, target);
        }
    }
    
    public static class RZ extends Gate {
        public final String qubit;
        public final Expr angle;
        public RZ(String qubit, Expr angle) { this.qubit = qubit; this.angle = angle; }
        public String toEggString() { return String.format("(RZ (Q \"%s\") %s)", qubit, angle.toEggString()); }

        @Override
        public int getTwoQubitsCount() {
            return 0;
        }

        @Override
        public void getQubitVars(Set<String> vars) {
            vars.add(qubit);
        }

        @Override
        public String toAlphaEquivalentString(Map<String, String> qubitMap) {
            String qubitVar = qubitMap.computeIfAbsent(qubit, q -> "q" + qubitMap.size());
            return String.format("(RZ %s %s)", qubitVar, angle.toEggString());
        }

        @Override
        public int getMaxQubits(){
            return Integer.valueOf(qubit.replaceAll("q", ""));
        }


        @Override
        public void getAllSymbols(Set<String> vars) {
            angle.getAllSymbols(vars);
        }

        @Override
        public String toQASM() {
            return toQASM(true);
        }

        @Override
        public String toQASM(boolean linebreak) {
            if(linebreak){
                return String.format("rz(%s) %s;\n", angle.toString(), qubit);
            } else {
                return String.format("rz(%s) %s;", angle.toString(), qubit);
            }
        }

        @Override
        public Gate instantiate(Map<String, Expr> angleMap) {
           return new RZ(qubit, CircuitDAG.eval(angle, angleMap));
        }

    }
    
    public static class H extends Gate {
        public final String qubit;
        public H(String qubit) { this.qubit = qubit; }
        public String toEggString() { return String.format("(H (Q \"%s\"))", qubit); }

        @Override
        public int getTwoQubitsCount() {
            return 0;
        }

        @Override
        public void getQubitVars(Set<String> vars) {
            vars.add(qubit);
        }



        @Override
        public String toAlphaEquivalentString(Map<String, String> qubitMap) {
            String var = qubitMap.computeIfAbsent(qubit, q -> "q" + qubitMap.size());
            return String.format("(H %s)", var);
        }

        @Override
        public int getMaxQubits(){
            return Integer.valueOf(qubit.replaceAll("q", ""));
        }

        @Override
        public String toQASM() {
            return toQASM(true);
        }

        @Override
        public String toQASM(boolean linebreak) {
            if(linebreak){
                return String.format("h %s;\n", qubit);
            } else {
                return String.format("h %s;", qubit);
            }
        }

        @Override
        public Gate instantiate(Map<String, Expr> angleMap) {
            return new H(qubit);
        }
    }

    public static class SYMB extends Gate {
        public final int maxQubits;
        public SYMB(int maxQubits) { this.maxQubits = maxQubits; }
        public String toEggString() { return String.format("(SYMB %d)", maxQubits); }

        @Override
        public String toAlphaEquivalentString(Map<String, String> qubitMap) {
            return toEggString();
        }

        @Override
        public int getMaxQubits(){
            return maxQubits-1;
        }

        @Override
        public String toQASM() {
            return toQASM(true);
        }

        @Override
        public String toQASM(boolean linebreak) {
            if(linebreak){
                return String.format("symb %d;\n", maxQubits);
            } else {
                return String.format("symb %d;", maxQubits);
            }
        }

        @Override
        public Gate instantiate(Map<String, Expr> angleMap) {
            return new SYMB(maxQubits);
        }
    }

    public static class U1 extends Gate {
        public final String qubit;
        public final Expr lambda;
        public U1(String qubit, Expr lambda) { this.qubit = qubit; this.lambda = lambda; }
        public String toEggString() { return String.format("(U1 (Q \"%s\") %s)", qubit, lambda.toEggString()); }

        @Override
        public int getTwoQubitsCount() {
            return 0;
        }

        @Override
        public String toAlphaEquivalentString(Map<String, String> qubitMap) {
            String qubitVar = qubitMap.computeIfAbsent(qubit, q -> "q" + qubitMap.size());
            return String.format("(U1 %s %s)", qubitVar, lambda.toEggString());
        }

        @Override
        public int getMaxQubits(){
            return Integer.valueOf(qubit.replaceAll("q", ""));
        }


        @Override
        public void getAllSymbols(Set<String> vars) {
            lambda.getAllSymbols(vars);
        }

        @Override
        public String toQASM() {
            return toQASM(true);
        }

        @Override
        public String toQASM(boolean linebreak) {
            if(linebreak){
                return String.format("u1(%s) %s;\n", lambda.toString(), qubit);
            } else {
                return String.format("u1(%s) %s;", lambda.toString(), qubit);
            }
        }

        @Override
        public Gate instantiate(Map<String, Expr> angleMap) {
            return new U1(qubit, CircuitDAG.eval(lambda, angleMap));
        }
    }

    public static class U2 extends Gate {
        public final String qubit;
        public final Expr phi;
        public final Expr lambda;
        public U2(String qubit, Expr phi, Expr lambda) { this.qubit = qubit; this.phi = phi; this.lambda = lambda; }
        public String toEggString() { return String.format("(U2 (Q \"%s\") %s %s)", qubit, phi.toEggString(), lambda.toEggString()); }

        @Override
        public int getTwoQubitsCount() {
            return 0;
        }

        @Override
        public String toAlphaEquivalentString(Map<String, String> qubitMap) {
            String qubitVar = qubitMap.computeIfAbsent(qubit, q -> "q" + qubitMap.size());
            return String.format("(U2 %s %s %s)", qubitVar, phi.toEggString(), lambda.toEggString());
        }

        @Override
        public int getMaxQubits(){
            return Integer.valueOf(qubit.replaceAll("q", ""));
        }

        @Override
        public void getAllSymbols(Set<String> vars) {
            phi.getAllSymbols(vars);
            lambda.getAllSymbols(vars);
        }

        @Override
        public String toQASM() {
            return toQASM(true);
        }

        @Override
        public String toQASM(boolean linebreak) {
            if(linebreak){
                return String.format("u2(%s,%s) %s;\n", phi.toString(), lambda.toEggString(), qubit);
            } else {
                return String.format("u2(%s,%s) %s;", phi.toString(), lambda.toEggString(), qubit);
            }
        }

        @Override
        public Gate instantiate(Map<String, Expr> angleMap) {
            return new U2(qubit, CircuitDAG.eval(phi, angleMap), CircuitDAG.eval(lambda, angleMap));
        }
    }

    public static class U3 extends Gate {
        public final String qubit;
        public final Expr theta;
        public final Expr phi;
        public final Expr lambda;
        public U3(String qubit, Expr theta, Expr phi, Expr lambda) { this.qubit = qubit; this.theta = theta; this.phi = phi; this.lambda = lambda; }
        public String toEggString() { return String.format("(U3 (Q \"%s\") %s %s %s)", qubit, theta.toEggString(), phi.toEggString(), lambda.toEggString()); }

        @Override
        public int getTwoQubitsCount() {
            return 0;
        }

        @Override
        public String toAlphaEquivalentString(Map<String, String> qubitMap) {
            String qubitVar = qubitMap.computeIfAbsent(qubit, q -> "q" + qubitMap.size());
            return String.format("(U3 %s %s %s %s)", qubitVar, theta.toEggString(), phi.toEggString(), lambda.toEggString());
        }

        @Override
        public int getMaxQubits(){
            return Integer.valueOf(qubit.replaceAll("q", ""));
        }

        @Override
        public void getAllSymbols(Set<String> vars) {
            theta.getAllSymbols(vars);
            phi.getAllSymbols(vars);
            lambda.getAllSymbols(vars);
        }

        @Override
        public String toQASM() {
            return toQASM(true);
        }

        @Override
        public String toQASM(boolean linebreak) {
            if(linebreak){
                return String.format("u3(%s,%s,%s) %s;\n", theta.toString(), phi.toString(), lambda.toString(), qubit);
            } else {
                return String.format("u3(%s,%s,%s) %s;", theta.toString(), phi.toString(), lambda.toString(), qubit);
            }
        }

        @Override
        public Gate instantiate(Map<String, Expr> angleMap) {
            return new U3(qubit, CircuitDAG.eval(theta, angleMap), CircuitDAG.eval(phi, angleMap), CircuitDAG.eval(lambda, angleMap));
        }
    }

    public static class RX extends Gate {
        public final String qubit;
        public final Expr angle;
        public RX(String qubit, Expr angle) { this.qubit = qubit; this.angle = angle; }
        public String toEggString() { return String.format("(RX (Q \"%s\") %s)", qubit, angle.toEggString()); }

        @Override
        public int getTwoQubitsCount() {
            return 0;
        }

        @Override
        public String toAlphaEquivalentString(Map<String, String> qubitMap) {
            String qubitVar = qubitMap.computeIfAbsent(qubit, q -> "q" + qubitMap.size());
            return String.format("(RX %s %s)", qubitVar, angle.toEggString());
        }

        @Override
        public int getMaxQubits(){
            return Integer.valueOf(qubit.replaceAll("q", ""));
        }

        @Override
        public void getAllSymbols(Set<String> vars) {
            angle.getAllSymbols(vars);
        }

        @Override
        public String toQASM() {
            return toQASM(true);
        }

        @Override
        public String toQASM(boolean linebreak) {
            if(linebreak){
                return String.format("rx(%s) %s;\n", angle.toEggString(), qubit);
            } else {
                return String.format("rx(%s) %s;", angle.toEggString(), qubit);
            }
        }

        @Override
        public Gate instantiate(Map<String, Expr> angleMap) {
            return new RX(qubit, CircuitDAG.eval(angle, angleMap));
        }
    }

    public static class CZ extends Gate {
        public final String control;
        public final String target;
        public CZ(String control, String target) { this.control = control; this.target = target; }
        public String toEggString() { return String.format("(CZ (Q \"%s\") (Q \"%s\"))", control, target); }

        @Override
        public void getQubitVars(Set<String> vars) {
            vars.add(control);
            vars.add(target);
        }
        @Override
        public int getTwoQubitsCount() {
            return 1;
        }

        @Override
        public String toAlphaEquivalentString(Map<String, String> qubitMap) {
            String controlVar = qubitMap.computeIfAbsent(control, q -> "q" + qubitMap.size());
            String targetVar = qubitMap.computeIfAbsent(target, q -> "q" + qubitMap.size());
            return String.format("(CZ %s %s)", controlVar, targetVar);
        }

       @Override
        public int getMaxQubits(){
            return Integer.max(Integer.valueOf(control.replaceAll("q", "")), Integer.valueOf(target.replaceAll("q", "")));
        }

        @Override
        public String toQASM() {
            return toQASM(true);
        }

        @Override
        public String toQASM(boolean linebreak) {
            if(linebreak){
                return String.format("cz %s, %s;\n", control, target);
            } else {
                return String.format("cz %s, %s;", control, target);
            }
        }

        @Override
        public Gate instantiate(Map<String, Expr> angleMap) {
            return new CZ(control, target);
        }
    }

    public static class RY extends Gate {
        public final String qubit;
        public final Expr angle;
        public RY(String qubit, Expr angle) { this.qubit = qubit; this.angle = angle; }
        public String toEggString() { return String.format("(RY (Q \"%s\") %s)", qubit, angle.toEggString()); }

        @Override
        public int getTwoQubitsCount() {
            return 0;
        }

        @Override
        public String toAlphaEquivalentString(Map<String, String> qubitMap) {
            String qubitVar = qubitMap.computeIfAbsent(qubit, q -> "q" + qubitMap.size());
            return String.format("(RY %s %s)", qubitVar, angle.toEggString());
        }
        @Override
        public int getMaxQubits(){
            return Integer.valueOf(qubit.replaceAll("q", ""));
        }

        @Override
        public void getAllSymbols(Set<String> vars) {
            angle.getAllSymbols(vars);
        }

        @Override
        public Gate instantiate(Map<String, Expr> angleMap) {
            return new RY(qubit, CircuitDAG.eval(angle, angleMap));
        }

        @Override
        public String toQASM() {
            return toQASM(true);
        }

        @Override
        public String toQASM(boolean linebreak) {
            if(linebreak){
                return String.format("ry(%s) %s;\n", angle.toString(), qubit);
            } else {
                return String.format("ry(%s) %s;", angle.toString(), qubit);
            }
        }
    }

    public static class RXX extends Gate {
        public final String qubit1;
        public final String qubit2;
        public final Expr angle;
        public RXX(String qubit1, String qubit2, Expr angle) { this.qubit1 = qubit1; this.qubit2 = qubit2; this.angle = angle; }
        public String toEggString() { return String.format("(RXX (Q \"%s\") (Q \"%s\") %s)", qubit1, qubit2, angle.toEggString()); }

        @Override
        public int getTwoQubitsCount() {
            return 1;
        }

        @Override
        public String toAlphaEquivalentString(Map<String, String> qubitMap) {
            String qubit1Var = qubitMap.computeIfAbsent(qubit1, q -> "q" + qubitMap.size());
            String qubit2Var = qubitMap.computeIfAbsent(qubit2, q -> "q" + qubitMap.size());
            return String.format("(RXX %s %s %s)", qubit1Var, qubit2Var, angle.toEggString());
        }

        @Override
        public int getMaxQubits(){
            return Integer.max(Integer.valueOf(qubit1.replaceAll("q", "")), Integer.valueOf(qubit2.replaceAll("q", "")));
        }


        public void getAllSymbols(Set<String> vars) {
            angle.getAllSymbols(vars);
        }

        @Override
        public String toQASM() {
            return toQASM(true);
        }

        @Override
        public String toQASM(boolean linebreak) {
            if(linebreak){
                return String.format("rxx(%s) %s, %s;\n", angle.toString(), qubit1, qubit2);
            } else {
                return String.format("rxx(%s) %s, %s;", angle.toString(), qubit1, qubit2);
            }
        }

        @Override
        public Gate instantiate(Map<String, Expr> angleMap) {
            return new RXX(qubit1, qubit2, CircuitDAG.eval(angle, angleMap));
        }
    }

    public static class GPI extends Gate {
        public final String qubit;
        public final Expr phi;
        public GPI(String qubit, Expr phi) { this.qubit = qubit; this.phi = phi; }
        public String toEggString() { return String.format("(GPI (Q \"%s\") %s)", qubit, phi.toEggString()); }

        @Override
        public int getTwoQubitsCount() {
            return 0;
        }

        @Override
        public String toAlphaEquivalentString(Map<String, String> qubitMap) {
            String qubitVar = qubitMap.computeIfAbsent(qubit, q -> "q" + qubitMap.size());
            return String.format("(GPI %s %s)", qubitVar, phi.toEggString());
        }

         @Override
        public int getMaxQubits(){
            return Integer.valueOf(qubit.replaceAll("q", ""));
        }


        @Override
        public void getAllSymbols(Set<String> vars) {
            phi.getAllSymbols(vars);
        }

        @Override
        public String toQASM() {
            return toQASM(true);
        }

        @Override
        public String toQASM(boolean linebreak) {
            if(linebreak){
                return String.format("gpi(%s) %s;\n", phi.toString(), qubit);
            } else {
                return String.format("gpi(%s) %s;", phi.toString(), qubit);
            }
        }

        @Override
        public Gate instantiate(Map<String, Expr> angleMap) {
            return new GPI(qubit, CircuitDAG.eval(phi, angleMap));
        }
    }

    public static class GPI2 extends Gate {
        public final String qubit;
        public final Expr phi;
        public GPI2(String qubit, Expr phi) { this.qubit = qubit; this.phi = phi; }
        public String toEggString() { return String.format("(GPI2 (Q \"%s\") %s)", qubit, phi.toEggString()); }

        @Override
        public int getTwoQubitsCount() {
            return 0;
        }

        @Override
        public String toAlphaEquivalentString(Map<String, String> qubitMap) {
            String qubitVar = qubitMap.computeIfAbsent(qubit, q -> "q" + qubitMap.size());
            return String.format("(GPI2 %s %s)", qubitVar, phi.toEggString());
        }

         @Override
        public int getMaxQubits(){
            return Integer.valueOf(qubit.replaceAll("q", ""));
        }

        @Override
        public void getAllSymbols(Set<String> vars) {
            phi.getAllSymbols(vars);
        }

        @Override
        public String toQASM() {
            return toQASM(true);
        }

        @Override
        public String toQASM(boolean linebreak) {
            if(linebreak){
                return String.format("gpi2(%s) %s;\n", phi.toString(), qubit);
            } else {
                return String.format("gpi2(%s) %s;", phi.toString(), qubit);
            }
        }

        @Override
        public Gate instantiate(Map<String, Expr> angleMap) {
            return new GPI2(qubit, CircuitDAG.eval(phi, angleMap));
        }
    }

    public static class VZ extends Gate {
        public final String qubit;
        public final Expr theta;
        public VZ(String qubit, Expr theta) { this.qubit = qubit; this.theta = theta; }
        public String toEggString() { return String.format("(VZ (Q \"%s\") %s)", qubit, theta.toEggString()); }

        @Override
        public int getTwoQubitsCount() {
            return 0;
        }

        @Override
        public String toAlphaEquivalentString(Map<String, String> qubitMap) {
            String qubitVar = qubitMap.computeIfAbsent(qubit, q -> "q" + qubitMap.size());
            return String.format("(VZ %s %s)", qubitVar, theta.toEggString());
        }

        @Override
        public int getMaxQubits(){
            return Integer.valueOf(qubit.replaceAll("q", ""));
        }


        @Override
        public void getAllSymbols(Set<String> vars) {
            theta.getAllSymbols(vars);
        }

        @Override
        public String toQASM() {
            return toQASM(true);
        }

        @Override
        public String toQASM(boolean linebreak) {
            if(linebreak){
                return String.format("vz(%s) %s;\n", theta.toString(), qubit);
            } else {
                return String.format("vz(%s) %s;", theta.toString(), qubit);
            }
        }

        @Override
        public Gate instantiate(Map<String, Expr> angleMap) {
            return new VZ(qubit, CircuitDAG.eval(theta, angleMap));
        }
    }

    public static class MS extends Gate {
        public final String qubit1;
        public final String qubit2;
        public final Expr phi1;
        public final Expr phi2;
        public MS(String qubit1, String qubit2, Expr phi1, Expr phi2) { this.qubit1 = qubit1; this.qubit2 = qubit2; this.phi1 = phi1; this.phi2 = phi2; }
        public String toEggString() { return String.format("(MS (Q \"%s\") (Q \"%s\") %s %s)", qubit1, qubit2, phi1.toEggString(), phi2.toEggString()); }

        @Override
        public int getTwoQubitsCount() {
            return 1;
        }

        @Override
        public String toAlphaEquivalentString(Map<String, String> qubitMap) {
            String qubit1Var = qubitMap.computeIfAbsent(qubit1, q -> "q" + qubitMap.size());
            String qubit2Var = qubitMap.computeIfAbsent(qubit2, q -> "q" + qubitMap.size());
            return String.format("(MS %s %s %s %s)", qubit1Var, qubit2Var, phi1.toEggString(), phi2.toEggString());
        }

         @Override
        public int getMaxQubits(){
            return Integer.max(Integer.valueOf(qubit1.replaceAll("q", "")), Integer.valueOf(qubit2.replaceAll("q", "")));
        }


        @Override
        public void getAllSymbols(Set<String> vars) {
            phi1.getAllSymbols(vars);
            phi2.getAllSymbols(vars);
        }

        @Override
        public String toQASM() {
            return toQASM(true);
        }

        @Override
        public String toQASM(boolean linebreak) {
            if(linebreak){
                return String.format("ms(%s,%s) %s, %s;\n", phi1.toString(), phi2.toString(), qubit1, qubit2);
            } else {
                return String.format("ms(%s,%s) %s, %s;", phi1.toString(), phi2.toString(), qubit1, qubit2);
            }
        }

        @Override
        public Gate instantiate(Map<String, Expr> angleMap) {
            return new MS(qubit1, qubit2, CircuitDAG.eval(phi1, angleMap), CircuitDAG.eval(phi2, angleMap));
        }
    }

    public static class SX extends Gate {
        public final String qubit;
        public SX(String qubit) { this.qubit = qubit; }
        public String toEggString() { return String.format("(SX (Q \"%s\"))", qubit); }

        @Override
        public int getTwoQubitsCount() {
            return 0;
        }

        @Override
        public String toAlphaEquivalentString(Map<String, String> qubitMap) {
            String var = qubitMap.computeIfAbsent(qubit, q -> "q" + qubitMap.size());
            return String.format("(SX %s)", var);
        }


        @Override
        public int getMaxQubits(){
            return Integer.valueOf(qubit.replaceAll("q", ""));
        }

        @Override
        public String toQASM() {
            return toQASM(true);
        }

        @Override
        public String toQASM(boolean linebreak) {
            if(linebreak){
                return String.format("sx %s;\n", qubit);
            } else {
                return String.format("sx %s;", qubit);
            }
        }

        @Override
        public Gate instantiate(Map<String, Expr> angleMap) {
            return new SX(qubit);
        }
    }
}
