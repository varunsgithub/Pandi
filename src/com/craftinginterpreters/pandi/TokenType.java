package com.craftinginterpreters.pandi;


//Enums are fixed variable naming conventions.
// We are using enums to store the reserved lexeme
// ENUMS ARE STORED IN UPPER CASE AND ARE ACCESSED by .
// so enum obj;.... obj.LEFT_PAREN.....

enum TokenType {
    //Single character
    LEFT_PAREN, RIGHT_PAREN, LEFT_BRACE, RIGHT_BRACE,
    COMMA, DOT, MINUS, PLUS, SEMICOLON, SLASH, STAR,

    //One or two character tokens.
    BANG, BANG_EQUAL, EQUAL, EQUAL_EQUAL, GREATER, GREATER_EQUAL, LESS, LESS_EQUAL,

    //Literals
    IDENTIFIER, STRING, NUMBER,

    //Keywords:
    AND, CLASS, ELSE, FALSE, FUN, FOR, IF, NIL, OR,
    PRINT, RETURN, SUPER, THIS, TRUE, VAR,WHILE,

    //Easter eggs:
    VNM,VARUN, PANDI,

    EOF
}
