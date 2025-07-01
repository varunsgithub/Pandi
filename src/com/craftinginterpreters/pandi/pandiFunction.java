package com.craftinginterpreters.pandi;

import java.util.List;


public class pandiFunction implements pandiCallable {
    private final Stmt.Function declaration;


    pandiFunction(Stmt.Function declaration) {
        this.declaration = declaration;
    }


    @Override
    public int arity() {
        // This will check if the parameters and the function declaration have the same size
        return declaration.params.size();
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        //The function's environment is tagged to the outermost global environment of the interpreter
        Environment environment = new Environment(interpreter.globals);

        for (int i = 0; i < declaration.params.size(); i++) {
            //In that environment define the name of the parameters and the arguments.
            environment.define(declaration.params.get(i).lexeme, arguments.get(i));
        }

        //The try catch block actually helps jumping out of the function calls if it hits a return value
        try {
            //The block (the function) is executed in the given environment.
            interpreter.executeBlock(declaration.body, environment);
        } catch (Return returnValue) {
            return returnValue.value;
        }

        //This is in case the function does not have a return statement, it returns null by default.
        return null;
    }

    @Override
    public String toString() {
        //Return the function name's lexeme ...
        return "<fn " + declaration.name.lexeme + ">";
    }
}
