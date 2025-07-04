package com.craftinginterpreters.pandi;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;


public class Resolver implements Expr.Visitor<Void>, Stmt.Visitor<Void> {
    // a private field of the interpreter.
    private final Interpreter interpreter;
    // stack of scopes:
    // It has the lexeme of the token and a boolean value -
    // the boolean value stores ... the
    private final Stack<Map<String, Boolean>> scopes = new Stack<>();


    // The constructor initialises the interpreter variable
    Resolver(Interpreter interpreter) {
        this.interpreter = interpreter;
    }

    // as of now the only variables that need to be checked for static analysis are:
    // Block statements (for analyzing the scope since a new scope is introduced)
    // Function declaration (each declaration creates a new scope)
    // Variable declarations as these add new variables to our map
    // Assignment expressions and variable expressions since they need to have the variables resolved


    @Override
    public Void visitBlockStmt(Stmt.Block stmt) {
        //Create an environment on the stack
        beginScope();
        //Visit and evaluate the expression/ statement
        resolve(stmt.statements);
        //exit the environment
        endScope();

        return null;
    }


    @Override
    public Void visitVarStmt(Stmt.Var stmt) {
        //Declare the variable first
        declare(stmt.name);

        //Interpret the value of the variable (assignment)
        if (stmt.initializer != null) {
            resolve(stmt.initializer);
        }

        //Define the variable in the map
        define(stmt.name);

        return null;
    }

    @Override
    public Void visitVariableExpr(Expr.Variable expr) {
        if (!scopes.isEmpty() && scopes.peek().get(expr.name.lexeme) == Boolean.FALSE) {
            pandi.error(expr.name, "Can't read local variable in its own initializer");
        }

        resolveLocal(expr, expr.name);
        return null;
    }


    void resolve(List<Stmt> statements) {
        for (Stmt statement : statements) {
            resolve(statement);
        }
    }

    //A resolve method which accepts the statement
    private void resolve(Stmt stmt) {
        // This calls the accept method which in turn evaluates the statement's value
        stmt.accept(this);
    }

    //Resolve method for accepting the expressions
    private void resolve(Expr expr) {
        // This calls the accept method which in turn evaluates the expression's value
        expr.accept(this);
    }

    private void beginScope() {
        // The moment a new scope is encountered, the environment is pushed onto the stack as a new hashmap
        scopes.push(new HashMap<String, Boolean> ());
    }

    private void endScope() {
        // We can pop the environment from the stack once its done.
        scopes.pop();
    }

    private void declare(Token name) {
        //If the stack of scope is empty then return
        if (scopes.isEmpty()) {return;}

        //Else find the innermost scope and declare the variable in it
        Map<String, Boolean> scope = scopes.peek();
        //and mark it as unresolved "false"
        scope.put(name.lexeme, false);
    }

    private void define(Token name) {
        // First check if there are scopes in the stack
        if (scopes.isEmpty()) {return;}

        // In the innermost scope, peek and put the name of the
        // token and mark it as resolved.
        scopes.peek().put(name.lexeme, Boolean.TRUE);
    }

    private void resolveLocal(Expr expr, Token name) {
        for (int i = scopes.size() - 1; i >= 0; i--) {
            if (scopes.get(i).containsKey(name.lexeme)) {
                interpreter.resolve(expr, scopes.size() - 1 - i);
                return;
            }
        }
    }

}
