package lox.lang;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.Getter;

public class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {

    @Getter
    private final Environment globals = new Environment();

    private Environment environment = globals;

    private final Map<Expr, Integer> locals = new HashMap<>();

    Interpreter() {
        // globals
        this.globals.define("clock", new LoxCallable() {
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
    public Object visitGetExpr(Expr.Get expr) {
        var object = evaluate(expr.getObject());
        if (object instanceof LoxInstance) {
            return ((LoxInstance) object).get(expr.getName());
        }

        throw new RuntimeError(expr.getName(), "Only instances have properties.");
    }

    @Override
    public Object visitGroupingExpr(Expr.Grouping expr) {
        return evaluate(expr.getExpression());
    }

    @Override
    public Object visitLambdaExpr(Expr.Lambda expr) {
        return new LoxLambda(expr.getParams(), expr.getBody(), environment);
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
    public Object visitSetExpr(Expr.Set expr) {
        var object = evaluate(expr.getObject());

        if (!(object instanceof LoxInstance)) {
            throw new RuntimeError(expr.getName(), "Only instances have fields.");
        }

        var value = evaluate(expr.getValue());
        return ((LoxInstance) object).set(expr.getName(), value);
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
    public Object visitThisExpr(Expr.This expr) {
        return lookUpVariable(expr.getKeyword(), expr);
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
    public Object visitVariableExpr(Expr.Variable expr) {
        return lookUpVariable(expr.getName(), expr);
    }

    @Override
    public Void visitBlockStmt(Stmt.Block stmt) {
        executeBlock(stmt.getStatements(), new Environment(environment));
        return null;
    }

    @Override
    public Void visitClassStmt(Stmt.Class stmt) {
        var loxClass = new LoxClass(stmt, environment);
        environment.define(stmt.getName().getLexeme(), loxClass);
        return null;
    }

    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt) {
        evaluate(stmt.getExpression());
        return null;
    }

    @Override
    public Void visitFunctionStmt(Stmt.Function stmt) {
        var function = new LoxFunction(stmt, environment);
        environment.define(stmt.getName().getLexeme(), function);
        return null;
    }

    @Override
    public Void visitIfStmt(Stmt.If stmt) {
        if (isTruthy(evaluate(stmt.getCondition()))) {
            execute(stmt.getThenBranch());
        } else if (stmt.getElseBranch() != null) {
            execute(stmt.getElseBranch());
        }
        return null;
    }

    @Override
    public Void visitPrintStmt(Stmt.Print stmt) {
        var value = evaluate(stmt.getExpression());
        System.out.println(stringify(value));
        return null;
    }

    @Override
    public Void visitReturnStmt(Stmt.Return stmt) {
        var value = stmt.getValue() == null ? null : evaluate(stmt.getValue());
        throw new Return(value);
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

    @Override
    public Void visitWhileStmt(Stmt.While stmt) {
        while (isTruthy(evaluate(stmt.getCondition()))) {
            execute(stmt.getBody());
        }
        return null;
    }

    private Object lookUpVariable(Token name, Expr expr) {
        var distance = locals.get(expr);
        if (distance != null) {
            return environment.getAt(distance, name.getLexeme());
        } else {
            return globals.get(name);
        }
    }

    private void checkNumberOperands(Token operator, Object... operands) {
        for (var operand : operands) {
            if (!(operand instanceof Double)) {
                var operandClass = operand != null ? operand.getClass() : null;
                throw new RuntimeError(operator, "Operands must be numbers, got " + operandClass);
            }
        }
    }

    private Object evaluate(Expr expr) {
        return expr.accept(this);
    }

    private void execute(Stmt stmt) {
        stmt.accept(this);
    }

    void resolve(Expr expr, int depth) {
        locals.put(expr, depth);
    }

    void executeBlock(List<Stmt> statements, Environment environment) {
        var previous = this.environment;
        try {
            this.environment = environment;
            for (var statement : statements) {
                execute(statement);
            }
        } finally {
            this.environment = previous;
        }
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
