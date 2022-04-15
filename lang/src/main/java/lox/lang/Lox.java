package lox.lang;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class Lox {

    public static void main(String[] args) throws IOException {
        int exitCode;
        if (args.length > 1) {
            System.out.println("Usage: jlox [script]");
            exitCode = 64;
        } else if (args.length == 1) {
            exitCode = runFile(args[0]);
        } else {
            exitCode = runPrompt();
        }
        System.exit(exitCode);
    }

    private static int runFile(String path) throws IOException {
        byte[] bytes;
        if ("-".equals(path)) {
            bytes = System.in.readAllBytes();
        } else {
            bytes = Files.readAllBytes(Paths.get(path));
        }
        run(new String(bytes, Charset.defaultCharset()));

        if (hadError) {
            return 65;
        }

        if (hadRuntimeError) {
            return 70;
        }

        return 0;
    }

    private static final Interpreter interpreter = new Interpreter();
    static boolean hadError = false;
    static boolean hadRuntimeError = false;

    private static int runPrompt() throws IOException {
        var input = new InputStreamReader(System.in);
        var reader = new BufferedReader(input);

        var unmatchedBraces = 0;
        var lineBuffer = new ArrayList<String>();
        var printTokens = false;
        var printAst = false;
        var printEvaluable = false;
        for (;;) {
            var prompt = String.format("lox:%02d> ", lineBuffer.size());
            System.out.print(prompt);
            var line = reader.readLine();

            if (line == null || ":q".equals(line)) {
                break;
            } else if (":b".equals(line)) {
                int n = 0;
                for (var l : lineBuffer) {
                    System.out.println(String.format("%02d  %s", ++n, l));
                }
            } else if (line.startsWith(":tok ")) {
                var arg = line.substring(5);
                printTokens = Boolean.parseBoolean(arg) || arg.equals("on");
                System.out.println("print tokens: " + (printTokens ? "on" : "off"));
            } else if (line.startsWith(":ast ")) {
                var arg = line.substring(5);
                printAst = Boolean.parseBoolean(arg) || arg.equals("on");
                System.out.println("print ast: " + (printAst ? "on" : "off"));
            } else if (line.startsWith(":pev ")) {
                var arg = line.substring(5);
                printEvaluable = Boolean.parseBoolean(arg) || arg.equals("on");
                System.out.println("print evaluable: " + (printEvaluable ? "on" : "off"));
            } else if (!line.isEmpty()) {
                lineBuffer.add(line);
                unmatchedBraces += countUnmatchedBraces(line);

                // if braces are at least balanced, flush the buffer
                if (unmatchedBraces <= 0) {
                    var source = String.join("\n", lineBuffer);
                    lineBuffer.clear();
                    unmatchedBraces = 0;
                    runConsole(source, printTokens, printAst, printEvaluable);
                }
                hadError = false;
            }

        }

        return 0;
    }

    private static int countUnmatchedBraces(String line) {
        // scan as tokens
        var scanner = new Scanner(line);
        var tokens = scanner.scanTokens();

        // count number of unmatched braces
        int count = 0;
        for (var token : tokens) {
            var type = token.getType();
            if (type.equals(TokenType.BRACE_LEFT)) {
                count++;
            }
            if (type.equals(TokenType.BRACE_RIGHT)) {
                count--;
            }
        }
        return count;
    }

    private static void run(String source) {
        var scanner = new Scanner(source);
        var tokens = scanner.scanTokens();

        var parser = new Parser(tokens);
        var statements = parser.parse();

        if (hadError) {
            return;
        }

        interpreter.interpret(statements);
    }

    private static void runConsole(String source, boolean printTokens, boolean printAst, boolean printEvaluable) {
        var scanner = new Scanner(source);
        var tokens = scanner.scanTokens();

        if (printTokens) {
            for (var token : tokens) {
                System.out.println(token);
            }
        }

        var parser = new Parser(tokens);
        var statements = parser.parse();

        if (hadError) {
            return;
        }

        if (printAst) {
            System.out.println(new AstPrinter().print(statements));
        }

        if (printEvaluable) {
            statements = printLastEvaluable(statements);
        }

        interpreter.interpret(statements);
    }

    private static List<Stmt> printLastEvaluable(List<Stmt> stmts) {
        var statements = new ArrayList<>(stmts);
        // get the final statement, and if it's evaluable, print the value (being careful of side-effects)
        int lastIndex = statements.size() - 1;
        var stmt = lastIndex > 0 ? null : statements.get(lastIndex);
        if (stmt instanceof Stmt.Expression) {
            var expr = ((Stmt.Expression) stmt).getExpression();
            statements.set(lastIndex, printEvaluable(expr));
        }
        if (stmt instanceof Stmt.Var) {
            var varDecl = (Stmt.Var) stmt;
            var expr = varDecl.getInitializer();
            if (expr != null) {
                statements.add(printEvaluable(new Expr.Variable(varDecl.getName())));
            }
        }
        return statements;
    }

    private static Stmt.Print printEvaluable(Expr expr) {
        var prefixExpr = new Expr.Literal("lox:==> ");
        var operator = new Token(TokenType.PLUS, "+", null, -1);
        var printExpr = new Expr.Binary(prefixExpr, operator, expr);
        return new Stmt.Print(printExpr);
    }

    static void error(int line, String message) {
        report(line, "", message);
    }

    static void runtimeError(RuntimeError error) {
        System.err.println(error.getMessage() + "\n[line " + error.getToken().getLine() + "]");
        hadRuntimeError = true;
    }

    private static void report(int line, String where, String message) {
        System.err.println("[line " + line + "] Error" + where + ": " + message);
        hadError = true;
    }

    static void error(Token token, String message) {
        if (token.getType() == TokenType.EOF) {
            report(token.getLine(), " at end", message);
        } else {
            report(token.getLine(), " at '" + token.getLexeme() + "'", message);
        }
    }
}
