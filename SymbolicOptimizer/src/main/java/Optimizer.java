import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;


public class Optimizer {
    public void optimize(ConstrainedCircuit circuit, EggGen egraph, int maxQubits) {
        String name = egraph.addConstrainedCircuit(CircuitTranslator.translate(circuit));
        egraph.runSaturation("opt");
        EggGen.ConstrainedCircuit extracted = egraph.extract(name);
        ConstrainedCircuit optimizedCircuit = CircuitTranslator.translateBack(extracted, maxQubits);
        System.out.println("Optimized Circuit: " + optimizedCircuit.getCircuit().toString());
    }

    public static void main(String[] args) throws IOException {
        Options options = new Options();

        Option benchmarkO = new Option("b", "benchmark", true, "benchmark file path");
        benchmarkO.setRequired(true);
        options.addOption(benchmarkO);

        Option rulesO = new Option("r", "rules", true, "ruleset file path");
        rulesO.setRequired(true);
        options.addOption(rulesO);
        
        Option maxQubitsO = new Option("q", "maxQubits", true, "max qubits");
        maxQubitsO.setRequired(true);
        options.addOption(maxQubitsO);

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd;

        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("Optimizer", options);
            System.exit(1);
            return;
        }

        String benchmarkFile = cmd.getOptionValue("benchmark");
        String rulesFile = cmd.getOptionValue("rules");
        int maxQubits = Integer.parseInt(cmd.getOptionValue("maxQubits"));

        EggGen egraph = new EggGen();

        try (BufferedReader br = new BufferedReader(new FileReader(rulesFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                egraph.addRewrite(line);
            }
        }

        String circuitString = new String(Files.readAllBytes(Paths.get(benchmarkFile)));
        // Circuit circuit = EggAstBuilder.parseCircuit(circuitString);
        // ConstrainedCircuit constrainedCircuit = new ConstrainedCircuit(circuit, new EggGen.Permutation(new ArrayList<>()));

        // Optimizer optimizer = new Optimizer();
        // optimizer.optimize(constrainedCircuit, egraph, maxQubits);
        
        egraph.stopEgglogREPL();
    }
}