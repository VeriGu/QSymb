// Generated from QASM.g4 by ANTLR 4.12.0
import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link QASMParser}.
 */
public interface QASMListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link QASMParser#program}.
	 * @param ctx the parse tree
	 */
	void enterProgram(QASMParser.ProgramContext ctx);
	/**
	 * Exit a parse tree produced by {@link QASMParser#program}.
	 * @param ctx the parse tree
	 */
	void exitProgram(QASMParser.ProgramContext ctx);
	/**
	 * Enter a parse tree produced by {@link QASMParser#header}.
	 * @param ctx the parse tree
	 */
	void enterHeader(QASMParser.HeaderContext ctx);
	/**
	 * Exit a parse tree produced by {@link QASMParser#header}.
	 * @param ctx the parse tree
	 */
	void exitHeader(QASMParser.HeaderContext ctx);
	/**
	 * Enter a parse tree produced by {@link QASMParser#declaration}.
	 * @param ctx the parse tree
	 */
	void enterDeclaration(QASMParser.DeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link QASMParser#declaration}.
	 * @param ctx the parse tree
	 */
	void exitDeclaration(QASMParser.DeclarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link QASMParser#qreg_decl}.
	 * @param ctx the parse tree
	 */
	void enterQreg_decl(QASMParser.Qreg_declContext ctx);
	/**
	 * Exit a parse tree produced by {@link QASMParser#qreg_decl}.
	 * @param ctx the parse tree
	 */
	void exitQreg_decl(QASMParser.Qreg_declContext ctx);
	/**
	 * Enter a parse tree produced by {@link QASMParser#creg_decl}.
	 * @param ctx the parse tree
	 */
	void enterCreg_decl(QASMParser.Creg_declContext ctx);
	/**
	 * Exit a parse tree produced by {@link QASMParser#creg_decl}.
	 * @param ctx the parse tree
	 */
	void exitCreg_decl(QASMParser.Creg_declContext ctx);
	/**
	 * Enter a parse tree produced by {@link QASMParser#rewrites}.
	 * @param ctx the parse tree
	 */
	void enterRewrites(QASMParser.RewritesContext ctx);
	/**
	 * Exit a parse tree produced by {@link QASMParser#rewrites}.
	 * @param ctx the parse tree
	 */
	void exitRewrites(QASMParser.RewritesContext ctx);
	/**
	 * Enter a parse tree produced by {@link QASMParser#rewrite_body}.
	 * @param ctx the parse tree
	 */
	void enterRewrite_body(QASMParser.Rewrite_bodyContext ctx);
	/**
	 * Exit a parse tree produced by {@link QASMParser#rewrite_body}.
	 * @param ctx the parse tree
	 */
	void exitRewrite_body(QASMParser.Rewrite_bodyContext ctx);
	/**
	 * Enter a parse tree produced by {@link QASMParser#rewrite}.
	 * @param ctx the parse tree
	 */
	void enterRewrite(QASMParser.RewriteContext ctx);
	/**
	 * Exit a parse tree produced by {@link QASMParser#rewrite}.
	 * @param ctx the parse tree
	 */
	void exitRewrite(QASMParser.RewriteContext ctx);
	/**
	 * Enter a parse tree produced by {@link QASMParser#equality}.
	 * @param ctx the parse tree
	 */
	void enterEquality(QASMParser.EqualityContext ctx);
	/**
	 * Exit a parse tree produced by {@link QASMParser#equality}.
	 * @param ctx the parse tree
	 */
	void exitEquality(QASMParser.EqualityContext ctx);
	/**
	 * Enter a parse tree produced by {@link QASMParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterStatement(QASMParser.StatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link QASMParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitStatement(QASMParser.StatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link QASMParser#gate_statement}.
	 * @param ctx the parse tree
	 */
	void enterGate_statement(QASMParser.Gate_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link QASMParser#gate_statement}.
	 * @param ctx the parse tree
	 */
	void exitGate_statement(QASMParser.Gate_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link QASMParser#qubits}.
	 * @param ctx the parse tree
	 */
	void enterQubits(QASMParser.QubitsContext ctx);
	/**
	 * Exit a parse tree produced by {@link QASMParser#qubits}.
	 * @param ctx the parse tree
	 */
	void exitQubits(QASMParser.QubitsContext ctx);
	/**
	 * Enter a parse tree produced by {@link QASMParser#qubit}.
	 * @param ctx the parse tree
	 */
	void enterQubit(QASMParser.QubitContext ctx);
	/**
	 * Exit a parse tree produced by {@link QASMParser#qubit}.
	 * @param ctx the parse tree
	 */
	void exitQubit(QASMParser.QubitContext ctx);
	/**
	 * Enter a parse tree produced by {@link QASMParser#measure_statement}.
	 * @param ctx the parse tree
	 */
	void enterMeasure_statement(QASMParser.Measure_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link QASMParser#measure_statement}.
	 * @param ctx the parse tree
	 */
	void exitMeasure_statement(QASMParser.Measure_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link QASMParser#reset_statement}.
	 * @param ctx the parse tree
	 */
	void enterReset_statement(QASMParser.Reset_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link QASMParser#reset_statement}.
	 * @param ctx the parse tree
	 */
	void exitReset_statement(QASMParser.Reset_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link QASMParser#barrier_statement}.
	 * @param ctx the parse tree
	 */
	void enterBarrier_statement(QASMParser.Barrier_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link QASMParser#barrier_statement}.
	 * @param ctx the parse tree
	 */
	void exitBarrier_statement(QASMParser.Barrier_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link QASMParser#expression}.
	 * @param ctx the parse tree
	 */
	void enterExpression(QASMParser.ExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link QASMParser#expression}.
	 * @param ctx the parse tree
	 */
	void exitExpression(QASMParser.ExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link QASMParser#multExpr}.
	 * @param ctx the parse tree
	 */
	void enterMultExpr(QASMParser.MultExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link QASMParser#multExpr}.
	 * @param ctx the parse tree
	 */
	void exitMultExpr(QASMParser.MultExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link QASMParser#atomExpr}.
	 * @param ctx the parse tree
	 */
	void enterAtomExpr(QASMParser.AtomExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link QASMParser#atomExpr}.
	 * @param ctx the parse tree
	 */
	void exitAtomExpr(QASMParser.AtomExprContext ctx);
}