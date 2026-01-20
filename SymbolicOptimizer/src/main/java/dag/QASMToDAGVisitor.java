


import java.util.HashMap;
import java.util.Map;

import org.antlr.v4.runtime.tree.ParseTree;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import ast.*;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

import java.util.HashSet;
import java.util.Set;

public class QASMToDAGVisitor extends QASMBaseVisitor<Object> {

    private CircuitDAG dag;
    private Map<String, Node> lastNodeOnQubit;
    private Map<String, Node> qubitNodes;
    public QASMToDAGVisitor() {
        this.dag = new CircuitDAG();
        this.lastNodeOnQubit = new HashMap<>();
        this.qubitNodes = new HashMap<>();
    }

    public CircuitDAG getDAG() {
        return dag;
    }



    public static CircuitDAG parse(String circuit) {
        QASMLexer lexer = new QASMLexer(CharStreams.fromString(circuit));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        QASMParser parser = new QASMParser(tokens);
        QASMToDAGVisitor visitor = new QASMToDAGVisitor();
        visitor.visit(parser.program());

        return visitor.getDAG();
    }


    @Override
    public Object visitProgram(QASMParser.ProgramContext ctx) {
        for (QASMParser.StatementContext statementCtx : ctx.statement()) {
            visit(statementCtx);
        }
        for (String qubit : lastNodeOnQubit.keySet()) {
            Node qubitSink = new Node(qubit, Node.Type.QUBIT_SINK, null, null);
            dag.addVertex(qubitSink);
            dag.addEdge(lastNodeOnQubit.get(qubit), qubitSink, dag.getEdge(lastNodeOnQubit.get(qubit), qubitSink, qubit));
            //System.out.println("added sink qubit: " + lastNodeOnQubit.get(qubit).getId() + " -> " + qubitSink.getId() + " on qubit: " + qubit);
        }
        return null;
    }


    @Override
    public Object visitGate_statement(QASMParser.Gate_statementContext ctx) {
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

        if (!params.isEmpty() && CircuitDAG.allAnglesZero(params) && !gateName.equals("u2")) {
            return null;
        }

        List<String> qubits = new ArrayList<>();
        for (QASMParser.QubitContext qubitCtx : ctx.qubits().qubit()) {
            if (qubitCtx.ID() != null) {
                qubits.add(qubitCtx.ID().getText() + qubitCtx.INT_LITERAL().getText());
            } else {
                qubits.add(qubitCtx.QUBIT().getText());
            }
        }
        // Create a new node for the gate
        Node gateNode = new Node(gateName, Node.Type.GATE, qubits, params);
       
        dag.addVertex(gateNode);

        

        // Add edges from the last node on each qubit
        for (String qubitName: qubits) {
            if (lastNodeOnQubit.containsKey(qubitName)) {
                Node lastNode = lastNodeOnQubit.get(qubitName);
                dag.addEdge(lastNode, gateNode, dag.getEdge(lastNode, gateNode, qubitName));
                //System.out.println("added edge: " + lastNode.toString() + " -> " + gateNode.toString() + " on qubit: " + qubitName);
            }
            lastNodeOnQubit.put(qubitName, gateNode);
            if (!qubitNodes.containsKey(qubitName)) {
                Node qubitNode = new Node(qubitName, Node.Type.QUBIT_SOURCE, null, null);
                dag.addVertex(qubitNode);
                //System.out.println("added source qubit: " + qubitName);
                dag.addEdge(qubitNode, gateNode, dag.getEdge(qubitNode, gateNode, qubitName));
                //System.out.println("added edge: " + qubitNode.toString() + " -> " + gateNode.toString() + " on qubit: " + qubitName);
                qubitNodes.put(qubitName, qubitNode);
            }
        }
        return null;
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
            result = new Real(Double.parseDouble(ctx.REAL_LITERAL().getText()));
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
}
