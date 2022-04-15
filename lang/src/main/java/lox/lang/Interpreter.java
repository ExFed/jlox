package lox.lang;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;

public class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<CallResult> {

    @Getter
    private final Environment environment;

    Interpreter() {
        this(new Environment());

        // globals
        this.environment.define("clock", new LoxCallable() {
            @Override
            public int arity() { return 0; }

            @Override
            public Object call(Interpreter interpreter, List<Object> arguments) {
                return System.currentTimeMillis() / 1000d;
            }

            @Override
            public String toString() { return "<native fn>"; }
        });
    }

    Interpreter(Environment environment) {
        this.environment = environment;
    }

    Interpreter push() {
        return new Interpreter(new Environment(environment));
    }

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
    public Object visitGroupingExpr(Expr.Grouping expr) {
        return evaluate(expr.getExpression());
    }

    @Override
    public Object visitLiteralExpr(Expr.Literal expr) {
        return expr.getValue();
    }

    @Override
    public Object visitLogicalExpr(Expr.Logical expr) {
        var left = evaluate(expr.getLeft());
        var operator = expr.getOperator();
        var leftIsTruthy = isTruthy(left);

        if (operator.getType() == TokenType.OR) {
            if (leftIsTruthy) {
                return left;
            }
        } else if (operator.getType() == TokenType.AND) {
            if (!leftIsTruthy) {
                return left;
            }
        } else {
            throw new UnsupportedOperationException("unsupported operation: " + operator);
        }

        return evaluate(expr.getRight());
    }

    @Override
    public Object visitVariableExpr(Expr.Variable expr) {
        return environment.get(expr.getName());
    }

    @Override
    public Object visitUnaryExpr(Expr.Unary expr) {
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
    public Object visitBinaryExpr(Expr.Binary expr) {
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
    public Object visitCallExpr(Expr.Call expr) {
        var callee = evaluate(expr.getCallee());
        var arguments = new ArrayList<Object>();
        for (var argument : expr.getArguments()) {
            arguments.add(evaluate(argument));
        }

        if (!(callee instanceof LoxCallable)) {
            throw new RuntimeError(expr.getParen(), "Not callable.");
        }

        var function = (LoxCallable) callee;
        if (arguments.size() != function.arity()) {
            throw new RuntimeError(expr.getParen(),
                    "Expected " + function.arity() + " arguments but got " + arguments.size() + ".");
        }

        return function.call(this, arguments);
    }

    @Override
    public Object visitTernaryExpr(Expr.Ternary expr) {
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
    public CallResult visitBlockStmt(Stmt.Block stmt) {
        return executeBlock(stmt.getStatements());
    }

    @Override
    public CallResult visitExpressionStmt(Stmt.Expression stmt) {
        var value = evaluate(stmt.getExpression());
        return new CallResult(false, value);
    }

    @Override
    public CallResult visitFunctionStmt(Stmt.Function stmt) {
        var function = new LoxFunction(stmt);
        environment.define(stmt.getName().getLexeme(), function);
        return new CallResult(false, function);
    }

    @Override
    public CallResult visitIfStmt(Stmt.If stmt) {
        if (isTruthy(evaluate(stmt.getCondition()))) {
            return execute(stmt.getThenBranch());
        } else if (stmt.getElseBranch() != null) {
            return execute(stmt.getElseBranch());
        }
        return new CallResult(false, null);
    }

    @Override
    public CallResult visitPrintStmt(Stmt.Print stmt) {
        var value = evaluate(stmt.getExpression());
        System.out.println(stringify(value));
        return new CallResult(false, value);
    }

    @Override
    public CallResult visitReturnStmt(Stmt.Return stmt) {
        var value = stmt.getValue() == null ? null : evaluate(stmt.getValue());
        return new CallResult(true, value);
    }

    @Override
    public CallResult visitVarStmt(Stmt.Var stmt) {
        if (stmt.getInitializer() != null) {
            var value = evaluate(stmt.getInitializer());
            environment.define(stmt.getName().getLexeme(), value);
            return new CallResult(false, value);
        } else {
            environment.declare(stmt.getName().getLexeme());
            return new CallResult(false, null);
        }
    }

    @Override
    public CallResult visitWhileStmt(Stmt.While stmt) {
        var result = new CallResult(false, null);
        while (isTruthy(evaluate(stmt.getCondition())) && !result.isReturning()) {
            result = execute(stmt.getBody());
        }
        return result;
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

    private CallResult execute(Stmt stmt) {
        return stmt.accept(this);
    }

    CallResult executeBlock(List<Stmt> statements) {
        var inner = push();
        var result = new CallResult(false, null);
        for (var statement : statements) {
            result = inner.execute(statement);
            if (result.isReturning()) {
                break;
            }
        }
        return result;
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
        if (obj == null) {
            return "nil";
        }

        var text = obj.toString();
        if (obj instanceof Double && text.endsWith(".0")) {
            text = text.substring(0, text.length() - 2);
        }

        return text;
    }
}
