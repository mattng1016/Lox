package com.craftinginterpreters.lox;

import static com.craftinginterpreters.lox.TokenType.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Parser {
    private static class ParseError extends RuntimeException {}

    private final List<Token> tokens; //List of tokens
    private int current = 0; //

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    private Expr expression() {
        return assignment();
    }

    //equality -> comparison(( "!=" | "==" ) comparison)*
    private Expr equality() {
        Expr expr = comparison(); //Calls the first comparison

        while(match(BANG_EQUAL, EQUAL_EQUAL)) { //While there's a equality operator
            Token operator = previous(); 
            Expr right = comparison();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    //Checks to see if the current token has type, consumes the token!!!
    private boolean match(TokenType... types) {
        for (TokenType type : types) { //Loops through the given types
            if (check(type)) {
                advance();
                return true;
            }
        }
        return false;
    }

    //Checks if current token is given type, does not consume token
    private boolean check(TokenType type) {
        if (isAtEnd()) {
            return false;
        }
        return peek().type == type;
    }

    //Advances to the next token and returns the current token
    private Token advance() {
        if (!isAtEnd()) {
            current++;
        }
        return previous();
    }

    //Checks if end of token list
    private boolean isAtEnd() {
        return peek().type == EOF;
    }

    //Returns the current token
    private Token peek() {
        return tokens.get(current);
    }

    //Returns the previous token
    private Token previous() {
        return tokens.get(current - 1);
    }

    //comparison -> term(( ">" | ">=" | "<" | "<=" )term)*
    private Expr comparison() {
        Expr expr = term();

        while(match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
            Token operator = previous();
            Expr right = term();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    //term -> factor(( "- " | "+") factor)*
    private Expr term() {
        Expr expr = factor();

        while (match(MINUS, PLUS)) {
            Token operator = previous();
            Expr right = factor();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    //factor -> unary(( "/" | "*") unary)*
    private Expr factor() {
        Expr expr = unary();

        while (match(SLASH, STAR)) {
            Token operator = previous();
            Expr right = unary();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    //unary -> ( "!" | "-" ) unary \ primary
    private Expr unary() {
        if (match(BANG, MINUS)) {
            Token operator = previous();
            Expr right = unary();
            return new Expr.Unary(operator, right);
        }
        return call();
    }

    //primary -> NUMBER | STRING | "true" | "false" | "nil" | "(" expression ")"
    private Expr primary() {
        if (match(FALSE)) return new Expr.Literal(false);
        if (match(TRUE)) return new Expr.Literal(true);
        if (match(NIL)) return new Expr.Literal(null);

        if (match(NUMBER, STRING)) {
            return new Expr.Literal(previous().literal);
        }

        if (match(IDENTIFIER)) {
            return new Expr.Variable(previous());
        }

        if (match(LEFT_PAREN)) {
            Expr expr = expression();
            consume(RIGHT_PAREN, "Expect ) after expression.");
            return new Expr.Grouping(expr);
        }

        throw error(peek(), "Expect expression.");
    }   

    //Throws an error if some other token is there
    private Token consume(TokenType type, String message) {
        if (check(type)) {
            return advance();
        }
        throw error(peek(), message);
    }

    //Report the error
    private ParseError error(Token token, String message) {
        Lox.error(token, message);
        return new ParseError();
    }

    //Panic mode recovery
    private void synchronize() {
        advance();
        while (!isAtEnd()) {
            if (previous().type == SEMICOLON) { //Discards token until statement boundary
                return;
            }
            switch (peek().type) {
                case CLASS:
                case FUN:
                case VAR:
                case FOR:
                case IF:
                case WHILE:
                case PRINT:
                case RETURN:
                    return;
            }
            advance();
        }
    }

    //Parses series of statements
    List<Stmt> parse() {
        List<Stmt> statements = new ArrayList<>();
        while (!isAtEnd()) {
            statements.add(declaration());
        }

        return statements;
    }

    //Method to parse one statment
    private Stmt statement() {
        if (match(FUN)) {
            return function("function");
        }
        if (match(FOR)) {
            return forStatement();
        }
        if (match(IF)) {
            return ifStatement();
        }
        if (match(PRINT)) {
            return printStatement();
        }
        if (match(RETURN)) {
            return returnStatement();
        }
        if (match(WHILE)) {
            return whileStatement();
        }
        if (match(LEFT_BRACE)) {
            return new Stmt.Block(block());
        }
        return expressionStatement();
    }

    //Method for Print statement
    private Stmt printStatement() {
        Expr value = expression();
        consume(SEMICOLON, "Expect ';' after value");
        return new Stmt.Print(value);
    }

    private Stmt expressionStatement() {
        Expr value = expression();
        consume(SEMICOLON, "Expect ';' after value");
        return new Stmt.Expression(value);
    }

    private List<Stmt> block() {
        List<Stmt> statements = new ArrayList<>(); //Empty list

        while (!check(RIGHT_BRACE) && !isAtEnd()) { //Add statements until } 
            statements.add(declaration());
        }

        consume(RIGHT_BRACE, "Expect '}' after block.");
        return statements;
    }

    //Call repeatedly when parsing a series of statements in a block or script
    private Stmt declaration() {
        try {
            if (match(VAR)) {
                return varDeclaration();
            }
            return statement();
        } catch (ParseError error){ //Panic mode
            synchronize();
            return null;
        }
    }

    private Stmt varDeclaration() { //var beverage = "espresso";
        Token name = consume(IDENTIFIER, "Expect variable name.");

        Expr initializer = null;
        if (match(EQUAL)) {
            initializer = expression();
        }

        consume(SEMICOLON, "Expect ';' after variable declaration.");
        return new Stmt.Var(name, initializer);
    }

    private Expr assignment() {
        Expr expr = or();

        if (match(EQUAL)) {
            Token equals = previous();
            Expr value = assignment();

            if (expr instanceof Expr.Variable) {
                Token name = ((Expr.Variable)expr).name;
                return new Expr.Assign(name, value);
            }

            error(equals, "Invalid assignment target.");
        }

        return expr;
    }

    //Parse if statement
    private Stmt ifStatement() {
        consume(LEFT_PAREN, "Expect '(' after 'if'.");
        Expr condition = expression();
        consume(RIGHT_PAREN, "Expect ')' after if condition.");

        Stmt thenBranch = statement();
        Stmt elseBranch = null;
        if (match(ELSE)) {
            elseBranch = statement();
        }

        return new Stmt.If(condition, thenBranch, elseBranch);
    }

    private Expr or() {
        Expr expr = and();

        while (match(OR)) {
            Token operator = previous();
            Expr right = and();
            expr = new Expr.Logical(expr, operator, right);
        }

        return expr;
    }

    private Expr and() {
        Expr expr = equality();

        while (match(AND)) {
            Token operator = previous();
            Expr right = equality();
            expr = new Expr.Logical(expr, operator, right);
        }

        return expr;
    }

    private Stmt whileStatement() {
        consume(LEFT_PAREN, "Expect '(' after 'while'.");
        Expr condition = expression();
        consume(RIGHT_PAREN, "Expect ')' after 'condition.'");
        Stmt body = statement();

        return new Stmt.While(condition, body);
    }

    private Stmt forStatement() { //for (var i = 0; i < 10; i++)
        consume(LEFT_PAREN, "Expect '(' after 'for'.");

        Stmt initializer; //Initializer
        if (match(SEMICOLON)) {
            initializer = null;
        } else if (match(VAR)) {
            initializer = varDeclaration();
        } else {
            initializer = expressionStatement();
        }

        Expr condition = null; //Condition
        if (!check(SEMICOLON)) {
            condition = expression();
        }
        consume(SEMICOLON, "Expect ';' after loop condition.");

        Expr increment = null; //Increment
        if (!check(RIGHT_PAREN)) {
            increment = expression();
        }
        consume(RIGHT_PAREN, "Expect ')' after for clauses.");
        Stmt body = statement();

        if (increment != null) { //Increment after the body in each iteration of loop
            body = new Stmt.Block(Arrays.asList(body, new Stmt.Expression(increment)));
        }

        if (condition == null) { //Infinite loop if condition is ommitted
            condition = new Expr.Literal(true);
        }

        body = new Stmt.While(condition, body); //Build loop using while loop

        if (initializer != null) {
            body = new Stmt.Block(Arrays.asList(initializer, body));
        }

        return body;
    }

    private Expr call() {
        Expr expr = primary();

        while (true) {
            if (match(LEFT_PAREN)) {
                expr = finishCall(expr);
            } else {
                break;
            }
        }
        return expr;
    }

    private Expr finishCall(Expr callee) {
        List<Expr> arguments = new ArrayList<>();
        if (!check(RIGHT_PAREN)) {
            do { 
                if (arguments.size() >= 255) {
                    error(peek(), "Can't have more than 255 arguments.");
                }
                arguments.add(expression());
            } while (match(COMMA));
        }

        Token paren = consume(RIGHT_PAREN, "Expect ')' after arguments.");

        return new Expr.Call(callee, paren, arguments);
    }

    //Function declaration
    private Stmt.Function function(String kind) { //fun foo(a, b){}
        Token name = consume(IDENTIFIER, "Expect " + kind + "name.");
        consume(LEFT_PAREN, "Expect '(' after " + kind + "name.");
        List<Token> parameters = new ArrayList<>();
        if (!check(RIGHT_PAREN)) {
            do { 
                if (parameters.size() >= 255) {
                    error(peek(), "Can't have more than 255 parameters.");
                }
                parameters.add(consume(IDENTIFIER, "Expect parameter name."));
            } while (match(COMMA));
        }
        
        consume(RIGHT_PAREN, "Expect ')' after parameters.");

        consume(LEFT_BRACE, "Expect '{' before " + kind + "body.");
        List<Stmt> body = block();
        return new Stmt.Function(name, parameters, body);
    }

    private Stmt returnStatement() {
        Token keyword = previous();
        Expr value = null;
        if (!check(SEMICOLON)) {
            value = expression();
        }

        consume(SEMICOLON, "Expect ';' after return value");
        return new Stmt.Return(keyword, value);
    }
}

