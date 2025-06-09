package com.craftinginterpreters.pandi;

import java.util.ArrayList;
import java.util.List;

// Importing the enum class ! (Saves me from writing TokenType.XYZ everywhere in the code :)
import static com.craftinginterpreters.pandi.TokenType.*;

//Scanner class to scan the characters and do lexical analysis !

public class Scanner {
    private final String source;
    //The list of tokens is designed as an array list !
    private final List<Token> tokens = new ArrayList<>();

    //the start of the token
    private int start = 0;
    // If the token is a hit in the scan tokens method the current points to the end of the token,
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

            //These are ingeniously designed operators which actually check, if there are two characters or one
            //If there are 2, the code will automatically mark the token as the 2char tok, else just one char tok
            case '!':
                addToken(match('=') ? BANG_EQUAL:BANG); break;
            case '=':
                addToken(match('=') ? EQUAL_EQUAL:EQUAL); break;
            case '<':
                addToken(match('=') ? LESS_EQUAL:LESS); break;
            case '>':
                addToken(match('=') ? GREATER_EQUAL:LESS); break;
            case '/':
                if (match('/')) {
                    //if the next character is a / that means it's a comment
                    // peek to check if you are at end, or you have hit a newline
                    // To read the entire comment.
                    // AND THE POINTER GOES OVER THE ENTIRE COMMENT AND IGNORES IT :)
                    while (peek() != '\n' && !isAtEnd()) {advance();}
                } else {
                    addToken(SLASH);
                } break;

            //Just for now the character that doesn't match the given cases are characterised as unexpected.
            // The code keeps scanning the remaining lines to catch more errors, this ensures that all errors are handled.

            //Case clubbing -> whitespaces are ignored
            case ' ':
            case '\r':
            case '\t':
                break;

            //New line causes an increase in the line number.
            // This is how the scanner actually ends up counting the number of lines in the code.
            // IF THE PEEK function reaches at end, a newline is returned which helps increment the line NUMBER !!!!!
            case '\n':
                line++; break;

            //String literals
            case '"': string(); break;

            default:
                if (isDigit(c)) {
                    number();
                } else {
                pandi.error(line, "Unexpected character."); break;}
        }
    }

    private void number() {

    }



    private void string() {
        //While scanning, check if the string has ended or is at end
        // Peek does not consume the character :)
        while (peek() != '"' && !isAtEnd()) {
            //If you encounter a newline then increment the line counter
            if (peek() == '\n') line++;
            //Keep increasing the current counter
            advance();
        }

        //If it is at end and has not ended, then unterminated string
        if (isAtEnd()) {
            pandi.error(line, "Unterminated string");
        }

        //If it is not at end, and has been terminated because of ", then advance to "
        advance();

        //String value is taken
        //Start -> was the " and current is " so +1 and -1
        String value = source.substring(start + 1, current - 1);
        addToken(STRING, value);
    }

    private boolean match(char expected) {
        // Check if the line is at end.
        if (isAtEnd()) return false;

        // check next char, if it doesnt match then return false
        if (source.charAt(current) != expected) return false;

        // Now the next char matches so we scan both the chars,
        // and move the current pointer to the next char.
        current++;
        return true;
    }

    private char peek() {
        // This is just one character of lookahead (low scanner overhead)
        // if is at end -> returns a newline.
        if (isAtEnd()) return '\0';
        return source.charAt(current);
    }

    // Boolean function that checks whether the character read is
    // actually in the range of 0 and 9.
    private boolean isDigit(char c) {
        return c>= '0' && c <= '9';
    }

    //Helper function isAtEnd() helps assess whether we have consumed all characters:
    private boolean isAtEnd() {
        return current >= source.length();
    }

    //The advance method helps find the character at the specific position:
    private char advance() {
        //Read the char at the specific location and then increment the counter.
        return source.charAt(current++);
    }

    // the helper method creates a Token object for the tokens scanned:
    private void addToken(TokenType type) {
        //Links to the main method
        addToken(type, null);
    }

    //A different method signature for the same method !
    private void addToken(TokenType type, Object literal) {
        String text = source.substring(start, current);
        tokens.add(new Token(type, text, literal, line));
    }
}