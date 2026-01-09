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
    public static void rulesetCoverageTest(List<String> ruleset1, List<String> ruleset2, List<String> commutative) {
        EggGen egraph = new EggGen();

        for(String rule : commutative) {
            egraph.addRewrite(rule);
        }

        for(String rule : ruleset1) {
            Rule r = QASMAstBuilder.parseRule(rule);
            List<Rule.Equality> equalities = r.getEqualities();
            String eggrule = String.format("(%s %s %s %s :ruleset %s)", "rewrite",EggGen.circuitToGeneralizedOnlyRemoveQ(r.lhs, "c"), EggGen.circuitToGeneralizedOnlyRemoveQ(r.rhs, "c"), ":when (" + equalities.stream().map(e -> String.format("(%s %s %s)",  e.isEqual ? "=" : "!=", e.qubit1, e.qubit2)).collect(Collectors.joining(" ")) + ")", "opt");
            String eggrule2 = String.format("(%s %s %s %s :ruleset %s)", "rewrite",EggGen.circuitToGeneralizedOnlyRemoveQ(r.rhs, "c"), EggGen.circuitToGeneralizedOnlyRemoveQ(r.lhs, "c"), ":when (" + equalities.stream().map(e -> String.format("(%s %s %s)",  e.isEqual ? "=" : "!=", e.qubit1, e.qubit2)).collect(Collectors.joining(" ")) + ")", "opt");
            egraph.addRewrite(eggrule);
            egraph.addRewrite(eggrule2);
        }

        List<String> uncovered = new ArrayList<>();
     
        for(String rule : ruleset2) {
            String[] compo = rule.split("\\|");
            String lhs = compo[0].trim();
            String rhs = compo[1].trim();
            //String type = compo[2];
            System.out.println("Rule: " + rule);
            egraph.push();
            EggGen.Circuit c1 = QASMAstBuilder.parse(lhs);
            EggGen.Circuit c2 = QASMAstBuilder.parse(rhs);
            System.out.println("LHS Circuit: " + c1.toEggString());
            System.out.println("RHS Circuit: " + c2.toEggString());
            egraph.addCircuit(c1);
            egraph.addCircuit(c2);
            egraph.runN("wire", 6);
            egraph.runBackoff("opt", 15);
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
        //take two files as input and read the ruleset1 and ruleset2 from the files
        File file1 = new File(args[0]);
       
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
        try (BufferedReader br = new BufferedReader(new FileReader("rules_ibmnew.txt", StandardCharsets.UTF_8))) {
            String line;
            commutative.clear();
            while ((line = br.readLine()) != null) {
                commutative.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        rulesetCoverageTest(ruleset1, ruleset2, commutative);
    }
}
