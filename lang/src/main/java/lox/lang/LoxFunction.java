package lox.lang;

import java.util.List;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class LoxFunction implements LoxCallable {
    private final Stmt.Function declaration;

    @Override
    public int arity() {
        return declaration.getParams().size();
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        var environment = new Environment(interpreter.getEnvironment());
        for (int i = 0; i < declaration.getParams().size(); i++) {
            environment.define(declaration.getParams().get(i).getLexeme(), arguments.get(i));
        }
        new Interpreter(environment).executeBlock(declaration.getBody());
        return null;
    }

    @Override
    public String toString() {
        return "<fn " + declaration.getName().getLexeme() + ">";
    }
}
