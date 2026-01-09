// Generated from QASM.g4 by ANTLR 4.12.0
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.*;
import org.antlr.v4.runtime.tree.*;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast", "CheckReturnValue"})
public class QASMParser extends Parser {
	static { RuntimeMetaData.checkVersion("4.12.0", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		T__0=1, T__1=2, T__2=3, T__3=4, T__4=5, T__5=6, T__6=7, T__7=8, T__8=9, 
		T__9=10, OPENQASM_KW=11, INCLUDE_KW=12, QREG_KW=13, CREG_KW=14, MEASURE_KW=15, 
		TO=16, RESET_KW=17, BARRIER_KW=18, PI_KW=19, QUBIT=20, ID=21, REAL_LITERAL=22, 
		INT_LITERAL=23, STRING_LITERAL=24, WORD=25, PLUS=26, MINUS=27, MULT=28, 
		DIV=29, WHITESPACE=30;
	public static final int
		RULE_program = 0, RULE_header = 1, RULE_declaration = 2, RULE_qreg_decl = 3, 
		RULE_creg_decl = 4, RULE_rewrites = 5, RULE_rewrite_body = 6, RULE_rewrite = 7, 
		RULE_equality = 8, RULE_statement = 9, RULE_gate_statement = 10, RULE_qubits = 11, 
		RULE_qubit = 12, RULE_measure_statement = 13, RULE_reset_statement = 14, 
		RULE_barrier_statement = 15, RULE_expression = 16, RULE_multExpr = 17, 
		RULE_atomExpr = 18;
	private static String[] makeRuleNames() {
		return new String[] {
			"program", "header", "declaration", "qreg_decl", "creg_decl", "rewrites", 
			"rewrite_body", "rewrite", "equality", "statement", "gate_statement", 
			"qubits", "qubit", "measure_statement", "reset_statement", "barrier_statement", 
			"expression", "multExpr", "atomExpr"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, "';'", "'['", "']'", "'|'", "'when'", "','", "'='", "'!='", "'('", 
			"')'", "'OPENQASM'", "'include'", "'qreg'", "'creg'", "'measure'", "'->'", 
			"'reset'", "'barrier'", "'pi'", null, null, null, null, null, null, "'+'", 
			"'-'", "'*'", "'/'"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, null, null, null, null, null, null, null, null, null, null, "OPENQASM_KW", 
			"INCLUDE_KW", "QREG_KW", "CREG_KW", "MEASURE_KW", "TO", "RESET_KW", "BARRIER_KW", 
			"PI_KW", "QUBIT", "ID", "REAL_LITERAL", "INT_LITERAL", "STRING_LITERAL", 
			"WORD", "PLUS", "MINUS", "MULT", "DIV", "WHITESPACE"
		};
	}
	private static final String[] _SYMBOLIC_NAMES = makeSymbolicNames();
	public static final Vocabulary VOCABULARY = new VocabularyImpl(_LITERAL_NAMES, _SYMBOLIC_NAMES);

	/**
	 * @deprecated Use {@link #VOCABULARY} instead.
	 */
	@Deprecated
	public static final String[] tokenNames;
	static {
		tokenNames = new String[_SYMBOLIC_NAMES.length];
		for (int i = 0; i < tokenNames.length; i++) {
			tokenNames[i] = VOCABULARY.getLiteralName(i);
			if (tokenNames[i] == null) {
				tokenNames[i] = VOCABULARY.getSymbolicName(i);
			}

			if (tokenNames[i] == null) {
				tokenNames[i] = "<INVALID>";
			}
		}
	}

	@Override
	@Deprecated
	public String[] getTokenNames() {
		return tokenNames;
	}

	@Override

	public Vocabulary getVocabulary() {
		return VOCABULARY;
	}

	@Override
	public String getGrammarFileName() { return "QASM.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public ATN getATN() { return _ATN; }

	public QASMParser(TokenStream input) {
		super(input);
		_interp = new ParserATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ProgramContext extends ParserRuleContext {
		public TerminalNode EOF() { return getToken(QASMParser.EOF, 0); }
		public HeaderContext header() {
			return getRuleContext(HeaderContext.class,0);
		}
		public List<DeclarationContext> declaration() {
			return getRuleContexts(DeclarationContext.class);
		}
		public DeclarationContext declaration(int i) {
			return getRuleContext(DeclarationContext.class,i);
		}
		public List<StatementContext> statement() {
			return getRuleContexts(StatementContext.class);
		}
		public StatementContext statement(int i) {
			return getRuleContext(StatementContext.class,i);
		}
		public ProgramContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_program; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof QASMListener ) ((QASMListener)listener).enterProgram(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof QASMListener ) ((QASMListener)listener).exitProgram(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof QASMVisitor ) return ((QASMVisitor<? extends T>)visitor).visitProgram(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ProgramContext program() throws RecognitionException {
		ProgramContext _localctx = new ProgramContext(_ctx, getState());
		enterRule(_localctx, 0, RULE_program);
		int _la;
		try {
			setState(56);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case EOF:
			case OPENQASM_KW:
			case QREG_KW:
			case CREG_KW:
			case ID:
				enterOuterAlt(_localctx, 1);
				{
				setState(39);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==OPENQASM_KW) {
					{
					setState(38);
					header();
					}
				}

				setState(44);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==QREG_KW || _la==CREG_KW) {
					{
					{
					setState(41);
					declaration();
					}
					}
					setState(46);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(50);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==ID) {
					{
					{
					setState(47);
					statement();
					}
					}
					setState(52);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(53);
				match(EOF);
				}
				break;
			case T__0:
				enterOuterAlt(_localctx, 2);
				{
				setState(54);
				match(T__0);
				setState(55);
				match(EOF);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class HeaderContext extends ParserRuleContext {
		public TerminalNode OPENQASM_KW() { return getToken(QASMParser.OPENQASM_KW, 0); }
		public TerminalNode REAL_LITERAL() { return getToken(QASMParser.REAL_LITERAL, 0); }
		public TerminalNode INCLUDE_KW() { return getToken(QASMParser.INCLUDE_KW, 0); }
		public TerminalNode STRING_LITERAL() { return getToken(QASMParser.STRING_LITERAL, 0); }
		public HeaderContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_header; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof QASMListener ) ((QASMListener)listener).enterHeader(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof QASMListener ) ((QASMListener)listener).exitHeader(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof QASMVisitor ) return ((QASMVisitor<? extends T>)visitor).visitHeader(this);
			else return visitor.visitChildren(this);
		}
	}

	public final HeaderContext header() throws RecognitionException {
		HeaderContext _localctx = new HeaderContext(_ctx, getState());
		enterRule(_localctx, 2, RULE_header);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(58);
			match(OPENQASM_KW);
			setState(59);
			match(REAL_LITERAL);
			setState(60);
			match(T__0);
			setState(61);
			match(INCLUDE_KW);
			setState(62);
			match(STRING_LITERAL);
			setState(63);
			match(T__0);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class DeclarationContext extends ParserRuleContext {
		public Qreg_declContext qreg_decl() {
			return getRuleContext(Qreg_declContext.class,0);
		}
		public Creg_declContext creg_decl() {
			return getRuleContext(Creg_declContext.class,0);
		}
		public DeclarationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_declaration; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof QASMListener ) ((QASMListener)listener).enterDeclaration(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof QASMListener ) ((QASMListener)listener).exitDeclaration(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof QASMVisitor ) return ((QASMVisitor<? extends T>)visitor).visitDeclaration(this);
			else return visitor.visitChildren(this);
		}
	}

	public final DeclarationContext declaration() throws RecognitionException {
		DeclarationContext _localctx = new DeclarationContext(_ctx, getState());
		enterRule(_localctx, 4, RULE_declaration);
		try {
			setState(67);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case QREG_KW:
				enterOuterAlt(_localctx, 1);
				{
				setState(65);
				qreg_decl();
				}
				break;
			case CREG_KW:
				enterOuterAlt(_localctx, 2);
				{
				setState(66);
				creg_decl();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Qreg_declContext extends ParserRuleContext {
		public TerminalNode QREG_KW() { return getToken(QASMParser.QREG_KW, 0); }
		public TerminalNode ID() { return getToken(QASMParser.ID, 0); }
		public TerminalNode INT_LITERAL() { return getToken(QASMParser.INT_LITERAL, 0); }
		public Qreg_declContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_qreg_decl; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof QASMListener ) ((QASMListener)listener).enterQreg_decl(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof QASMListener ) ((QASMListener)listener).exitQreg_decl(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof QASMVisitor ) return ((QASMVisitor<? extends T>)visitor).visitQreg_decl(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Qreg_declContext qreg_decl() throws RecognitionException {
		Qreg_declContext _localctx = new Qreg_declContext(_ctx, getState());
		enterRule(_localctx, 6, RULE_qreg_decl);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(69);
			match(QREG_KW);
			setState(70);
			match(ID);
			setState(71);
			match(T__1);
			setState(72);
			match(INT_LITERAL);
			setState(73);
			match(T__2);
			setState(74);
			match(T__0);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Creg_declContext extends ParserRuleContext {
		public TerminalNode CREG_KW() { return getToken(QASMParser.CREG_KW, 0); }
		public TerminalNode ID() { return getToken(QASMParser.ID, 0); }
		public TerminalNode INT_LITERAL() { return getToken(QASMParser.INT_LITERAL, 0); }
		public Creg_declContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_creg_decl; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof QASMListener ) ((QASMListener)listener).enterCreg_decl(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof QASMListener ) ((QASMListener)listener).exitCreg_decl(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof QASMVisitor ) return ((QASMVisitor<? extends T>)visitor).visitCreg_decl(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Creg_declContext creg_decl() throws RecognitionException {
		Creg_declContext _localctx = new Creg_declContext(_ctx, getState());
		enterRule(_localctx, 8, RULE_creg_decl);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(76);
			match(CREG_KW);
			setState(77);
			match(ID);
			setState(78);
			match(T__1);
			setState(79);
			match(INT_LITERAL);
			setState(80);
			match(T__2);
			setState(81);
			match(T__0);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class RewritesContext extends ParserRuleContext {
		public List<RewriteContext> rewrite() {
			return getRuleContexts(RewriteContext.class);
		}
		public RewriteContext rewrite(int i) {
			return getRuleContext(RewriteContext.class,i);
		}
		public RewritesContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_rewrites; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof QASMListener ) ((QASMListener)listener).enterRewrites(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof QASMListener ) ((QASMListener)listener).exitRewrites(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof QASMVisitor ) return ((QASMVisitor<? extends T>)visitor).visitRewrites(this);
			else return visitor.visitChildren(this);
		}
	}

	public final RewritesContext rewrites() throws RecognitionException {
		RewritesContext _localctx = new RewritesContext(_ctx, getState());
		enterRule(_localctx, 10, RULE_rewrites);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(86);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==T__0 || _la==ID) {
				{
				{
				setState(83);
				rewrite();
				}
				}
				setState(88);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Rewrite_bodyContext extends ParserRuleContext {
		public List<StatementContext> statement() {
			return getRuleContexts(StatementContext.class);
		}
		public StatementContext statement(int i) {
			return getRuleContext(StatementContext.class,i);
		}
		public Rewrite_bodyContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_rewrite_body; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof QASMListener ) ((QASMListener)listener).enterRewrite_body(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof QASMListener ) ((QASMListener)listener).exitRewrite_body(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof QASMVisitor ) return ((QASMVisitor<? extends T>)visitor).visitRewrite_body(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Rewrite_bodyContext rewrite_body() throws RecognitionException {
		Rewrite_bodyContext _localctx = new Rewrite_bodyContext(_ctx, getState());
		enterRule(_localctx, 12, RULE_rewrite_body);
		try {
			int _alt;
			setState(95);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case ID:
				enterOuterAlt(_localctx, 1);
				{
				{
				setState(90); 
				_errHandler.sync(this);
				_alt = 1;
				do {
					switch (_alt) {
					case 1:
						{
						{
						setState(89);
						statement();
						}
						}
						break;
					default:
						throw new NoViableAltException(this);
					}
					setState(92); 
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,6,_ctx);
				} while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER );
				}
				}
				break;
			case T__0:
				enterOuterAlt(_localctx, 2);
				{
				setState(94);
				match(T__0);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class RewriteContext extends ParserRuleContext {
		public Rewrite_bodyContext lhs;
		public Rewrite_bodyContext rhs;
		public List<Rewrite_bodyContext> rewrite_body() {
			return getRuleContexts(Rewrite_bodyContext.class);
		}
		public Rewrite_bodyContext rewrite_body(int i) {
			return getRuleContext(Rewrite_bodyContext.class,i);
		}
		public List<EqualityContext> equality() {
			return getRuleContexts(EqualityContext.class);
		}
		public EqualityContext equality(int i) {
			return getRuleContext(EqualityContext.class,i);
		}
		public RewriteContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_rewrite; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof QASMListener ) ((QASMListener)listener).enterRewrite(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof QASMListener ) ((QASMListener)listener).exitRewrite(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof QASMVisitor ) return ((QASMVisitor<? extends T>)visitor).visitRewrite(this);
			else return visitor.visitChildren(this);
		}
	}

	public final RewriteContext rewrite() throws RecognitionException {
		RewriteContext _localctx = new RewriteContext(_ctx, getState());
		enterRule(_localctx, 14, RULE_rewrite);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			{
			setState(97);
			((RewriteContext)_localctx).lhs = rewrite_body();
			}
			setState(98);
			match(T__3);
			{
			setState(99);
			((RewriteContext)_localctx).rhs = rewrite_body();
			}
			setState(109);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==T__4) {
				{
				setState(100);
				match(T__4);
				setState(101);
				equality();
				setState(106);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==T__5) {
					{
					{
					setState(102);
					match(T__5);
					{
					setState(103);
					equality();
					}
					}
					}
					setState(108);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class EqualityContext extends ParserRuleContext {
		public Token op;
		public List<TerminalNode> QUBIT() { return getTokens(QASMParser.QUBIT); }
		public TerminalNode QUBIT(int i) {
			return getToken(QASMParser.QUBIT, i);
		}
		public EqualityContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_equality; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof QASMListener ) ((QASMListener)listener).enterEquality(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof QASMListener ) ((QASMListener)listener).exitEquality(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof QASMVisitor ) return ((QASMVisitor<? extends T>)visitor).visitEquality(this);
			else return visitor.visitChildren(this);
		}
	}

	public final EqualityContext equality() throws RecognitionException {
		EqualityContext _localctx = new EqualityContext(_ctx, getState());
		enterRule(_localctx, 16, RULE_equality);
		try {
			setState(117);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,10,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				{
				setState(111);
				match(QUBIT);
				setState(112);
				((EqualityContext)_localctx).op = match(T__6);
				setState(113);
				match(QUBIT);
				}
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				{
				setState(114);
				match(QUBIT);
				setState(115);
				((EqualityContext)_localctx).op = match(T__7);
				setState(116);
				match(QUBIT);
				}
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class StatementContext extends ParserRuleContext {
		public Gate_statementContext gate_statement() {
			return getRuleContext(Gate_statementContext.class,0);
		}
		public StatementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_statement; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof QASMListener ) ((QASMListener)listener).enterStatement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof QASMListener ) ((QASMListener)listener).exitStatement(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof QASMVisitor ) return ((QASMVisitor<? extends T>)visitor).visitStatement(this);
			else return visitor.visitChildren(this);
		}
	}

	public final StatementContext statement() throws RecognitionException {
		StatementContext _localctx = new StatementContext(_ctx, getState());
		enterRule(_localctx, 18, RULE_statement);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(119);
			gate_statement();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Gate_statementContext extends ParserRuleContext {
		public TerminalNode ID() { return getToken(QASMParser.ID, 0); }
		public QubitsContext qubits() {
			return getRuleContext(QubitsContext.class,0);
		}
		public List<ExpressionContext> expression() {
			return getRuleContexts(ExpressionContext.class);
		}
		public ExpressionContext expression(int i) {
			return getRuleContext(ExpressionContext.class,i);
		}
		public Gate_statementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_gate_statement; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof QASMListener ) ((QASMListener)listener).enterGate_statement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof QASMListener ) ((QASMListener)listener).exitGate_statement(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof QASMVisitor ) return ((QASMVisitor<? extends T>)visitor).visitGate_statement(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Gate_statementContext gate_statement() throws RecognitionException {
		Gate_statementContext _localctx = new Gate_statementContext(_ctx, getState());
		enterRule(_localctx, 20, RULE_gate_statement);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(121);
			match(ID);
			setState(133);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==T__8) {
				{
				setState(122);
				match(T__8);
				setState(123);
				expression();
				setState(128);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==T__5) {
					{
					{
					setState(124);
					match(T__5);
					setState(125);
					expression();
					}
					}
					setState(130);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(131);
				match(T__9);
				}
			}

			setState(135);
			qubits();
			setState(136);
			match(T__0);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class QubitsContext extends ParserRuleContext {
		public List<QubitContext> qubit() {
			return getRuleContexts(QubitContext.class);
		}
		public QubitContext qubit(int i) {
			return getRuleContext(QubitContext.class,i);
		}
		public QubitsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_qubits; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof QASMListener ) ((QASMListener)listener).enterQubits(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof QASMListener ) ((QASMListener)listener).exitQubits(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof QASMVisitor ) return ((QASMVisitor<? extends T>)visitor).visitQubits(this);
			else return visitor.visitChildren(this);
		}
	}

	public final QubitsContext qubits() throws RecognitionException {
		QubitsContext _localctx = new QubitsContext(_ctx, getState());
		enterRule(_localctx, 22, RULE_qubits);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(138);
			qubit();
			setState(143);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==T__5) {
				{
				{
				setState(139);
				match(T__5);
				setState(140);
				qubit();
				}
				}
				setState(145);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class QubitContext extends ParserRuleContext {
		public Token num;
		public TerminalNode ID() { return getToken(QASMParser.ID, 0); }
		public TerminalNode INT_LITERAL() { return getToken(QASMParser.INT_LITERAL, 0); }
		public TerminalNode QUBIT() { return getToken(QASMParser.QUBIT, 0); }
		public QubitContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_qubit; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof QASMListener ) ((QASMListener)listener).enterQubit(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof QASMListener ) ((QASMListener)listener).exitQubit(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof QASMVisitor ) return ((QASMVisitor<? extends T>)visitor).visitQubit(this);
			else return visitor.visitChildren(this);
		}
	}

	public final QubitContext qubit() throws RecognitionException {
		QubitContext _localctx = new QubitContext(_ctx, getState());
		enterRule(_localctx, 24, RULE_qubit);
		try {
			setState(151);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case ID:
				enterOuterAlt(_localctx, 1);
				{
				{
				setState(146);
				match(ID);
				setState(147);
				match(T__1);
				setState(148);
				((QubitContext)_localctx).num = match(INT_LITERAL);
				setState(149);
				match(T__2);
				}
				}
				break;
			case QUBIT:
				enterOuterAlt(_localctx, 2);
				{
				setState(150);
				match(QUBIT);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Measure_statementContext extends ParserRuleContext {
		public TerminalNode MEASURE_KW() { return getToken(QASMParser.MEASURE_KW, 0); }
		public TerminalNode TO() { return getToken(QASMParser.TO, 0); }
		public List<TerminalNode> ID() { return getTokens(QASMParser.ID); }
		public TerminalNode ID(int i) {
			return getToken(QASMParser.ID, i);
		}
		public List<TerminalNode> INT_LITERAL() { return getTokens(QASMParser.INT_LITERAL); }
		public TerminalNode INT_LITERAL(int i) {
			return getToken(QASMParser.INT_LITERAL, i);
		}
		public Measure_statementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_measure_statement; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof QASMListener ) ((QASMListener)listener).enterMeasure_statement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof QASMListener ) ((QASMListener)listener).exitMeasure_statement(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof QASMVisitor ) return ((QASMVisitor<? extends T>)visitor).visitMeasure_statement(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Measure_statementContext measure_statement() throws RecognitionException {
		Measure_statementContext _localctx = new Measure_statementContext(_ctx, getState());
		enterRule(_localctx, 26, RULE_measure_statement);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(153);
			match(MEASURE_KW);
			setState(159);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,15,_ctx) ) {
			case 1:
				{
				setState(154);
				match(ID);
				}
				break;
			case 2:
				{
				setState(155);
				match(ID);
				setState(156);
				match(T__1);
				setState(157);
				match(INT_LITERAL);
				setState(158);
				match(T__2);
				}
				break;
			}
			setState(161);
			match(TO);
			setState(167);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,16,_ctx) ) {
			case 1:
				{
				setState(162);
				match(ID);
				}
				break;
			case 2:
				{
				setState(163);
				match(ID);
				setState(164);
				match(T__1);
				setState(165);
				match(INT_LITERAL);
				setState(166);
				match(T__2);
				}
				break;
			}
			setState(169);
			match(T__0);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Reset_statementContext extends ParserRuleContext {
		public TerminalNode RESET_KW() { return getToken(QASMParser.RESET_KW, 0); }
		public TerminalNode ID() { return getToken(QASMParser.ID, 0); }
		public TerminalNode INT_LITERAL() { return getToken(QASMParser.INT_LITERAL, 0); }
		public Reset_statementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_reset_statement; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof QASMListener ) ((QASMListener)listener).enterReset_statement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof QASMListener ) ((QASMListener)listener).exitReset_statement(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof QASMVisitor ) return ((QASMVisitor<? extends T>)visitor).visitReset_statement(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Reset_statementContext reset_statement() throws RecognitionException {
		Reset_statementContext _localctx = new Reset_statementContext(_ctx, getState());
		enterRule(_localctx, 28, RULE_reset_statement);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(171);
			match(RESET_KW);
			setState(177);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,17,_ctx) ) {
			case 1:
				{
				setState(172);
				match(ID);
				}
				break;
			case 2:
				{
				setState(173);
				match(ID);
				setState(174);
				match(T__1);
				setState(175);
				match(INT_LITERAL);
				setState(176);
				match(T__2);
				}
				break;
			}
			setState(179);
			match(T__0);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Barrier_statementContext extends ParserRuleContext {
		public TerminalNode BARRIER_KW() { return getToken(QASMParser.BARRIER_KW, 0); }
		public List<TerminalNode> ID() { return getTokens(QASMParser.ID); }
		public TerminalNode ID(int i) {
			return getToken(QASMParser.ID, i);
		}
		public List<TerminalNode> INT_LITERAL() { return getTokens(QASMParser.INT_LITERAL); }
		public TerminalNode INT_LITERAL(int i) {
			return getToken(QASMParser.INT_LITERAL, i);
		}
		public Barrier_statementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_barrier_statement; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof QASMListener ) ((QASMListener)listener).enterBarrier_statement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof QASMListener ) ((QASMListener)listener).exitBarrier_statement(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof QASMVisitor ) return ((QASMVisitor<? extends T>)visitor).visitBarrier_statement(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Barrier_statementContext barrier_statement() throws RecognitionException {
		Barrier_statementContext _localctx = new Barrier_statementContext(_ctx, getState());
		enterRule(_localctx, 30, RULE_barrier_statement);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(181);
			match(BARRIER_KW);
			setState(187);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,18,_ctx) ) {
			case 1:
				{
				setState(182);
				match(ID);
				}
				break;
			case 2:
				{
				setState(183);
				match(ID);
				setState(184);
				match(T__1);
				setState(185);
				match(INT_LITERAL);
				setState(186);
				match(T__2);
				}
				break;
			}
			setState(199);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==T__5) {
				{
				{
				setState(189);
				match(T__5);
				setState(195);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,19,_ctx) ) {
				case 1:
					{
					setState(190);
					match(ID);
					}
					break;
				case 2:
					{
					setState(191);
					match(ID);
					setState(192);
					match(T__1);
					setState(193);
					match(INT_LITERAL);
					setState(194);
					match(T__2);
					}
					break;
				}
				}
				}
				setState(201);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(202);
			match(T__0);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ExpressionContext extends ParserRuleContext {
		public List<MultExprContext> multExpr() {
			return getRuleContexts(MultExprContext.class);
		}
		public MultExprContext multExpr(int i) {
			return getRuleContext(MultExprContext.class,i);
		}
		public List<TerminalNode> PLUS() { return getTokens(QASMParser.PLUS); }
		public TerminalNode PLUS(int i) {
			return getToken(QASMParser.PLUS, i);
		}
		public List<TerminalNode> MINUS() { return getTokens(QASMParser.MINUS); }
		public TerminalNode MINUS(int i) {
			return getToken(QASMParser.MINUS, i);
		}
		public ExpressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_expression; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof QASMListener ) ((QASMListener)listener).enterExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof QASMListener ) ((QASMListener)listener).exitExpression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof QASMVisitor ) return ((QASMVisitor<? extends T>)visitor).visitExpression(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ExpressionContext expression() throws RecognitionException {
		ExpressionContext _localctx = new ExpressionContext(_ctx, getState());
		enterRule(_localctx, 32, RULE_expression);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(204);
			multExpr();
			setState(209);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==PLUS || _la==MINUS) {
				{
				{
				setState(205);
				_la = _input.LA(1);
				if ( !(_la==PLUS || _la==MINUS) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(206);
				multExpr();
				}
				}
				setState(211);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class MultExprContext extends ParserRuleContext {
		public List<AtomExprContext> atomExpr() {
			return getRuleContexts(AtomExprContext.class);
		}
		public AtomExprContext atomExpr(int i) {
			return getRuleContext(AtomExprContext.class,i);
		}
		public List<TerminalNode> MULT() { return getTokens(QASMParser.MULT); }
		public TerminalNode MULT(int i) {
			return getToken(QASMParser.MULT, i);
		}
		public List<TerminalNode> DIV() { return getTokens(QASMParser.DIV); }
		public TerminalNode DIV(int i) {
			return getToken(QASMParser.DIV, i);
		}
		public MultExprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_multExpr; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof QASMListener ) ((QASMListener)listener).enterMultExpr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof QASMListener ) ((QASMListener)listener).exitMultExpr(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof QASMVisitor ) return ((QASMVisitor<? extends T>)visitor).visitMultExpr(this);
			else return visitor.visitChildren(this);
		}
	}

	public final MultExprContext multExpr() throws RecognitionException {
		MultExprContext _localctx = new MultExprContext(_ctx, getState());
		enterRule(_localctx, 34, RULE_multExpr);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(212);
			atomExpr();
			setState(217);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==MULT || _la==DIV) {
				{
				{
				setState(213);
				_la = _input.LA(1);
				if ( !(_la==MULT || _la==DIV) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(214);
				atomExpr();
				}
				}
				setState(219);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class AtomExprContext extends ParserRuleContext {
		public TerminalNode REAL_LITERAL() { return getToken(QASMParser.REAL_LITERAL, 0); }
		public TerminalNode INT_LITERAL() { return getToken(QASMParser.INT_LITERAL, 0); }
		public TerminalNode ID() { return getToken(QASMParser.ID, 0); }
		public TerminalNode PI_KW() { return getToken(QASMParser.PI_KW, 0); }
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public TerminalNode MINUS() { return getToken(QASMParser.MINUS, 0); }
		public AtomExprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_atomExpr; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof QASMListener ) ((QASMListener)listener).enterAtomExpr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof QASMListener ) ((QASMListener)listener).exitAtomExpr(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof QASMVisitor ) return ((QASMVisitor<? extends T>)visitor).visitAtomExpr(this);
			else return visitor.visitChildren(this);
		}
	}

