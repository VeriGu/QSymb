

import java.util.ArrayList;
import java.util.List;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

import ast.BinOp;
import ast.Expr;
import ast.Real;
import ast.Symbol;
import ast.UnOp;


public class QASMAstBuilder extends QASMBaseVisitor<Object> {

    public static EggGen.Circuit parse(String circuit) {
        QASMLexer lexer = new QASMLexer(CharStreams.fromString(circuit));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        QASMParser parser = new QASMParser(tokens);
        ParseTree tree = parser.program();
        QASMAstBuilder builder = new QASMAstBuilder();
        return (EggGen.Circuit) builder.visit(tree);
    }


    public static List<Rule> parseRules(String rewrite) {
        QASMLexer lexer = new QASMLexer(CharStreams.fromString(rewrite));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        QASMParser parser = new QASMParser(tokens);
        ParseTree tree = parser.rewrites();
        QASMAstBuilder builder = new QASMAstBuilder();
        return (List<Rule>) builder.visit(tree);
    }


    public static Rule parseRule(String rewrite) {
        QASMLexer lexer = new QASMLexer(CharStreams.fromString(rewrite));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        QASMParser parser = new QASMParser(tokens);
        ParseTree tree = parser.rewrite();
        QASMAstBuilder builder = new QASMAstBuilder();
        return (Rule) builder.visit(tree);
    }


    @Override
    public List<Rule> visitRewrites(QASMParser.RewritesContext ctx) {
        List<Rule> rules = new ArrayList<>();
        for (QASMParser.RewriteContext ruleCtx : ctx.rewrite()) {
            Rule r = visitRewrite(ruleCtx);
            rules.add(r);
        }
        return rules;
    }

    @Override
    public Rule visitRewrite(QASMParser.RewriteContext ctx) {
        List<EggGen.Gate> gateslhs = new ArrayList<>();
        List<EggGen.Gate> gatesrhs = new ArrayList<>();
        for (QASMParser.StatementContext stmtCtx : ctx.lhs.statement()) {
            gateslhs.add((EggGen.Gate) visit(stmtCtx));
        }
        for (QASMParser.StatementContext stmtCtx : ctx.rhs.statement()) {
            gatesrhs.add((EggGen.Gate) visit(stmtCtx));
        }
        EggGen.Circuit lhs = new EggGen.Circuit(gateslhs);
        EggGen.Circuit rhs = new EggGen.Circuit(gatesrhs);
        List<Rule.Equality> conditions = new ArrayList<>();
        for (QASMParser.EqualityContext eqCtx : ctx.equality()) {
            String qubit1 = eqCtx.QUBIT(0).getText();
            String qubit2 = eqCtx.QUBIT(1).getText();
            boolean isEqual = eqCtx.op.getText().equals("==");
            conditions.add(new Rule.Equality(qubit1, qubit2, isEqual));
        }
        return new Rule(lhs, rhs, conditions);
    }

    
    @Override
    public EggGen.Circuit visitProgram(QASMParser.ProgramContext ctx) {
        // Visit all statements to collect gates
        List<EggGen.Gate> gates = new ArrayList<>();
        for (QASMParser.StatementContext statementCtx : ctx.statement()) {
            gates.add((EggGen.Gate) visit(statementCtx));
        }
        return new EggGen.Circuit(gates);
    }

