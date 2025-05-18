package com.essa.compiler.parser.ast.expr;

import com.essa.compiler.parser.ast.ASTVisitor;

/**
 * Represents a literal expression in the AST
 */
public class LiteralExpr extends Expression {
    public enum LiteralType {
        NUMBER, STRING, BOOLEAN, NULL, UNDEFINED
    }
    
    private Object value;
    private LiteralType literalType;
    
    public LiteralExpr(Object value, LiteralType literalType, int line, int column) {
        super(line, column);
        this.value = value;
        this.literalType = literalType;
    }
    
    public Object getValue() {
        return value;
    }
    
    public LiteralType getLiteralType() {
        return literalType;
    }
    
    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }
}