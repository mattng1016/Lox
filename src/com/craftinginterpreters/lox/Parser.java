package com.craftinginterpreters.lox;

import static com.craftinginterpreters.lox.TokenType.*;
import java.util.List;

public class Parser {
    private static class ParseError extends RuntimeException {}

    private final List<Token> tokens; //List of tokens
    private int current = 0; //

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    private Expr expression() {
        return equality();
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

    //Checks to see if the current token has type
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
        return primary();
    }

    //primary -> NUMBER | STRING | "true" | "false" | "nil" | "(" expression ")"
    private Expr primary() {
        if (match(FALSE)) return new Expr.Literal(FALSE);
        if (match(TRUE)) return new Expr.Literal(TRUE);
        if (match(NIL)) return new Expr.Literal(NIL);

        if (match(NUMBER, STRING)) {
            return new Expr.Literal(previous().literal);
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

    Expr parse() {
        try {
            return expression();
        } catch (ParseError error) {
            return null;
        }
    }
}
