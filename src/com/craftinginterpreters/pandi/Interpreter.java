package com.craftinginterpreters.pandi;

public class Interpreter implements Expr.Visitor<Object> {


    @Override
    public Object visitLiteralExpr(Expr.Literal expr) {
        //The value forst got scanned by the scanner and stored in a token
        // Once the parser realised that these are literal values, it got saved to
        // the expr.literal :)
        return expr.value;
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


    //The visit binary expression is helpful to evaluate binary expressions
    // it checks where of what instances the left and right objects are and performs the necessary
    // calculations
    @Override
    public Object visitBinaryExpr(Expr.Binary expr) {
        Object left = evaluate(expr.left);
        Object right = evaluate(expr.right);

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
        switch(expr.operator.type) {
            case BANG:
                return !isTruthy(right);
            case MINUS:
                checkNumberOperand(expr.operator, right);
                return -(double)right;
        }
        //unreachable -> but safety checks
        return null;
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
    void interpret(Expr expr) {
        try{
            //This method takes in the expression which has been parsed (AST)
            // If the object gets evaluated, it returns the value which is stored in a variable of the same name
            // and the same is printed out to the user as a string
            Object value = evaluate(expr);
            System.out.println(stringify(value));
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