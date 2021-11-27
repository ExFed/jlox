package lox.lang;

import lombok.NonNull;
import lombok.Value;

@Value
class Token {
    @NonNull TokenType type;
    @NonNull String lexeme;
    Object literal;
    int line;

    public String toString() {
        return type + " " + lexeme + " " + literal;
    }
}
