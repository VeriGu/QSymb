
import java.util.ArrayList;
import java.util.List;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import ast.*;

public class EggAstBuilder extends EggBaseVisitor<Object> {

    public static EggGen.ConstrainedCircuit parse(String constrainedCircuit) {
        EggLexer lexer = new EggLexer(CharStreams.fromString(constrainedCircuit));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        EggParser parser = new EggParser(tokens);
        ParseTree tree = parser.constrainedCircuit();
        EggAstBuilder builder = new EggAstBuilder();
        return (EggGen.ConstrainedCircuit) builder.visit(tree);
    }

    public static EggGen.Circuit parseCircuit(String circuit) {
        EggLexer lexer = new EggLexer(CharStreams.fromString(circuit));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        EggParser parser = new EggParser(tokens);
        ParseTree tree = parser.circuit();
        EggAstBuilder builder = new EggAstBuilder();
        return (EggGen.Circuit) builder.visit(tree);
    }

    public static EggGen.Permutation parsePerm(String circuit) {
        EggLexer lexer = new EggLexer(CharStreams.fromString(circuit));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        EggParser parser = new EggParser(tokens);
        ParseTree tree = parser.permutation();
        EggAstBuilder builder = new EggAstBuilder();
        return (EggGen.Permutation) builder.visit(tree);
    }

    @Override
    public EggGen.ConstrainedCircuit visitConstrainedCircuit(EggParser.ConstrainedCircuitContext ctx) {
        EggGen.Circuit circuit = (EggGen.Circuit) visit(ctx.circuit());
        EggGen.Permutation permutation = (EggGen.Permutation) visit(ctx.permutation());
        return new EggGen.ConstrainedCircuit(circuit, permutation);
    }

    @Override
    public EggGen.Circuit visitCircuit(EggParser.CircuitContext ctx) {
        if (ctx.Nil() != null) {
            return new EggGen.Circuit(new ArrayList<>());
        } else {
            EggGen.Gate gate = (EggGen.Gate) visit(ctx.gate());
            EggGen.Circuit circuit = (EggGen.Circuit) visit(ctx.circuit());
            circuit.gates.add(0, gate);
            return circuit;
        }
    }

    @Override
    public EggGen.Permutation visitPermutation(EggParser.PermutationContext ctx) {
        if (ctx.cons.getText().equals("PermNil")) {
            return new EggGen.Permutation(new ArrayList<>());
        } else {
            int head = Integer.parseInt(ctx.NUMBER().getText());
            EggGen.Permutation tail = (EggGen.Permutation) visit(ctx.permutation());
            tail.perm.add(0, head);
            return tail;
        }
    }

