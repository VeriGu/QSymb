// Generated from QASM.g4 by ANTLR 4.12.0
import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link QASMParser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface QASMVisitor<T> extends ParseTreeVisitor<T> {
	/**
	 * Visit a parse tree produced by {@link QASMParser#program}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitProgram(QASMParser.ProgramContext ctx);
	/**
	 * Visit a parse tree produced by {@link QASMParser#header}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitHeader(QASMParser.HeaderContext ctx);
	/**
	 * Visit a parse tree produced by {@link QASMParser#declaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDeclaration(QASMParser.DeclarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link QASMParser#qreg_decl}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitQreg_decl(QASMParser.Qreg_declContext ctx);
	/**
	 * Visit a parse tree produced by {@link QASMParser#creg_decl}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreg_decl(QASMParser.Creg_declContext ctx);
	/**
	 * Visit a parse tree produced by {@link QASMParser#rewrites}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRewrites(QASMParser.RewritesContext ctx);
	/**
	 * Visit a parse tree produced by {@link QASMParser#rewrite_body}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRewrite_body(QASMParser.Rewrite_bodyContext ctx);
	/**
	 * Visit a parse tree produced by {@link QASMParser#rewrite}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRewrite(QASMParser.RewriteContext ctx);
	/**
	 * Visit a parse tree produced by {@link QASMParser#equality}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEquality(QASMParser.EqualityContext ctx);
	/**
	 * Visit a parse tree produced by {@link QASMParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStatement(QASMParser.StatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link QASMParser#gate_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGate_statement(QASMParser.Gate_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link QASMParser#qubits}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitQubits(QASMParser.QubitsContext ctx);
	/**
	 * Visit a parse tree produced by {@link QASMParser#qubit}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitQubit(QASMParser.QubitContext ctx);
	/**
	 * Visit a parse tree produced by {@link QASMParser#measure_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMeasure_statement(QASMParser.Measure_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link QASMParser#reset_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReset_statement(QASMParser.Reset_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link QASMParser#barrier_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBarrier_statement(QASMParser.Barrier_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link QASMParser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpression(QASMParser.ExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link QASMParser#multExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMultExpr(QASMParser.MultExprContext ctx);
	/**
	 * Visit a parse tree produced by {@link QASMParser#atomExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAtomExpr(QASMParser.AtomExprContext ctx);
}