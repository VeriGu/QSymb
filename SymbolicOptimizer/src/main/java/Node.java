import ast.Expr;
import org.apache.commons.collections4.CollectionUtils;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import java.util.stream.Collectors;

public class Node {

    public enum Type {
        GATE,
        QUBIT_SOURCE,
        QUBIT_SINK,
        PARAMETER
    }

    private String id;
    private Type type;
    private List<String> qubits;
    private List<Expr> angles;

    public Node(String id, Type type, List<String> qubits, List<Expr> angles) {
        this.id = id;
        this.type = type;
        this.qubits = qubits;
        this.angles = angles;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public List<String> getQubits() {
        return qubits;
    }

    public void setQubits(List<String> qubits) {
        this.qubits = qubits;
    }

    public List<Expr> getAngles() {
        return angles;
    }

    public void setAngles(List<Expr> angles) {
        this.angles = angles;
    }

    public boolean isGate() {
        return this.type.equals(Type.GATE);
    }

    public boolean isQubit() {
        return this.type.equals(Type.QUBIT_SOURCE) || this.type.equals(Type.QUBIT_SINK);
    }

    public boolean isSourceQubit() {
        return this.type.equals(Type.QUBIT_SOURCE);
    }

    public boolean isSinkQubit() {
        return this.type.equals(Type.QUBIT_SINK);
    }

    public boolean isCX() {
        return this.id.equals("cx") || this.id.equals("cz") || this.id.equals("rxx");
    }

    public boolean isCCZ() {
        return this.id.equals("ccz");
    }

    public boolean is2QGate() {
        return this.isGate() && this.qubits.size() == 2;
    }

    public boolean isTGate() {
        return this.isGate() && (this.id.equals("t") || this.id.equals("tdg") || (this.id.equals("rz") && Math.abs((CircuitDAG.eval(this.getAngles().get(0)) / Math.PI) % 0.5) == 0.25));
    }



    public int hash() {
        int result = Objects.hash(id, type, angles);
        result = 31 * result + (qubits != null ? qubits.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return toStringHelper(HashBiMap.create());
    }


    public String toString(BiMap<String, String> qubitRenameMap) {
        return toStringHelper(qubitRenameMap);
    }

    private String toStringHelper(BiMap<String, String> qubitRenameMap) {
        String result = id;

        if (isQubit()) {
            return id + " " + type;
        }

        if (angles != null && !angles.isEmpty()) {
            result = result.concat("(");
            result = result.concat(String.join(",", angles.stream().map(Expr::toString).collect(Collectors.toList())));
            result = result.concat(")");
        }

        result = result.concat(" ");
        result = result.concat(String.join(",", qubits.stream().map(q -> qubitRenameMap.inverse().getOrDefault(q, "q" + "["+q.replaceAll("q", "")+"]")).collect(Collectors.toList())));

        return result;
    }
}
