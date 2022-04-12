package lox.lang;

import java.util.List;

import lox.lang.Expr.Binary;
import lox.lang.Expr.Grouping;
import lox.lang.Expr.Literal;
import lox.lang.Expr.Ternary;
import lox.lang.Expr.Unary;

public class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {

    private Environment environment = new Environment();

    public void interpret(List<Stmt> statements) {
        try {
            for (var statement : statements) {
                execute(statement);
            }
        } catch (RuntimeError error) {
            Lox.runtimeError(error);
        }
    }

    @Override
    public Object visitAssignExpr(Expr.Assign expr) {
        var value = evaluate(expr.getValue());
        environment.assign(expr.getName(), value);
        return value;
    }

    @Override
    public Object visitGroupingExpr(Grouping expr) {
        return evaluate(expr.getExpression());
    }

    @Override
    public Object visitLiteralExpr(Literal expr) {
        return expr.getValue();
    }

    @Override
    public Object visitVariableExpr(Expr.Variable expr) {
        return environment.get(expr.getName());
    }

    @Override
    public Object visitUnaryExpr(Unary expr) {
        var operator = expr.getOperator();
        var right = evaluate(expr.getRight());

        switch (operator.getType()) {
            case MINUS:
                checkNumberOperands(expr.getOperator(), right);
                return -(double) right;
            case BANG:
                return !isTruthy(right);
            default:
                throw new UnsupportedOperationException("unsupported operation: " + operator);
        }
    }

    @Override
    public Object visitBinaryExpr(Binary expr) {
        var operator = expr.getOperator();
        var left = evaluate(expr.getLeft());
        var right = evaluate(expr.getRight());

        switch (operator.getType()) {
            case GREATER:
                checkNumberOperands(expr.getOperator(), left, right);
                return (double) left > (double) right;
            case GREATER_EQUAL:
                checkNumberOperands(expr.getOperator(), left, right);
                return (double) left >= (double) right;
            case LESS:
                checkNumberOperands(expr.getOperator(), left, right);
                return (double) left < (double) right;
            case LESS_EQUAL:
                checkNumberOperands(expr.getOperator(), left, right);
                return (double) left <= (double) right;
            case BANG_EQUAL:
                return !isEqual(left, right);
            case EQUAL_EQUAL:
                return isEqual(left, right);
            case MINUS:
                checkNumberOperands(expr.getOperator(), left, right);
                return (double) left - (double) right;
            case SLASH:
                checkNumberOperands(expr.getOperator(), left, right);
                return (double) left / (double) right;
            case STAR:
                checkNumberOperands(expr.getOperator(), left, right);
                return (double) left * (double) right;
            case PLUS:
                if (left instanceof Double && right instanceof Double) {
                    return (double) left + (double) right;
                }

                if (left instanceof String || right instanceof String) {
                    return stringify(left) + stringify(right);
                }

                throw new RuntimeError(expr.getOperator(), "No operation applicable for operands.");
            default:
                throw new UnsupportedOperationException("unsupported operation: " + operator);
        }
    }

    @Override
    public Object visitTernaryExpr(Ternary expr) {
        var leftOp = expr.getLeftOp();
        var rightOp = expr.getRightOp();
        var left = evaluate(expr.getLeft());

        switch (leftOp.getType()) {
            case QUESTION:
                switch (rightOp.getType()) {
                    case COLON:
                        if (isTruthy(left)) {
                            return evaluate(expr.getMiddle());
                        }
                        return evaluate(expr.getRight());
                    default:
                        throw new UnsupportedOperationException("unsupported operation: " + rightOp);
                }
            default:
                throw new UnsupportedOperationException("unsupported operation: " + leftOp);
        }
    }

    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt) {
        evaluate(stmt.getExpression());
        return null;
    }

    @Override
    public Void visitPrintStmt(Stmt.Print stmt) {
        var value = evaluate(stmt.getExpression());
        System.out.println(stringify(value));
        return null;
    }

    @Override
    public Void visitVarStmt(Stmt.Var stmt) {
        if (stmt.getInitializer() != null) {
            var value = evaluate(stmt.getInitializer());
            environment.define(stmt.getName().getLexeme(), value);
        } else {
            environment.declare(stmt.getName().getLexeme());
        }
        return null;
    }

    private void checkNumberOperands(Token operator, Object... operands) {
        for (var operand : operands) {
            if (!(operand instanceof Double)) {
                throw new RuntimeError(operator, "Operands must be numbers");
            }
        }
    }

    private Object evaluate(Expr expr) {
        return expr.accept(this);
    }

    private void execute(Stmt stmt) {
        stmt.accept(this);
    }

    private boolean isTruthy(Object object) {
        if (object == null)
            return false;
        if (object instanceof Boolean)
            return (boolean) object;
        return true;
    }

    private boolean isEqual(Object a, Object b) {
        if (a == null && b == null)
            return true;
        if (a == null)
            return false;
        return a.equals(b);
    }

    private String stringify(Object obj) {
        if (obj == null) return "nil";

        var text = obj.toString();
        if (obj instanceof Double && text.endsWith(".0")) {
            text = text.substring(0, text.length() - 2);
        }

        return text;
    }
}
