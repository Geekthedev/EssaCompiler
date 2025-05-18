package com.essa.compiler.parser.ast;

import com.essa.compiler.parser.ast.expr.*;
import com.essa.compiler.parser.ast.stmt.*;
import com.essa.compiler.parser.ast.type.*;

/**
 * Visitor interface for traversing the Abstract Syntax Tree
 */
public interface ASTVisitor<T> {
    // Program
    T visit(Program program);
    
    // Statements
    T visit(BlockStmt stmt);
    T visit(VarDeclStmt stmt);
    T visit(FunctionDeclStmt stmt);
    T visit(ClassDeclStmt stmt);
    T visit(InterfaceDeclStmt stmt);
    T visit(ExpressionStmt stmt);
    T visit(ReturnStmt stmt);
    T visit(IfStmt stmt);
    T visit(WhileStmt stmt);
    T visit(ForStmt stmt);
    T visit(ImportStmt stmt);
    T visit(ExportStmt stmt);
    
    // Expressions
    T visit(BinaryExpr expr);
    T visit(UnaryExpr expr);
    T visit(CallExpr expr);
    T visit(MemberExpr expr);
    T visit(IndexExpr expr);
    T visit(AssignExpr expr);
    T visit(LiteralExpr expr);
    T visit(IdentifierExpr expr);
    T visit(ObjectExpr expr);
    T visit(ArrayExpr expr);
    T visit(NewExpr expr);
    T visit(FunctionExpr expr);
    T visit(ConditionalExpr expr);
    
    // Types
    T visit(TypeAnnotation type);
    T visit(ObjectType type);
    T visit(ArrayType type);
    T visit(FunctionType type);
    T visit(UnionType type);
    T visit(IntersectionType type);
    T visit(GenericType type);
}