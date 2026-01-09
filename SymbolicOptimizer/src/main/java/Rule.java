
import java.util.List;
import java.util.stream.Collectors;
public class Rule {
    public EggGen.Circuit lhs;
    public EggGen.Circuit rhs;
    
    public List<Equality> conditions;

    public Rule(EggGen.Circuit lhs, EggGen.Circuit rhs, List<Equality> conditions) {
        this.lhs = lhs;
        this.rhs = rhs;
        this.conditions = conditions;
    }


    public static class Equality {
        public String qubit1;
        public String qubit2;
        public boolean isEqual; // true for '=', false for '!='

        public Equality(String qubit1, String qubit2, boolean isEqual) {
            this.qubit1 = qubit1;
            this.qubit2 = qubit2;
            this.isEqual = isEqual;
        }
    }

    public List<Equality> getEqualities() {
        return conditions;
    }

    public EggGen.Circuit getLHS() {
        return lhs;
    }

    public EggGen.Circuit getRHS() {
        return rhs;
    }

    public String toString() {
        return lhs.toQASM() + " | " + rhs.toQASM() + "; when (" + conditions.stream().map(e -> String.format("(%s %s %s)",  e.isEqual ? "=" : "!=", e.qubit1, e.qubit2)).collect(Collectors.joining(" ")) + ")";
    }
}
