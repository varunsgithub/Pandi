package com.craftinginterpreters.pandi;

import java.util.HashMap;
import java.util.Map;

//Class for storing the state of the variables.
public class Environment {
    //This field helps the environment keep a track of the previous environment
    final Environment enclosing;

    private final Map<String, Object> values = new HashMap<>();


    //Creating an empty constructor to initialise the variable (This is for the outermost class)
    // Because the global class does not have a scope to it.
    Environment() {
        this.enclosing = null;
    }

    //Creating a constructor to store the value of an incoming nested class
    Environment(Environment enclosing) {
        this.enclosing = enclosing;
    }


    //We need to bind the environment to store the variables.
    //So it needs to hook the variables to a value, uniqueness to be maintained......

    Object get(Token name) {
        if (values.containsKey(name.lexeme)) {
            return values.get(name.lexeme);
        }

        //The beautiful line of code, if the value was not found, it will go one level
        // up to the enclosing environment's reference, get the name if it is present in there and try to reach uptop !!!!!
        // until the enclosing is actually null (global variable's enclosing scope)

        if (enclosing != null) {
            return enclosing.get(name);
        }

        throw new RuntimeError(name, "Undefined variable '" + name.lexeme + ".");
    }

    //The following method is useful for assigning get
    void assign(Token name, Object value) {
        //If the hashmap already contains the key then add the value
        if (values.containsKey(name.lexeme)) {
            values.put(name.lexeme, value);
            return;
        }

        //same logic as get
        if (enclosing != null) {
            enclosing.assign(name, value);
            return;
        }

        //else throw runtimeerror
        throw new RuntimeError(name, "Undefined variable '" + name.lexeme + ".");
    }



    void define(String name, Object value) {
        //A new name will bind the value to the name !
        // Now the moment you redefine the variable -> it will replace the variable !!!
        values.put(name, value);
    }

    Object getAt(int distance, String name) {
        return ancestor(distance).values.get(name);
    }

    Environment ancestor(int distance) {
        Environment environment = this;

        //First reach the current environment
        for (int i = 0; i < distance; i++) {
            //Then as you know the distance from the current keep
            // looping inside that environment
            environment = environment.enclosing;
        }

        //Return that environment
        return environment;
    }

    void assignAt(int distance, Token name, Object value) {
        ancestor(distance).values.put(name.lexeme, value);
    }

}
