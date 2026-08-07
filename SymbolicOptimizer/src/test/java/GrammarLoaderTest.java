import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import ast.BinOp;
import ast.Expr;
import ast.Expr.Op;
import ast.Real;
import ast.Symbol;
import ast.UnOp;

import org.junit.jupiter.api.Test;

/**
 * Verifies that each shipped grammar file parses to the exact same
 * (gates, symbAngles) tuple as the hardcoded switch in EnumeratorPrune.main.
 * Also spot-checks the expression parser on unary minus, parens, and precedence.
 */
public class GrammarLoaderTest {

    // -------------------------------------------------------------------
    //  Reference expected values -- must match EnumeratorPrune.main switch
    // -------------------------------------------------------------------

    private static Expr piOverTwo() {
        return new BinOp(Op.DIV, new Symbol("pi"), new Real(2));
    }
    private static Expr negPiOverTwo() {
        return new UnOp(Op.MINUS, piOverTwo());
    }
    private static Expr plus(Expr a, Expr b) { return new BinOp(Op.PLUS, a, b); }

    // -------------------------------------------------------------------
    //  Per-gateset expectations
    // -------------------------------------------------------------------

    @Test void namGrammar() throws IOException {
        EnumeratorPrune.Grammar gr = EnumeratorPrune.loadGrammar("SymbolicOptimizer/grammars/nam.grammar");
        assertArrayEquals(new String[] {"x", "h", "rz", "cx"}, gr.gates);
        assertEqualExpr(new Expr[] { new Symbol("theta1"), new Symbol("theta2") }, gr.symbAngles);
    }

    @Test void ibmGrammar() throws IOException {
        EnumeratorPrune.Grammar gr = EnumeratorPrune.loadGrammar("SymbolicOptimizer/grammars/ibm.grammar");
        assertArrayEquals(new String[] {"u1", "u2", "u3", "cx"}, gr.gates);
        assertEqualExpr(new Expr[] {
                new Symbol("theta1"),
                new Symbol("theta2"),
                new Symbol("theta3"),
                plus(new Symbol("theta1"), new Symbol("theta2")),
                plus(new Symbol("theta1"), plus(new Symbol("theta2"), new Symbol("theta3")))
        }, gr.symbAngles);
    }

    @Test void rigettiGrammar() throws IOException {
        EnumeratorPrune.Grammar gr = EnumeratorPrune.loadGrammar("SymbolicOptimizer/grammars/rigetti.grammar");
        assertArrayEquals(new String[] {"rx1", "rx2", "rx3", "rz", "cz"}, gr.gates);
        assertEqualExpr(new Expr[] { new Symbol("theta1") }, gr.symbAngles);
    }

    @Test void ionGrammar() throws IOException {
        EnumeratorPrune.Grammar gr = EnumeratorPrune.loadGrammar("SymbolicOptimizer/grammars/ion.grammar");
        assertArrayEquals(new String[] {"rx", "ry", "rz", "rxx"}, gr.gates);
        assertEqualExpr(new Expr[] {
                new Symbol("theta1"),
                new Symbol("pi"),
                piOverTwo(),
                negPiOverTwo()
        }, gr.symbAngles);
    }

    @Test void ibmnewGrammar() throws IOException {
        EnumeratorPrune.Grammar gr = EnumeratorPrune.loadGrammar("SymbolicOptimizer/grammars/ibmnew.grammar");
        assertArrayEquals(new String[] {"cx", "rz", "x", "sx"}, gr.gates);
        assertEqualExpr(new Expr[] {
                new Symbol("theta1"),
                piOverTwo(),
                negPiOverTwo()
        }, gr.symbAngles);
    }

    // -------------------------------------------------------------------
    //  Parser edge cases
    // -------------------------------------------------------------------

    @Test void parsesPrecedenceAndParens() {
        // a + b*c   ->  a + (b*c)
        Expr e = EnumeratorPrune.parseAngleExpr("a + b*c", "<test>");
        assertEqualExpr(plus(new Symbol("a"),
                new BinOp(Op.MULT, new Symbol("b"), new Symbol("c"))), e);
        // (a+b)*c   parens force left grouping
        Expr f = EnumeratorPrune.parseAngleExpr("(a+b)*c", "<test>");
        assertEqualExpr(new BinOp(Op.MULT,
                plus(new Symbol("a"), new Symbol("b")), new Symbol("c")), f);
    }

    @Test void parsesUnaryMinus() {
        Expr e = EnumeratorPrune.parseAngleExpr("-theta1", "<test>");
        assertEqualExpr(new UnOp(Op.MINUS, new Symbol("theta1")), e);
    }

    @Test void parsesNumericLiteral() {
        Expr e = EnumeratorPrune.parseAngleExpr("2.5", "<test>");
        assertEqualExpr(new Real(2.5), e);
    }

    @Test void trailingInputIsAnError() {
        assertThrows(RuntimeException.class,
                () -> EnumeratorPrune.parseAngleExpr("theta1 garbage", "<test>"));
    }

    @Test void unknownSectionIsAnError() throws IOException {
        Path p = Files.createTempFile("grammar", ".grammar");
        Files.writeString(p, "[bogus]\nfoo\n");
        assertThrows(IOException.class, () -> EnumeratorPrune.loadGrammar(p.toString()));
        Files.deleteIfExists(p);
    }

    @Test void emptyGatesIsAnError() throws IOException {
        Path p = Files.createTempFile("grammar", ".grammar");
        Files.writeString(p, "[symbAngles]\ntheta1\n");
        assertThrows(IOException.class, () -> EnumeratorPrune.loadGrammar(p.toString()));
        Files.deleteIfExists(p);
    }

    // -------------------------------------------------------------------
    //  Helpers
    // -------------------------------------------------------------------

    /** Structural equality using EnumeratorPrune's own canonical key. */
    private static void assertEqualExpr(Expr expected, Expr actual) {
        assertNotNull(actual);
        assertEquals(canonKey(expected), canonKey(actual));
    }
    private static void assertEqualExpr(Expr[] expected, Expr[] actual) {
        assertEquals(expected.length, actual.length,
                "arity mismatch: expected " + expected.length + " but got " + actual.length);
        for (int i = 0; i < expected.length; i++) {
            assertEquals(canonKey(expected[i]), canonKey(actual[i]),
                    "mismatch at index " + i);
        }
    }
    /** Mirrors EnumeratorPrune.canonicalKey (which is package-private). */
    private static String canonKey(Expr e) {
        if (e instanceof Symbol) return "S:" + ((Symbol) e).getSymbol();
        if (e instanceof Real)   return "R:" + ((Real) e).getNumber();
        if (e instanceof UnOp)   return "U(" + ((UnOp) e).getOp() + "," + canonKey(((UnOp) e).getE()) + ")";
        if (e instanceof BinOp)  return "B(" + ((BinOp) e).getOp() + "," + canonKey(((BinOp) e).getE1())
                                            + "," + canonKey(((BinOp) e).getE2()) + ")";
        return e.toString();
    }
}
