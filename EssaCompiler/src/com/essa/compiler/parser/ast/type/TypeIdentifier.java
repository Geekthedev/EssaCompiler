package com.essa.compiler.parser.ast.type;

import com.essa.compiler.parser.ast.ASTVisitor;

/**
 * Represents a type identifier in the AST
 */
public class TypeIdentifier extends TypeAnnotation {
    private String name;
    
    public TypeIdentifier(String name, int line, int column) {
        super(line, column);
        this.name = name;
    }
    
    public String getName() {
        return name;
    }
    
    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }
    
    @Override
    public String toString() {
        return name;
    }
}