	public final AtomExprContext atomExpr() throws RecognitionException {
		AtomExprContext _localctx = new AtomExprContext(_ctx, getState());
		enterRule(_localctx, 36, RULE_atomExpr);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(221);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==MINUS) {
				{
				setState(220);
				match(MINUS);
				}
			}

			setState(231);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case REAL_LITERAL:
				{
				setState(223);
				match(REAL_LITERAL);
				}
				break;
			case INT_LITERAL:
				{
				setState(224);
				match(INT_LITERAL);
				}
				break;
			case ID:
				{
				setState(225);
				match(ID);
				}
				break;
			case PI_KW:
				{
				setState(226);
				match(PI_KW);
				}
				break;
			case T__8:
				{
				setState(227);
				match(T__8);
				setState(228);
				expression();
				setState(229);
				match(T__9);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static final String _serializedATN =
		"\u0004\u0001\u001e\u00ea\u0002\u0000\u0007\u0000\u0002\u0001\u0007\u0001"+
		"\u0002\u0002\u0007\u0002\u0002\u0003\u0007\u0003\u0002\u0004\u0007\u0004"+
		"\u0002\u0005\u0007\u0005\u0002\u0006\u0007\u0006\u0002\u0007\u0007\u0007"+
		"\u0002\b\u0007\b\u0002\t\u0007\t\u0002\n\u0007\n\u0002\u000b\u0007\u000b"+
		"\u0002\f\u0007\f\u0002\r\u0007\r\u0002\u000e\u0007\u000e\u0002\u000f\u0007"+
		"\u000f\u0002\u0010\u0007\u0010\u0002\u0011\u0007\u0011\u0002\u0012\u0007"+
		"\u0012\u0001\u0000\u0003\u0000(\b\u0000\u0001\u0000\u0005\u0000+\b\u0000"+
		"\n\u0000\f\u0000.\t\u0000\u0001\u0000\u0005\u00001\b\u0000\n\u0000\f\u0000"+
		"4\t\u0000\u0001\u0000\u0001\u0000\u0001\u0000\u0003\u00009\b\u0000\u0001"+
		"\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001"+
		"\u0001\u0001\u0002\u0001\u0002\u0003\u0002D\b\u0002\u0001\u0003\u0001"+
		"\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0001"+
		"\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0001"+
		"\u0004\u0001\u0005\u0005\u0005U\b\u0005\n\u0005\f\u0005X\t\u0005\u0001"+
		"\u0006\u0004\u0006[\b\u0006\u000b\u0006\f\u0006\\\u0001\u0006\u0003\u0006"+
		"`\b\u0006\u0001\u0007\u0001\u0007\u0001\u0007\u0001\u0007\u0001\u0007"+
		"\u0001\u0007\u0001\u0007\u0005\u0007i\b\u0007\n\u0007\f\u0007l\t\u0007"+
		"\u0003\u0007n\b\u0007\u0001\b\u0001\b\u0001\b\u0001\b\u0001\b\u0001\b"+
		"\u0003\bv\b\b\u0001\t\u0001\t\u0001\n\u0001\n\u0001\n\u0001\n\u0001\n"+
		"\u0005\n\u007f\b\n\n\n\f\n\u0082\t\n\u0001\n\u0001\n\u0003\n\u0086\b\n"+
		"\u0001\n\u0001\n\u0001\n\u0001\u000b\u0001\u000b\u0001\u000b\u0005\u000b"+
		"\u008e\b\u000b\n\u000b\f\u000b\u0091\t\u000b\u0001\f\u0001\f\u0001\f\u0001"+
		"\f\u0001\f\u0003\f\u0098\b\f\u0001\r\u0001\r\u0001\r\u0001\r\u0001\r\u0001"+
		"\r\u0003\r\u00a0\b\r\u0001\r\u0001\r\u0001\r\u0001\r\u0001\r\u0001\r\u0003"+
		"\r\u00a8\b\r\u0001\r\u0001\r\u0001\u000e\u0001\u000e\u0001\u000e\u0001"+
		"\u000e\u0001\u000e\u0001\u000e\u0003\u000e\u00b2\b\u000e\u0001\u000e\u0001"+
		"\u000e\u0001\u000f\u0001\u000f\u0001\u000f\u0001\u000f\u0001\u000f\u0001"+
		"\u000f\u0003\u000f\u00bc\b\u000f\u0001\u000f\u0001\u000f\u0001\u000f\u0001"+
		"\u000f\u0001\u000f\u0001\u000f\u0003\u000f\u00c4\b\u000f\u0005\u000f\u00c6"+
		"\b\u000f\n\u000f\f\u000f\u00c9\t\u000f\u0001\u000f\u0001\u000f\u0001\u0010"+
		"\u0001\u0010\u0001\u0010\u0005\u0010\u00d0\b\u0010\n\u0010\f\u0010\u00d3"+
		"\t\u0010\u0001\u0011\u0001\u0011\u0001\u0011\u0005\u0011\u00d8\b\u0011"+
		"\n\u0011\f\u0011\u00db\t\u0011\u0001\u0012\u0003\u0012\u00de\b\u0012\u0001"+
		"\u0012\u0001\u0012\u0001\u0012\u0001\u0012\u0001\u0012\u0001\u0012\u0001"+
		"\u0012\u0001\u0012\u0003\u0012\u00e8\b\u0012\u0001\u0012\u0000\u0000\u0013"+
		"\u0000\u0002\u0004\u0006\b\n\f\u000e\u0010\u0012\u0014\u0016\u0018\u001a"+
		"\u001c\u001e \"$\u0000\u0002\u0001\u0000\u001a\u001b\u0001\u0000\u001c"+
		"\u001d\u00f2\u00008\u0001\u0000\u0000\u0000\u0002:\u0001\u0000\u0000\u0000"+
		"\u0004C\u0001\u0000\u0000\u0000\u0006E\u0001\u0000\u0000\u0000\bL\u0001"+
		"\u0000\u0000\u0000\nV\u0001\u0000\u0000\u0000\f_\u0001\u0000\u0000\u0000"+
		"\u000ea\u0001\u0000\u0000\u0000\u0010u\u0001\u0000\u0000\u0000\u0012w"+
		"\u0001\u0000\u0000\u0000\u0014y\u0001\u0000\u0000\u0000\u0016\u008a\u0001"+
		"\u0000\u0000\u0000\u0018\u0097\u0001\u0000\u0000\u0000\u001a\u0099\u0001"+
		"\u0000\u0000\u0000\u001c\u00ab\u0001\u0000\u0000\u0000\u001e\u00b5\u0001"+
		"\u0000\u0000\u0000 \u00cc\u0001\u0000\u0000\u0000\"\u00d4\u0001\u0000"+
		"\u0000\u0000$\u00dd\u0001\u0000\u0000\u0000&(\u0003\u0002\u0001\u0000"+
		"\'&\u0001\u0000\u0000\u0000\'(\u0001\u0000\u0000\u0000(,\u0001\u0000\u0000"+
		"\u0000)+\u0003\u0004\u0002\u0000*)\u0001\u0000\u0000\u0000+.\u0001\u0000"+
		"\u0000\u0000,*\u0001\u0000\u0000\u0000,-\u0001\u0000\u0000\u0000-2\u0001"+
		"\u0000\u0000\u0000.,\u0001\u0000\u0000\u0000/1\u0003\u0012\t\u00000/\u0001"+
		"\u0000\u0000\u000014\u0001\u0000\u0000\u000020\u0001\u0000\u0000\u0000"+
		"23\u0001\u0000\u0000\u000035\u0001\u0000\u0000\u000042\u0001\u0000\u0000"+
		"\u000059\u0005\u0000\u0000\u000167\u0005\u0001\u0000\u000079\u0005\u0000"+
		"\u0000\u00018\'\u0001\u0000\u0000\u000086\u0001\u0000\u0000\u00009\u0001"+
		"\u0001\u0000\u0000\u0000:;\u0005\u000b\u0000\u0000;<\u0005\u0016\u0000"+
		"\u0000<=\u0005\u0001\u0000\u0000=>\u0005\f\u0000\u0000>?\u0005\u0018\u0000"+
		"\u0000?@\u0005\u0001\u0000\u0000@\u0003\u0001\u0000\u0000\u0000AD\u0003"+
		"\u0006\u0003\u0000BD\u0003\b\u0004\u0000CA\u0001\u0000\u0000\u0000CB\u0001"+
		"\u0000\u0000\u0000D\u0005\u0001\u0000\u0000\u0000EF\u0005\r\u0000\u0000"+
		"FG\u0005\u0015\u0000\u0000GH\u0005\u0002\u0000\u0000HI\u0005\u0017\u0000"+
		"\u0000IJ\u0005\u0003\u0000\u0000JK\u0005\u0001\u0000\u0000K\u0007\u0001"+
		"\u0000\u0000\u0000LM\u0005\u000e\u0000\u0000MN\u0005\u0015\u0000\u0000"+
		"NO\u0005\u0002\u0000\u0000OP\u0005\u0017\u0000\u0000PQ\u0005\u0003\u0000"+
		"\u0000QR\u0005\u0001\u0000\u0000R\t\u0001\u0000\u0000\u0000SU\u0003\u000e"+
		"\u0007\u0000TS\u0001\u0000\u0000\u0000UX\u0001\u0000\u0000\u0000VT\u0001"+
		"\u0000\u0000\u0000VW\u0001\u0000\u0000\u0000W\u000b\u0001\u0000\u0000"+
		"\u0000XV\u0001\u0000\u0000\u0000Y[\u0003\u0012\t\u0000ZY\u0001\u0000\u0000"+
		"\u0000[\\\u0001\u0000\u0000\u0000\\Z\u0001\u0000\u0000\u0000\\]\u0001"+
		"\u0000\u0000\u0000]`\u0001\u0000\u0000\u0000^`\u0005\u0001\u0000\u0000"+
		"_Z\u0001\u0000\u0000\u0000_^\u0001\u0000\u0000\u0000`\r\u0001\u0000\u0000"+
		"\u0000ab\u0003\f\u0006\u0000bc\u0005\u0004\u0000\u0000cm\u0003\f\u0006"+
		"\u0000de\u0005\u0005\u0000\u0000ej\u0003\u0010\b\u0000fg\u0005\u0006\u0000"+
		"\u0000gi\u0003\u0010\b\u0000hf\u0001\u0000\u0000\u0000il\u0001\u0000\u0000"+
		"\u0000jh\u0001\u0000\u0000\u0000jk\u0001\u0000\u0000\u0000kn\u0001\u0000"+
		"\u0000\u0000lj\u0001\u0000\u0000\u0000md\u0001\u0000\u0000\u0000mn\u0001"+
		"\u0000\u0000\u0000n\u000f\u0001\u0000\u0000\u0000op\u0005\u0014\u0000"+
		"\u0000pq\u0005\u0007\u0000\u0000qv\u0005\u0014\u0000\u0000rs\u0005\u0014"+
		"\u0000\u0000st\u0005\b\u0000\u0000tv\u0005\u0014\u0000\u0000uo\u0001\u0000"+
		"\u0000\u0000ur\u0001\u0000\u0000\u0000v\u0011\u0001\u0000\u0000\u0000"+
		"wx\u0003\u0014\n\u0000x\u0013\u0001\u0000\u0000\u0000y\u0085\u0005\u0015"+
		"\u0000\u0000z{\u0005\t\u0000\u0000{\u0080\u0003 \u0010\u0000|}\u0005\u0006"+
		"\u0000\u0000}\u007f\u0003 \u0010\u0000~|\u0001\u0000\u0000\u0000\u007f"+
		"\u0082\u0001\u0000\u0000\u0000\u0080~\u0001\u0000\u0000\u0000\u0080\u0081"+
		"\u0001\u0000\u0000\u0000\u0081\u0083\u0001\u0000\u0000\u0000\u0082\u0080"+
		"\u0001\u0000\u0000\u0000\u0083\u0084\u0005\n\u0000\u0000\u0084\u0086\u0001"+
		"\u0000\u0000\u0000\u0085z\u0001\u0000\u0000\u0000\u0085\u0086\u0001\u0000"+
		"\u0000\u0000\u0086\u0087\u0001\u0000\u0000\u0000\u0087\u0088\u0003\u0016"+
		"\u000b\u0000\u0088\u0089\u0005\u0001\u0000\u0000\u0089\u0015\u0001\u0000"+
		"\u0000\u0000\u008a\u008f\u0003\u0018\f\u0000\u008b\u008c\u0005\u0006\u0000"+
		"\u0000\u008c\u008e\u0003\u0018\f\u0000\u008d\u008b\u0001\u0000\u0000\u0000"+
		"\u008e\u0091\u0001\u0000\u0000\u0000\u008f\u008d\u0001\u0000\u0000\u0000"+
		"\u008f\u0090\u0001\u0000\u0000\u0000\u0090\u0017\u0001\u0000\u0000\u0000"+
		"\u0091\u008f\u0001\u0000\u0000\u0000\u0092\u0093\u0005\u0015\u0000\u0000"+
		"\u0093\u0094\u0005\u0002\u0000\u0000\u0094\u0095\u0005\u0017\u0000\u0000"+
		"\u0095\u0098\u0005\u0003\u0000\u0000\u0096\u0098\u0005\u0014\u0000\u0000"+
		"\u0097\u0092\u0001\u0000\u0000\u0000\u0097\u0096\u0001\u0000\u0000\u0000"+
		"\u0098\u0019\u0001\u0000\u0000\u0000\u0099\u009f\u0005\u000f\u0000\u0000"+
		"\u009a\u00a0\u0005\u0015\u0000\u0000\u009b\u009c\u0005\u0015\u0000\u0000"+
		"\u009c\u009d\u0005\u0002\u0000\u0000\u009d\u009e\u0005\u0017\u0000\u0000"+
		"\u009e\u00a0\u0005\u0003\u0000\u0000\u009f\u009a\u0001\u0000\u0000\u0000"+
		"\u009f\u009b\u0001\u0000\u0000\u0000\u00a0\u00a1\u0001\u0000\u0000\u0000"+
		"\u00a1\u00a7\u0005\u0010\u0000\u0000\u00a2\u00a8\u0005\u0015\u0000\u0000"+
		"\u00a3\u00a4\u0005\u0015\u0000\u0000\u00a4\u00a5\u0005\u0002\u0000\u0000"+
		"\u00a5\u00a6\u0005\u0017\u0000\u0000\u00a6\u00a8\u0005\u0003\u0000\u0000"+
		"\u00a7\u00a2\u0001\u0000\u0000\u0000\u00a7\u00a3\u0001\u0000\u0000\u0000"+
		"\u00a8\u00a9\u0001\u0000\u0000\u0000\u00a9\u00aa\u0005\u0001\u0000\u0000"+
		"\u00aa\u001b\u0001\u0000\u0000\u0000\u00ab\u00b1\u0005\u0011\u0000\u0000"+
		"\u00ac\u00b2\u0005\u0015\u0000\u0000\u00ad\u00ae\u0005\u0015\u0000\u0000"+
		"\u00ae\u00af\u0005\u0002\u0000\u0000\u00af\u00b0\u0005\u0017\u0000\u0000"+
		"\u00b0\u00b2\u0005\u0003\u0000\u0000\u00b1\u00ac\u0001\u0000\u0000\u0000"+
		"\u00b1\u00ad\u0001\u0000\u0000\u0000\u00b2\u00b3\u0001\u0000\u0000\u0000"+
		"\u00b3\u00b4\u0005\u0001\u0000\u0000\u00b4\u001d\u0001\u0000\u0000\u0000"+
		"\u00b5\u00bb\u0005\u0012\u0000\u0000\u00b6\u00bc\u0005\u0015\u0000\u0000"+
		"\u00b7\u00b8\u0005\u0015\u0000\u0000\u00b8\u00b9\u0005\u0002\u0000\u0000"+
		"\u00b9\u00ba\u0005\u0017\u0000\u0000\u00ba\u00bc\u0005\u0003\u0000\u0000"+
		"\u00bb\u00b6\u0001\u0000\u0000\u0000\u00bb\u00b7\u0001\u0000\u0000\u0000"+
		"\u00bc\u00c7\u0001\u0000\u0000\u0000\u00bd\u00c3\u0005\u0006\u0000\u0000"+
		"\u00be\u00c4\u0005\u0015\u0000\u0000\u00bf\u00c0\u0005\u0015\u0000\u0000"+
		"\u00c0\u00c1\u0005\u0002\u0000\u0000\u00c1\u00c2\u0005\u0017\u0000\u0000"+
		"\u00c2\u00c4\u0005\u0003\u0000\u0000\u00c3\u00be\u0001\u0000\u0000\u0000"+
		"\u00c3\u00bf\u0001\u0000\u0000\u0000\u00c4\u00c6\u0001\u0000\u0000\u0000"+
		"\u00c5\u00bd\u0001\u0000\u0000\u0000\u00c6\u00c9\u0001\u0000\u0000\u0000"+
		"\u00c7\u00c5\u0001\u0000\u0000\u0000\u00c7\u00c8\u0001\u0000\u0000\u0000"+
		"\u00c8\u00ca\u0001\u0000\u0000\u0000\u00c9\u00c7\u0001\u0000\u0000\u0000"+
		"\u00ca\u00cb\u0005\u0001\u0000\u0000\u00cb\u001f\u0001\u0000\u0000\u0000"+
		"\u00cc\u00d1\u0003\"\u0011\u0000\u00cd\u00ce\u0007\u0000\u0000\u0000\u00ce"+
		"\u00d0\u0003\"\u0011\u0000\u00cf\u00cd\u0001\u0000\u0000\u0000\u00d0\u00d3"+
		"\u0001\u0000\u0000\u0000\u00d1\u00cf\u0001\u0000\u0000\u0000\u00d1\u00d2"+
		"\u0001\u0000\u0000\u0000\u00d2!\u0001\u0000\u0000\u0000\u00d3\u00d1\u0001"+
		"\u0000\u0000\u0000\u00d4\u00d9\u0003$\u0012\u0000\u00d5\u00d6\u0007\u0001"+
		"\u0000\u0000\u00d6\u00d8\u0003$\u0012\u0000\u00d7\u00d5\u0001\u0000\u0000"+
		"\u0000\u00d8\u00db\u0001\u0000\u0000\u0000\u00d9\u00d7\u0001\u0000\u0000"+
		"\u0000\u00d9\u00da\u0001\u0000\u0000\u0000\u00da#\u0001\u0000\u0000\u0000"+
		"\u00db\u00d9\u0001\u0000\u0000\u0000\u00dc\u00de\u0005\u001b\u0000\u0000"+
		"\u00dd\u00dc\u0001\u0000\u0000\u0000\u00dd\u00de\u0001\u0000\u0000\u0000"+
		"\u00de\u00e7\u0001\u0000\u0000\u0000\u00df\u00e8\u0005\u0016\u0000\u0000"+
		"\u00e0\u00e8\u0005\u0017\u0000\u0000\u00e1\u00e8\u0005\u0015\u0000\u0000"+
		"\u00e2\u00e8\u0005\u0013\u0000\u0000\u00e3\u00e4\u0005\t\u0000\u0000\u00e4"+
		"\u00e5\u0003 \u0010\u0000\u00e5\u00e6\u0005\n\u0000\u0000\u00e6\u00e8"+
		"\u0001\u0000\u0000\u0000\u00e7\u00df\u0001\u0000\u0000\u0000\u00e7\u00e0"+
		"\u0001\u0000\u0000\u0000\u00e7\u00e1\u0001\u0000\u0000\u0000\u00e7\u00e2"+
		"\u0001\u0000\u0000\u0000\u00e7\u00e3\u0001\u0000\u0000\u0000\u00e8%\u0001"+
		"\u0000\u0000\u0000\u0019\',28CV\\_jmu\u0080\u0085\u008f\u0097\u009f\u00a7"+
		"\u00b1\u00bb\u00c3\u00c7\u00d1\u00d9\u00dd\u00e7";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}