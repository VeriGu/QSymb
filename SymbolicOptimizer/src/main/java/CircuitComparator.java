import java.util.Comparator;

public class CircuitComparator implements Comparator<CircuitDAG> {
    private CircuitDAG.OptObj optObj;


    public CircuitComparator(CircuitDAG.OptObj optObj) {
        this.optObj = optObj;
    }

    @Override
    public int compare(CircuitDAG circuit1, CircuitDAG circuit2) {
        switch (this.optObj) {
            case TOTAL: { // total gate count -> create time
                return compareTotalGateCount(circuit1, circuit2);
            }
            case T: { // tcount -> total gate count -> create time
                return compareTGateCount(circuit1, circuit2);
            }
            case TWO_Q: { // 2q count -> total gate count -> create time
                return compare2qGateCount(circuit1, circuit2);
            }
            case TOTAL_IGNORE_RZ: { // total gate count ignoring rz -> create time
                return compareTotalGateCountIgnoreRz(circuit1, circuit2);
            }
            default:
                throw new RuntimeException("Unsupported optObj: " + this.optObj);
        }
    }


    public int compareTotalGateCount(CircuitDAG circuit1, CircuitDAG circuit2) {
        return circuit1.cost(CircuitDAG.OptObj.TOTAL) - circuit2.cost(CircuitDAG.OptObj.TOTAL);
    }

    public int compareTGateCount(CircuitDAG circuit1, CircuitDAG circuit2) {
        if(circuit1.cost(CircuitDAG.OptObj.T) < circuit2.cost(CircuitDAG.OptObj.T)) {
            return -1;
        } else if(circuit1.cost(CircuitDAG.OptObj.T) > circuit2.cost(CircuitDAG.OptObj.T)) {
            return 1;
        } else {
            return compareTotalGateCount(circuit1, circuit2);
        }
    }
    

    public int compare2qGateCount(CircuitDAG circuit1, CircuitDAG circuit2) {
        if(circuit1.cost(CircuitDAG.OptObj.TWO_Q) < circuit2.cost(CircuitDAG.OptObj.TWO_Q)) {
            return -1;
        } else if(circuit1.cost(CircuitDAG.OptObj.TWO_Q) > circuit2.cost(CircuitDAG.OptObj.TWO_Q)) {
            return 1;
        } else {
            return compareTotalGateCount(circuit1, circuit2);
        }
    }

    public int compareTotalGateCountIgnoreRz(CircuitDAG circuit1, CircuitDAG circuit2) {
        if(circuit1.cost(CircuitDAG.OptObj.TOTAL_IGNORE_RZ) < circuit2.cost(CircuitDAG.OptObj.TOTAL_IGNORE_RZ)) {
            return -1;
        } else if(circuit1.cost(CircuitDAG.OptObj.TOTAL_IGNORE_RZ) > circuit2.cost(CircuitDAG.OptObj.TOTAL_IGNORE_RZ)) {
            return 1;
        } else {
            return compareTotalGateCount(circuit1, circuit2);
        }
    }
}
