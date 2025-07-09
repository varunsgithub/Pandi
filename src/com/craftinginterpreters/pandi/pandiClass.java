package com.craftinginterpreters.pandi;

import java.util.List;
import java.util.Map;

public class pandiClass implements pandiCallable {
    //The name of the class
    final String name;
    private final Map<String, pandiFunction> methods;

    pandiClass(String name, Map<String, pandiFunction> methods) {
        this.name = name;
        this.methods = methods;
    }

    pandiFunction findMethod(String name) {
        if (methods.containsKey(name)) {
            return methods.get(name);
        }

        return null;
    }



    @Override
    public String toString() {
        return name;
    }

    @Override
    public int arity() {
        pandiFunction initializer = findMethod("init");
        if (initializer == null) {return 0;}
        return initializer.arity();
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        //When the class is called, a new instance of the class is created
        pandiInstance instance = new pandiInstance(this);

        pandiFunction initializer = findMethod("init");
        if (initializer != null) {
            //If you find the initializer method declared in the body, then
            // call the interpreter and arguments.
            initializer.bind(instance).call(interpreter, arguments);
        }


        return instance;
    }
}