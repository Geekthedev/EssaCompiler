package com.essa.compiler.parser.ast.expr;

import com.essa.compiler.parser.ast.ASTVisitor;

/**
 * Represents a binary expression in the AST
 */
public class BinaryExpr extends Expression {
    public enum Operator {
        ADD, SUBTRACT, MULTIPLY, DIVIDE, MODULO, POWER,
        EQUAL, NOT_EQUAL, STRICT_EQUAL, STRICT_NOT_EQUAL,
        GREATER_THAN, LESS_THAN, GREATER_THAN_EQUAL, LESS_THAN_EQUAL,
        AND, OR, BITWISE_AND, BITWISE_OR, BITWISE_XOR,
        LEFT_SHIFT, RIGHT_SHIFT, UNSIGNED_RIGHT_SHIFT
    }
    
    private Expression left;
    private Expression right;
    private Operator operator;
    
    public BinaryExpr(Expression left, Expression right, Operator operator, int line, int column) {
        super(line, column);
        this.left = left;
        this.right = right;
        this.operator = operator;
    }
    
    public Expression getLeft() {
        return left;
    }
    
    public Expression getRight() {
        return right;
    }
    
    public Operator getOperator() {
        return operator;
    }
    
    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }
}