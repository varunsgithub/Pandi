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
        return 0;
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        //When the class is called, a new instance of the class is created
        pandiInstance instance = new pandiInstance(this);
        return instance;
    }
}