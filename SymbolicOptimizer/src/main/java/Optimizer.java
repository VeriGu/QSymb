
import java.util.List;

public class Optimizer {
    private EggGen egraph;






    public void optimize(ConstrainedCircuit circuit, EggGen egraph, int maxQubits) {
        // Initialize the e-graph with the circuit
        String name = egraph.addConstrainedCircuit(CircuitTranslator.translate(circuit));

        List<String> rules = egraph.getAllRewriteRules();
        //select some rules to apply
        // Extract the optimized circuit from the e-graph
        
        ConstrainedCircuit optimizedCircuit = CircuitTranslator.translateBack(egraph.extract(name), maxQubits);


        System.out.println("Optimized Circuit: " + optimizedCircuit);
    }
}
