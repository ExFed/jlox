package lox.lang;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
class Resolver implements Expr.Visitor<Void>, Stmt.Visitor<Void> {
    private final Interpreter interpreter;
    private final Deque<Map<String, Boolean>> scopes = new ArrayDeque<>();

    private FunctionType currentFunction = FunctionType.NONE;

    @Override
    public Void visitBlockStmt(Stmt.Block stmt) {
        beginScope();
        resolve(stmt.getStatements());
        endScope();
        return null;
    }

    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt) {
        resolve(stmt.getExpression());
        return null;
    }

    @Override
    public Void visitFunctionStmt(Stmt.Function stmt) {
        declare(stmt.getName());
        define(stmt.getName());
        resolveFunction(stmt, FunctionType.FUNCTION);
        return null;
    }

    @Override
    public Void visitIfStmt(Stmt.If stmt) {
        resolve(stmt.getCondition());
        resolve(stmt.getThenBranch());
        if (stmt.getElseBranch() != null) {
            resolve(stmt.getElseBranch());
        }
        return null;
    }

    @Override
    public Void visitPrintStmt(Stmt.Print stmt) {
        resolve(stmt.getExpression());
        return null;
    }

    @Override
    public Void visitReturnStmt(Stmt.Return stmt) {
        if (currentFunction == FunctionType.NONE) {
            Lox.error(stmt.getKeyword(), "Cannot return from top-level code.");
        }
        if (stmt.getValue() != null) {
            resolve(stmt.getValue());
        }
        return null;
    }

    @Override
    public Void visitVarStmt(Stmt.Var stmt) {
        declare(stmt.getName());
        if (stmt.getInitializer() != null) {
            resolve(stmt.getInitializer());
        }
        define(stmt.getName());
        return null;
    }

    @Override
    public Void visitWhileStmt(Stmt.While stmt) {
        resolve(stmt.getCondition());
        resolve(stmt.getBody());
        return null;
    }

    @Override
    public Void visitAssignExpr(Expr.Assign expr) {
        resolve(expr.getValue());
        resolveLocal(expr, expr.getName());
        return null;
    }

    @Override
    public Void visitBinaryExpr(Expr.Binary expr) {
        resolve(expr.getLeft());
        resolve(expr.getRight());
        return null;
    }

    @Override
    public Void visitCallExpr(Expr.Call expr) {
        resolve(expr.getCallee());
        for (var argument : expr.getArguments()) {
            resolve(argument);
        }
        return null;
    }

    @Override
    public Void visitGroupingExpr(Expr.Grouping expr) {
        resolve(expr.getExpression());
        return null;
    }

    @Override
    public Void visitLambdaExpr(Expr.Lambda expr) {
        beginScope();
        for (var param : expr.getParams()) {
            declare(param);
            define(param);
        }
        resolve(expr.getBody());
        endScope();
        return null;
    }

    @Override
    public Void visitLiteralExpr(Expr.Literal expr) {
        return null;
    }

    @Override
    public Void visitLogicalExpr(Expr.Logical expr) {
        resolve(expr.getLeft());
        resolve(expr.getRight());
        return null;
    }

    @Override
    public Void visitTernaryExpr(Expr.Ternary expr) {
        resolve(expr.getLeft());
        resolve(expr.getMiddle());
        resolve(expr.getRight());
        return null;
    }

    @Override
    public Void visitUnaryExpr(Expr.Unary expr) {
        resolve(expr.getRight());
        return null;
    }

    @Override
    public Void visitVariableExpr(Expr.Variable expr) {
        if (!scopes.isEmpty() && scopes.peek().get(expr.getName().getLexeme()) == Boolean.FALSE) {
            Lox.error(expr.getName(), "Can't read local variable within its own initializer.");
        }
        resolveLocal(expr, expr.getName());
        return null;
    }

    void resolve(List<Stmt> statements) {
        for (var statement : statements) {
            resolve(statement);
        }
    }

    private void resolve(Stmt stmt) {
        stmt.accept(this);
    }

    private void resolve(Expr expr) {
        expr.accept(this);
    }

    private void resolveFunction(Stmt.Function function, FunctionType type) {
        beginScope();
        var enclosingFunction = currentFunction;
        currentFunction = type;
        for (var param : function.getParams()) {
            declare(param);
            define(param);
        }
        resolve(function.getBody());
        endScope();
        currentFunction = enclosingFunction;
    }

    private void beginScope() {
        scopes.push(new HashMap<String, Boolean>());
    }

    private void endScope() {
        scopes.pop();
    }

    private void declare(Token name) {
        if (scopes.isEmpty()) {
            return;
        }
        var scope = scopes.peek();
        if (scope.containsKey(name.getLexeme())) {
            Lox.error(name, "Already a variable with this name in scope.");
        }
        scope.put(name.getLexeme(), false);
    }

    private void define(Token name) {
        if (scopes.isEmpty()) {
            return;
        }
        scopes.peek().put(name.getLexeme(), true);
    }

    private void resolveLocal(Expr expr, Token name) {
        var iter = scopes.descendingIterator();
        var depth = 0;
        while (iter.hasNext()) {
            var scope = iter.next();
            if (scope.containsKey(name.getLexeme())) {
                interpreter.resolve(expr, depth);
                return;
            }
            depth++;
        }
    }

    private enum FunctionType {
        NONE,
        FUNCTION
    }
}
