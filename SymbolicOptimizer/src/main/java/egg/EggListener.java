// Generated from Egg.g4 by ANTLR 4.12.0
import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link EggParser}.
 */
public interface EggListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link EggParser#parse}.
	 * @param ctx the parse tree
	 */
	void enterParse(EggParser.ParseContext ctx);
	/**
	 * Exit a parse tree produced by {@link EggParser#parse}.
	 * @param ctx the parse tree
	 */
	void exitParse(EggParser.ParseContext ctx);
	/**
	 * Enter a parse tree produced by {@link EggParser#constrainedCircuit}.
	 * @param ctx the parse tree
	 */
	void enterConstrainedCircuit(EggParser.ConstrainedCircuitContext ctx);
	/**
	 * Exit a parse tree produced by {@link EggParser#constrainedCircuit}.
	 * @param ctx the parse tree
	 */
	void exitConstrainedCircuit(EggParser.ConstrainedCircuitContext ctx);
	/**
	 * Enter a parse tree produced by {@link EggParser#circuit}.
	 * @param ctx the parse tree
	 */
	void enterCircuit(EggParser.CircuitContext ctx);
	/**
	 * Exit a parse tree produced by {@link EggParser#circuit}.
	 * @param ctx the parse tree
	 */
	void exitCircuit(EggParser.CircuitContext ctx);
	/**
	 * Enter a parse tree produced by {@link EggParser#permutation}.
	 * @param ctx the parse tree
	 */
	void enterPermutation(EggParser.PermutationContext ctx);
	/**
	 * Exit a parse tree produced by {@link EggParser#permutation}.
	 * @param ctx the parse tree
	 */
	void exitPermutation(EggParser.PermutationContext ctx);
	/**
	 * Enter a parse tree produced by {@link EggParser#gate}.
	 * @param ctx the parse tree
	 */
	void enterGate(EggParser.GateContext ctx);
	/**
	 * Exit a parse tree produced by {@link EggParser#gate}.
	 * @param ctx the parse tree
	 */
	void exitGate(EggParser.GateContext ctx);
	/**
	 * Enter a parse tree produced by {@link EggParser#qubit}.
	 * @param ctx the parse tree
	 */
	void enterQubit(EggParser.QubitContext ctx);
	/**
	 * Exit a parse tree produced by {@link EggParser#qubit}.
	 * @param ctx the parse tree
	 */
	void exitQubit(EggParser.QubitContext ctx);
	/**
	 * Enter a parse tree produced by {@link EggParser#expr}.
	 * @param ctx the parse tree
	 */
	void enterExpr(EggParser.ExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link EggParser#expr}.
	 * @param ctx the parse tree
	 */
	void exitExpr(EggParser.ExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link EggParser#op}.
	 * @param ctx the parse tree
	 */
	void enterOp(EggParser.OpContext ctx);
	/**
	 * Exit a parse tree produced by {@link EggParser#op}.
	 * @param ctx the parse tree
	 */
	void exitOp(EggParser.OpContext ctx);
}