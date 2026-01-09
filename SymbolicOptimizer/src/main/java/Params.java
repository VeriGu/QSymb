

import org.apache.commons.math3.util.Pair;

import java.util.Map;
import java.util.Random;

public class Params {
    public static int MAX_QUBITS_SYMB = 7;
    /**
     * max priority queue size
     */
    public static int QUEUE_SIZE = 1;
    /**
     * max number of qubits allowed for symbolic circuit
     */
    public static int MAX_SYMB_QUBITS = 7;
    /**
     * max size (number of gates) to consider for symbolic circuit
     */
    public static int MAX_SYMB_SIZE = 10;
    /**
     * max qubits allowed in rules. -1 for no limit
     */
    public static int MAX_RULE_QUBITS = -1;
    /**
     * remove size-preserving nonsymbolic rules
     */
    public static boolean REMOVE_SIZE_PRESERVING_RULES = false;
    /**
     * use size-preserving symbolic rules
     */
    public static boolean USE_SIZE_PRESERVING_SYMB_RULES = false;
    /**
     * use the reflection of size preserving rule as well. i.e. use both directions of rule
     */
    public static boolean USE_SIZE_PRESERVE_RULE_REFLECTION = false;
    /**
     * use size increasing nonsymb rules
     */
    public static boolean USE_SIZE_INCREASING_RULES = false;
    /**
     * preserve connectivity of input circuit (preserve mapping)
     */
    public static boolean PRESERVE_MAPPING = false;
    /**
     * relative path to directory for output circuit. creates directory if it does not exist
     */
    public static String OUTPUT_DIR = "";
    /**
     * job info to add to output file name
     */
    public static String JOB_INFO = "";
    /**
    
    /**
     * optimization objective
     */
    public static CircuitDAG.OptObj OPTIMIZATION_OBJECTIVE = CircuitDAG.OptObj.TWO_Q;
    /**
     * 
     * Cost of a 2q gate in terms of 1q gates for FIDELITY opt obj. Or weight of T gate vs. 2q gate for FT opt obj.
     */
    public static int FIDELITY_BREAKEVEN = 1;
    /**
     *
     */
    public static Double ERROR_1Q = null;
    /**
     *
     */
    public static Double ERROR_2Q = null;
   
  

    public static int MAX_RESYNTH_ALLOWED = 100;
    /**
     * error threshold for final circuit
     */
    public static double EPSILON = 1e-8;
    /**
     * seed
     */
    public static int SEED = new Random().nextInt();
    /**
     * temperature for simulated annealing or beta for mcmc. for beam search, temperature is for softmax. If 0 then polls priority queue.
     */
    public static double TEMPERATURE = 10.0;
    /**
     * cooling rate for simulated annealing. 0 for mcmc. for beam search, this is the rate to prune rules so 0 for no pruning.
     */
    public static double COOLING_RATE = 0.0;
    /**
     * temperature for pruning rules. 0 samples rules greedily
     */
    public static double PRUNE_TEMPERATURE = 0.0;
    /**
     * iterations to wait before starting to prune rules
     */
    public static int ITERS_BEFORE_PRUNE = -1;
    /**
     * seconds to wait before starting to prune rules
     */
    public static int SECS_BEFORE_PRUNE = -1;
    /**
     * number of transformations to sample per iteration
     */
    public static int NUM_TRANSFORMATIONS_SAMPLE = 1;
    /**
     * weight of resynthesis when sampling transformations randomly
     */
    public static int RESYNTH_WEIGHT = 1;
    public static double RESYNTH_PERCENTAGE = 0.015;
    /**
     * apply rewrite rule only once per iteration if true instead of all disjoint matches
     */
    public static boolean APPLY_ONCE = false;
    /**
     * 0: no logs
     * 1: log progress
     * 2: log progress and config info
     * 3: log progress, config info, and rules applied
     */
    public static int VERBOSITY = 0;
    /**
     * gate set
     */

    /**
     * nonsymbolic rule file
     */
    public static String RULE_FILE = null;
    /**
     * symbolic rule file
     */
    public static String SYMB_RULE_FILE = null;

    public static String RULES_DIR = "";

   
}
