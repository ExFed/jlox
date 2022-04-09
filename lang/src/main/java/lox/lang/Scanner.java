package lox.lang;

import static java.util.Map.entry;
import static lox.lang.TokenType.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
class Scanner {

    private static final Map<String, TokenType> keywords;

    static {
        keywords = Map.ofEntries(
            entry("and",    AND),
            entry("class",  CLASS),
            entry("else",   ELSE),
            entry("false",  FALSE),
            entry("for",    FOR),
            entry("fun",    FUN),
            entry("if",     IF),
            entry("nil",    NIL),
            entry("or",     OR),
            entry("print",  PRINT),
            entry("return", RETURN),
            entry("super",  SUPER),
            entry("this",   THIS),
            entry("true",   TRUE),
            entry("var",    VAR),
            entry("while",  WHILE)
        );
    }

    private final @NonNull String source;
    private final List<Token> tokens = new ArrayList<>();

    private int start = 0;
    private int current = 0;
    private int line = 1;

    List<Token> scanTokens() {
        while (!isAtEnd()) {
            start = current;
            scanToken();
        }

        tokens.add(new Token(EOF, "", null, line));
        return tokens;
    }

    private boolean isAtEnd() {
        return current >= source.length();
    }

    private void scanToken() {
        var c = advance();
        switch (c) {
        case '(':
            addToken(PAREN_LEFT);
            break;
        case ')':
            addToken(PAREN_RIGHT);
            break;
        case '{':
            addToken(BRACE_LEFT);
            break;
        case '}':
            addToken(BRACE_RIGHT);
            break;
        case ',':
            addToken(COMMA);
            break;
        case '.':
            addToken(DOT);
            break;
        case '-':
            addToken(MINUS);
            break;
        case '+':
            addToken(PLUS);
            break;
        case ';':
            addToken(SEMICOLON);
            break;
        case '*':
            addToken(STAR);
            break;
        case '!':
            addToken(match('=') ? BANG_EQUAL : BANG);
            break;
        case '=':
            addToken(match('=') ? EQUAL_EQUAL : EQUAL);
            break;
        case '<':
            addToken(match('=') ? LESS_EQUAL : LESS);
            break;
        case '>':
            addToken(match('=') ? GREATER_EQUAL : GREATER);
            break;
        case '/':
            if (match('/')) {
                while (peek() != '\n' && !isAtEnd()) {
                    advance();
                }
            } else {
                addToken(SLASH);
            }
            break;

        // whitespace
        case ' ':
        case '\r':
        case '\t':
            break;

        case '\n':
            line++;
            break;

        case '"':
            string();
            break;

        default:
            if (isDigit(c)) {
                number();
            } else if (isAlpha(c)) {
                identifier();
            } else {
                Lox.error(line, "Unexpected character: '" + String.valueOf(c) + "'");
            }
        }
    }

    private static boolean isAlphaNumeric(char c) {
        return isAlpha(c) || isDigit(c);
    }

    private static boolean isAlpha(char c) {
        return (c >= 'a' && c <= 'z')
            || (c >= 'A' && c <= 'Z')
            || c == '_';
    }

    private static boolean isDigit(char c) {
        return c >= '0' && c <='9';
    }

    private void identifier() {
        while (isAlphaNumeric(peek())) {
            advance();
        }
        var text = source.substring(start, current);
        var type = keywords.getOrDefault(text, IDENTIFIER);
        addToken(type);
    }

    private void number() {
        while (isDigit(peek())) {
            advance();
        }

        // is decimal?
        if (peek() == '.' && isDigit(peekNext())) {
            // consume the decimal
            advance();

            while (isDigit(peek())) {
                advance();
            }
        }

        addToken(NUMBER, Double.parseDouble(source.substring(start, current)));
    }

    private void string() {
        while (peek() != '"' && !isAtEnd()) {
            if (peek() == '\n') {
                line++;
            }
            advance();
        }
        if (isAtEnd()) {
            Lox.error(line, "Unterminated string.");
            return;
        }
        advance();
        var value = source.substring(start + 1, current - 1);
        addToken(STRING, value);
    }

    private char advance() {
        return source.charAt(current++);
    }

    private boolean match(char expected) {
        if (isAtEnd()) {
            return false;
        }
        if (source.charAt(current) != expected) {
            return false;
        }

        current++;
        return true;
    }

    private char peek() {
        if (isAtEnd()) {
            return '\0';
        }
        return source.charAt(current);
    }

    private char peekNext() {
        if (current + 1 >= source.length()) {
            return '\0';
        }
        return source.charAt(current + 1);
    }

    private void addToken(TokenType type) {
        addToken(type, null);
    }

    private void addToken(TokenType type, Object literal) {
        var text = source.substring(start, current);
        tokens.add(new Token(type, text, literal, line));
    }
}
