package com.craftinginterpreters.pandi;


import java.util.List;

// Statements do not produce any values so they return Void
// Java does not allow returning lowercase void objects.... so we use Void
public class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {

    //This is the outermost environment variable with enclosing = null
    private Environment environment = new Environment();

    @Override
    public Object visitLiteralExpr(Expr.Literal expr) {
        //The value first got scanned by the scanner and stored in a token
        // Once the parser realised that these are literal values, it got saved to
        // the expr.literal :)
        return expr.value;
    }

    @Override
    public Object visitLogicalExpr(Expr.Logical expr) {
        //Check the left object
        Object left = evaluate(expr.left);

        // This is a short-circuiting technique
        // The expression on the left is always checked first for the true/ false values
        // in a OR/ AND situation.
        if (expr.operator.type == TokenType.OR) {
            if (isTruthy(left)) {return left;}
        } else {
            if (!isTruthy(left)) {return left;}
        }
        //Return the evaluated right statement
        //Because if the short circuit didn't work then definitely the answer
        // lies in the right statement.
        return evaluate(expr.right);
    }


    @Override
    public Object visitGroupingExpr(Expr.Grouping expr) {
        return evaluate(expr.expression);
    }


    //Since the grouping expression itself has an expression inside of it
    // The method places an accept call on the entire inner expression
    // Which in turn keeps calling the accept method inside !
    private Object evaluate(Expr expr) {
        return expr.accept(this);
    }


    private void execute(Stmt stmt) {
        stmt.accept(this);
    }

    @Override
    public Void visitBlockStmt(Stmt.Block stmt) {
        //When the code calls a visit block statement, it creates a
        // new environment so that environment's enclosing is linked to
        // the interpreter's enclosing (so it is linked to global)
        // which in turn is linked to null

        //So this is the new environment created for the block
        executeBlock(stmt.statements, new Environment(environment));
        return null;
    }


