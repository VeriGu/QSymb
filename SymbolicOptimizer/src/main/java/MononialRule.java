import java.util.List;
import java.util.Map;

public class MononialRule {
    private String lhs;
    private String rhs;
    private List<Map<boolean[], boolean[]>> constraints;

    public MononialRule(String lhs, String rhs, List<Map<boolean[], boolean[]>> constraints) {
        this.lhs = lhs;
        this.rhs = rhs;
        this.constraints = constraints;
    }

    public String getLhs() {
        return lhs;
    }

    public String getRhs() {
        return rhs;
    }

    public List<Map<boolean[], boolean[]>> getConstraints() {
        return constraints;
    }
}
