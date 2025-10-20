import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class Circuit {
  private List<String> qubits;
  private Set<String> usedQubits;
  private List<Symbolic> pathSum;
  private List<String> qasm;

  // list of gates representation;
  private List<EggGen.Gate> gates;

  public Circuit(List<String> qubits, List<Symbolic> pathSum, List<String> qasm, List<EggGen.Gate> gates) {
    this.qubits = qubits;
    this.usedQubits = new TreeSet<>();
    this.pathSum = pathSum;
    this.qasm = qasm;
    this.gates = gates;
  }


   public Circuit(Circuit c) {
    this.qubits = c.qubits;
    this.usedQubits = c.usedQubits;
    this.pathSum = c.pathSum;
    this.qasm = c.qasm;
    this.gates = new ArrayList<>(c.gates);
  }

  public List<String> getQubits() {
    return qubits;
  }

  public void setQubits(List<String> qubits) {
    this.qubits = qubits;
  }

  public List<EggGen.Gate> getGates() {
    return new ArrayList<>(gates);
  }


  public void addGate(int index, EggGen.Gate gate) {
    this.gates.add(index, gate);
  }

  public void addGate(EggGen.Gate gate) {
    this.gates.add(gate);
  }

  public Set<String> getUsedQubits() {
    return usedQubits;
  }

  public void setUsedQubits(Set<String> usedQubits) {
    this.usedQubits = usedQubits;
  }

  public List<Symbolic> getPathSum() {
    return pathSum;
  }

  public void setPathSum(List<Symbolic> pathSum) {
    this.pathSum = pathSum;
  }

  public List<String> getQasm() {
    return qasm;
  }

  public void setQasm(List<String> qasm) {
    this.qasm = qasm;
  }

  public String getQasmString() {
    return String.join("; ", qasm).concat(";");
  }

  public String getQasmStringDropFirst() {
    return String.join("; ", qasm.subList(1, qasm.size())).concat(";");
  }

  public boolean hasSymb() {
    return qasm.contains(Symbolic.SYMB);
  }

  public boolean hasQubit(String qubit) {
    return usedQubits.contains(qubit);
  }

  public boolean hasQubitGreaterThan(int max) {
    for (String qubit : usedQubits) {
      if (Integer.valueOf(qubit.replace("q", "")) >= max) {
        return true;
      }
    }
    return false;
  }

  public void addQubit(String qubit) {
    this.usedQubits.add(qubit);
  }

  public String getLastOp() {
    if (!qasm.isEmpty()) {
      return qasm.get(qasm.size() - 1);
    } else {
      return "";
    }
  }

  public boolean hasCXH() {
    for (String op : qasm) {
      if (op.contains("cx") || op.contains("h ") || op.contains("cz") || op.contains("rx") || op.contains("ry") || op.contains("rxx") || op.contains("sx")) {
        return true;
      }
    }
    return false;
  }

  public int getSize() {
    return this.qasm.size();
  }

  @Override
  public String toString() {
    return "Circuit [qubits=" + qubits + ", usedQubits=" + usedQubits + ", qasm=" + getQasmString() + ", pathSum=" + pathSum + "]";
  }
}
