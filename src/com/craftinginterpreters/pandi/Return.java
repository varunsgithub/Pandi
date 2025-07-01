package com.craftinginterpreters.pandi;

public class Return extends RuntimeException {
    final Object value;

    Return(Object value) {
        //The super method stores the
        //1. Message, 2. Throwable, 3. EnableSuppression, 4. writeableStackTrace
        super(null,null,false,false);
        //the value of the return is set to whatever value was passed from the interpreter
        this.value = value;
    }
}
