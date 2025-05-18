package com.essa.compiler.semantic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Symbol table for tracking variables, functions, and types during semantic analysis
 */
public class SymbolTable {
    private final List<Map<String, Object>> scopes = new ArrayList<>();
    
    public SymbolTable() {
        // Start with an empty scope
        enterScope();
    }
    
    public void enterScope() {
        scopes.add(new HashMap<>());
    }
    
    public void exitScope() {
        if (!scopes.isEmpty()) {
            scopes.remove(scopes.size() - 1);
        }
    }
    
    public void define(String name, Object symbol) {
        if (scopes.isEmpty()) {
            return;
        }
        
        // Define in current scope
        scopes.get(scopes.size() - 1).put(name, symbol);
    }
    
    public Object resolve(String name) {
        // Look for the symbol in all scopes, from innermost to outermost
        for (int i = scopes.size() - 1; i >= 0; i--) {
            Map<String, Object> scope = scopes.get(i);
            if (scope.containsKey(name)) {
                return scope.get(name);
            }
        }
        
        return null;
    }
    
    public Object resolveLocal(String name) {
        // Look for the symbol only in the current scope
        if (!scopes.isEmpty()) {
            Map<String, Object> currentScope = scopes.get(scopes.size() - 1);
            if (currentScope.containsKey(name)) {
                return currentScope.get(name);
            }
        }
        
        return null;
    }
    
    public boolean isDefined(String name) {
        return resolve(name) != null;
    }
    
    public boolean isDefinedInCurrentScope(String name) {
        return resolveLocal(name) != null;
    }
}