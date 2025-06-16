package com.craftinginterpreters.pandi;

//This class is typically used for runtime error reporting

public class RuntimeError extends RuntimeException {
    final Token token;

    RuntimeError(Token token, String message) {
        super(message);
        this.token = token;
    }

}