import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;



public class RuleSetCoverage {
    public static void rulesetCoverageTest(List<String> ruleset1, List<String> ruleset2, List<String> commutative, String gateset) {
        EggGen egraph = new EggGen();

        // Merge wire rules into the opt ruleset (same approach as Optimizer:
        // single saturation ruleset, no separate wire pass).
        for(String rule : commutative) {
            String merged = rule.replace(":ruleset wire", ":ruleset opt")
                                .replace(":ruleset merge", ":ruleset opt");
            egraph.addRewrite(merged);
        }

        // Add ruleset1 (rule_copy.txt) using the same RuleReverser policy the
        // optimizer uses: pattern-detect Pauli pushthrough / CX-braid and add
        // BOTH directions only for those; non-pattern increasing rules get
        // their reverse only (size-decreasing direction); size-preserving
        // becomes birewrite.
        for(String rule : ruleset1) {
            Rule r;
            try {
                r = QASMAstBuilder.parseRule(rule);
            } catch (Throwable ex) {
                continue;
            }
            RuleReverser.Direction d = RuleReverser.decide(r, gateset);
            if (d == RuleReverser.Direction.DROP) continue;

            int lhsSize = r.lhs.gates.size();
            int rhsSize = r.rhs.gates.size();

            if (lhsSize == rhsSize) {
                // Symmetric → birewrite in opt ruleset
                egraph.addOptRule(r, "opt", "birewrite");
                continue;
            }
            boolean forwardIsDec = lhsSize > rhsSize;
            // Forward direction
            if (d == RuleReverser.Direction.FORWARD_ONLY || d == RuleReverser.Direction.BOTH) {
                egraph.addOptRule(r, "opt", "rewrite");
            }
            // Reverse direction (when fireable)
            if (d == RuleReverser.Direction.REVERSE_ONLY || d == RuleReverser.Direction.BOTH) {
                if (RuleReverser.reverseIsFireable(r)) {
                    Rule reversed = new Rule(r.rhs, r.lhs, r.conditions);
                    egraph.addOptRule(reversed, "opt", "rewrite");
                }
            }
        }

        List<String> uncovered = new ArrayList<>();
     
        for(String rule : ruleset2) {
            String[] compo = rule.split("\\|");
            String lhs = compo[0].trim();
            String rhs = compo[1].trim();
            // Strip trailing "when q0 != q1, ..." clause from rhs — it's a
            // rule-level condition, not part of the circuit.
            int whenIdx = rhs.toLowerCase().indexOf(" when ");
            if (whenIdx >= 0) rhs = rhs.substring(0, whenIdx).trim();
            //String type = compo[2];
            System.out.println("Rule: " + rule);
            egraph.push();
            EggGen.Circuit c1 = QASMAstBuilder.parse(lhs);
            EggGen.Circuit c2 = QASMAstBuilder.parse(rhs);
            System.out.println("LHS Circuit: " + c1.toEggString());
            System.out.println("RHS Circuit: " + c2.toEggString());
            egraph.addCircuit(c1);
            egraph.addCircuit(c2);
            // Add BOTH sides so rules fire on each, letting them meet in the
            // middle (e.g. LHS reduces via merge+const, RHS via mod-2π, then
            // unify). Without adding c2, rules never run on the RHS structure.
            egraph.runN("opt", 5);
            if(egraph.check(String.format("(= %s %s)", c1.toEggString(), c2.toEggString()))) {
                System.out.println("Rule: " + rule + " is covered");
            } else {
                uncovered.add(rule);
                System.out.println("Rule: " + rule + " is not covered");    
            }
            egraph.pop();
        }


        for(String rule : uncovered) {
            System.out.println("Uncovered Rule: " + rule);
        }
    }


    public static void filterRules(List<SimpleEntry<EggGen.Circuit, EggGen.Circuit>> entries) {
        EggGen egraph = new EggGen();
        int i = 0;
        for(SimpleEntry<EggGen.Circuit, EggGen.Circuit> entry : entries) {
          System.out.println("Processing rule: " + i + " of " + entries.size());
          System.out.println("Rule: " + entry.getKey().toQASM() + " | " + entry.getValue().toQASM());
          i++;
          egraph.push();
          egraph.addCircuit(entry.getKey());
          egraph.addCircuit(entry.getValue());
          egraph.runBackoff("opt", 5);
          egraph.check(String.format("(= %s %s)", entry.getKey().toEggString(), entry.getValue().toEggString()));
          boolean ifcovered = false;
          if(egraph.check(String.format("(= %s %s)", entry.getKey().toEggString(), entry.getValue().toEggString()))) {
            ifcovered = true;
          }
          egraph.pop();
          if(!ifcovered) {
            egraph.addRewriteRule(new SimpleEntry<>(new EggGen.ConstrainedCircuit(entry.getKey(), new EggGen.Permutation(new ArrayList<>())), new EggGen.ConstrainedCircuit(entry.getValue(), new EggGen.Permutation(new ArrayList<>()))), true);
            List<Rule> rules = egraph.processRules(egraph.optrules);
            for(Rule r : rules) {
                egraph.addOptRule(r, "opt", "birewrite");
            }
          }
        }

        try {
            FileWriter fw = new FileWriter("filtered_rules.txt");
            PrintWriter pw = new PrintWriter(fw);
            for(String rule : egraph.optrules) {
                pw.println(rule);
            }
            pw.close();
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        // Args: <ruleset1> <ruleset2> [gateset]
        // ruleset1: rules used as derivation source (added to opt ruleset)
        // ruleset2: rules to check coverage for
        // gateset:  optional, default "ibmnew" (controls RuleReverser policy
        //           and which rules_<g>.txt is loaded as wire rules)
        File file1 = new File(args[0]);
        String gateset = args.length >= 3 ? args[2] : "ibmnew";
       
        // List<SimpleEntry<EggGen.Circuit, EggGen.Circuit>> ruleset = new ArrayList<>();
        // try (BufferedReader br = new BufferedReader(new FileReader(file1, StandardCharsets.UTF_8))) {
        //     String line;
        //     while ((line = br.readLine()) != null) {
        //         String[] compo = line.split("\\|");
        //         String lhs = compo[0];
        //         String rhs = compo[1];
        //         EggGen.Circuit c1 = QASMAstBuilder.parse(lhs);
        //         EggGen.Circuit c2 = QASMAstBuilder.parse(rhs);
        //         SimpleEntry<EggGen.Circuit, EggGen.Circuit> entry = new SimpleEntry<>(c1, c2);
        //         ruleset.add(entry);
        //     }
        // } catch (IOException e) {
        //     e.printStackTrace();
        // }

        // filterRules(ruleset);
        File file2 = new File(args[1]);
        List<String> ruleset1 = new ArrayList<>();
        List<String> ruleset2 = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(file1, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                ruleset1.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        try (BufferedReader br = new BufferedReader(new FileReader(file2, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                ruleset2.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        List<String> commutative = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader("rules_" + gateset + ".txt", StandardCharsets.UTF_8))) {
            String line;
            commutative.clear();
            while ((line = br.readLine()) != null) {
                commutative.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        rulesetCoverageTest(ruleset1, ruleset2, commutative, gateset);
    }
}
