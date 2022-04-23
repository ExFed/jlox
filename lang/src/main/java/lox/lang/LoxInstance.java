package lox.lang;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
class LoxInstance {
    private final LoxClass loxClass;

    @Override
    public String toString() {
        return "instance of " + loxClass.getName();
    }
}
