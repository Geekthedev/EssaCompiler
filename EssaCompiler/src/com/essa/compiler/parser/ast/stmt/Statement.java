package com.essa.compiler.parser.ast.stmt;

import com.essa.compiler.parser.ast.ASTNode;

/**
 * Base class for all statement nodes in the AST
 */
public abstract class Statement extends ASTNode {
    public Statement(int line, int column) {
        super(line, column);
    }
}