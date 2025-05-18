package com.essa.compiler.parser.ast.expr;

import com.essa.compiler.parser.ast.ASTNode;
import com.essa.compiler.parser.ast.type.TypeAnnotation;

/**
 * Base class for all expression nodes in the AST
 */
public abstract class Expression extends ASTNode {
    private TypeAnnotation type;
    
    public Expression(int line, int column) {
        super(line, column);
    }
    
    public TypeAnnotation getType() {
        return type;
    }
    
    public void setType(TypeAnnotation type) {
        this.type = type;
    }
}