package com.craftinginterpreters.pandi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    //To check the key identifiers !
    private static final Map<String, TokenType> keywords;

    static {
        keywords = new HashMap<>();
        keywords.put("and", AND);
        keywords.put("class", CLASS);
        keywords.put("else", ELSE);
        keywords.put("for", FOR);
        keywords.put("false", FALSE);
        keywords.put("fun", FUN);
        keywords.put("if", IF);
        keywords.put("nil", NIL);
        keywords.put("or", OR);
        keywords.put("return", RETURN);
        keywords.put("true", TRUE);
        keywords.put("print", PRINT);
        keywords.put("var", VAR);
        keywords.put("while", WHILE);
        keywords.put("super", SUPER);
        keywords.put("this", THIS);
        keywords.put("varun", VARUN);
        keywords.put("vnm", VNM);
        keywords.put("pandi", PANDI);
    }

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
                addToken(LEFT_PAREN);
                break;
            case ')':
                addToken(RIGHT_PAREN);
                break;
            case '{':
                addToken(LEFT_BRACE);
                break;
            case '}':
                addToken(RIGHT_BRACE);
                break;
            case ',':
                addToken(COMMA);
                break;
            case '.':
                addToken(DOT);
                break;
            case '-':
                addToken(MINUS);
                break;
            case '+':
                addToken(PLUS);
                break;
            case ':':
                addToken(SEMICOLON);
                break;
            case '*':
                addToken(STAR);
                break;

            //These are ingeniously designed operators which actually check, if there are two characters or one
            //If there are 2, the code will automatically mark the token as the 2char tok, else just one char tok
            case '!':
                addToken(match('=') ? BANG_EQUAL : BANG);
                break;
            case '=':
                addToken(match('=') ? EQUAL_EQUAL : EQUAL);
                break;
            case '<':
                addToken(match('=') ? LESS_EQUAL : LESS);
                break;
            case '>':
                addToken(match('=') ? GREATER_EQUAL : LESS);
                break;
            case '/':
                if (match('/')) {
                    //if the next character is a / that means it's a comment
                    // peek to check if you are at end, or you have hit a newline
                    // To read the entire comment.
                    // AND THE POINTER GOES OVER THE ENTIRE COMMENT AND IGNORES IT :)
                    while (peek() != '\n' && !isAtEnd()) {
                        advance();
                    }
                } else if (match('*')) {
                    // While you do not encounter the next */ you keep advancing
                    while (peek() != '*' && peekNext() != '/' && !isAtEnd()) {

                        char chary = advance();
                        if (chary == '\n') {
                            //Increase the line number on new lines !
                            line++;
                        }
                    }
                    if (peek() == '*' && peekNext() == '/' && !isAtEnd()) {
                        advance();
                        advance();
                    } else {
                        pandi.error(line, "Unexpected termination of comment");
                    }

                } else {
                    addToken(SLASH);
                }
                break;

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
                line++;
                break;

            //String literals
            case '"':
                string();
                break;

            default:
                if (isDigit(c)) {
                    number();
                } else if (isAlpha(c)) {
                    //Comes here if the code is a character
                    //Note that strings are starting with "
                    identifier();
                } else {
                pandi.error(line, "Unexpected character."); break;}
        }
    }

    //Used to identify the token
    private void identifier() {
        //Keep advancing if the scanned char is a number or a digit.
        while (isAlphaNumeric(peek())) advance();

        String text = source.substring(start, current);
        TokenType type = keywords.get(text);
        if (type == null) {
            type = IDENTIFIER;
        }

        //Add the token to IDENTIFIER !
        addToken(type);
    }


    private void number() {
        //So the program will advance if the next number peeked into is a digit
        while(isDigit(peek())) advance();

        //The function will peek one character ahead to check for decimals, and 2 charcters forward to check for a digit.
        //Only then is the fraction recognised !!!!
        if (peek() == '.' && isDigit(peekNext())) {
            //consume the .
            advance();

            while (isDigit(peek())) advance();
        }
        //At end -> Just parse the string to a double !
        addToken(NUMBER, Double.parseDouble(source.substring(start, current)));
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

    //Function to see two characters forward:
    private char peekNext() {
        //If the current char + 1 leads to reaching end of source, return a new line
        //Returning a newline -> hits in incrementing the line number in the scan token method.
        if (current + 1 >= source.length()) return '\0';

        //Else it returns the character at that location.
        return source.charAt(current + 1);
    }

    //check the character is either an alphabet (upper or lower case) or an underscore
    private boolean isAlpha(char c) {
        return (c >= 'a' && c <= 'z') ||
                (c >= 'A' && c <= 'Z') ||
                (c == '_');
    }

    private boolean isAlphaNumeric(char c) {
        return isAlpha(c) || isDigit(c);
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