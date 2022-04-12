package lox.lang;

import lox.lang.Expr.Binary;
import lox.lang.Expr.Ternary;
import lox.lang.Expr.Grouping;
import lox.lang.Expr.Literal;
import lox.lang.Expr.Unary;
import lox.lang.Expr.Variable;

public class AstPrinter implements Expr.Visitor<String> {

    public String print(Expr expr) {
        return expr.accept(this);
    }

    @Override
    public String visitBinaryExpr(Binary expr) {
        return parenthesize(expr.getOperator().getLexeme(), expr.getLeft(), expr.getRight());
    }

    @Override
    public String visitGroupingExpr(Grouping expr) {
        return parenthesize("group", expr.getExpression());
    }

    @Override
    public String visitLiteralExpr(Literal expr) {
        if (expr.getValue() == null) {
            return "nil";
        }
        return expr.getValue().toString();
    }

    @Override
    public String visitUnaryExpr(Unary expr) {
        return parenthesize(expr.getOperator().getLexeme(), expr.getRight());
    }

    @Override
    public String visitTernaryExpr(Ternary expr) {
        return parenthesize(expr.getLeftOp().getLexeme() + expr.getRightOp().getLexeme(), expr.getLeft(), expr.getMiddle(), expr.getRight());
    }

    @Override
    public String visitVariableExpr(Variable expr) {
        return parenthesize("variable", expr);
    }

    @Override
    public String visitAssignExpr(Expr.Assign expr) {
        return "(=" + expr.getName().getLexeme() + " " + expr.accept(this) + ")";
    }

    private String parenthesize(String name, Expr... exprs) {
        var builder = new StringBuilder();
        builder.append("(").append(name);
        for (var expr : exprs) {
            builder.append(" ").append(expr.accept(this));
        }
        return builder.append(")").toString();
    }
}
