package lox.lang;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;

@Getter
@AllArgsConstructor
class LoxClass implements LoxCallable {
    private final @NonNull String name;

    @Override
    public String toString() {
        return "class " + name;
    }

    @Override
    public int arity() {
        return 0;
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        var instance = new LoxInstance(this);
        return instance;
    }
}
