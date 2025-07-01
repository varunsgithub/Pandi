package com.craftinginterpreters.pandi;

import java.util.List;

//This is the interface that every function has to implement to achieve callability
interface pandiCallable {
    //This field is to keep a track of all the arguments that are expected by a given function
    int arity();

    //The job of this method is to return the value that the callee produces
    // That is to evaluate the results of the callee !!
    Object call(Interpreter interpreter, List<Object> arguments);

}
