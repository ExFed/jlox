package lox.lang;

import lombok.Getter;

class RuntimeError extends RuntimeException {
    @Getter
    private final Token token;

    RuntimeError(Token token, String message) {
        super(message);
        this.token = token;
    }
}
