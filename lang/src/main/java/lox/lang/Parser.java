package lox.lang;

import java.util.ArrayList;
import java.util.List;

import static lox.lang.TokenType.*;

class Parser {
    private static class ParseError extends RuntimeException {
    }

    private final List<Token> tokens;
    private int current = 0;

    Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    List<Stmt> parse() {
        var statements = new ArrayList<Stmt>();
        while (!isAtEnd()) {
            statements.add(declaration());
        }
        return statements;
    }

    private Stmt declaration() {
        try {
            if (match(CLASS)) {
                return classDeclaration();
            }
            if (match(VAR)) {
                return varDeclaration();
            }
            if (check(FUN) && checkNext(IDENTIFIER)) {
                advance();
                return function("function");
            }

            return statement();
        } catch (ParseError error) {
            synchronize();
            return null;
        }
    }

    private Stmt classDeclaration() {
        var name = consume(IDENTIFIER, "Expect class name.");
        var parameters = match(PAREN_LEFT) ? parameters() : List.<Token>of();

        consume(BRACE_LEFT, "Expect '{' before class body.");

        var methods = new ArrayList<Stmt.Function>();
        while (!check(BRACE_RIGHT) && !isAtEnd()) {
            methods.add(function("method"));
        }
        consume(BRACE_RIGHT, "Expect '}' after class body.");

        return new Stmt.Class(name, parameters, methods);
    }

    private List<Token> parameters() {
        ArrayList<Token> parameters = new ArrayList<Token>();
        if (!check(PAREN_RIGHT)) {
            do {
                if (parameters.size() >= 255) {
                    error(peek(), "Cannot declare more than 255 paramters.");
                }
                parameters.add(consume(IDENTIFIER, "Expect parameter name."));
            } while (match(COMMA));
        }
        consume(PAREN_RIGHT, "Expect ')' after parameters.");
        return parameters;
    }

    private Stmt statement() {
        if (match(FOR)) {
            return forStatement();
        }

        if (match(IF)) {
            return ifStatement();
        }

        if (match(PRINT)) {
            return printStatement();
        }

        if (match(RETURN)) {
            return returnStatement();
        }

        if (match(WHILE)) {
            return whileStatement();
        }

        if (match(BRACE_LEFT)) {
            return new Stmt.Block(block());
        }

        return expressionStatement();
    }

    private Stmt forStatement() {
        consume(PAREN_LEFT, "Expect '(' after 'for'.");

        Stmt initializer;
        if (match(SEMICOLON)) {
            initializer = null;
        } else if (match(VAR)) {
            initializer = varDeclaration();
        } else {
            initializer = expressionStatement();
        }

        var condition = check(SEMICOLON) ? new Expr.Literal(true) : expression();
        consume(SEMICOLON, "Expect ';' after for condition.");

        var increment = check(PAREN_RIGHT) ? null : expression();
        consume(PAREN_RIGHT, "Expect ')' after for clauses.");

        var body = statement();
        if (increment != null) {
            var incrementStmtExpr = new Stmt.Expression(increment);
            body = new Stmt.Block(List.of(body, incrementStmtExpr));
        }

        body = new Stmt.While(condition, body);

        if (initializer != null) {
            body = new Stmt.Block(List.of(initializer, body));
        }

        return body;
    }

    private Stmt ifStatement() {
        consume(PAREN_LEFT, "Expect '(' after 'if'.");
        var condition = expression();
        consume(PAREN_RIGHT, "Expect ')' after if condition.");
        var thenBranch = statement();
        var elseBranch = match(ELSE) ? statement() : null;

        return new Stmt.If(condition, thenBranch, elseBranch);
    }

    private Stmt printStatement() {
        var value = expression();
        consume(SEMICOLON, "Expect ';' after expression.");
        return new Stmt.Print(value);
    }

    private Stmt returnStatement() {
        var keyword = previous();
        var value = check(SEMICOLON) ? null : expression();
        consume(SEMICOLON, "Expect ';' after return value.");
        return new Stmt.Return(keyword, value);
    }

    private Stmt whileStatement() {
        consume(PAREN_LEFT, "Expect '(' after 'while'.");
        var condition = expression();
        consume(PAREN_RIGHT, "Expect ')' after while condition.");
        var body = statement();

        return new Stmt.While(condition, body);
    }

    private Stmt varDeclaration() {
        var name = consume(IDENTIFIER, "Expect variable name.");

        Expr initializer = null;
        if (match(EQUAL)) {
            initializer = expression();
        }

        consume(SEMICOLON, "Expect ';' after variable declaration.");
        return new Stmt.Var(name, initializer);
    }

    private Stmt expressionStatement() {
        var expr = expression();
        consume(SEMICOLON, "Expect ';' after expression.");
        return new Stmt.Expression(expr);
    }

    private Stmt.Function function(String kind) {
        var name = consume(IDENTIFIER, "Expect " + kind + " name.");
        var defn = funDefinition(kind);
        return new Stmt.Function(name, defn.getParams(), defn.getBody());
    }

    private List<Stmt> block() {
        var statements = new ArrayList<Stmt>();
        while (!check(BRACE_RIGHT) && !isAtEnd()) {
            statements.add(declaration());
        }
        consume(BRACE_RIGHT, "Expect '}' after block.");
        return statements;
    }

    private Expr assignment() {
        var expr = conditional();
        if (match(EQUAL)) {
            var equals = previous();
            var value = assignment();
            if (expr instanceof Expr.Variable) {
                var name = ((Expr.Variable) expr).getName();
                return new Expr.Assign(name, value);
            } else if (expr instanceof Expr.Get) {
                var get = (Expr.Get) expr;
                return new Expr.Set(get.getObject(), get.getName(), value);
            }
            error(equals, "Invalid assignment target.");
        }
        return expr;
    }

