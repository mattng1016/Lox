package com.craftinginterpreters.lox;

import static com.craftinginterpreters.lox.TokenType.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class Scanner {
    private final String source; //Source code stored as String
    private final List<Token> tokens = new ArrayList<>(); //Array list of tokens
    private int start = 0; //Points to the first character being scanned
    private int current = 0; //Points to the character that is being scanned
    private int line = 1; //Tracks what line current is on

    //HashMap for important keywords
    private static final Map<String, TokenType> keywords;
    static {
        keywords = new HashMap<>();
        keywords.put("and", AND);
        keywords.put("class", CLASS);
        keywords.put("else", ELSE);
        keywords.put("false", FALSE);
        keywords.put("for", FOR);
        keywords.put("fun", FUN);
        keywords.put("if", IF);
        keywords.put("nil", NIL);
        keywords.put("or", OR);
        keywords.put("print", PRINT);
        keywords.put("return", RETURN);
        keywords.put("super", SUPER);
        keywords.put("this", THIS);
        keywords.put("true", TRUE);
        keywords.put("var", VAR);
        keywords.put("while", WHILE);
    }

    //Constructor for Scanner
    Scanner(String source) {
        this.source = source;
    }

    //Scans the entire line on source
    List<Token> scanTokens() {
        while (!isAtEnd()) { //Next lexeme
            start = current;
            scanToken();
        }
        //End of line token
        tokens.add(new Token(EOF, "", null, line)); 
        return tokens; 
    }

    //Checks if we have scanned the entire line
    private boolean isAtEnd() {
        return current >= source.length();
    }

    //Adds token depending on the character
    private void scanToken() {
        char c = advance();
        switch (c) {
            //Single characters
            case '(': addToken(LEFT_PAREN); break;
            case ')': addToken(RIGHT_PAREN); break;
            case '{': addToken(LEFT_BRACE); break;
            case '}': addToken(RIGHT_BRACE); break;
            case ',': addToken(COMMA); break;
            case '.': addToken(DOT); break;
            case '-': addToken(MINUS); break;
            case '+': addToken(PLUS); break;
            case ';': addToken(SEMICOLON); break;
            case '*': addToken(STAR); break;

            //One or two character token
            case '!':
                addToken(match('=') ? BANG_EQUAL : BANG);
                break;
            case '=':
                addToken(match('=') ? EQUAL_EQUAL : EQUAL);
                break;
            case '<':
                addToken(match('=') ? LESS_EQUAL : LESS);
                break;
            case '>':
                addToken(match('=') ? GREATER_EQUAL : GREATER);
                break;
            case '/':
                if (match('/')) {
                    if (peek() == '*') {
                        while (peek() != '*' && peekNext() != '/') {
                            if (isAtEnd()) {
                                line++;
                                advance();
                            }
                            advance();
                        }
                    }
                    while (peek() != '\n' && !isAtEnd()) {
                        advance();
                    }

                } else {
                    addToken(SLASH);
                }
                break;

            //Whitespace
            case ' ': 
            case '\r':
            case '\t':
                break;

            //Line
            case '\n':
                line++;
                break;

            //String
            case '"':
                string();
                break;
                
            case 'o':
                if (peek() == 'r') {
                    addToken(OR);
                }
                break;

            default:
                if (isDigit(c)) {
                    number();
                } else if (isAlpha(c)) {
                    identifier();
                } else {
                    Lox.error(line, "Unexpected character");
                    break;
                }
        }
    }

    //Returns the current character and advances
    private char advance() {
        current++;
        return source.charAt(current - 1);
    }

    //Creates a new token 
    private void addToken(TokenType type) {
        addToken(type, null);
    }

    //Creates a new token for literal values
    private void addToken(TokenType type, Object literal) {
        String text = source.substring(start, current);
        tokens.add(new Token(type, text, literal, line));   
    }

    //advances if the current character is same as parameter
    private boolean match(char expected) {
        if (isAtEnd()) {
            return false;
        }
        if (source.charAt(current) != expected) {
            return false;
        }
        current++;
        return true;
    }   

    //Peeks the next character 
    private char peek() {
        if (isAtEnd()) {
            return '\0'; //Returns null terminator if line is ending
        }
        return source.charAt(current);
    }

    //Adds token for string
    private void string() {
        while (peek() != '"' && !isAtEnd()) { //While loop that goes through the entire string
            if (peek() == '\n') {
                line++;
            }
            advance();
        }

        if (isAtEnd()) {
            Lox.error(line, "Unterminated string.");
            return;
        }

        advance(); //Closing "

        String value = source.substring(start + 1, current - 1); //Trims the quotations with +1, -1
        addToken(STRING, value);
    }

    //Checks if input parameter is a digit
    private boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    //Adds token for numbers
    private void number() {
        //Goes through the entire number
        while (isDigit(peek())) {
            advance();
        }

        //Looks for decimal point
        if (peek() == '.' && isDigit(peekNext())) { 
            advance();
        }

        //Goes through the remaining digits if decimal point
        while (isDigit(peek())) {
            advance();
        }

        addToken(NUMBER, Double.parseDouble(source.substring(start, current)));
    }
        
    //Adds token for identifier (e.g. variable names)
    private void identifier() {
        //Goes through the identifiers 
        while (isAlphaNumeric(peek())) {
            advance();
        }
        //Checks if the text is a keyword in the HashMap
        String text = source.substring(start, current);
        TokenType type = keywords.get(text); 
        if (type == null) { 
            type = IDENTIFIER;
        }
        addToken(type);
    }

    //Peeks for the next next character
    private char peekNext() {
        if (current + 1 >= source.length()) {
            return '\0';
        }
        return source.charAt(current + 1);
    }

    //Checks if the input parameter is a letter or underscore
    private boolean isAlpha(char c) {
        return (c >= 'a' && c <= 'z')||(c >= 'A' && c <= 'Z')||c == '_';
    }

    //Checks if the input parameter is a digit or alpha
    private boolean isAlphaNumeric(char c) {
        return isAlpha(c) || isDigit(c);
    }

    
}