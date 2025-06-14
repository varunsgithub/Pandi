package com.craftinginterpreters.pandi;

import java.util.List;
import static com.craftinginterpreters.pandi.TokenType.*;

//This class handles parsing.
public class Parser {

    private static class ParseError extends RuntimeException {}

    //The list of tokens that we receive from the scanner !
    private final List<Token> tokens;
    //The tracker for letting us know at what position we are at in parsing the tokens
    private int current;

    Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    Expr parse() {
        try {
            return expression();
        } catch (ParseError error) {
            return null;
        }
    }


    // The lowest precedence -> expression returns equality
    // Each grammar rule is a method and the body of it can contain either
    // a terminal (some action being performed) or a non-terminal (reference to a different rule)
    // the expression has equality referenced which is a non-terminal.
    private Expr expression() {
        return equality();
    }

    //
    private Expr equality() {
        Expr expr = comparison();

        // While the expression match != or ==
        while (match(BANG_EQUAL, EQUAL_EQUAL)) {
            // The operator is the previous token
            Token operator = previous();
            // Expression to right is comparison
            Expr right = comparison();
            //It creates a new binary syntax tree
            // with left node as expr(recursion on itself) , operator and the right node as comparison.
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    //Comparison method: used for >, >=, <, <=
    private Expr comparison() {
        Expr expr = term();

        while (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
            Token operator = previous();
            Expr right = term();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    // term which is used for - and +
    private Expr term() {
        Expr expr = factor();

        while(match(MINUS, PLUS)) {
            Token operator = previous();
            Expr right = factor();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    //factor is used for * and /
    private Expr factor() {
        Expr expr = unary();

        while(match(SLASH, STAR)) {
            Token operator = previous();
            Expr right = unary();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    //Unary operations:
    private Expr unary() {
        if(match(BANG, MINUS)) {
            Token operator = previous();
            Expr right = unary();
            return new Expr.Unary(operator, right);
        }
        return primary();
    }

    //
    private Expr primary() {
        if (match(FALSE)) return new Expr.Literal(false);
        if (match(TRUE)) return new Expr.Literal(true);
        if (match(NIL)) return new Expr.Literal(null);

        if (match(NUMBER, STRING)) {
            return new Expr.Literal(previous().literal);
        }

        if (match(LEFT_PAREN)) {
            Expr expr = expression();
            consume(RIGHT_PAREN, "Expect ')' after expression.");
            return new Expr.Grouping(expr);
        }

        //Each token statement has to reach the primary case method
        // If it is unable to match any of the primary case then an error is thrown
        // meaning -> we have reached an unexpected expression.
        throw error(peek(), "Expect expression.");


    }

    //The boolean match method will take in a list of
    // token types and check if they match the given types
    private boolean match(TokenType... types) {
        for (TokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }
        return false;
    }

    //The consume method will check if the grouping expression has been closed properly and will advance until it
    // has been closed:
    private Token consume(TokenType type, String message) {
        if (check(type)) return advance();

        //if not then throw an error !
        throw error(peek(), message);
    }


    // The check function will check if the token is EOF
    // if not it will check if the next token type is the required type
    private boolean check (TokenType type) {
        if (isAtEnd()) return false;
        return peek().type == type;
    }

    // The advance token advances the position of current and returns the
    // token at current - 1
    private Token advance() {
        if (!isAtEnd()) current++;
        return previous();
    }

    // The isAtEnd function checks if the type of the token is EOF
    // and returns the boolean for that
    private boolean isAtEnd() {
        return peek().type == EOF;
    }

    // The peek function gets the value of the current position
    // Now if advance is called it advances the counter and returns the
    // token at current - 1.
    private Token peek() {
        return tokens.get(current);
    }

    // Returns the value of the current position - 1
    private Token previous() {
        return tokens.get(current - 1);
    }

    //returning errors using the pandi's parse error method
    private ParseError error (Token token, String message) {
        pandi.error(token, message);
        return new ParseError();
    }

    //Synchronization of errors
    private void synchronize() {
        //Advance to the next token
        advance();

        //while you have not reached the end
        while (!isAtEnd()) {
            //If the previous token was a semicolon then exit
            //because you are at the beginning of a new statement line
            // you can parse from here
            if (previous().type == SEMICOLON) return;


            // in case any of the below are the next token
            // then return to parsing because hopefully these will not be
            // error infested
            switch (peek().type) {
                case CLASS:
                case FOR:
                case FUN:
                case IF:
                case PRINT:
                case RETURN:
                case VAR:
                case WHILE:
                    return;
            }

            // if none of these match then keep advancing.
            advance();
        }
    }
}