    private Expr expression() {
        return assignment();
    }

    // sequence -> conditional ( ',' conditional ) *
    private Expr sequence() {
        var expr = conditional();

        while (match(COMMA)) {
            var operator = previous();
            var right = conditional();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    // conditional -> equality ( '?' expression ':' expression )?
    private Expr conditional() {
        var expr = or();

        if (match(QUESTION)) {
            var question = previous();
            var truthy = expression();
            consume(COLON, "Expect ':' in ternary conditional.");
            var colon = previous();
            var falsy = expression();
            expr = new Expr.Ternary(expr, question, truthy, colon, falsy);
        }

        return expr;
    }

    private Expr or() {
        var expr = and();

        while (match(OR)) {
            var operator = previous();
            var right = and();
            expr = new Expr.Logical(expr, operator, right);
        }

        return expr;
    }

    private Expr and() {
        var expr = equality();

        while (match(AND)) {
            var operator = previous();
            var right = equality();
            expr = new Expr.Logical(expr, operator, right);
        }

        return expr;
    }

    private Expr equality() {
        var expr = comparison();

        while (match(BANG_EQUAL, EQUAL_EQUAL)) {
            var operator = previous();
            var right = comparison();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr comparison() {
        var expr = term();

        while (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
            var operator = previous();
            var right = term();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr term() {
        var expr = factor();

        while (match(MINUS, PLUS)) {
            var operator = previous();
            var right = factor();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr factor() {
        var expr = unary();

        while (match(SLASH, STAR)) {
            var operator = previous();
            var right = unary();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr unary() {
        if (match(BANG, MINUS)) {
            var operator = previous();
            var right = unary();
            return new Expr.Unary(operator, right);
        } else if (match(PLUS)) {
            var operator = previous();
            unary();
            throw error(operator, "Unsupported unary operator");
        }

        return call();
    }

    private Expr call() {
        var expr = lambda();
        while (true) {
            if (match(PAREN_LEFT)) {
                expr = finishCall(expr);
            } else if (match(DOT)) {
                var name = consume(IDENTIFIER, "Expect property name after '.'.");
                expr = new Expr.Get(expr, name);
            } else {
                break;
            }
        }
        return expr;
    }

    private Expr finishCall(Expr callee) {
        var arguments = new ArrayList<Expr>();
        if (!check(PAREN_RIGHT)) {
            do {
                if (arguments.size() >= 255) {
                    error(peek(), "Cannot have more than 255 arguments.");
                }
                arguments.add(expression());
            } while (match(COMMA));
        }
        var paren = consume(PAREN_RIGHT, "Expect ')' after arguments.");
        return new Expr.Call(callee, paren, arguments);
    }

    private Expr lambda() {
        if (match(FUN)) {
            return funDefinition("lambda");
        }
        return primary();
    }

    private Expr.Lambda funDefinition(String kind) {
        consume(PAREN_LEFT, "Expect '(' before " + kind + " parameters.");
        var parameters = new ArrayList<Token>();
        if (!check(PAREN_RIGHT)) {
            do {
                if (parameters.size() >= 255) {
                    error(peek(), "Cannot declare more than 255 paramters.");
                }
                parameters.add(consume(IDENTIFIER, "Expect parameter name."));
            } while (match(COMMA));
        }
        consume(PAREN_RIGHT, "Expect ')' after parameters.");

        consume(BRACE_LEFT, "Expect '{' before " + kind + " body.");
        var body = block();
        return new Expr.Lambda(parameters, body);
    }

    private Expr primary() {
        if (match(FALSE)) {
            return new Expr.Literal(false);
        }
        if (match(TRUE)) {
            return new Expr.Literal(true);
        }
        if (match(NIL)) {
            return new Expr.Literal(null);
        }
        if (match(NUMBER, STRING)) {
            return new Expr.Literal(previous().getLiteral());
        }
        if (match(THIS)) {
            return new Expr.This(previous());
        }
        if (match(IDENTIFIER)) {
            return new Expr.Variable(previous());
        }
        if (match(PAREN_LEFT)) {
            var expr = expression();
            consume(PAREN_RIGHT, "Expect ')' after expression.");
            return new Expr.Grouping(expr);
        }

        throw error(peek(), "Expect expression.");
    }

    private Token consume(TokenType type, String message) {
        if (check(type))
            return advance();

        throw error(peek(), message);
    }

    private ParseError error(Token token, String message) {
        Lox.error(token, message);
        return new ParseError();
    }

    private void synchronize() {
        advance();

        while (!isAtEnd()) {
            if (previous().getType() == SEMICOLON)
                return;

            switch (peek().getType()) {
                case CLASS:
                case FUN:
                case VAR:
                case FOR:
                case IF:
                case WHILE:
                case PRINT:
                case RETURN:
                    return;
                default:
                    break;
            }

            advance();
        }
    }

    private boolean match(TokenType... types) {
        for (var type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }

        return false;
    }

    private boolean check(TokenType type) {
        return !isAtEnd() && peek().getType() == type;
    }

    private boolean checkNext(TokenType type) {
        var nextType = peekNext().getType();
        return nextType != EOF && nextType == type;
    }

    private Token advance() {
        if (!isAtEnd()) {
            current++;
        }
        var token = previous();
        return token;
    }

    private boolean isAtEnd() {
        return peek().getType() == EOF;
    }

    private Token peek() {
        return tokens.get(current);
    }

    private Token peekNext() {
        if (isAtEnd()) {
            return peek();
        }
        return tokens.get(current + 1);
    }

    private Token previous() {
        return tokens.get(current - 1);
    }
}
