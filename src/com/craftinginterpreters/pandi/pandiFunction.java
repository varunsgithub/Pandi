package com.craftinginterpreters.pandi;

import java.util.List;


public class pandiFunction implements pandiCallable {
    private final Stmt.Function declaration;
    //This is used to hold onto the variables declared in an enclosing environment
    // This is the environment that is active when the function is DECLARED and not when it is called
    private final Environment closure;

    private final boolean isInitializer;

    pandiFunction(Stmt.Function declaration, Environment closure, boolean isInitializer) {

        this.closure = closure;
        this.declaration = declaration;
        this.isInitializer = isInitializer;
    }

    pandiFunction bind(pandiInstance instance) {
        Environment environment = new Environment(closure);
        environment.define("this", instance);
        return new pandiFunction(declaration, environment, isInitializer);
    }


    @Override
    public int arity() {
        // This will check if the parameters and the function declaration have the same size
        return declaration.params.size();
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        //The function's environment is tagged to the environment that calls it when it was declared.
        Environment environment = new Environment(closure);

        for (int i = 0; i < declaration.params.size(); i++) {
            //In that environment define the name of the parameters and the arguments.
            environment.define(declaration.params.get(i).lexeme, arguments.get(i));
        }

        //The try catch block actually helps jumping out of the function calls if it hits a return value
        try {
            //The block (the function) is executed in the given environment.
            interpreter.executeBlock(declaration.body, environment);
        } catch (Return returnValue) {
            if (isInitializer) {
                return closure.getAt(0, "this");
            }

            return returnValue.value;
        }

        if (isInitializer) return closure.getAt(0, "this");


        //This is in case the function does not have a return statement, it returns null by default.
        return null;
    }

    @Override
    public String toString() {
        //Return the function name's lexeme ...
        return "<fn " + declaration.name.lexeme + ">";
    }
}
