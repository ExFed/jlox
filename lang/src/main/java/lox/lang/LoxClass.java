package lox.lang;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.Getter;

@Getter
class LoxClass implements LoxCallable {
    private final Stmt.Class declaration;
    private final Environment closure;
    private final Map<String, LoxFunction> methods = new HashMap<>();

    public LoxClass(Stmt.Class declaration, Environment closure) {
        this.declaration = declaration;
        this.closure = closure;

        for (var method : declaration.getMethods()) {
            var function = new LoxFunction(method, closure);
            methods.put(method.getName().getLexeme(), function);
        }
    }

    LoxFunction findMethod(String name) {
        if (methods.containsKey(name)) {
            return methods.get(name);
        }

        return null;
    }

    @Override
    public String toString() {
        return "class " + declaration.getName().getLexeme();
    }

    @Override
    public int arity() {
        return 0;
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        var instance = new LoxInstance(this);
        var environment = new Environment(closure);
        environment.define("this", instance);
        return instance;
    }
}
