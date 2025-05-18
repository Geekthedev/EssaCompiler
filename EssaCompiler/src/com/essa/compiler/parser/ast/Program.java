package com.essa.compiler.parser.ast;

import java.util.ArrayList;
import java.util.List;
import com.essa.compiler.parser.ast.stmt.Statement;

/**
 * Represents the root node of the AST for a complete program
 */
public class Program extends ASTNode {
    private List<Statement> statements = new ArrayList<>();
    private boolean isModule;
    
    public Program(int line, int column) {
        super(line, column);
        this.isModule = false;
    }
    
    public void addStatement(Statement statement) {
        statements.add(statement);
    }
    
    public List<Statement> getStatements() {
        return statements;
    }
    
    public boolean isModule() {
        return isModule;
    }
    
    public void setModule(boolean isModule) {
        this.isModule = isModule;
    }
    
    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }
}