package lox.lang;

import java.util.Arrays;
import java.util.List;

import lox.lang.Expr.Binary;
import lox.lang.Expr.Ternary;
import lox.lang.Expr.Grouping;
import lox.lang.Expr.Literal;
import lox.lang.Expr.Unary;
import lox.lang.Expr.Variable;
import lox.lang.Stmt.Block;
import lox.lang.Stmt.Expression;
import lox.lang.Stmt.Print;
import lox.lang.Stmt.Var;

public class AstPrinter implements Expr.Visitor<String>, Stmt.Visitor<String> {

    public String print(Expr expr) {
        return expr.accept(this);
    }

    public String print(List<Stmt> statements) {
        var builder = new StringBuilder();
        for (var stmt : statements) {
            builder.append(stmt.accept(this)).append("\n");
        }
        return builder.toString();
    }

    @Override
    public String visitBinaryExpr(Binary expr) {
        return parenthesizeExprs(expr.getOperator().getLexeme(), expr.getLeft(), expr.getRight());
    }

    @Override
    public String visitGroupingExpr(Grouping expr) {
        return parenthesizeExprs("group", expr.getExpression());
    }

    @Override
    public String visitLiteralExpr(Literal expr) {
        if (expr.getValue() == null) {
            return "nil";
        }
        if (expr.getValue() instanceof String) {
            return "\"" + expr.getValue() + "\"";
        }
        return expr.getValue().toString();
    }

    @Override
    public String visitUnaryExpr(Unary expr) {
        return parenthesizeExprs(expr.getOperator().getLexeme(), expr.getRight());
    }

    @Override
    public String visitTernaryExpr(Ternary expr) {
        return parenthesizeExprs(
                expr.getLeftOp().getLexeme() + expr.getRightOp().getLexeme(),
                expr.getLeft(),
                expr.getMiddle(),
                expr.getRight());
    }

    @Override
    public String visitVariableExpr(Variable expr) {
        return parenthesizeExprs("variable " + expr.getName().getLexeme());
    }

    @Override
    public String visitAssignExpr(Expr.Assign expr) {
        return parenthesizeExprs("= " + expr.getName().getLexeme(), expr.getValue());
    }

    @Override
    public String visitBlockStmt(Block stmt) {
        return parenthesizeStmts("block", stmt.getStatements());
    }

    @Override
    public String visitExpressionStmt(Expression stmt) {
        return parenthesizeExprs("stmt", stmt.getExpression());
    }

    @Override
    public String visitPrintStmt(Print stmt) {
        return parenthesizeExprs("print", stmt.getExpression());
    }

    @Override
    public String visitVarStmt(Var stmt) {
        var name = "var " + stmt.getName().getLexeme();
        var init = stmt.getInitializer();
        if (init == null) {
            return parenthesizeExprs(name + " nil");
        }
        return parenthesizeExprs(name, stmt.getInitializer());
    }

    private String parenthesizeExprs(String name, Expr... exprs) {
        return parenthesizeExprs(name, Arrays.asList(exprs));
    }

    private String parenthesizeExprs(String name, List<Expr> exprs) {
        var builder = new StringBuilder();
        builder.append("(").append(name);
        for (var expr : exprs) {
            builder.append(" ").append(expr.accept(this));
        }
        return builder.append(")").toString();
    }

    private String parenthesizeStmts(String name, Stmt... stmts) {
        return parenthesizeStmts(name, Arrays.asList(stmts));
    }

    private String parenthesizeStmts(String name, List<Stmt> stmts) {
        var builder = new StringBuilder();
        builder.append("{").append(name).append(" ");
        for (var stmt : stmts) {
            builder.append(stmt.accept(this)).append("; ");
        }
        return builder.append("}").toString();
    }
}
