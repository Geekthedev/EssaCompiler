package com.essa.compiler.parser.ast.expr;

import com.essa.compiler.parser.ast.ASTVisitor;

/**
 * Represents an identifier expression in the AST
 */
public class IdentifierExpr extends Expression {
    private String name;
    
    public IdentifierExpr(String name, int line, int column) {
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
}