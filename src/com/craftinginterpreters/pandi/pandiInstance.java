package com.craftinginterpreters.pandi;

import java.util.HashMap;
import java.util.Map;

public class pandiInstance {
    private pandiClass klass;
    private final Map<String, Object> fields = new HashMap<>();



    pandiInstance(pandiClass klass) {
        //The class which calls the instance, is replicated
        // and finally is returned
        this.klass = klass;
    }

    Object get(Token name) {
        //if the map storing the fields has the property/ field
        if (fields.containsKey(name.lexeme)) {
            //return it
            return fields.get(name.lexeme);
        }

        pandiFunction method = klass.findMethod(name.lexeme);
        if (method != null) return method;



        //else throw a runtime error
        throw new RuntimeError(name, "Undefined property '" +
                name.lexeme + "'.");
    }

    void set(Token name, Object value) {
        fields.put(name.lexeme, value);
    }


    @Override
    public String toString() {
        return klass.name + " instance";
    }
}
