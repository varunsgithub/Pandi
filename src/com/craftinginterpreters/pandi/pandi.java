package com.craftinginterpreters.pandi;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;


public class pandi {
    //Field for interpreter
    private static final Interpreter interpreter = new Interpreter();
    //Public field which is used by the pandi class to check for error handling
    static boolean hadError = false;
    //field to set the runtime error
    static boolean hadRuntimeError = false;



    public static void main(String[] args) throws IOException{
        //This is a check to ensure that only one argument (if you have a file to load and run source code)
        if (args.length > 1) {
            System.err.print("Usage: pandi[script] - incorrect Args");
            // Exit with error number 64 that is incorrect number of arguments
            System.exit(64);
        }  else if (args.length == 1) {
            runFile(args[0]);
        }  else {
            runPrompt();
        }
    }


    //pandi is a scripting language and can be run using two ways:
    //Either the source code can be stored in a file and the path to the same can be mentioned
    //Or the prompt can be directly run


    //Method for running our pandi source code from the path
    public static void runFile(String path) throws IOException {
        //Read all binary data in the byte array
        byte[] bytes = Files.readAllBytes(Paths.get(path));

        run(new String(bytes, Charset.defaultCharset()));

        //Indicate error while exiting
        if (hadError) System.exit(65);

        //System exit 70 is "internal software error"
        if (hadRuntimeError) System.exit(70);

    }

    //Runs the language one prompt at a time !
    // You can code in the terminal !
    //(Fun fact - The interactive prompt is also called a REPL
    //Where the code does (read (eval (prints)))

    public static void runPrompt() throws IOException {
        InputStreamReader inny = new InputStreamReader(System.in);
        BufferedReader brry = new BufferedReader(inny);

        //Infinite for loop
        // The conditions are left empty
        for (;;) {
            System.out.print("^._.^: ");
            //Read a line from the System in
            String line = brry.readLine();
            // checks if EOF (Ctrl + D) is reached in system.in
            if (line == null) {
                break;
            }
            run (line);

            //In case the user made an error -> after the run command turn hadError to true
            hadError = false;
        }
    }


    //This is a main function that performs the tokenization and lexical analysis
    //for the code
    public static void run(String source) {
        Scanner scanny = new Scanner(source);
        List<Token> tokens = scanny.scanTokens();

        Parser parser = new Parser(tokens);
        List<Stmt> statements = parser.parse();

        if (hadError) return;

        Resolver resolver = new Resolver(interpreter);
        resolver.resolve(statements);

        //Stop if there is a resolution error
        if (hadError) return;

        //Use the interpreter to interpret the expression
        interpreter.interpret(statements);

//        System.out.println(new AstPrinter().print(expression));
    }

    //The error handling method in pandi will point out the specific line
    // where the user has encountered an error
    public static void error(int line, String message) {
        report (line, "" , message);
    }

    //The error reporting class for Runtime errors
    static void runtimeError(RuntimeError error) {
        //The moment we typed super in the RuntimeError class
        // it saved the message that we passed.
        // This message can then be called by the .getMessage() method
        // These are java inbuilt methods
        System.err.println(error.getMessage() + "\n[line " + error.token.line + "]");
        hadRuntimeError = true;
    }

    //Helper function for the error reporting
    public static void report(int line, String where, String message) {
        System.err.println ("[line " + line + "] Error" + where + ": " + message + " :(");
        hadError = true;
    }

    //This is a different error function for reporting errors during the parsing:
    // It is a wrapper to the report function
    static void error(Token token, String message) {
        //Check line position
        if (token.type == TokenType.EOF) {
            report (token.line," at end", message);
        } else {
            // Report the line and the particular lexeme so caught.
            report(token.line, " at '" + token.lexeme + "'", message);
        }
    }

}