    void executeBlock(List<Stmt> statements,
                      Environment environment) {
        Environment previous = this.environment;
        try {
            this.environment = environment;

            for (Stmt statement : statements) {
                execute(statement);
            }
        } finally {
            this.environment = previous;
        }
    }



    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt) {
        //The statement is evaluated basis the expression but the return value is discarded
        evaluate(stmt.expression);
        // A null is returned
        return null;
    }

    @Override
    public Void visitIfStmt (Stmt.If stmt) {
        if (isTruthy(evaluate(stmt.condition))) {
            execute(stmt.thenBranch);
        } else if (stmt.elseBranch != null) {
            execute(stmt.elseBranch);
        }
        return null;
    }


    @Override
    public Void visitPrintStmt(Stmt.Print stmt) {
        Object value = evaluate(stmt.expression);
        System.out.println(stringify(value));
        return null;
    }

    //The visit variable statement method helps identify the variable statements
    @Override
    public Void visitVarStmt(Stmt.Var stmt) {
        //The object value is stored as null initially
        Object value = null;
        //If the statement initializer is not null
        if (stmt.initializer != null) {
            //Start with evaluating the value of the initializer
            value = evaluate(stmt.initializer);
        }
        //After the value has been evaluated, store teh value in the environment hashmap
        //in case the variable does not have any assignment yet, the variable is just stored as null
        //eg: var varun;.... will be stored as null
        environment.define(stmt.name.lexeme, value);
        return null;
    }

    @Override
    public Void visitWhileStmt(Stmt.While stmt) {
        //While the condition statement is true
        while (isTruthy(evaluate(stmt.condition))) {
            //Execute the statement's body
            execute(stmt.body);
        }
        return null;
    }


    @Override
    public Object visitAssignExpr(Expr.Assign expr) {
        Object value = evaluate(expr.value);
        environment.assign(expr.name, value);
        return value;
    }



    //The visit binary expression is helpful to evaluate binary expressions
    // it checks where of what instances the left and right objects are and performs the necessary
    // calculations
    @Override
    public Object visitBinaryExpr(Expr.Binary expr) {
        Object left = evaluate(expr.left);
        Object right = evaluate(expr.right);

        //The parser gets hold of all the type error checks whereas the interpreter catches
        // all the runtime errors

        switch (expr.operator.type) {
            case GREATER:
                checkNumberOperands(expr.operator,left,right);
                return (double) left > (double) right;
            case GREATER_EQUAL:
                checkNumberOperands(expr.operator,left,right);
                return (double) left >= (double) right;
            case LESS:
                checkNumberOperands(expr.operator,left,right);
                return (double) left < (double) right;
            case LESS_EQUAL:
                checkNumberOperands(expr.operator,left,right);
                return (double) left <= (double) right;
            case BANG_EQUAL:
                // Why the != and == do not need the type checks is because we do not set specific restrictions on
                // the inputs for these, they check equality and return the bool value when required.
                return !isEqual(left, right);
            case EQUAL_EQUAL:
                return isEqual(left, right);
            case MINUS:
                checkNumberOperands(expr.operator,left,right);
                return (double) left - (double) right;

            //the plus operations are overloaded to handle string and numbers
            case PLUS:
                //Since the plus operator is already performing type checks
                // at runtime they don't need an extra level of checks for runtime error checks
                // we just throw a new exception in case the type checks do not match for the first two things !
                if (left instanceof Double && right instanceof Double) {
                    return (double) left + (double) right;
                }

                if (left instanceof String && right instanceof String) {
                    return (String) left + (String) right;
                }

                throw new RuntimeError(expr.operator, "Operands must be two numbers or strings.");

            case SLASH:
                checkNumberOperands(expr.operator,left,right);
                return (double) left / (double) right;
            case STAR:
                checkNumberOperands(expr.operator,left,right);
                return (double) left * (double) right;
        }

        return null;
    }


    @Override
    public Object visitUnaryExpr(Expr.Unary expr) {
        //The object to the right in a unary expression is first evaluated using the accept method
        // in evaluate
        Object right = evaluate(expr.right);

        //check if the operator is either ! or -
        switch (expr.operator.type) {
            case BANG:
                return !isTruthy(right);
            case MINUS:
                checkNumberOperand(expr.operator, right);
                return -(double)right;
        }
        //unreachable -> but safety checks
        return null;
    }

    //This method just
    @Override
    public Object visitVariableExpr(Expr.Variable expr) {
        return environment.get(expr.name);
    }


    //This method is specific for the unary operation
    private void checkNumberOperand(Token operator, Object operand) {
        //If the operand is a double, then we return saying that there is essentially no error !
        if (operand instanceof Double) return;

        //else throw a run time error
        throw new RuntimeError(operator, "Operand must be a number.");
    }


    //This method covers the runtime error checking for binary expressions
    private void checkNumberOperands(Token operator, Object left, Object right) {
        // if left and right are numbers then return.
        if (left instanceof Double && right instanceof Double) {return;}

        throw new RuntimeError(operator, "Operands must be numbers.");
    }


    private boolean isTruthy(Object object) {
        //Null is false
        if (object == null) return false;
        //If the object is an instance of boolean type then return that
        if (object instanceof Boolean) {return ((boolean)object);}
        //Else return true !
        return true;
    }


    //This checks equality between two objects
    private boolean isEqual(Object a, Object b) {
        //The null checks are in place to avoid the null pointer exceptions
        if (a==null && b==null) return true;
        if (a==null) return false;

        return a.equals(b);
    }


    //This is a wrapper around the entire interpreter class to prevent exposing the internal methods
    void interpret(List<Stmt> statements) {
        try{
            for (Stmt statement : statements) {
                execute(statement);
            }
        } catch (RuntimeError error) {
            pandi.runtimeError(error);
        }
    }


    private String stringify(Object object) {
        if (object == null) return "nil";

        if (object instanceof Double) {
            String text = object.toString();
            if (text.endsWith(".0")) {
                text = text.substring(0, text.length() - 2);
            }
            return text;
        }
        return object.toString();
    }
}