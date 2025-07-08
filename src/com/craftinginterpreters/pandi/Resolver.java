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
    // the boolean value stores ... ??
    private final Stack<Map<String, Boolean>> scopes = new Stack<>();
    private FunctionType currentFunction = FunctionType.NONE;

    // The constructor initialises the interpreter variable
    Resolver(Interpreter interpreter) {
        this.interpreter = interpreter;
    }

    private enum FunctionType {
        NONE,
        FUNCTION,
        METHOD
    }

    // as of now the only variables that need to be checked for static analysis are:
    // Block statements (for analyzing the scope since a new scope is introduced)
    // Function declaration (each declaration creates a new scope)
    // Variable declarations as these add new variables to our map
    // Assignment expressions and variable expressions since they need to have the variables resolved


    // By implementing the visit block statement we have completely changed what the visitor actually does
    @Override
    public Void visitBlockStmt(Stmt.Block stmt) {
        //Create an environment on the stack
        // Blocks are either created for if/else/or while/ for and functions
        beginScope();
        //Visit and evaluate the expression/ statement
        resolve(stmt.statements);
        //exit the environment
        endScope();

        return null;
    }

    @Override
    public Void visitClassStmt(Stmt.Class stmt) {

        declare(stmt.name);

        define(stmt.name);

        for (Stmt.Function method : stmt.methods) {
            //The declaration is stored as a method
            FunctionType declaration = FunctionType.METHOD;
            resolveFunction(method, declaration);
        }

        return null;
    }



    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt) {
        resolve(stmt.expression);
        return null;
    }

    @Override
    public Void visitFunctionStmt(Stmt.Function stmt) {
        //Declare the name (which is in the outer scope)
        declare(stmt.name);
        //
        define(stmt.name);
        //
        resolveFunction(stmt, FunctionType.FUNCTION);

        return null;
    }


    //Runs both the branches (different from dynamic runs)
    @Override
    public Void visitIfStmt(Stmt.If stmt) {
        resolve(stmt.condition);
        resolve(stmt.thenBranch);
        if (stmt.elseBranch != null) {
            resolve(stmt.elseBranch);
        }
        return null;
    }

    @Override
    public Void visitPrintStmt(Stmt.Print stmt) {
        resolve(stmt.expression);
        return null;
    }

    @Override
    public Void visitReturnStmt(Stmt.Return stmt) {
        if (currentFunction == FunctionType.NONE) {
            pandi.error(stmt.keyword, "Cant return from top level code");
        }

        if(stmt.value != null) {
            resolve(stmt.value);
        }
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
    public Void visitWhileStmt(Stmt.While stmt) {
        resolve(stmt.condition);
        resolve(stmt.body);
        return null;
    }


    @Override
    public Void visitAssignExpr(Expr.Assign expr) {
        resolve(expr.value);
        resolveLocal(expr, expr.name);
        return null;
    }


    @Override
    public Void visitBinaryExpr(Expr.Binary expr) {
        resolve(expr.left);
        resolve(expr.right);
        return null;
    }


    @Override
    public Void visitCallExpr(Expr.Call expr) {
        resolve(expr.callee);

        for (Expr argument : expr.arguments) {
            resolve(argument);
        }

        return null;
    }

    @Override
    public Void visitGetExpr(Expr.Get expr) {
        resolve(expr.object);
        return null;
    }

    @Override
    public Void visitGroupingExpr(Expr.Grouping expr) {
        resolve(expr.expression);
        return null;
    }


    @Override
    public Void visitLiteralExpr(Expr.Literal expr) {
        return null;
    }


    @Override
    public Void visitLogicalExpr(Expr.Logical expr) {
        resolve(expr.left);
        resolve(expr.right);
        return null;
    }

    @Override
    public Void visitSetExpr(Expr.Set expr) {
        resolve(expr.value);
        resolve(expr.value);
        return null;
    }


    @Override
    public Void visitUnaryExpr(Expr.Unary expr) {
        resolve(expr.right);
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

    private void resolveFunction(Stmt.Function function, FunctionType type) {
        FunctionType enclosingFunction = currentFunction;
        currentFunction = type;


        beginScope();
        for (Token param : function.params) {
            declare(param);
            define(param);
        }
        resolve(function.body);
        endScope();

        currentFunction = enclosingFunction;
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
        if (scope.containsKey(name.lexeme)) {
            pandi.error(name,
                    "Already a variable with this name in this scope maccha.");
        }

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