package lox.lang;

import java.util.HashMap;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
class LoxInstance {
    private final LoxClass loxClass;
    private final Map<String, Object> fields = new HashMap<>();

    public Object get(Token name) {
        if (fields.containsKey(name.getLexeme())) {
            return fields.get(name.getLexeme());
        }
        throw new RuntimeError(name, "Undefined property '" + name.getLexeme() + "'.");
    }

    public Object set(Token name, Object value) {
        fields.put(name.getLexeme(), value);
        return value;
    }

    @Override
    public String toString() {
        return "instance of " + loxClass.getName();
    }
}
