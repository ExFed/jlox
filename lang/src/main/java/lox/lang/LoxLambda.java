package lox.lang;

import java.util.List;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class LoxLambda implements LoxCallable {
    private final List<Token> params;
    private final List<Stmt> body;
    private final Environment closure;

    @Override
    public int arity() {
        return params.size();
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        var environment = new Environment(closure);
        for (int i = 0; i < params.size(); i++) {
            environment.define(params.get(i).getLexeme(), arguments.get(i));
        }

        var result = interpreter.executeBlock(body, environment);
        return result.getValue();
    }

    @Override
    public String toString() {
        return "<fn lambda/" + arity() + ">";
    }
}
