package com.essa.compiler.parser.ast.type;

import com.essa.compiler.parser.ast.ASTNode;

/**
 * Base class for all type annotation nodes in the AST
 */
public abstract class TypeAnnotation extends ASTNode {
    public TypeAnnotation(int line, int column) {
        super(line, column);
    }
}