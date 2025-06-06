package com.essa.compiler.codegen;

import com.essa.compiler.parser.ast.*;
import com.essa.compiler.parser.ast.expr.*;
import com.essa.compiler.parser.ast.stmt.*;
import com.essa.compiler.parser.ast.type.*;
import com.essa.compiler.utils.CompilationContext;

/**
 * Code generator for the Essa compiler
 * Generates JavaScript code from the AST
 */
public class CodeGenerator implements ASTVisitor<String> {
    private final Program program;
    private final CompilationContext context;
    private int indentLevel = 0;
    private final String indentString = "  ";
    
    public CodeGenerator(Program program, CompilationContext context) {
        this.program = program;
        this.context = context;
    }
    
    public String generate() {
        return visit(program);
    }
    
    private String indent() {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < indentLevel; i++) {
            builder.append(indentString);
        }
        return builder.toString();
    }
    
    @Override
    public String visit(Program program) {
        StringBuilder result = new StringBuilder();
        
        // Add any necessary preamble
        if (program.isModule()) {
            result.append("// Generated by Essa Compiler\n");
            result.append("// TypeScript/JavaScript compilation output\n\n");
        }
        
        // Generate code for each statement
        for (Statement stmt : program.getStatements()) {
            result.append(stmt.accept(this));
            
            // Add newline if needed
            if (!(stmt instanceof BlockStmt)) {
                result.append("\n");
            }
        }
        
        return result.toString();
    }
    
    @Override
    public String visit(BlockStmt stmt) {
        StringBuilder result = new StringBuilder();
        
        result.append("{\n");
        indentLevel++;
        
        for (Statement s : stmt.getStatements()) {
            result.append(indent()).append(s.accept(this));
            
            // Add newline if needed
            if (!(s instanceof BlockStmt)) {
                result.append("\n");
            }
        }
        
        indentLevel--;
        result.append(indent()).append("}\n");
        
        return result.toString();
    }
    
    @Override
    public String visit(VarDeclStmt stmt) {
        StringBuilder result = new StringBuilder();
        
        // Variable declaration keyword
        if (stmt.isConst()) {
            result.append("const ");
        } else {
            result.append("let ");
        }
        
        result.append(stmt.getName());
        
        // Skip type annotation in JavaScript output
        
        // Initializer
        if (stmt.getInitializer() != null) {
            result.append(" = ").append(stmt.getInitializer().accept(this));
        }
        
        result.append(";");
        
        return result.toString();
    }
    
    @Override
    public String visit(FunctionDeclStmt stmt) {
        StringBuilder result = new StringBuilder();
        
        result.append("function ").append(stmt.getName()).append("(");
        
        // Parameters
        for (int i = 0; i < stmt.getParameters().size(); i++) {
            Parameter param = stmt.getParameters().get(i);
            result.append(param.getName());
            
            if (i < stmt.getParameters().size() - 1) {
                result.append(", ");
            }
        }
        
        result.append(") ");
        
        // Function body
        result.append(stmt.getBody().accept(this));
        
        return result.toString();
    }
    
    @Override
    public String visit(ClassDeclStmt stmt) {
        StringBuilder result = new StringBuilder();
        
        result.append("class ").append(stmt.getName());
        
        // Inheritance
        if (stmt.getSuperClass() != null) {
            result.append(" extends ").append(stmt.getSuperClass());
        }
        
        result.append(" {\n");
        indentLevel++;
        
        // Constructor
        boolean hasConstructor = false;
        for (ClassMember member : stmt.getMembers()) {
            if (member instanceof MethodMember && ((MethodMember) member).getName().equals("constructor")) {
                hasConstructor = true;
                break;
            }
        }
        
        if (!hasConstructor) {
            result.append(indent()).append("constructor() {\n");
            
            if (stmt.getSuperClass() != null) {
                result.append(indent()).append(indentString).append("super();\n");
            }
            
            // Initialize instance properties
            for (ClassMember member : stmt.getMembers()) {
                if (member instanceof PropertyMember && !((PropertyMember) member).isStatic()) {
                    PropertyMember prop = (PropertyMember) member;
                    
                    if (prop.getInitializer() != null) {
                        result.append(indent()).append(indentString)
                              .append("this.").append(prop.getName())
                              .append(" = ").append(prop.getInitializer().accept(this))
                              .append(";\n");
                    }
                }
            }
            
            result.append(indent()).append("}\n\n");
        }
        
        // Static properties
        for (ClassMember member : stmt.getMembers()) {
            if (member instanceof PropertyMember && ((PropertyMember) member).isStatic()) {
                PropertyMember prop = (PropertyMember) member;
                
                result.append(indent()).append("static ").append(prop.getName());
                
                if (prop.getInitializer() != null) {
                    result.append(" = ").append(prop.getInitializer().accept(this));
                }
                
                result.append(";\n");
            }
        }
        
        // Methods
        for (ClassMember member : stmt.getMembers()) {
            if (member instanceof MethodMember) {
                MethodMember method = (MethodMember) member;
                
                if (method.isStatic()) {
                    result.append(indent()).append("static ");
                }
                
                result.append(indent()).append(method.getName()).append("(");
                
                // Parameters
                for (int i = 0; i < method.getParameters().size(); i++) {
                    Parameter param = method.getParameters().get(i);
                    result.append(param.getName());
                    
                    if (i < method.getParameters().size() - 1) {
                        result.append(", ");
                    }
                }
                
                result.append(") ");
                
                // Method body
                result.append(method.getBody().accept(this));
            }
        }
        
        indentLevel--;
        result.append(indent()).append("}\n");
        
        return result.toString();
    }
    
    @Override
    public String visit(InterfaceDeclStmt stmt) {
        // Interfaces are not emitted in JavaScript output
        return "// Interface " + stmt.getName() + " (not emitted in JavaScript)";
    }
    
    @Override
    public String visit(ExpressionStmt stmt) {
        return stmt.getExpression().accept(this) + ";";
    }
    
    @Override
    public String visit(ReturnStmt stmt) {
        if (stmt.getValue() == null) {
            return "return;";
        } else {
            return "return " + stmt.getValue().accept(this) + ";";
        }
    }
    
    @Override
    public String visit(IfStmt stmt) {
        StringBuilder result = new StringBuilder();
        
        result.append("if (").append(stmt.getCondition().accept(this)).append(") ");
        
        // Then branch
        if (stmt.getThenBranch() instanceof BlockStmt) {
            result.append(stmt.getThenBranch().accept(this));
        } else {
            result.append("{\n");
            indentLevel++;
            result.append(indent()).append(stmt.getThenBranch().accept(this)).append("\n");
            indentLevel--;
            result.append(indent()).append("}");
        }
        
        // Else branch
        if (stmt.getElseBranch() != null) {
            result.append(" else ");
            
            if (stmt.getElseBranch() instanceof BlockStmt) {
                result.append(stmt.getElseBranch().accept(this));
            } else {
                result.append("{\n");
                indentLevel++;
                result.append(indent()).append(stmt.getElseBranch().accept(this)).append("\n");
                indentLevel--;
                result.append(indent()).append("}");
            }
        }
        
        return result.toString();
    }
    
    @Override
    public String visit(WhileStmt stmt) {
        StringBuilder result = new StringBuilder();
        
        result.append("while (").append(stmt.getCondition().accept(this)).append(") ");
        
        // Body
        if (stmt.getBody() instanceof BlockStmt) {
            result.append(stmt.getBody().accept(this));
        } else {
            result.append("{\n");
            indentLevel++;
            result.append(indent()).append(stmt.getBody().accept(this)).append("\n");
            indentLevel--;
            result.append(indent()).append("}");
        }
        
        return result.toString();
    }
    
    @Override
    public String visit(ForStmt stmt) {
        StringBuilder result = new StringBuilder();
        
        result.append("for (");
        
        // Initializer
        if (stmt.getInitializer() != null) {
            if (stmt.getInitializer() instanceof VarDeclStmt) {
                VarDeclStmt varDecl = (VarDeclStmt) stmt.getInitializer();
                
                // Variable declaration keyword
                if (varDecl.isConst()) {
                    result.append("const ");
                } else {
                    result.append("let ");
                }
                
                result.append(varDecl.getName());
                
                // Initializer
                if (varDecl.getInitializer() != null) {
                    result.append(" = ").append(varDecl.getInitializer().accept(this));
                }
            } else {
                result.append(stmt.getInitializer().accept(this));
            }
        }
        
        result.append("; ");
        
        // Condition
        if (stmt.getCondition() != null) {
            result.append(stmt.getCondition().accept(this));
        }
        
        result.append("; ");
        
        // Increment
        if (stmt.getIncrement() != null) {
            result.append(stmt.getIncrement().accept(this));
        }
        
        result.append(") ");
        
        // Body
        if (stmt.getBody() instanceof BlockStmt) {
            result.append(stmt.getBody().accept(this));
        } else {
            result.append("{\n");
            indentLevel++;
            result.append(indent()).append(stmt.getBody().accept(this)).append("\n");
            indentLevel--;
            result.append(indent()).append("}");
        }
        
        return result.toString();
    }
    
    @Override
    public String visit(ImportStmt stmt) {
        StringBuilder result = new StringBuilder();
        
        result.append("// Import statement: ");
        
        for (ImportSpecifier specifier : stmt.getSpecifiers()) {
            if (specifier.getImported().equals("*")) {
                result.append("* as ").append(specifier.getLocal());
            } else if (specifier.getImported().equals("default")) {
                result.append(specifier.getLocal());
            } else {
                if (!specifier.getImported().equals(specifier.getLocal())) {
                    result.append(specifier.getImported()).append(" as ").append(specifier.getLocal());
                } else {
                    result.append(specifier.getLocal());
                }
            }
            result.append(", ");
        }
        
        result.append("from '").append(stmt.getSource()).append("'");
        
        return result.toString();
    }
    
    @Override
    public String visit(ExportStmt stmt) {
        StringBuilder result = new StringBuilder();
        
        result.append("// Export statement");
        
        return result.toString();
    }
    
    @Override
    public String visit(BinaryExpr expr) {
        String operator;
        
        switch (expr.getOperator()) {
            case ADD: operator = "+"; break;
            case SUBTRACT: operator = "-"; break;
            case MULTIPLY: operator = "*"; break;
            case DIVIDE: operator = "/"; break;
            case MODULO: operator = "%"; break;
            case POWER: operator = "**"; break;
            case EQUAL: operator = "=="; break;
            case NOT_EQUAL: operator = "!="; break;
            case STRICT_EQUAL: operator = "==="; break;
            case STRICT_NOT_EQUAL: operator = "!=="; break;
            case GREATER_THAN: operator = ">"; break;
            case LESS_THAN: operator = "<"; break;
            case GREATER_THAN_EQUAL: operator = ">="; break;
            case LESS_THAN_EQUAL: operator = "<="; break;
            case AND: operator = "&&"; break;
            case OR: operator = "||"; break;
            case BITWISE_AND: operator = "&"; break;
            case BITWISE_OR: operator = "|"; break;
            case BITWISE_XOR: operator = "^"; break;
            case LEFT_SHIFT: operator = "<<"; break;
            case RIGHT_SHIFT: operator = ">>"; break;
            case UNSIGNED_RIGHT_SHIFT: operator = ">>>"; break;
            default: operator = "?"; break;
        }
        
        return "(" + expr.getLeft().accept(this) + " " + operator + " " + expr.getRight().accept(this) + ")";
    }
    
    @Override
    public String visit(UnaryExpr expr) {
        String operator;
        
        switch (expr.getOperator()) {
            case NOT: operator = "!"; break;
            case NEGATE: operator = "-"; break;
            case PLUS: operator = "+"; break;
            case PREFIX_INCREMENT: operator = "++"; break;
            case PREFIX_DECREMENT: operator = "--"; break;
            case POSTFIX_INCREMENT: operator = "++"; break;
            case POSTFIX_DECREMENT: operator = "--"; break;
            case TYPEOF: operator = "typeof "; break;
            default: operator = "?"; break;
        }
        
        if (expr.isPrefix()) {
            return operator + expr.getOperand().accept(this);
        } else {
            return expr.getOperand().accept(this) + operator;
        }
    }
    
    @Override
    public String visit(CallExpr expr) {
        StringBuilder result = new StringBuilder();
        
        result.append(expr.getCallee().accept(this)).append("(");
        
        // Arguments
        for (int i = 0; i < expr.getArguments().size(); i++) {
            result.append(expr.getArguments().get(i).accept(this));
            
            if (i < expr.getArguments().size() - 1) {
                result.append(", ");
            }
        }
        
        result.append(")");
        
        return result.toString();
    }
    
    @Override
    public String visit(MemberExpr expr) {
        if (expr.isOptional()) {
            return expr.getObject().accept(this) + "?." + expr.getName();
        } else {
            return expr.getObject().accept(this) + "." + expr.getName();
        }
    }
    
    @Override
    public String visit(IndexExpr expr) {
        return expr.getObject().accept(this) + "[" + expr.getIndex().accept(this) + "]";
    }
    
    @Override
    public String visit(AssignExpr expr) {
        String operator;
        
        switch (expr.getOperator()) {
            case ASSIGN: operator = "="; break;
            case PLUS_ASSIGN: operator = "+="; break;
            case MINUS_ASSIGN: operator = "-="; break;
            case MULTIPLY_ASSIGN: operator = "*="; break;
            case DIVIDE_ASSIGN: operator = "/="; break;
            case MODULO_ASSIGN: operator = "%="; break;
            default: operator = "="; break;
        }
        
        return expr.getTarget().accept(this) + " " + operator + " " + expr.getValue().accept(this);
    }
    
    @Override
    public String visit(LiteralExpr expr) {
        switch (expr.getLiteralType()) {
            case NUMBER:
                return expr.getValue().toString();
                
            case STRING:
                return "\"" + escapeString((String) expr.getValue()) + "\"";
                
            case BOOLEAN:
                return expr.getValue().toString();
                
            case NULL:
                return "null";
                
            case UNDEFINED:
                return "undefined";
                
            default:
                return "/* unknown literal */";
        }
    }
    
    private String escapeString(String str) {
        return str.replace("\\", "\\\\")
                 .replace("\"", "\\\"")
                 .replace("\n", "\\n")
                 .replace("\r", "\\r")
                 .replace("\t", "\\t");
    }
    
    @Override
    public String visit(IdentifierExpr expr) {
        return expr.getName();
    }
    
    @Override
    public String visit(ObjectExpr expr) {
        if (expr.getProperties().isEmpty()) {
            return "{}";
        }
        
        StringBuilder result = new StringBuilder();
        
        result.append("{\n");
        indentLevel++;
        
        for (int i = 0; i < expr.getProperties().size(); i++) {
            ObjectProperty prop = expr.getProperties().get(i);
            
            result.append(indent())
                  .append(prop.getKey())
                  .append(": ")
                  .append(prop.getValue().accept(this));
            
            if (i < expr.getProperties().size() - 1) {
                result.append(",");
            }
            
            result.append("\n");
        }
        
        indentLevel--;
        result.append(indent()).append("}");
        
        return result.toString();
    }
    
    @Override
    public String visit(ArrayExpr expr) {
        if (expr.getElements().isEmpty()) {
            return "[]";
        }
        
        StringBuilder result = new StringBuilder();
        
        result.append("[");
        
        for (int i = 0; i < expr.getElements().size(); i++) {
            result.append(expr.getElements().get(i).accept(this));
            
            if (i < expr.getElements().size() - 1) {
                result.append(", ");
            }
        }
        
        result.append("]");
        
        return result.toString();
    }
    
    @Override
    public String visit(NewExpr expr) {
        StringBuilder result = new StringBuilder();
        
        result.append("new ").append(expr.getConstructor().accept(this)).append("(");
        
        // Arguments
        for (int i = 0; i < expr.getArguments().size(); i++) {
            result.append(expr.getArguments().get(i).accept(this));
            
            if (i < expr.getArguments().size() - 1) {
                result.append(", ");
            }
        }
        
        result.append(")");
        
        return result.toString();
    }
    
    @Override
    public String visit(FunctionExpr expr) {
        StringBuilder result = new StringBuilder();
        
        if (expr.getName() != null) {
            result.append("function ").append(expr.getName()).append("(");
        } else {
            result.append("function(");
        }
        
        // Parameters
        for (int i = 0; i < expr.getParameters().size(); i++) {
            Parameter param = expr.getParameters().get(i);
            result.append(param.getName());
            
            if (i < expr.getParameters().size() - 1) {
                result.append(", ");
            }
        }
        
        result.append(") ");
        
        // Function body
        result.append(expr.getBody().accept(this));
        
        return result.toString();
    }
    
    @Override
    public String visit(ConditionalExpr expr) {
        return "(" + expr.getCondition().accept(this) + " ? " + 
               expr.getThenExpr().accept(this) + " : " + 
               expr.getElseExpr().accept(this) + ")";
    }
    
    // Type annotations are not emitted in JavaScript
    
    @Override
    public String visit(TypeAnnotation type) {
        return "";
    }
    
    @Override
    public String visit(ObjectType type) {
        return "";
    }
    
    @Override
    public String visit(ArrayType type) {
        return "";
    }
    
    @Override
    public String visit(FunctionType type) {
        return "";
    }
    
    @Override
    public String visit(UnionType type) {
        return "";
    }
    
    @Override
    public String visit(IntersectionType type) {
        return "";
    }
    
    @Override
    public String visit(GenericType type) {
        return "";
    }
}