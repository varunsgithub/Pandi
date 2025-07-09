package com.craftinginterpreters.pandi;

import java.util.Arrays;
import java.util.ArrayList;
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

    List<Stmt> parse() {
        List<Stmt> statements = new ArrayList<>();
        while (!isAtEnd()) {
            statements.add(declaration());
        }
        return statements;
    }


    // The lowest precedence -> expression returns equality
    // Each grammar rule is a method and the body of it can contain either
    // a terminal (some action being performed) or a non-terminal (reference to a different rule)
    // the expression has equality referenced which is a non-terminal.
    private Expr expression() {
        return assignment();
    }


    private Stmt declaration() {
        try {
            if (match(CLASS)) return classDeclaration();
            if (match(FUN)) return function("function");
            if (match(VAR)) return varDeclaration();
            return statement();
        } catch (ParseError error) {
            synchronize();
            return null;
        }
    }

    private Stmt classDeclaration() {
        //So the first token has to be an identifier, which is the name of the class
        Token name = consume(IDENTIFIER, "Expect class name");
        //Consume the left brace first
        consume(LEFT_BRACE, "Expect '{' before class body.");

        //we create a new array list to store the methods declared
        List<Stmt.Function> methods = new ArrayList<>();
        //While we have not reached the end of the token list and the class has not been closed
        // with a }
        while(!check(RIGHT_BRACE) && !isAtEnd()) {
            //This will add the function tree to the list
            //Function -> Block -> return
            methods.add(function("method"));
        }
        //There should be a right brace after the class body to end the class
        consume(RIGHT_BRACE, "Expect '}' after class body.");

        return new Stmt.Class(name, methods);
    }




    private Stmt statement() {
        //For statements do not have a separate
        if (match(FOR)) return forStatement();

        if (match(IF)) return ifStatement();

        //There are two kinds of statements as of now, a print statement
        // and an expression statement. The expression statement is an expression that ends with a ';'
        if (match(PRINT)) return printStatement();

        //Checks for a return statement
        if (match(RETURN)) return returnStatement();

        //Checks for while statement
        if (match(WHILE)) return whileStatement();

        //If the statement matches the likes of a block, then we have encountered a block statement
        if (match(LEFT_BRACE)) {return new Stmt.Block(block());}

        //it returns the method for an expression statement
        return expressionStatement();

    }

    private Stmt forStatement() {
        //Consume the left parenthesis after the For
        consume(LEFT_PAREN, "Expect '(' after 'for'.");

        //An initial statement intializer is created
        // for (Var i = 0;)
        Stmt initializer;
        //if the first token is a semicolon, then the initializer has been skipped.
        if (match(SEMICOLON)) {
            initializer = null;
        } else if (match(VAR)) {
            //if it is VAR then a new variable is declared
            initializer = varDeclaration();
        } else {
            //else an expression statement is used !
            initializer = expressionStatement();
        }

        //next the condition is checked... if the next token is
        // not a semicolon, then get the condition
        Expr condition = null;
        if (!check(SEMICOLON)) {
            //Save the condition
            condition = expression();
        }
        //If there is a semicolon w/o a condition or vice versa -> report an error
        consume(SEMICOLON, "Expect ';' after loop condition.");

        //final incrementor !
        Expr increment = null;
        if (!check(RIGHT_PAREN)) {
            // if there is no right parenthesis !
            increment = expression();
        }
        //after saving the expression or not finding a right paren
        consume(RIGHT_PAREN, "Expect ')' after for clauses.");

        //next the body is saved
        Stmt body = statement();


        //If the increment variable is not null then the body is created
        // as a block statement, that has a list of statements
        if (increment != null) {
            body = new Stmt.Block(
                    Arrays.asList(body,
                            new Stmt.Expression(increment))
            );
        }

        //if the condition is null then the literal expression is saved as true
        // a new while condition si created
        if (condition == null) condition = new Expr.Literal(true);
        body = new Stmt.While(condition, body);

        //if the initializer is not null then a new body is created with a block statement.
        if (initializer != null) {
            body = new Stmt.Block(Arrays.asList(initializer, body));
        }

        return body;
    }



    private Stmt ifStatement() {
        consume(LEFT_PAREN, "Expect '(' after 'if'");
        Expr condition = expression();
        consume(RIGHT_PAREN, "Expect ')' after 'if' condition.");

        Stmt thenBranch = statement();

        Stmt elseBranch = null;

        if (match(ELSE)) {
            elseBranch = statement();
        }

        return new Stmt.If(condition, thenBranch, elseBranch);
    }

    private Stmt printStatement() {
        //The value of the statement is captured using the expression method !
        // So it creates an AST for the relevant expressions
        Expr value = expression();
        //Next it checks if there is an appropriate end to the expression
        consume(SEMICOLON, "Expected ';' after expression.");
        // This call is a nested outer class (statement) and an inner class (Print)
        // So what happens is that the Print object is created with expression as value
        return new Stmt.Print(value);
    }

    private Stmt returnStatement() {
        //Save the previous token as a keyword
        Token keyword = previous();
        //The value of the expression is saved as null
        Expr value = null;

        //If one has not reached the semicolon yet
        if(!check(SEMICOLON)) {
            //The value is saved (Recursive descent parsing)
            value = expression();
        }
        consume(SEMICOLON, "Expect ';' after return value.");

        //In case there is no value after the return statement a null value is returned
        // This is the case where the user wants to perform an early exit in the code.

        return new Stmt.Return(keyword, value);
    }



    private Stmt varDeclaration() {
        //First the variable declaration checks that is there a name for the
        // variable
        //(Note in the parser if there is no given category for the token then it is stored as an identifier)
        Token name = consume(IDENTIFIER, "Expected a variable name.");

        //Then the initializer is stored as null
        Expr initializer = null;

        //If the next token is an = this means that the variable declaration is done properly and the
        // next statement is stored in the initializer
        if (match(EQUAL)) {
            //Initializer calls the expression method which does the entire recursive descent parsing
            initializer = expression();
        }

        //The semicolon checks are placed so that the variable declaration is taken care of
        consume(SEMICOLON, "Expected ';' after variable declaration.");

        //Returns a new statement AST for the given variable declaration.
        return new Stmt.Var(name, initializer);
    }

    //If there is a while statement
    private Stmt whileStatement() {
        // Consume the left parenthesis.
        consume(LEFT_PAREN, "Expect '(' after 'while'");
        // Next the condition is evaluated using the expression.
        Expr condition = expression();
        // the token for right parenthesis is evaluated
        consume(RIGHT_PAREN, "Expect ')' after condition.");
        // The ast is created for the body
        Stmt body = statement();
        //return the ast for the condition and the body.
        return new Stmt.While(condition, body);
    }


    private Stmt expressionStatement() {
        //The value of the statement is captured using the expression method
        Expr expr = expression();

        //The last token in the list of tokens after scanning should be a semi colon
        // If this is not the case then we throw an error
        consume(SEMICOLON, "Expected ';' after expression.");

        //This calls the stmt class and the static class inside the stmt class
        // and creates an AST for the expression !
        return new Stmt.Expression(expr);
    }

    private Stmt.Function function(String kind) {
        //if the next token after the word fun is not an identifier
        // this code will just display the message saying that it expected the
        // name function !!!!
        Token name = consume(IDENTIFIER, "Expect " + kind + "name");

        //When you discover a function consume the next token which has to be a (
        consume(LEFT_PAREN, "Expect '(' after " + kind + "name.");
        //create a list of parameters
        List<Token> parameters = new ArrayList<>();
        if (!check(RIGHT_PAREN)) {
            do{
                //Check the parameters size (just in case if it exceeds 254)
                if (parameters.size() >= 255) {
                    error(peek(), "Can't have more than 255 parameters.");
                }
                parameters.add(
                        consume(IDENTIFIER, "Expect parameter name"));
            //Keep going while there is a comma
            } while (match(COMMA));
        }
        consume(RIGHT_PAREN, "Expect ')' after parameters.");

        //Consume the left BRACE after the arguments (here you reach the body)
        consume(LEFT_BRACE, "Expect '{' before " + kind +"body");
        //The body is given a new Environment and the scope changes with block
        List<Stmt> body = block();
        //Returns the new AST for the function as a statement.
        return new Stmt.Function(name, parameters, body);
    }



    //The statement now flows into a block check as well
    private List<Stmt> block() {
        //In the list of statements that one gets as per the block
        List<Stmt> statements = new ArrayList<>();

        // Check if we have not reached the right brace & we are not at end
        // Why we do this is because we assume the user knows where to stop the right brace
        // so we keep scanning until we reach the end.
        while (!check(RIGHT_BRACE) && !isAtEnd()) {
            //Run the declaration command to check if it is a statement or a variable declaration
            statements.add(declaration());
        }

        //Once the check is reached (there is a right brace) or we are at end
        consume(RIGHT_BRACE, "Expect '}' after block.");
        //Return the list of ASTs... :)
        return statements;
    }


    // Note that since the assignment operator is right associative
    // we end up doing a recursion only after the = sign
    private Expr assignment() {
        //We start with going down the tree for the l value
        Expr expr = or();

        //If the next token is EQUAL
        //(Note that there can only be 1 equal statement in a single line of code, hence we have an if statement)
        if (match(EQUAL)) {
            //So the token is stored....
            Token equals = previous();
            //The value to be assigned (r value) is stored in a diff expr
            Expr value = assignment();

            //If the l value is an instance of variable
            if (expr instanceof Expr.Variable) {
                //if the expr was a variable assignment
                //cast the expression token and store its name
                Token name = ((Expr.Variable)expr).name;
                return new Expr.Assign(name, value);
            } else if (expr instanceof Expr.Get) {
                // We check if the previous parsed expression is an instance of
                // the Expr.GET... (from Call())
                Expr.Get get = (Expr.Get)expr;
                // if it is then we create a new AST for the set expression.
                return new Expr.Set(get.object, get.name, value);
            }

            error (equals, "Invalid assignment target.");
        }

        return expr;
    }


    private Expr or() {
        //First goto and() and get the l value
        Expr expr = and();

        // While the next statement is an OR (can have multiple)
        // get the operator
        while (match(OR)) {
            Token operator = previous();
            Expr right = and();
            expr = new Expr.Logical(expr, operator, right);
        }
        //Return the AST if it matches !!
        return expr;
    }

    //This is the logical operator AND...
    // Helps in short circuits
    private Expr and() {
        //Get the l value first.
        Expr expr = equality();

        //While there are multiple AND operators...
        while (match(AND)) {
            //The operator is the previous
            Token operator = previous();
            //The right expression is taken up
            Expr right = equality();
            // a new syntax tree is created and returned
            expr = new Expr.Logical(expr, operator, right);
        }
        return expr;
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

    // Unary operations:
    // This is right associative hence the expression on right is evaluated first
    // then at the end in the
    private Expr unary() {
        if(match(BANG, MINUS)) {
            Token operator = previous();
            Expr right = unary();
            return new Expr.Unary(operator, right);
        }
        //Now unary does not jump to primary...
        // Since function calls are right associtive as well
        return call();
    }

    private Expr finishCall(Expr callee) {
        //The list of arguments are created as an array list !
        List<Expr> arguments = new ArrayList<>();
        // If the next token is not a right parenthesis
        if (!check(RIGHT_PAREN)) {
            do {
                if (arguments.size() >= 255) {
                    error(peek(), "Can't have more than 255 arguments.");
                }


                //Add the AST for the expression in the arraylist
                arguments.add(expression());
            } while (match(COMMA));
        }

        Token paren = consume (RIGHT_PAREN, "Expect ')' after arguments.");

        return new Expr.Call(callee, paren, arguments);
    }


    private Expr call() {
        // First if the code has arrived from unary it goes to primary to evaluate the
        // expression
        // The call method essentially creates the AST basis the () and .
        Expr expr = primary();

        while (true) {
            // If the next token matches (
            if (match(LEFT_PAREN)) {
                // The expression is then assigned to the finish call method
                expr = finishCall(expr);
            } else if (match(DOT)) {
                //Match the dot -> if it is an identifier after the dot then
                Token name = consume(IDENTIFIER, "Expect property name after '.'");
                //the expression is a new Get expression tree !
                expr = new Expr.Get(expr, name);
            }
            else {
                break;
            }
        }
        return expr;
    }

    //
    private Expr primary() {
        if (match(FALSE)) return new Expr.Literal(false);
        if (match(TRUE)) return new Expr.Literal(true);
        if (match(NIL)) return new Expr.Literal(null);

        if (match(NUMBER, STRING)) {
            return new Expr.Literal(previous().literal);
        }

        //If the token matches the keyword THIS -> return AST for this
        if (match(THIS)) return new Expr.This(previous());

        //if the token is a string, which does not get scanned as a proper token, it gets identified as an IDENTIFIER
        // If it is an identifier, then it gets treated as an Variable EXPRESSION.
        if (match(IDENTIFIER)) {
            return new Expr.Variable(previous());
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
    private boolean match (TokenType... types) {
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
    private Token consume (TokenType type, String message) {
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