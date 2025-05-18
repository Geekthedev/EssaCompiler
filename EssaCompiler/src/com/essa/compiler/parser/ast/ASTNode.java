package com.essa.compiler.parser.ast;

import java.util.HashMap;
import java.util.Map;

/**
 * Base class for all Abstract Syntax Tree nodes
 */
public abstract class ASTNode {
    private int line;
    private int column;
    private Map<String, Object> attributes = new HashMap<>();
    
    public ASTNode(int line, int column) {
        this.line = line;
        this.column = column;
    }
    
    public int getLine() {
        return line;
    }
    
    public int getColumn() {
        return column;
    }
    
    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }
    
    public Object getAttribute(String key) {
        return attributes.get(key);
    }
    
    public boolean hasAttribute(String key) {
        return attributes.containsKey(key);
    }
    
    /**
     * Accept method for the visitor pattern
     */
    public abstract <T> T accept(ASTVisitor<T> visitor);
}