import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import static org.junit.jupiter.api.Assertions.assertEquals;
import java.io.IOException;
public class QASMToDAGVisitorTest {

    @Test
    public void testParse2() {
        File file = new File("eggtest.txt");
        try {
            FileReader fileReader = new FileReader(file);
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            String line;
            String egg = "";
            while ((line = bufferedReader.readLine()) != null) {
                egg += line;
            }
            bufferedReader.close();
            fileReader.close();
             System.out.println(egg);
            EggGen.Circuit c = EggAstBuilder.parseCircuit(egg);
            System.out.println("Total Gates:" + c.gates.size());
            System.out.println("2q Gates:" + c.getTwoQubitsCount());
            assertTrue(true);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Test
    public void testParse() {
        String qasm = "OPENQASM 2.0;\n" +
                      "include \"qelib1.inc\";\n" +
                      "qreg q[2];\n" +
                      "creg c[2];\n" +
                      "h q[0];\n" +
                      "cx q[0], q[1];\n";
        CircuitDAG dag = QASMToDAGVisitor.parse(qasm);
        assertNotNull(dag);
    }

    @Test
    public void testParseNodeCount() {
        String qasm = "OPENQASM 2.0;\n" +
                      "include \"qelib1.inc\";\n" +
                      "qreg q[2];\n" +
                      "creg c[2];\n" +
                      "h q[0];\n" +
                      "cx q[0], q[1];\n";
        CircuitDAG dag = QASMToDAGVisitor.parse(qasm);
        assertEquals(6, dag.getDAG().vertexSet().size());
    }

    @Test
    public void testParseWithParameters() {
        String qasm = "rz(theta1 + theta2) q[0];\n" +
                      "h q[0];\n";
        CircuitDAG dag = QASMToDAGVisitor.parse(qasm);
        String parsedQasm = dag.toQASM();
        assertEquals("rz(theta1+theta2) q[0];\nh q[0];\n", parsedQasm);
    }

    @Test
    public void testParseWithMultipleParameters() {
        String qasm = "u3(pi, theta, 0) q[0];";
        CircuitDAG dag = QASMToDAGVisitor.parse(qasm);
        String parsedQasm = dag.toQASM();
        assertEquals("u3(3.141592653589793,theta,0.0) q[0];\n", parsedQasm);
    }

    @Test
    public void testParseComplexString() {
        String qasm = "rz(-pi/2.0+pi) q[2];\n" +
                      "h q[2];\n" +
                      "rz(-pi/2.0+2.0*pi+-pi/2.0) q[2];\n" +
                      "h q[2];\n" +
                      "rz(3.0*pi+-pi/2.0) q[2];\n" +
                      "cx q[0],q[2];\n" +
                      "cx q[2],q[1];\n" +
                      "h q[0];\n" +
                      "rz(pi/4.0) q[1];\n" +
                      "rz(pi/4.0) q[2];\n" +
                      "rz(pi/4.0+pi/2.0+-pi/2.0) q[0];\n" +
                      "cx q[2],q[1];\n" +
                      "cx q[0],q[2];\n" +
                      "cx q[1],q[0];\n" +
                      "rz(-pi/4.0) q[2];\n" +
                      "rz(pi/4.0) q[0];\n" +
                      "cx q[1],q[2];\n" +
                      "rz(-pi/4.0) q[1];\n" +
                      "rz(-pi/4.0) q[2];\n" +
                      "cx q[0],q[2];\n" +
                      "cx q[1],q[0];\n" +
                      "cx q[2],q[1];\n" +
                      "h q[0];\n" +
                      "rz(pi/4.0) q[1];\n" +
                      "rz(pi/4.0+pi/2.0+-pi/2.0) q[0];\n" +
                      "h q[2];\n" +
                      "cx q[1],q[0];\n" +
                      "rz(pi/4.0+pi/2.0+-pi/2.0) q[2];\n" +
                      "cx q[2],q[1];\n" +
                      "cx q[0],q[2];\n" +
                      "rz(-pi/4.0) q[1];\n" +
                      "rz(pi/4.0) q[2];\n" +
                      "cx q[0],q[1];\n" +
                      "rz(-pi/4.0) q[0];\n" +
                      "rz(-pi/4.0) q[1];\n" +
                      "cx q[2],q[1];\n" +
                      "cx q[0],q[2];\n" +
                      "cx q[1],q[0];\n" +
                      "h q[2];\n" +
                      "cx q[1],q[2];";
        CircuitDAG dag = QASMToDAGVisitor.parse(qasm);
        String expectedQasm =
                "rz(1.5707963267948966) q[2];\n" +
                "h q[2];\n" +
                "rz(3.141592653589793) q[2];\n" +
                "h q[2];\n" +
                "rz(7.853981633974483) q[2];\n" +
                "cx q[0],q[2];\n" +
                "cx q[2],q[1];\n" +
                "h q[0];\n" +
                "rz(0.7853981633974483) q[1];\n" +
                "rz(0.7853981633974483) q[2];\n" +
                "rz(0.7853981633974483) q[0];\n" +
                "cx q[2],q[1];\n" +
                "cx q[0],q[2];\n" +
                "cx q[1],q[0];\n" +
                "rz(-0.7853981633974483) q[2];\n" +
                "rz(0.7853981633974483) q[0];\n" +
                "cx q[1],q[2];\n" +
                "rz(-0.7853981633974483) q[1];\n" +
                "rz(-0.7853981633974483) q[2];\n" +
                "cx q[0],q[2];\n" +
                "cx q[1],q[0];\n" +
                "cx q[2],q[1];\n" +
                "h q[0];\n" +
                "rz(0.7853981633974483) q[1];\n" +
                "h q[2];\n" +
                "rz(0.7853981633974483) q[0];\n" +
                "rz(0.7853981633974483) q[2];\n" +
                "cx q[1],q[0];\n" +
                "cx q[2],q[1];\n" +
                "cx q[0],q[2];\n" +
                "rz(-0.7853981633974483) q[1];\n" +
                "rz(0.7853981633974483) q[2];\n" +
                "cx q[0],q[1];\n" +
                "rz(-0.7853981633974483) q[0];\n" +
                "rz(-0.7853981633974483) q[1];\n" +
                "cx q[2],q[1];\n" +
                "cx q[0],q[2];\n" +
                "cx q[1],q[0];\n" +
                "h q[2];\n" +
                "cx q[1],q[2];\n";
        assertEquals(expectedQasm, dag.toQASM());
    }

}
