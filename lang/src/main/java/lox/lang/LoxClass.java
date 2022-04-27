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
        return declaration.getParams().size();
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        var declParams = declaration.getParams();
        var instance = new LoxInstance(this);
        for (int i = 0; i < declParams.size(); i++) {
            instance.getFields().put(declParams.get(i).getLexeme(), arguments.get(i));
        }
        var environment = new Environment(closure);
        environment.define("this", instance);
        interpreter.executeBlock(declaration.getInit(), environment);
        return instance;
    }
}
