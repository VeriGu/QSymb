// Generated from Egg.g4 by ANTLR 4.12.0
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.*;
import org.antlr.v4.runtime.tree.*;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast", "CheckReturnValue"})
public class EggParser extends Parser {
	static { RuntimeMetaData.checkVersion("4.12.0", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		T__0=1, T__1=2, T__2=3, T__3=4, T__4=5, T__5=6, T__6=7, T__7=8, T__8=9, 
		T__9=10, T__10=11, T__11=12, T__12=13, T__13=14, T__14=15, T__15=16, T__16=17, 
		T__17=18, T__18=19, T__19=20, T__20=21, T__21=22, T__22=23, T__23=24, 
		T__24=25, T__25=26, T__26=27, T__27=28, T__28=29, T__29=30, T__30=31, 
		T__31=32, T__32=33, T__33=34, T__34=35, T__35=36, T__36=37, T__37=38, 
		T__38=39, T__39=40, T__40=41, T__41=42, T__42=43, BOOLEAN=44, F64=45, 
		Nil=46, LPAREN=47, RPAREN=48, STRING=49, INTEGER=50, NUMBER=51, WORD=52, 
		WHITESPACE=53;
	public static final int
		RULE_parse = 0, RULE_constrainedCircuit = 1, RULE_circuit = 2, RULE_permutation = 3, 
		RULE_gate = 4, RULE_qubit = 5, RULE_expr = 6, RULE_op = 7;
	private static String[] makeRuleNames() {
		return new String[] {
			"parse", "constrainedCircuit", "circuit", "permutation", "gate", "qubit", 
			"expr", "op"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, "'CCircuit'", "'Cons'", "'PermNil'", "'PermCons'", "'X'", "'CX'", 
			"'RZ'", "'H'", "'SYMB'", "'U1'", "'U2'", "'U3'", "'RX'", "'CZ'", "'RY'", 
			"'RXX'", "'GPI'", "'GPI2'", "'VZ'", "'MS'", "'SX'", "'Q'", "'Bool'", 
			"'Real'", "'Symbol'", "'Var'", "'Fun'", "'UnOp'", "'BinOp'", "'EXP'", 
			"'SQRT'", "'MINUS'", "'COS'", "'SIN'", "'NOT'", "'PLUS'", "'SUBTRACT'", 
			"'MULT'", "'DIV'", "'POWER'", "'XOR'", "'AND'", "'OR'", null, null, "'Nil'", 
			"'('", "')'"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, null, null, null, null, null, null, null, null, null, null, null, 
			null, null, null, null, null, null, null, null, null, null, null, null, 
			null, null, null, null, null, null, null, null, null, null, null, null, 
			null, null, null, null, null, null, null, null, "BOOLEAN", "F64", "Nil", 
			"LPAREN", "RPAREN", "STRING", "INTEGER", "NUMBER", "WORD", "WHITESPACE"
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
	public String getGrammarFileName() { return "Egg.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public ATN getATN() { return _ATN; }

	public EggParser(TokenStream input) {
		super(input);
		_interp = new ParserATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ParseContext extends ParserRuleContext {
		public ConstrainedCircuitContext constrainedCircuit() {
			return getRuleContext(ConstrainedCircuitContext.class,0);
		}
		public TerminalNode EOF() { return getToken(EggParser.EOF, 0); }
		public ParseContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_parse; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof EggListener ) ((EggListener)listener).enterParse(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof EggListener ) ((EggListener)listener).exitParse(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof EggVisitor ) return ((EggVisitor<? extends T>)visitor).visitParse(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ParseContext parse() throws RecognitionException {
		ParseContext _localctx = new ParseContext(_ctx, getState());
		enterRule(_localctx, 0, RULE_parse);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(16);
			constrainedCircuit();
			setState(17);
			match(EOF);
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
	public static class ConstrainedCircuitContext extends ParserRuleContext {
		public TerminalNode LPAREN() { return getToken(EggParser.LPAREN, 0); }
		public CircuitContext circuit() {
			return getRuleContext(CircuitContext.class,0);
		}
		public PermutationContext permutation() {
			return getRuleContext(PermutationContext.class,0);
		}
		public TerminalNode RPAREN() { return getToken(EggParser.RPAREN, 0); }
		public ConstrainedCircuitContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_constrainedCircuit; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof EggListener ) ((EggListener)listener).enterConstrainedCircuit(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof EggListener ) ((EggListener)listener).exitConstrainedCircuit(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof EggVisitor ) return ((EggVisitor<? extends T>)visitor).visitConstrainedCircuit(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ConstrainedCircuitContext constrainedCircuit() throws RecognitionException {
		ConstrainedCircuitContext _localctx = new ConstrainedCircuitContext(_ctx, getState());
		enterRule(_localctx, 2, RULE_constrainedCircuit);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(19);
			match(LPAREN);
			setState(20);
			match(T__0);
			setState(21);
			circuit();
			setState(22);
			permutation();
			setState(23);
			match(RPAREN);
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
	public static class CircuitContext extends ParserRuleContext {
		public TerminalNode LPAREN() { return getToken(EggParser.LPAREN, 0); }
		public TerminalNode Nil() { return getToken(EggParser.Nil, 0); }
		public TerminalNode RPAREN() { return getToken(EggParser.RPAREN, 0); }
		public GateContext gate() {
			return getRuleContext(GateContext.class,0);
		}
		public CircuitContext circuit() {
			return getRuleContext(CircuitContext.class,0);
		}
		public CircuitContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_circuit; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof EggListener ) ((EggListener)listener).enterCircuit(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof EggListener ) ((EggListener)listener).exitCircuit(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof EggVisitor ) return ((EggVisitor<? extends T>)visitor).visitCircuit(this);
			else return visitor.visitChildren(this);
		}
	}

	public final CircuitContext circuit() throws RecognitionException {
		CircuitContext _localctx = new CircuitContext(_ctx, getState());
		enterRule(_localctx, 4, RULE_circuit);
		try {
			setState(34);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,0,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(25);
				match(LPAREN);
				setState(26);
				match(Nil);
				setState(27);
				match(RPAREN);
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(28);
				match(LPAREN);
				setState(29);
				match(T__1);
				setState(30);
				gate();
				setState(31);
				circuit();
				setState(32);
				match(RPAREN);
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
	public static class PermutationContext extends ParserRuleContext {
		public Token cons;
		public TerminalNode LPAREN() { return getToken(EggParser.LPAREN, 0); }
		public TerminalNode RPAREN() { return getToken(EggParser.RPAREN, 0); }
		public TerminalNode INTEGER() { return getToken(EggParser.INTEGER, 0); }
		public PermutationContext permutation() {
			return getRuleContext(PermutationContext.class,0);
		}
		public PermutationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_permutation; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof EggListener ) ((EggListener)listener).enterPermutation(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof EggListener ) ((EggListener)listener).exitPermutation(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof EggVisitor ) return ((EggVisitor<? extends T>)visitor).visitPermutation(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PermutationContext permutation() throws RecognitionException {
		PermutationContext _localctx = new PermutationContext(_ctx, getState());
		enterRule(_localctx, 6, RULE_permutation);
		try {
			setState(45);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,1,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(36);
				match(LPAREN);
				setState(37);
				((PermutationContext)_localctx).cons = match(T__2);
				setState(38);
				match(RPAREN);
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(39);
				match(LPAREN);
				setState(40);
				((PermutationContext)_localctx).cons = match(T__3);
				setState(41);
				match(INTEGER);
				setState(42);
				permutation();
				setState(43);
				match(RPAREN);
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
	public static class GateContext extends ParserRuleContext {
		public Token gateN;
		public TerminalNode LPAREN() { return getToken(EggParser.LPAREN, 0); }
		public List<QubitContext> qubit() {
			return getRuleContexts(QubitContext.class);
		}
		public QubitContext qubit(int i) {
			return getRuleContext(QubitContext.class,i);
		}
		public TerminalNode RPAREN() { return getToken(EggParser.RPAREN, 0); }
		public List<ExprContext> expr() {
			return getRuleContexts(ExprContext.class);
		}
		public ExprContext expr(int i) {
			return getRuleContext(ExprContext.class,i);
		}
		public TerminalNode INTEGER() { return getToken(EggParser.INTEGER, 0); }
		public GateContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_gate; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof EggListener ) ((EggListener)listener).enterGate(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof EggListener ) ((EggListener)listener).exitGate(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof EggVisitor ) return ((EggVisitor<? extends T>)visitor).visitGate(this);
			else return visitor.visitChildren(this);
		}
	}

	public final GateContext gate() throws RecognitionException {
		GateContext _localctx = new GateContext(_ctx, getState());
		enterRule(_localctx, 8, RULE_gate);
		try {
			setState(150);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,2,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(47);
				match(LPAREN);
				setState(48);
				((GateContext)_localctx).gateN = match(T__4);
				setState(49);
				qubit();
				setState(50);
				match(RPAREN);
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(52);
				match(LPAREN);
				setState(53);
				((GateContext)_localctx).gateN = match(T__5);
				setState(54);
				qubit();
				setState(55);
				qubit();
				setState(56);
				match(RPAREN);
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(58);
				match(LPAREN);
				setState(59);
				((GateContext)_localctx).gateN = match(T__6);
				setState(60);
				qubit();
				setState(61);
				expr();
				setState(62);
				match(RPAREN);
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(64);
				match(LPAREN);
				setState(65);
				((GateContext)_localctx).gateN = match(T__7);
				setState(66);
				qubit();
				setState(67);
				match(RPAREN);
				}
				break;
			case 5:
				enterOuterAlt(_localctx, 5);
				{
				setState(69);
				match(LPAREN);
				setState(70);
				((GateContext)_localctx).gateN = match(T__8);
				setState(71);
				match(INTEGER);
				setState(72);
				match(RPAREN);
				}
				break;
			case 6:
				enterOuterAlt(_localctx, 6);
				{
				setState(73);
				match(LPAREN);
				setState(74);
				((GateContext)_localctx).gateN = match(T__9);
				setState(75);
				qubit();
				setState(76);
				expr();
				setState(77);
				match(RPAREN);
				}
				break;
			case 7:
				enterOuterAlt(_localctx, 7);
				{
				setState(79);
				match(LPAREN);
				setState(80);
				((GateContext)_localctx).gateN = match(T__10);
				setState(81);
				qubit();
				setState(82);
				expr();
				setState(83);
				expr();
				setState(84);
				match(RPAREN);
				}
				break;
			case 8:
				enterOuterAlt(_localctx, 8);
				{
				setState(86);
				match(LPAREN);
				setState(87);
				((GateContext)_localctx).gateN = match(T__11);
				setState(88);
				qubit();
				setState(89);
				expr();
				setState(90);
				expr();
				setState(91);
				expr();
				setState(92);
				match(RPAREN);
				}
				break;
			case 9:
				enterOuterAlt(_localctx, 9);
				{
				setState(94);
				match(LPAREN);
				setState(95);
				((GateContext)_localctx).gateN = match(T__12);
				setState(96);
				qubit();
				setState(97);
				expr();
				setState(98);
				match(RPAREN);
				}
				break;
			case 10:
				enterOuterAlt(_localctx, 10);
				{
				setState(100);
				match(LPAREN);
				setState(101);
				((GateContext)_localctx).gateN = match(T__13);
				setState(102);
				qubit();
				setState(103);
				qubit();
				setState(104);
				match(RPAREN);
				}
				break;
			case 11:
				enterOuterAlt(_localctx, 11);
				{
				setState(106);
				match(LPAREN);
				setState(107);
				((GateContext)_localctx).gateN = match(T__14);
				setState(108);
				qubit();
				setState(109);
				expr();
				setState(110);
				match(RPAREN);
				}
				break;
			case 12:
				enterOuterAlt(_localctx, 12);
				{
				setState(112);
				match(LPAREN);
				setState(113);
				((GateContext)_localctx).gateN = match(T__15);
				setState(114);
				qubit();
				setState(115);
				qubit();
				setState(116);
				expr();
				setState(117);
				match(RPAREN);
				}
				break;
			case 13:
				enterOuterAlt(_localctx, 13);
				{
				setState(119);
				match(LPAREN);
				setState(120);
				((GateContext)_localctx).gateN = match(T__16);
				setState(121);
				qubit();
				setState(122);
				expr();
				setState(123);
				match(RPAREN);
				}
				break;
			case 14:
				enterOuterAlt(_localctx, 14);
				{
				setState(125);
				match(LPAREN);
				setState(126);
				((GateContext)_localctx).gateN = match(T__17);
				setState(127);
				qubit();
				setState(128);
				expr();
				setState(129);
				match(RPAREN);
				}
				break;
			case 15:
				enterOuterAlt(_localctx, 15);
				{
				setState(131);
				match(LPAREN);
				setState(132);
				((GateContext)_localctx).gateN = match(T__18);
				setState(133);
				qubit();
				setState(134);
				expr();
				setState(135);
				match(RPAREN);
				}
				break;
			case 16:
				enterOuterAlt(_localctx, 16);
				{
				setState(137);
				match(LPAREN);
				setState(138);
				((GateContext)_localctx).gateN = match(T__19);
				setState(139);
				qubit();
				setState(140);
				qubit();
				setState(141);
				expr();
				setState(142);
				expr();
				setState(143);
				match(RPAREN);
				}
				break;
			case 17:
				enterOuterAlt(_localctx, 17);
				{
				setState(145);
				match(LPAREN);
				setState(146);
				((GateContext)_localctx).gateN = match(T__20);
				setState(147);
				qubit();
				setState(148);
				match(RPAREN);
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
	public static class QubitContext extends ParserRuleContext {
		public TerminalNode LPAREN() { return getToken(EggParser.LPAREN, 0); }
		public TerminalNode STRING() { return getToken(EggParser.STRING, 0); }
		public TerminalNode RPAREN() { return getToken(EggParser.RPAREN, 0); }
		public QubitContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_qubit; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof EggListener ) ((EggListener)listener).enterQubit(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof EggListener ) ((EggListener)listener).exitQubit(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof EggVisitor ) return ((EggVisitor<? extends T>)visitor).visitQubit(this);
			else return visitor.visitChildren(this);
		}
	}

	public final QubitContext qubit() throws RecognitionException {
		QubitContext _localctx = new QubitContext(_ctx, getState());
		enterRule(_localctx, 10, RULE_qubit);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(152);
			match(LPAREN);
			setState(153);
			match(T__21);
			setState(154);
			match(STRING);
			setState(155);
			match(RPAREN);
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
	public static class ExprContext extends ParserRuleContext {
		public Token cons;
		public List<TerminalNode> LPAREN() { return getTokens(EggParser.LPAREN); }
		public TerminalNode LPAREN(int i) {
			return getToken(EggParser.LPAREN, i);
		}
		public TerminalNode BOOLEAN() { return getToken(EggParser.BOOLEAN, 0); }
		public List<TerminalNode> RPAREN() { return getTokens(EggParser.RPAREN); }
		public TerminalNode RPAREN(int i) {
			return getToken(EggParser.RPAREN, i);
		}
		public TerminalNode F64() { return getToken(EggParser.F64, 0); }
		public TerminalNode STRING() { return getToken(EggParser.STRING, 0); }
		public List<ExprContext> expr() {
			return getRuleContexts(ExprContext.class);
		}
		public ExprContext expr(int i) {
			return getRuleContext(ExprContext.class,i);
		}
		public OpContext op() {
			return getRuleContext(OpContext.class,0);
		}
		public ExprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_expr; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof EggListener ) ((EggListener)listener).enterExpr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof EggListener ) ((EggListener)listener).exitExpr(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof EggVisitor ) return ((EggVisitor<? extends T>)visitor).visitExpr(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ExprContext expr() throws RecognitionException {
		ExprContext _localctx = new ExprContext(_ctx, getState());
		enterRule(_localctx, 12, RULE_expr);
		try {
			setState(196);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,3,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(157);
				match(LPAREN);
				setState(158);
				((ExprContext)_localctx).cons = match(T__22);
				setState(159);
				match(BOOLEAN);
				setState(160);
				match(RPAREN);
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(161);
				match(LPAREN);
				setState(162);
				((ExprContext)_localctx).cons = match(T__23);
				setState(163);
				match(F64);
				setState(164);
				match(RPAREN);
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(165);
				match(LPAREN);
				setState(166);
				((ExprContext)_localctx).cons = match(T__24);
				setState(167);
				match(STRING);
				setState(168);
				match(RPAREN);
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(169);
				match(LPAREN);
				setState(170);
				((ExprContext)_localctx).cons = match(T__25);
				setState(171);
				match(STRING);
				setState(172);
				match(RPAREN);
				}
				break;
			case 5:
				enterOuterAlt(_localctx, 5);
				{
				setState(173);
				match(LPAREN);
				setState(174);
				((ExprContext)_localctx).cons = match(T__26);
				setState(175);
				match(STRING);
				setState(176);
				expr();
				setState(177);
				match(RPAREN);
				}
				break;
			case 6:
				enterOuterAlt(_localctx, 6);
				{
				setState(179);
				match(LPAREN);
				setState(180);
				((ExprContext)_localctx).cons = match(T__27);
				setState(181);
				match(LPAREN);
				setState(182);
				op();
				setState(183);
				match(RPAREN);
				setState(184);
				expr();
				setState(185);
				match(RPAREN);
				}
				break;
			case 7:
				enterOuterAlt(_localctx, 7);
				{
				setState(187);
				match(LPAREN);
				setState(188);
				((ExprContext)_localctx).cons = match(T__28);
				setState(189);
				match(LPAREN);
				setState(190);
				op();
				setState(191);
				match(RPAREN);
				setState(192);
				expr();
				setState(193);
				expr();
				setState(194);
				match(RPAREN);
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
	public static class OpContext extends ParserRuleContext {
		public OpContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_op; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof EggListener ) ((EggListener)listener).enterOp(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof EggListener ) ((EggListener)listener).exitOp(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof EggVisitor ) return ((EggVisitor<? extends T>)visitor).visitOp(this);
			else return visitor.visitChildren(this);
		}
	}

	public final OpContext op() throws RecognitionException {
		OpContext _localctx = new OpContext(_ctx, getState());
		enterRule(_localctx, 14, RULE_op);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(198);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & 17591112302592L) != 0)) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
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
		"\u0004\u00015\u00c9\u0002\u0000\u0007\u0000\u0002\u0001\u0007\u0001\u0002"+
		"\u0002\u0007\u0002\u0002\u0003\u0007\u0003\u0002\u0004\u0007\u0004\u0002"+
		"\u0005\u0007\u0005\u0002\u0006\u0007\u0006\u0002\u0007\u0007\u0007\u0001"+
		"\u0000\u0001\u0000\u0001\u0000\u0001\u0001\u0001\u0001\u0001\u0001\u0001"+
		"\u0001\u0001\u0001\u0001\u0001\u0001\u0002\u0001\u0002\u0001\u0002\u0001"+
		"\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0003"+
		"\u0002#\b\u0002\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0001"+
		"\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0003\u0003.\b"+
		"\u0003\u0001\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0001"+
		"\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0001"+
		"\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0001"+
		"\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0001"+
		"\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0001"+
		"\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0001"+
		"\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0001"+
		"\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0001"+
		"\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0001"+
		"\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0001"+
		"\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0001"+
		"\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0001"+
		"\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0001"+
		"\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0001"+
		"\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0001"+
		"\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0001"+
		"\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0001"+
		"\u0004\u0001\u0004\u0003\u0004\u0097\b\u0004\u0001\u0005\u0001\u0005\u0001"+
		"\u0005\u0001\u0005\u0001\u0005\u0001\u0006\u0001\u0006\u0001\u0006\u0001"+
		"\u0006\u0001\u0006\u0001\u0006\u0001\u0006\u0001\u0006\u0001\u0006\u0001"+
		"\u0006\u0001\u0006\u0001\u0006\u0001\u0006\u0001\u0006\u0001\u0006\u0001"+
		"\u0006\u0001\u0006\u0001\u0006\u0001\u0006\u0001\u0006\u0001\u0006\u0001"+
		"\u0006\u0001\u0006\u0001\u0006\u0001\u0006\u0001\u0006\u0001\u0006\u0001"+
		"\u0006\u0001\u0006\u0001\u0006\u0001\u0006\u0001\u0006\u0001\u0006\u0001"+
		"\u0006\u0001\u0006\u0001\u0006\u0001\u0006\u0001\u0006\u0001\u0006\u0003"+
		"\u0006\u00c5\b\u0006\u0001\u0007\u0001\u0007\u0001\u0007\u0000\u0000\b"+
		"\u0000\u0002\u0004\u0006\b\n\f\u000e\u0000\u0001\u0001\u0000\u001e+\u00d8"+
		"\u0000\u0010\u0001\u0000\u0000\u0000\u0002\u0013\u0001\u0000\u0000\u0000"+
		"\u0004\"\u0001\u0000\u0000\u0000\u0006-\u0001\u0000\u0000\u0000\b\u0096"+
		"\u0001\u0000\u0000\u0000\n\u0098\u0001\u0000\u0000\u0000\f\u00c4\u0001"+
		"\u0000\u0000\u0000\u000e\u00c6\u0001\u0000\u0000\u0000\u0010\u0011\u0003"+
		"\u0002\u0001\u0000\u0011\u0012\u0005\u0000\u0000\u0001\u0012\u0001\u0001"+
		"\u0000\u0000\u0000\u0013\u0014\u0005/\u0000\u0000\u0014\u0015\u0005\u0001"+
		"\u0000\u0000\u0015\u0016\u0003\u0004\u0002\u0000\u0016\u0017\u0003\u0006"+
		"\u0003\u0000\u0017\u0018\u00050\u0000\u0000\u0018\u0003\u0001\u0000\u0000"+
		"\u0000\u0019\u001a\u0005/\u0000\u0000\u001a\u001b\u0005.\u0000\u0000\u001b"+
		"#\u00050\u0000\u0000\u001c\u001d\u0005/\u0000\u0000\u001d\u001e\u0005"+
		"\u0002\u0000\u0000\u001e\u001f\u0003\b\u0004\u0000\u001f \u0003\u0004"+
		"\u0002\u0000 !\u00050\u0000\u0000!#\u0001\u0000\u0000\u0000\"\u0019\u0001"+
		"\u0000\u0000\u0000\"\u001c\u0001\u0000\u0000\u0000#\u0005\u0001\u0000"+
		"\u0000\u0000$%\u0005/\u0000\u0000%&\u0005\u0003\u0000\u0000&.\u00050\u0000"+
		"\u0000\'(\u0005/\u0000\u0000()\u0005\u0004\u0000\u0000)*\u00052\u0000"+
		"\u0000*+\u0003\u0006\u0003\u0000+,\u00050\u0000\u0000,.\u0001\u0000\u0000"+
		"\u0000-$\u0001\u0000\u0000\u0000-\'\u0001\u0000\u0000\u0000.\u0007\u0001"+
		"\u0000\u0000\u0000/0\u0005/\u0000\u000001\u0005\u0005\u0000\u000012\u0003"+
		"\n\u0005\u000023\u00050\u0000\u00003\u0097\u0001\u0000\u0000\u000045\u0005"+
		"/\u0000\u000056\u0005\u0006\u0000\u000067\u0003\n\u0005\u000078\u0003"+
		"\n\u0005\u000089\u00050\u0000\u00009\u0097\u0001\u0000\u0000\u0000:;\u0005"+
		"/\u0000\u0000;<\u0005\u0007\u0000\u0000<=\u0003\n\u0005\u0000=>\u0003"+
		"\f\u0006\u0000>?\u00050\u0000\u0000?\u0097\u0001\u0000\u0000\u0000@A\u0005"+
		"/\u0000\u0000AB\u0005\b\u0000\u0000BC\u0003\n\u0005\u0000CD\u00050\u0000"+
		"\u0000D\u0097\u0001\u0000\u0000\u0000EF\u0005/\u0000\u0000FG\u0005\t\u0000"+
		"\u0000GH\u00052\u0000\u0000H\u0097\u00050\u0000\u0000IJ\u0005/\u0000\u0000"+
		"JK\u0005\n\u0000\u0000KL\u0003\n\u0005\u0000LM\u0003\f\u0006\u0000MN\u0005"+
		"0\u0000\u0000N\u0097\u0001\u0000\u0000\u0000OP\u0005/\u0000\u0000PQ\u0005"+
		"\u000b\u0000\u0000QR\u0003\n\u0005\u0000RS\u0003\f\u0006\u0000ST\u0003"+
		"\f\u0006\u0000TU\u00050\u0000\u0000U\u0097\u0001\u0000\u0000\u0000VW\u0005"+
		"/\u0000\u0000WX\u0005\f\u0000\u0000XY\u0003\n\u0005\u0000YZ\u0003\f\u0006"+
		"\u0000Z[\u0003\f\u0006\u0000[\\\u0003\f\u0006\u0000\\]\u00050\u0000\u0000"+
		"]\u0097\u0001\u0000\u0000\u0000^_\u0005/\u0000\u0000_`\u0005\r\u0000\u0000"+
		"`a\u0003\n\u0005\u0000ab\u0003\f\u0006\u0000bc\u00050\u0000\u0000c\u0097"+
		"\u0001\u0000\u0000\u0000de\u0005/\u0000\u0000ef\u0005\u000e\u0000\u0000"+
		"fg\u0003\n\u0005\u0000gh\u0003\n\u0005\u0000hi\u00050\u0000\u0000i\u0097"+
		"\u0001\u0000\u0000\u0000jk\u0005/\u0000\u0000kl\u0005\u000f\u0000\u0000"+
		"lm\u0003\n\u0005\u0000mn\u0003\f\u0006\u0000no\u00050\u0000\u0000o\u0097"+
		"\u0001\u0000\u0000\u0000pq\u0005/\u0000\u0000qr\u0005\u0010\u0000\u0000"+
		"rs\u0003\n\u0005\u0000st\u0003\n\u0005\u0000tu\u0003\f\u0006\u0000uv\u0005"+
		"0\u0000\u0000v\u0097\u0001\u0000\u0000\u0000wx\u0005/\u0000\u0000xy\u0005"+
		"\u0011\u0000\u0000yz\u0003\n\u0005\u0000z{\u0003\f\u0006\u0000{|\u0005"+
		"0\u0000\u0000|\u0097\u0001\u0000\u0000\u0000}~\u0005/\u0000\u0000~\u007f"+
		"\u0005\u0012\u0000\u0000\u007f\u0080\u0003\n\u0005\u0000\u0080\u0081\u0003"+
		"\f\u0006\u0000\u0081\u0082\u00050\u0000\u0000\u0082\u0097\u0001\u0000"+
		"\u0000\u0000\u0083\u0084\u0005/\u0000\u0000\u0084\u0085\u0005\u0013\u0000"+
		"\u0000\u0085\u0086\u0003\n\u0005\u0000\u0086\u0087\u0003\f\u0006\u0000"+
		"\u0087\u0088\u00050\u0000\u0000\u0088\u0097\u0001\u0000\u0000\u0000\u0089"+
		"\u008a\u0005/\u0000\u0000\u008a\u008b\u0005\u0014\u0000\u0000\u008b\u008c"+
		"\u0003\n\u0005\u0000\u008c\u008d\u0003\n\u0005\u0000\u008d\u008e\u0003"+
		"\f\u0006\u0000\u008e\u008f\u0003\f\u0006\u0000\u008f\u0090\u00050\u0000"+
		"\u0000\u0090\u0097\u0001\u0000\u0000\u0000\u0091\u0092\u0005/\u0000\u0000"+
		"\u0092\u0093\u0005\u0015\u0000\u0000\u0093\u0094\u0003\n\u0005\u0000\u0094"+
		"\u0095\u00050\u0000\u0000\u0095\u0097\u0001\u0000\u0000\u0000\u0096/\u0001"+
		"\u0000\u0000\u0000\u00964\u0001\u0000\u0000\u0000\u0096:\u0001\u0000\u0000"+
		"\u0000\u0096@\u0001\u0000\u0000\u0000\u0096E\u0001\u0000\u0000\u0000\u0096"+
		"I\u0001\u0000\u0000\u0000\u0096O\u0001\u0000\u0000\u0000\u0096V\u0001"+
		"\u0000\u0000\u0000\u0096^\u0001\u0000\u0000\u0000\u0096d\u0001\u0000\u0000"+
		"\u0000\u0096j\u0001\u0000\u0000\u0000\u0096p\u0001\u0000\u0000\u0000\u0096"+
		"w\u0001\u0000\u0000\u0000\u0096}\u0001\u0000\u0000\u0000\u0096\u0083\u0001"+
		"\u0000\u0000\u0000\u0096\u0089\u0001\u0000\u0000\u0000\u0096\u0091\u0001"+
		"\u0000\u0000\u0000\u0097\t\u0001\u0000\u0000\u0000\u0098\u0099\u0005/"+
		"\u0000\u0000\u0099\u009a\u0005\u0016\u0000\u0000\u009a\u009b\u00051\u0000"+
		"\u0000\u009b\u009c\u00050\u0000\u0000\u009c\u000b\u0001\u0000\u0000\u0000"+
		"\u009d\u009e\u0005/\u0000\u0000\u009e\u009f\u0005\u0017\u0000\u0000\u009f"+
		"\u00a0\u0005,\u0000\u0000\u00a0\u00c5\u00050\u0000\u0000\u00a1\u00a2\u0005"+
		"/\u0000\u0000\u00a2\u00a3\u0005\u0018\u0000\u0000\u00a3\u00a4\u0005-\u0000"+
		"\u0000\u00a4\u00c5\u00050\u0000\u0000\u00a5\u00a6\u0005/\u0000\u0000\u00a6"+
		"\u00a7\u0005\u0019\u0000\u0000\u00a7\u00a8\u00051\u0000\u0000\u00a8\u00c5"+
		"\u00050\u0000\u0000\u00a9\u00aa\u0005/\u0000\u0000\u00aa\u00ab\u0005\u001a"+
		"\u0000\u0000\u00ab\u00ac\u00051\u0000\u0000\u00ac\u00c5\u00050\u0000\u0000"+
		"\u00ad\u00ae\u0005/\u0000\u0000\u00ae\u00af\u0005\u001b\u0000\u0000\u00af"+
		"\u00b0\u00051\u0000\u0000\u00b0\u00b1\u0003\f\u0006\u0000\u00b1\u00b2"+
		"\u00050\u0000\u0000\u00b2\u00c5\u0001\u0000\u0000\u0000\u00b3\u00b4\u0005"+
		"/\u0000\u0000\u00b4\u00b5\u0005\u001c\u0000\u0000\u00b5\u00b6\u0005/\u0000"+
		"\u0000\u00b6\u00b7\u0003\u000e\u0007\u0000\u00b7\u00b8\u00050\u0000\u0000"+
		"\u00b8\u00b9\u0003\f\u0006\u0000\u00b9\u00ba\u00050\u0000\u0000\u00ba"+
		"\u00c5\u0001\u0000\u0000\u0000\u00bb\u00bc\u0005/\u0000\u0000\u00bc\u00bd"+
		"\u0005\u001d\u0000\u0000\u00bd\u00be\u0005/\u0000\u0000\u00be\u00bf\u0003"+
		"\u000e\u0007\u0000\u00bf\u00c0\u00050\u0000\u0000\u00c0\u00c1\u0003\f"+
		"\u0006\u0000\u00c1\u00c2\u0003\f\u0006\u0000\u00c2\u00c3\u00050\u0000"+
		"\u0000\u00c3\u00c5\u0001\u0000\u0000\u0000\u00c4\u009d\u0001\u0000\u0000"+
		"\u0000\u00c4\u00a1\u0001\u0000\u0000\u0000\u00c4\u00a5\u0001\u0000\u0000"+
		"\u0000\u00c4\u00a9\u0001\u0000\u0000\u0000\u00c4\u00ad\u0001\u0000\u0000"+
		"\u0000\u00c4\u00b3\u0001\u0000\u0000\u0000\u00c4\u00bb\u0001\u0000\u0000"+
		"\u0000\u00c5\r\u0001\u0000\u0000\u0000\u00c6\u00c7\u0007\u0000\u0000\u0000"+
		"\u00c7\u000f\u0001\u0000\u0000\u0000\u0004\"-\u0096\u00c4";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}