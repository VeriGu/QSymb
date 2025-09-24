// Generated from Egg.g4 by ANTLR 4.12.0
import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link EggParser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface EggVisitor<T> extends ParseTreeVisitor<T> {
	/**
	 * Visit a parse tree produced by {@link EggParser#parse}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParse(EggParser.ParseContext ctx);
	/**
	 * Visit a parse tree produced by {@link EggParser#constrainedCircuit}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConstrainedCircuit(EggParser.ConstrainedCircuitContext ctx);
	/**
	 * Visit a parse tree produced by {@link EggParser#circuit}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCircuit(EggParser.CircuitContext ctx);
	/**
	 * Visit a parse tree produced by {@link EggParser#permutation}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPermutation(EggParser.PermutationContext ctx);
	/**
	 * Visit a parse tree produced by {@link EggParser#gate}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGate(EggParser.GateContext ctx);
	/**
	 * Visit a parse tree produced by {@link EggParser#qubit}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitQubit(EggParser.QubitContext ctx);
	/**
	 * Visit a parse tree produced by {@link EggParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpr(EggParser.ExprContext ctx);
	/**
	 * Visit a parse tree produced by {@link EggParser#op}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOp(EggParser.OpContext ctx);
}