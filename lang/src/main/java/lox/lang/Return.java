package lox.lang;

import lombok.Getter;

class Return extends RuntimeException {
    @Getter
    private final Object value;

    Return(Object value) {
        super(null, null, false, false);
        this.value = value;
    }
}
