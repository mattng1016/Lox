package com.craftinginterpreters.lox;

import java.util.List;

class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {

    private Enviroment environment = new Enviroment();

    @Override
    public Void visitBlockStmt(Stmt.Block stmt) {
        executeBlock(stmt.statements, new Enviroment(environment));
        return null;
    }

    @Override //Evaluates right hand side then stores it in variable
    public Object visitAssignExpr(Expr.Assign expr) {
        Object value = evaluate(expr.value);
        environment.assign(expr.name, value);
        return value;
    }

    @Override //Declaration statements
    public Void visitVarStmt(Stmt.Var stmt) {
        Object value = null;
        if (stmt.initializer != null) { //If var has initializer
            value = evaluate(stmt.initializer);
        }
        environment.define(stmt.name.lexeme, value); //Set value to nil (e.g. var a; print a;)
        return null;
    }

    @Override //Evaluate variable expression
    public Object visitVariableExpr(Expr.Variable expr) {
        return environment.get(expr.name);
    }
    
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
                if (left instanceof String && !(right instanceof String)) {
                    String newRight = right.toString();
                    newRight = newRight.substring(0, newRight.length() - 2);
                    return (String)left + (String)newRight;
                }
                if (!(left instanceof String) && right instanceof String) {
                    String newLeft = left.toString();
                    newLeft = newLeft.substring(0, newLeft.length() - 2);
                    return (String)newLeft + (String)right;
                }
                throw new RuntimeError(expr.operator, "Operands must be two numbers or two strings.");
            case MINUS:
                checkNumberOperands(expr.operator, left, right);
                return (double)left - (double)right;
            case SLASH:
                checkNumberOperands(expr.operator, left, right);
                if ((double)right == 0) {
                    throw new RuntimeError(expr.operator, "Divisor cannot be 0");
                }
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

    //Method for Expression statements
    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt) {
        evaluate(stmt.expression);
        return null;
    }

    //Method for Print statements
    @Override
    public Void visitPrintStmt(Stmt.Print stmt) {
        Object value = evaluate(stmt.expression);
        System.out.println(stringify(value));
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

    //Accepts list of statements (program) and executes it (or error)
    void interpet(List<Stmt> statements) {
        try {
            for (Stmt statement : statements) {
                execute(statement);
            }
        } catch (RuntimeError error) {
            Lox.runtimeError(error);
        }
    }

    //Helper method for interpet method
    private void execute(Stmt statement) {
        statement.accept(this);
    }

    //Convert Lox value to String
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

    void executeBlock(List<Stmt> statements, Enviroment enviroment) {
        Enviroment previous = this.environment;
        try {
            this.environment = enviroment;
            for (Stmt statement : statements) {
                execute(statement);
            }
        } finally {
            this.environment = previous;
        }
    }
}
