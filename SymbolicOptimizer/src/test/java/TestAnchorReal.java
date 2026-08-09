import java.io.*;
import java.util.*;
import java.util.regex.*;
import ast.*;

public class TestAnchorReal {

    static List<MatrixConstrainedRule> loadCanonicalSymbRules(String path) throws Exception {
        List<MatrixConstrainedRule> out = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split("\\|");
                if (parts.length < 4) continue;
                String lhs = parts[0], rhs = parts[1], type = parts[2], mstr = parts[3];
                Pattern p = Pattern.compile("\\[(.*)\\]");
                Matcher m = p.matcher(mstr);
                if (!m.matches()) continue;
                String[] matrices = m.group(1).split("::");
                List<SymbolicSolve.SparseMatrix> mlist = new ArrayList<>();
                for (String mm : matrices) {
                    if (!mm.startsWith("Matrix(")) continue;
                    String content = mm.substring("Matrix(".length(), mm.length() - 1);
                    String[] entries = content.split(";");
                    int rows = Integer.valueOf(entries[0]);
                    int cols = Integer.valueOf(entries[1]);
                    List<SymbolicSolve.SparseMatrix.MatrixEntry> ents = new ArrayList<>();
                    for (int i = 2; i < entries.length; i++) {
                        String e = entries[i].substring(1, entries[i].length() - 1);
                        String[] elems = e.split(",");
                        ents.add(new SymbolicSolve.SparseMatrix.MatrixEntry(
                                Integer.valueOf(elems[0].trim()),
                                Integer.valueOf(elems[1].trim()),
                                SymbolicSolve.parseComplex(elems[2])));
                    }
                    mlist.add(new SymbolicSolve.SparseMatrix(rows, cols, ents));
                }
                out.add(new MatrixConstrainedRule(lhs, rhs, mlist, type));
            }
        }
        return out;
    }

    static Rule mkRule(String lhsQasm, String rhsQasm) {
        EggGen.Circuit l = QASMAstBuilder.parse(lhsQasm);
        EggGen.Circuit r = QASMAstBuilder.parse(rhsQasm);
        return new Rule(l, r, new ArrayList<>());
    }

    public static void main(String[] args) throws Exception {
        List<MatrixConstrainedRule> symbRules = loadCanonicalSymbRules("/root/rules_ion_q3_2_symb_nm.txt");
        System.out.println("Loaded " + symbRules.size() + " canonical symbolic rules");

        List<Rule> concreteRules = new ArrayList<>();
        concreteRules.add(mkRule(
                "rxx(pi/2) q[0],q[1]; rz(theta3) q[0]; rx(-pi/2) q[0]; rxx(pi/2) q[0],q[1];",
                "rxx(theta3) q[0],q[1]; rx(pi/2) q[0]; rx(pi) q[1];"
        ));
        concreteRules.add(mkRule(
                "rxx(theta1) q[0],q[1]; rxx(theta2) q[0],q[1];",
                "rxx((theta1+theta2)) q[0],q[1];"
        ));
        System.out.println("Loaded " + concreteRules.size() + " concrete rules");

        System.out.println("\n========== Running Anchor ==========\n");
        List<MatrixConstrainedRule> anchored = Anchor.anchor(concreteRules, symbRules);
        System.out.println("Total anchored rules generated: " + anchored.size());

        int original = symbRules.size();
        System.out.println("\n========== NEW anchored rules ==========");
        int newCount = 0;
        for (int i = 0; i < anchored.size(); i++) {
            MatrixConstrainedRule r = anchored.get(i);
            if (i < original) continue;
            newCount++;
            if (newCount > 30) break;
            System.out.println("\nNEW #" + newCount + ":");
            String lhs = r.getLHS();
            String rhs = r.getRHS();
            System.out.println("  LHS: " + (lhs.length() > 200 ? lhs.substring(0, 200) + "..." : lhs));
            System.out.println("  RHS: " + (rhs.length() > 200 ? rhs.substring(0, 200) + "..." : rhs));
        }
        System.out.println("\nTotal NEW anchored rules: " + (anchored.size() - original));
    }
}
