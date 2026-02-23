import java.util.List;

public class MatrixConstrainedRule {

    private String lhs;
    private String rhs;
    private String type;
    private List<SymbolicSolve.SparseMatrix> constraint;

    public MatrixConstrainedRule(String lhs, String rhs, List<SymbolicSolve.SparseMatrix> constraint, String type) {
        this.lhs = lhs;
        this.rhs = rhs;
        this.type = type;
        this.constraint = constraint;
    }

     public MatrixConstrainedRule(String lhs, String rhs, String type) {
        this.lhs = lhs;
        this.rhs = rhs;
        this.type = type;
    }

    public String getLHS() {
        return lhs;
    }

    public String getRHS() {
        return rhs;
    }


    public List<SymbolicSolve.SparseMatrix> getConstraint() {
        return constraint;
    }

    public String getType() {
        return type;
    }

    public void setConstraint(List<SymbolicSolve.SparseMatrix> constraint) {
        this.constraint = constraint;
    }

    @Override
    public String toString() {
        StringBuilder start = new StringBuilder();
        start.append(lhs + "|" + rhs + "|" + type + "|[");
        for(int i = 0; i < constraint.size(); i++) {
            if(i != constraint.size()-1) {
                start.append(constraint.get(i)).append("::");
            } else {
                start.append(constraint.get(i));
            }
        }
        start.append("]");
        return start.toString();
    }


    @Override
    public boolean equals(Object o) {
        if(o instanceof MatrixConstrainedRule) {
            return lhs.equals(((MatrixConstrainedRule) o).lhs) && rhs.equals(((MatrixConstrainedRule) o).rhs);
        }

        return false;
    }

    @Override
    public int hashCode() {
        return lhs.hashCode() * 10 + rhs.hashCode();
    }
}