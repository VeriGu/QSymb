import org.apache.commons.math3.util.Pair;

import java.util.Map;
import java.util.Random;

public class Params {
    public static int MAX_QUBITS_SYMB = 5;
    public static int QUEUE_SIZE = 1;
    public static int MAX_SYMB_QUBITS = 7;
    public static int MAX_SYMB_SIZE = 10;
    public static int MAX_RULE_QUBITS = -1;
    public static boolean REMOVE_SIZE_PRESERVING_RULES = false;
    public static boolean USE_SIZE_PRESERVING_SYMB_RULES = false;
    public static boolean USE_SIZE_PRESERVE_RULE_REFLECTION = false;
    public static boolean USE_SIZE_INCREASING_RULES = false;
    public static boolean PRESERVE_MAPPING = false;
    public static String OUTPUT_DIR = "";
    public static String JOB_INFO = "";
    public static CircuitDAG.OptObj OPTIMIZATION_OBJECTIVE = CircuitDAG.OptObj.TWO_Q;
    public static int FIDELITY_BREAKEVEN = 40;
    public static Double ERROR_1Q = null;
    public static Double ERROR_2Q = null;

    public static int MAX_RESYNTH_ALLOWED = 100;
    public static double EPSILON = 1e-8;
    public static Double SYMB_APPROX_EPS = null;
    public static int SEED = new Random().nextInt();
    public static int ENUMERATOR_SEED = 42;
    public static int SYMB_SOLVE_TIMEOUT_SEC = 600;
    public static double TEMPERATURE = 10.0;
    public static double COOLING_RATE = 0.0;
    public static double PRUNE_TEMPERATURE = 0.0;
    public static int ITERS_BEFORE_PRUNE = -1;
    public static int SECS_BEFORE_PRUNE = -1;
    public static int NUM_TRANSFORMATIONS_SAMPLE = 1;
    public static int RESYNTH_WEIGHT = 1;
    public static double RESYNTH_PERCENTAGE = 0.015;
    public static boolean APPLY_ONCE = false;
    public static int VERBOSITY = 0;

    public static String RULE_FILE = null;
    public static String SYMB_RULE_FILE = null;

    public static String RULES_DIR = "";

    public static int EGRAPH_CHUNK_THRESHOLD = Integer.getInteger("egraph.chunk.threshold", 500);
    public static int EGRAPH_CHUNK_SIZE = Integer.getInteger("egraph.chunk.size", 500);

    public static String ILP_PYTHON = "python3";
    public static String ILP_SCRIPT = "/root/SymbolicOptimizer/scripts/ilp_compact.py";
    public static int ILP_TIME_LIMIT_SEC = 10;
    public static int ILP_PERIOD = 10;
    public static int ILP_MAX_GATES = 300;
}
