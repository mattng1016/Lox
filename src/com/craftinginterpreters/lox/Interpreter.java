package com.craftinginterpreters.lox;

class Interpreter implements Expr.Visitor<Object>{
    
    @Override
    public Object visitLiteralExpr(Expr.Literal expr) {
        return expr.value;
    }    

    //Recursively evaluate that subexpression and return it
    @Override
    public Object visitGroupingExpr(Expr.Grouping expr) {
        return evaluate(expr.expression);
    }

    @Override
    public Object visitUnaryExpr(Expr.Unary expr) {
        Object right = evaluate(expr.right);

        switch (expr.operator.type) {
            case BANG:
                return !isTruthy(right);
            case MINUS:
                checkNumberOperand(expr.operator, right);
                return -(double)right; //Negates expression if operator is minus
        }

        return null;
    }

    @Override
    public Object visitBinaryExpr(Expr.Binary expr) {
        Object left = evaluate(expr.left);
        Object right = evaluate(expr.right);

        switch (expr.operator.type) {
            //Arithmetic operators
            case PLUS:
                if (left instanceof Double && right instanceof Double) {
                    return (double)left + (double)right; //Add value
                }
                if (left instanceof String && right instanceof String) {
                    return (String)left + (String)right; //Concatenate string 
                }
                throw new RuntimeError(expr.operator, "Operands must be two numbers or two strings.");
            case MINUS:
                checkNumberOperands(expr.operator, left, right);
                return (double)left - (double)right;
            case SLASH:
                checkNumberOperands(expr.operator, left, right);
                return (double)left / (double)right;
            case STAR:
                checkNumberOperands(expr.operator, left, right);
                return (double)left * (double)right;
            //Comparison operators returns boolean
            case GREATER:
                checkNumberOperands(expr.operator, left, right);
                return (double)left > (double)right;
            case GREATER_EQUAL:
                checkNumberOperands(expr.operator, left, right);
                return (double)left >= (double)right;
            case LESS:
                checkNumberOperands(expr.operator, left, right);
                return (double)left < (double)right;
            case LESS_EQUAL:
                checkNumberOperands(expr.operator, left, right);
                return (double)left <= (double)right;
            //Eqaulity operators
            case BANG_EQUAL:
                return !isEqual(left, right);
            case EQUAL_EQUAL:
                return isEqual(left, right);
        }

        return null;
    }

    //False and nil are falsey and everything else is truthy
    private boolean isTruthy(Object object) {
        if (object == null) { //Nil
            return false;
        }
        if (object instanceof Boolean) { 
            return (boolean)object;
        }
        return true;
    }

    //Sends the expression back into visitor implementation
    private Object evaluate(Expr expr) {
        return expr.accept(this);
    }

    //Checks if parameters are equal
    private boolean isEqual(Object left, Object right) {
        if (left == null && right == null) {
            return true;
        }
        if (left == null) {
            return false;
        }
        return left.equals(right);
    }

    //Checks if operand is a double
    private void checkNumberOperand(Token operator, Object operand) {
        if (operand instanceof Double) {
            return;
        }
        throw new RuntimeError(operator, "Operand must be a number.");
    }

    //Checks if both left and right are doubles
    private void checkNumberOperands(Token operator, Object left, Object right) {
        if (left instanceof Double && right instanceof Double) {
            return;
        }
        throw new RuntimeError(operator, "Operands must be numbers.");
    }

    //Takes in expression, evaluates it and print it to user
    void interpet(Expr expression) {
        try {
            Object value = evaluate(expression);
            System.out.println(stringify(value));
        } catch (RuntimeError error) {
            Lox.runtimeError(error);
        }
    }

    private String stringify(Object object) {
        if (object == null) {
            return "nil";
        }

        if (object instanceof Double) {
            String text = object.toString();
            if (text.endsWith(".0")) { 
                text = text.substring(0, text.length() - 2); //Display without decimal
            }
            return text;
        }
        
        return object.toString();
    }
}
