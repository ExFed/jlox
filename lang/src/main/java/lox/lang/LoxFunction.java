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
    public Object call(Interpreter enclosing, List<Object> arguments) {
        var interpreter = enclosing.push();
        var environment = interpreter.getEnvironment();
        for (int i = 0; i < declaration.getParams().size(); i++) {
            environment.define(declaration.getParams().get(i).getLexeme(), arguments.get(i));
        }
        try {
            interpreter.executeBlock(declaration.getBody());
        } catch (Return returnValue) {
            return returnValue.getValue();
        }
        return null;
    }

    @Override
    public String toString() {
        return "<fn " + declaration.getName().getLexeme() + ">";
    }
}
