import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.nio.file.FileSystems;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EnumeratorPruneTest {

    static String FILENAME = "test_rules";
    static String PATH_NONSYMB = FileSystems.getDefault().getPath(new String()).toAbsolutePath() + String.format("/%s.txt", FILENAME);
    static String PATH_SYMB = FileSystems.getDefault().getPath(new String()).toAbsolutePath() + String.format("/%s_symb.txt", FILENAME);
    List<String> EXPECTED_NONSYMB_RULES = new ArrayList<>(Arrays.asList(
            "cx q0, q1; x q0; h q0; | x q0; cx q0, q1; h q0; x q1;",
            "cx q0, q1; rz(theta1) q0; | rz(theta1) q0; cx q0, q1;",
            "cx q0, q1; cx q2, q1; | cx q2, q1; cx q0, q1;",
            "cx q2, q0; cx q2, q1; | cx q2, q1; cx q2, q0;",
            "; | cx q1, q0; cx q1, q0;",
            "; | h q1; h q1;",
            "; | h q2; h q2;",
            "; | h q0; h q0;",
            "; | cx q2, q0; cx q2, q0;",
            "; | cx q2, q1; cx q2, q1;",
            "; | cx q0, q1; cx q0, q1;",
            "; | x q1; x q1;",
            "; | x q2; x q2;",
            "; | x q0; x q0;",
            "cx q0, q1; h q0; h q1; | h q0; h q1; cx q1, q0;",
            "h q0; cx q0, q1; h q0; | h q1; cx q1, q0; h q1;"
    ));
    String ROTATION_MERGING = "rz(theta1 PLUS theta2) q0; symb q; | rz(theta1) q0; symb q; rz(theta2) q1; | [{[false, false]=[true, false], [true, false]=[false, true], [false, true]=[false, false], [true, true]=[true, true]}, {[false, false]=[false, false], [true, false]=[false, true], [false, true]=[true, false], [true, true]=[true, true]}, {[false, false]=[true, false], [true, false]=[true, true], [false, true]=[false, false], [true, true]=[false, true]}, {[false, false]=[false, false], [true, false]=[true, true], [false, true]=[true, false], [true, true]=[false, true]}]";

    private static EnumeratorPrune enumerator;

    @BeforeAll
    public static void setupAll() throws FileNotFoundException, UnsupportedEncodingException {
        String[] gates = {"x", "h", "rz", "cx"};
        int maxQubits = 3;
        Random rand = new Random();
        long seed = rand.nextLong();
        rand.setSeed(seed);
        System.out.println(String.format("Seed: %s", seed));

        enumerator = new EnumeratorPrune(gates, maxQubits, rand);
        enumerator.enumerate(3, 5);
        enumerator.pruneECS();
        enumerator.gatherRules(FILENAME);
    }

    @AfterAll
    public static void cleanupAll() {
//        File test = new File(PATH_NONSYMB);
//        File test_symb = new File(PATH_SYMB);
//        test.delete();
//        test_symb.delete();
    }

    @Test
    public void testNonSymb() throws FileNotFoundException {
        Scanner scn = new Scanner(new File(PATH_NONSYMB));
        int count = 0;

        while(scn.hasNextLine()) {
            if (EXPECTED_NONSYMB_RULES.contains(scn.nextLine())) {
                count++;
            }
            if (count == EXPECTED_NONSYMB_RULES.size()) {
                break;
            }
        }

        assertEquals(EXPECTED_NONSYMB_RULES.size(), count);
    }

    @Test
    public void testRotationMerging() throws FileNotFoundException {
        Scanner scn = new Scanner(new File(PATH_SYMB));
        boolean pass = false;
        while(scn.hasNextLine()) {
            if (scn.nextLine().equals(ROTATION_MERGING)) {
                pass = true;
                break;
            }
        }

        assertTrue(pass);
    }
}