    @Override
    public EggGen.Gate visitGate(EggParser.GateContext ctx) {
        if (ctx.gateN.getText().equals("X")) {
            return new EggGen.X(visitQubit(ctx.qubit(0)).toString());
        } else if (ctx.gateN.getText().equals("CX")) {
            return new EggGen.CX(visitQubit(ctx.qubit(0)).toString(), visitQubit(ctx.qubit(1)).toString());
        } else if (ctx.gateN.getText().equals("RZ")) {
            return new EggGen.RZ(visitQubit(ctx.qubit(0)).toString(), (Expr) visit(ctx.expr(0)));
        } else if (ctx.gateN.getText().equals("H")) {
            return new EggGen.H(visitQubit(ctx.qubit(0)).toString());
        } else if (ctx.gateN.getText().equals("SYMB")) {
            return new EggGen.SYMB(Integer.parseInt(ctx.NUMBER().getText()));
        } else if (ctx.gateN.getText().equals("U1")) {
            return new EggGen.U1(visitQubit(ctx.qubit(0)).toString(), (Expr) visit(ctx.expr(0)));
        } else if (ctx.gateN.getText().equals("U2")) {
            return new EggGen.U2(visitQubit(ctx.qubit(0)).toString(), (Expr) visit(ctx.expr(0)), (Expr) visit(ctx.expr(1)));
        } else if (ctx.gateN.getText().equals("U3")) {
            return new EggGen.U3(visitQubit(ctx.qubit(0)).toString(), (Expr) visit(ctx.expr(0)), (Expr) visit(ctx.expr(1)), (Expr) visit(ctx.expr(2)));
        } else if (ctx.gateN.getText().equals("RX")) {
            return new EggGen.RX(visitQubit(ctx.qubit(0)).toString(), (Expr) visit(ctx.expr(0)));
        } else if (ctx.gateN.getText().equals("CZ")) {
            return new EggGen.CZ(visitQubit(ctx.qubit(0)).toString(), visitQubit(ctx.qubit(1)).toString());
        } else if (ctx.gateN.getText().equals("RY")) {
            return new EggGen.RY(visitQubit(ctx.qubit(0)).toString(), (Expr) visit(ctx.expr(0)));
        } else if (ctx.gateN.getText().equals("RXX")) {
            return new EggGen.RXX(visitQubit(ctx.qubit(0)).toString(), visitQubit(ctx.qubit(1)).toString(), (Expr) visit(ctx.expr(0)));
        } else if (ctx.gateN.getText().equals("GPI")) {
            return new EggGen.GPI(visitQubit(ctx.qubit(0)).toString(), (Expr) visit(ctx.expr(0)));
        } else if (ctx.gateN.getText().equals("GPI2")) {
            return new EggGen.GPI2(visitQubit(ctx.qubit(0)).toString(), (Expr) visit(ctx.expr(0)));
        } else if (ctx.gateN.getText().equals("VZ")) {
            return new EggGen.VZ(visitQubit(ctx.qubit(0)).toString(), (Expr) visit(ctx.expr(0)));
        } else if (ctx.gateN.getText().equals("MS")) {
            return new EggGen.MS(visitQubit(ctx.qubit(0)).toString(), visitQubit(ctx.qubit(1)).toString(), (Expr) visit(ctx.expr(0)), (Expr) visit(ctx.expr(1)));
        } else if (ctx.gateN.getText().equals("SX")) {
            return new EggGen.SX(visitQubit(ctx.qubit(0)).toString());
        }
        throw new IllegalArgumentException("Unknown gate: " + ctx.getText());
    }

    @Override
    public String visitQubit(EggParser.QubitContext ctx) {
        return ctx.STRING().getText().replace("\"", "");
    }

    @Override
    public Expr visitExpr(EggParser.ExprContext ctx) {
        if (ctx.cons.getText().equals("Symbol")) {
            return new Symbol(ctx.STRING().getText().replace("\"", ""));
        } else if (ctx.cons.getText().equals("Bool")) {
            return new Bool(Boolean.parseBoolean(ctx.BOOLEAN().getText()));
        } else if (ctx.cons.getText().equals("Real")) {
            return new Real(Double.parseDouble(ctx.F64().getText()));
        } else if (ctx.cons.getText().equals("Fun")) {
            return new Fun(ctx.STRING().getText().replace("\"", ""), (Expr) visit(ctx.expr(0)));
        } else if (ctx.cons.getText().equals("UnOp")) {
            return new UnOp(Expr.Op.valueOf(visitOp(ctx.op())), (Expr) visit(ctx.expr(0)));
        } else if (ctx.cons.getText().equals("Binop")) {
            return new BinOp(Expr.Op.valueOf(visitOp(ctx.op())), (Expr) visit(ctx.expr(0)), (Expr) visit(ctx.expr(1)));
        } else if (ctx.cons.getText().equals("Var")) {
            return new Var(ctx.STRING().getText().replace("\"", ""));
        }
        throw new IllegalArgumentException("Unknown expression: " + ctx.getText());
    }

    @Override
    public String visitOp(EggParser.OpContext ctx) {
        return ctx.getText();
    }
}
