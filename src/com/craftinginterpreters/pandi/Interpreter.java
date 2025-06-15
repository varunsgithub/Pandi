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
                return (double) left > (double) right;
            case GREATER_EQUAL:
                return (double) left >= (double) right;
            case LESS:
                return (double) left < (double) right;
            case LESS_EQUAL:
                return (double) left <= (double) right;
            case BANG_EQUAL:
                return !isEqual(left, right);
            case EQUAL_EQUAL:
                return isEqual(left, right);
            case MINUS:
                return (double) left - (double) right;

            //the plus operations are overloaded to handle string and numbers
            case PLUS:
                if (left instanceof Double && right instanceof Double) {
                    return (double) left + (double) right;
                }

                if (left instanceof String && right instanceof String) {
                    return (String) left + (String) right;
                }

//                if (left.equals("Pandi") && right.equals("Pandi")) {
//                    return "meoowwww";
//                }

                break;

            case SLASH:
                return (double) left / (double) right;
            case STAR:
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
                return -(double)right;
        }
        //unreachable -> but safety checks
        return null;
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



}
