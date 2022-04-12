package lox.lang;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;

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

        for (;;) {
            System.out.print("> ");
            var line = reader.readLine();
            if (line == null) {
                break;
            }
            run(line);
            hadError = false;
        }

        return 0;
    }

    private static void run(String source) {
        var scanner = new Scanner(source);
        var tokens = scanner.scanTokens();
        var parser = new Parser(tokens);
        var statements = parser.parse();

        if (hadError) return;

        interpreter.interpret(statements);
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