    @Override
    public EggGen.Gate visitGate_statement(QASMParser.Gate_statementContext ctx) {
        String gateName = ctx.ID().getText();
        
        
        List<Expr> params = new ArrayList<>();
        if (ctx.expression() != null) {
            for (QASMParser.ExpressionContext exprCtx : ctx.expression()) {
                params.add((Expr) visit(exprCtx));
            }
        }

        List<Expr> evaluatedParams = new ArrayList<>();
        if(!params.isEmpty()) {
            for(Expr param: params) {
                if(param.toString().contains("theta")) {
                    evaluatedParams.add(param);
                } else {
                    evaluatedParams.add(new Real(CircuitDAG.eval(param)));
                }
            }
            params = evaluatedParams;
        }

        // Handle gates based on their name and number of parameters/qubits
        List<String> qubits = new ArrayList<>();
        for (QASMParser.QubitContext qubitCtx : ctx.qubits().qubit()) {
            if (qubitCtx.ID() != null) {
                if (qubitCtx.INT_LITERAL() != null) {
                    qubits.add(qubitCtx.ID().getText() + qubitCtx.INT_LITERAL().getText());
                } else {
                    qubits.add(qubitCtx.ID().getText());
                }
            } else {
                qubits.add(qubitCtx.QUBIT().getText());
            }
        }
        switch (gateName) {
            case "h":
                return new EggGen.H(qubits.get(0));
            case "cx":
                return new EggGen.CX(qubits.get(0), qubits.get(1));
            case "x":
                return new EggGen.X(qubits.get(0));
            case "rz":
               return new EggGen.RZ(qubits.get(0), params.get(0));
             
            case "rx":
                return new EggGen.RX(qubits.get(0), params.get(0));
               
            case "ry":
               return new EggGen.RY(qubits.get(0), params.get(0));
            
            case "cz":
                return new EggGen.CZ(qubits.get(0), qubits.get(1));
               
            case "u1":
                return new EggGen.U1(qubits.get(0), params.get(0));
               
            case "u2":
                return new EggGen.U2(qubits.get(0), params.get(0), params.get(1));
            
            case "u3":
                return new EggGen.U3(qubits.get(0), params.get(0), params.get(1), params.get(2));
       
            case "sx":
                return new EggGen.SX(qubits.get(0));

            case "rxx":
                return new EggGen.RXX(qubits.get(0), qubits.get(1), params.get(0));

            case "ms":
                return new EggGen.MS(qubits.get(0), qubits.get(1), params.get(0), params.get(1));

            case "gpi":
                return new EggGen.GPI(qubits.get(0), params.get(0));

            case "gpi2":
                return new EggGen.GPI2(qubits.get(0), params.get(0));

            case "vz":
                return new EggGen.VZ(qubits.get(0), params.get(0));

            default:
                throw new RuntimeException("Unsupported gate in QASMAstBuilder: " + gateName);
        }
    }

    @Override
    public Expr visitExpression(QASMParser.ExpressionContext ctx) {
        Expr result = (Expr) visit(ctx.multExpr(0));
        for (int i = 1; i < ctx.multExpr().size(); i++) {
            if (ctx.PLUS(i - 1) != null) {
                result = new BinOp(Expr.Op.PLUS, result, (Expr) visit(ctx.multExpr(i)));
            } else if (ctx.MINUS(i - 1) != null) {
                result = new BinOp(Expr.Op.SUBTRACT, result, (Expr) visit(ctx.multExpr(i)));
            }
        }
        return result;
    }

    @Override
    public Expr visitMultExpr(QASMParser.MultExprContext ctx) {
        Expr result = (Expr) visit(ctx.atomExpr(0));
        for (int i = 1; i < ctx.atomExpr().size(); i++) {
            if (ctx.MULT(i - 1) != null) {
                result = new BinOp(Expr.Op.MULT, result, (Expr) visit(ctx.atomExpr(i)));
            } else if (ctx.DIV(i - 1) != null) {
                result = new BinOp(Expr.Op.DIV, result, (Expr) visit(ctx.atomExpr(i)));
            }
        }
        return result;
    }

    @Override
    public Expr visitAtomExpr(QASMParser.AtomExprContext ctx) {
        Expr result = null;
        if (ctx.REAL_LITERAL() != null) {
            double d = Double.parseDouble(ctx.REAL_LITERAL().getText());
            if(Math.abs(d) < 1e-5) {
                d = 0.0;
            }
            result = new Real(d);
        } else if (ctx.INT_LITERAL() != null) {
            result = new Real(Double.parseDouble(ctx.INT_LITERAL().getText()));
        } else if (ctx.ID() != null) {
            result = new Symbol(ctx.ID().getText());
        } else if (ctx.PI_KW() != null) {
            result = new Symbol("pi");
        } else if (ctx.expression() != null) {
            result = (Expr) visit(ctx.expression());
        }

        if (ctx.MINUS() != null) {
            result = new UnOp(Expr.Op.MINUS, result);
        }
        return result;
    }

    // Other visit methods for declarations, measures, etc. can be added here if needed
    @Override
    public Object visitDeclaration(QASMParser.DeclarationContext ctx) {
        // For now, we don't need to do anything with declarations for circuit construction
        return null;
    }

    @Override
    public Object visitMeasure_statement(QASMParser.Measure_statementContext ctx) {
        // Ignore measure statements for circuit construction
        return null;
    }

    @Override
    public Object visitReset_statement(QASMParser.Reset_statementContext ctx) {
        // Ignore reset statements for circuit construction
        return null;
    }

    @Override
    public Object visitBarrier_statement(QASMParser.Barrier_statementContext ctx) {
        // Ignore barrier statements for circuit construction
        return null;
    }

    @Override
    public Object visitHeader(QASMParser.HeaderContext ctx) {
        // Ignore header for circuit construction
        return null;
    }
}