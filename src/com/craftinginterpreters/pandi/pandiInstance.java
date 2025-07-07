package com.craftinginterpreters.pandi;

import java.util.HashMap;
import java.util.Map;

public class pandiInstance {
    private pandiClass klass;

    pandiInstance(pandiClass klass) {
        //The class which calls the instance, is replicated
        // and finally is returned
        this.klass = klass;
    }

    @Override
    public String toString() {
        return klass.name + " instance";
    }
}
