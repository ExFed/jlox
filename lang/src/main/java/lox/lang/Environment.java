package lox.lang;

import java.util.HashMap;
import java.util.Map;

import lombok.ToString;

@ToString
class Environment {

    private static final Object UNDEFINED = new Object();

    private final Environment enclosing;
    private final Map<String, Object> values = new HashMap<>();

    Environment() {
        this.enclosing = null;
    }

    Environment(Environment enclosing) {
        this.enclosing = enclosing;
    }

    void declare(String name) {
        define(name, UNDEFINED);
    }

    void define(String name, Object value) {
        values.put(name, value);
    }

    Environment ancestor(int distance) {
        var environment = this;
        for (int i = 0; i < distance; i++) {
            environment = environment.enclosing;
        }
        return environment;
    }

    Object getAt(int distance, String name) {
        return ancestor(distance).values.get(name);
    }

    void assignAt(int distance, Token name, Object value) {
        ancestor(distance).values.put(name.getLexeme(), value);
    }

    Object get(Token name) {
        if (values.containsKey(name.getLexeme())) {
            var value = values.get(name.getLexeme());
            if (value == UNDEFINED) {
                throw new RuntimeError(name, "Undefined variable '" + name.getLexeme() + "'.");
            }
            return value;
        }

        if (enclosing != null) {
            return enclosing.get(name);
        }

        throw new RuntimeError(name, "Undeclared variable '" + name.getLexeme() + "'.");
    }

    void assign(Token name, Object value) {
        if (values.containsKey(name.getLexeme())) {
            values.put(name.getLexeme(), value);
            return;
        }

        if (enclosing != null) {
            enclosing.assign(name, value);
            return;
        }

        throw new RuntimeError(name, "Undeclared variable '" + name.getLexeme() + "'.");
    }
}
