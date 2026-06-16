package com.craftinginterpreters.lox;

import java.util.HashMap;
import java.util.Map;

class Environment {
    final Environment enclosing; //Ref to enclosing
    private final Map<String, Object> values = new HashMap<>();

    Environment() { //For global scope
        enclosing = null;
    }

    Environment(Environment enclosing) { //Creates new local scope nested inside the given outer one
        this.enclosing = enclosing;
    }

    //Gets the value by name
    Object get(Token name) {
        if (values.containsKey(name.lexeme)) {
            return values.get(name.lexeme);
        }
        if (enclosing != null) { //Recursively checks outer scope
            return enclosing.get(name);
        }
        throw new RuntimeError(name, "Undefined variable '" + name.lexeme + "'.");
    }

    //Binds a new name to a value 
    void define(String name, Object value) {
        values.put(name, value);
    }

    //Doesnt create new variable, runtime erorr if name doesn't exist in runtime
    void assign(Token name, Object value) {
        if (values.containsKey(name.lexeme)) {
            values.put(name.lexeme, value);
            return;
        }

        if (enclosing != null) { //Recursively checks outer scope
            enclosing.assign(name, value);
            return;
        }

        throw new RuntimeError(name, "Undefined variable '" + name.lexeme + "'.");
    }

    Object getAt(int distance, String name) {
        return ancestor(distance).values.get(name);
    }

    void assignAt(int distance, Token name, Object value) {
        ancestor(distance).values.put(name.lexeme, value);
    }

    Environment ancestor(int distance) {
        Environment environment = this;
        for (int i = 0; i < distance; i++) {
            environment = environment.enclosing;
        }   
        return environment;
    }
}
