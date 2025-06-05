package com.craftinginterpreters.cit;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

// Importing the enum class ! (Saves me from writing TokenType.XYZ everywhere in the code :)
import static com.craftinginterpreters.cit.TokenType.*;

//Scanner class to scan the characters and do lexical analysis !

public class Scanner {
    private final String source;
    //The list of tokens is designed as an array list !
    private final List<Token> tokens = new ArrayList<>();

    //for position tracking inside the source code:
    private int start = 0;
    private int current = 0;
    private int line = 1;



    //Constructor to initialize the source (String for source code written)
    public Scanner(String source) {
        this.source = source;
    }

    //The method actually ends up scanning the source string into tokens :)
    public List<Token> scanTokens() {
        while (!isAtEnd()) {
            // we are at the beginning of the next lexeme
            start = current;
            scanToken();
        }

        //After the scanner does the lexical analysis of the source it ends up adding the EOF.
        // EOF is -> EOF enum, empty lexeme, the literal is null and line number !!
        tokens.add(new Token(EOF, "", null, line));

        return tokens;
    }

    //Method to scan an individual token
    private void scanToken() {

        char c = advance();

        switch (c) {
            case '(':
                addToken(LEFT_PAREN);break;
            case ')':
                addToken(RIGHT_PAREN);break;
            case '{':
                addToken(LEFT_BRACE);break;
            case '}':
                addToken(RIGHT_BRACE);break;
            case ',':
                addToken(COMMA);break;
            case '.':
                addToken(DOT);break;
            case '-':
                addToken(MINUS);break;
            case '+':
                addToken(PLUS);break;
            case ':':
                addToken(SEMICOLON);break;
            case '*':
                addToken(STAR);break;
        }
    }

    //Helper function isAtEnd() helps assess whether we have consumed all characters:
    private boolean isAtEnd() {
        return current >= source.length();
    }

    //The advance method helps find the character at the specific position:
    private char advance() {
        return source.charAt(current);
    }

    // the helper method creates a Token object for the tokens scanned:
    private void addToken(TokenType type) {
        addToken(type, null);
    }

    private void addToken(TokenType type, Object literal) {
        String text = source.substring(start, current);
        tokens.add(new Token(type, text, literal, line));
    }
}
