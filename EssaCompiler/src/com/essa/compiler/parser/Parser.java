package com.essa.compiler.parser;

import java.util.ArrayList;
import java.util.List;

import com.essa.compiler.lexer.Token;
import com.essa.compiler.lexer.TokenType;
import com.essa.compiler.parser.ast.Program;
import com.essa.compiler.parser.ast.expr.*;
import com.essa.compiler.parser.ast.stmt.*;
import com.essa.compiler.parser.ast.type.*;
import com.essa.compiler.utils.ErrorReporter;

/**
 * Parser for the Essa compiler
 * Constructs an AST from a list of tokens
 */
public class Parser {
    private final List<Token> tokens;
    private final ErrorReporter errorReporter;
    private int current = 0;
    
    public Parser(List<Token> tokens, ErrorReporter errorReporter) {
        this.tokens = tokens;
        this.errorReporter = errorReporter;
    }
    
    public Program parse() {
        try {
            return parseProgram();
        } catch (Exception e) {
            // Log unexpected errors
            Token token = peek();
            errorReporter.report(token.getLine(), token.getColumn(), 
                                 "Unexpected error during parsing: " + e.getMessage());
            e.printStackTrace();
            return new Program(token.getLine(), token.getColumn());
        }
    }
    
    private Program parseProgram() {
        Token token = peek();
        Program program = new Program(token.getLine(), token.getColumn());
        
        // Check if it's a module (has imports or exports)
        for (Token t : tokens) {
            if (t.getType() == TokenType.IMPORT || t.getType() == TokenType.EXPORT) {
                program.setModule(true);
                break;
            }
        }
        
        while (!isAtEnd() && peek().getType() != TokenType.EOF) {
            try {
                Statement stmt = parseStatement();
                program.addStatement(stmt);
            } catch (Exception e) {
                errorReporter.report(peek().getLine(), peek().getColumn(), "Error parsing statement: " + e.getMessage());
                synchronize();
            }
        }
        
        return program;
    }
    
    private Statement parseStatement() {
        Token token = peek();
        
        switch (token.getType()) {
            case LET:
            case CONST:
            case VAR:
                return parseVariableDeclaration();
            case FUNCTION:
                return parseFunctionDeclaration();
            case CLASS:
                return parseClassDeclaration();
            case INTERFACE:
                return parseInterfaceDeclaration();
            case IF:
                return parseIfStatement();
            case FOR:
                return parseForStatement();
            case WHILE:
                return parseWhileStatement();
            case RETURN:
                return parseReturnStatement();
            case IMPORT:
                return parseImportStatement();
            case EXPORT:
                return parseExportStatement();
            case LEFT_BRACE:
                return parseBlockStatement();
            case SEMICOLON:
                advance();
                return new EmptyStmt(token.getLine(), token.getColumn());
            default:
                return parseExpressionStatement();
        }
    }
    
    private Statement parseVariableDeclaration() {
        Token token = advance(); // Consume let/const/var
        TokenType declarationType = token.getType();
        
        String name = consume(TokenType.IDENTIFIER, "Expected variable name").getValue();
        
        TypeAnnotation typeAnnotation = null;
        if (check(TokenType.COLON)) {
            advance(); // Consume colon
            typeAnnotation = parseTypeAnnotation();
        }
        
        Expression initializer = null;
        if (match(TokenType.ASSIGN)) {
            initializer = parseExpression();
        }
        
        consume(TokenType.SEMICOLON, "Expected ';' after variable declaration");
        
        boolean isConst = declarationType == TokenType.CONST;
        return new VarDeclStmt(name, typeAnnotation, initializer, isConst, token.getLine(), token.getColumn());
    }
    
    private FunctionDeclStmt parseFunctionDeclaration() {
        Token token = advance(); // Consume 'function'
        
        String name = consume(TokenType.IDENTIFIER, "Expected function name").getValue();
        
        consume(TokenType.LEFT_PAREN, "Expected '(' after function name");
        List<Parameter> parameters = parseParameterList();
        consume(TokenType.RIGHT_PAREN, "Expected ')' after parameters");
        
        TypeAnnotation returnType = null;
        if (match(TokenType.COLON)) {
            returnType = parseTypeAnnotation();
        }
        
        BlockStmt body = parseBlockStatement();
        
        return new FunctionDeclStmt(name, parameters, returnType, body, token.getLine(), token.getColumn());
    }
    
    private List<Parameter> parseParameterList() {
        List<Parameter> parameters = new ArrayList<>();
        
        if (!check(TokenType.RIGHT_PAREN)) {
            do {
                Token paramToken = peek();
                String name = consume(TokenType.IDENTIFIER, "Expected parameter name").getValue();
                
                TypeAnnotation type = null;
                if (match(TokenType.COLON)) {
                    type = parseTypeAnnotation();
                }
                
                parameters.add(new Parameter(name, type, paramToken.getLine(), paramToken.getColumn()));
                
            } while (match(TokenType.COMMA));
        }
        
        return parameters;
    }
    
    private ClassDeclStmt parseClassDeclaration() {
        Token token = advance(); // Consume 'class'
        
        String name = consume(TokenType.IDENTIFIER, "Expected class name").getValue();
        
        String superClass = null;
        if (match(TokenType.EXTENDS)) {
            superClass = consume(TokenType.IDENTIFIER, "Expected superclass name").getValue();
        }
        
        List<String> interfaces = new ArrayList<>();
        if (match(TokenType.IMPLEMENTS)) {
            do {
                interfaces.add(consume(TokenType.IDENTIFIER, "Expected interface name").getValue());
            } while (match(TokenType.COMMA));
        }
        
        consume(TokenType.LEFT_BRACE, "Expected '{' before class body");
        
        List<ClassMember> members = parseClassMembers();
        
        consume(TokenType.RIGHT_BRACE, "Expected '}' after class body");
        
        return new ClassDeclStmt(name, superClass, interfaces, members, token.getLine(), token.getColumn());
    }
    
    private List<ClassMember> parseClassMembers() {
        List<ClassMember> members = new ArrayList<>();
        
        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            try {
                Token memberToken = peek();
                
                // Parse access modifier if present
                AccessModifier accessModifier = AccessModifier.PUBLIC; // Default
                boolean isStatic = false;
                boolean isReadonly = false;
                
                while (check(TokenType.PUBLIC) || check(TokenType.PRIVATE) || 
                       check(TokenType.PROTECTED) || check(TokenType.STATIC) || 
                       check(TokenType.READONLY)) {
                    
                    TokenType type = advance().getType();
                    switch (type) {
                        case PUBLIC: accessModifier = AccessModifier.PUBLIC; break;
                        case PRIVATE: accessModifier = AccessModifier.PRIVATE; break;
                        case PROTECTED: accessModifier = AccessModifier.PROTECTED; break;
                        case STATIC: isStatic = true; break;
                        case READONLY: isReadonly = true; break;
                    }
                }
                
                // Parse member type
                if (check(TokenType.FUNCTION) || check(TokenType.IDENTIFIER) && peek(1).getType() == TokenType.LEFT_PAREN) {
                    // Method
                    String name;
                    if (check(TokenType.FUNCTION)) {
                        advance(); // Consume 'function'
                        name = consume(TokenType.IDENTIFIER, "Expected method name").getValue();
                    } else {
                        name = advance().getValue(); // Get identifier
                    }
                    
                    consume(TokenType.LEFT_PAREN, "Expected '(' after method name");
                    List<Parameter> parameters = parseParameterList();
                    consume(TokenType.RIGHT_PAREN, "Expected ')' after parameters");
                    
                    TypeAnnotation returnType = null;
                    if (match(TokenType.COLON)) {
                        returnType = parseTypeAnnotation();
                    }
                    
                    BlockStmt body = parseBlockStatement();
                    
                    members.add(new MethodMember(name, parameters, returnType, body, 
                                              accessModifier, isStatic, 
                                              memberToken.getLine(), memberToken.getColumn()));
                } else {
                    // Property
                    String name = consume(TokenType.IDENTIFIER, "Expected property name").getValue();
                    
                    TypeAnnotation type = null;
                    if (match(TokenType.COLON)) {
                        type = parseTypeAnnotation();
                    }
                    
                    Expression initializer = null;
                    if (match(TokenType.ASSIGN)) {
                        initializer = parseExpression();
                    }
                    
                    consume(TokenType.SEMICOLON, "Expected ';' after property declaration");
                    
                    members.add(new PropertyMember(name, type, initializer, 
                                               accessModifier, isStatic, isReadonly,
                                               memberToken.getLine(), memberToken.getColumn()));
                }
            } catch (Exception e) {
                errorReporter.report(peek().getLine(), peek().getColumn(), 
                                    "Error parsing class member: " + e.getMessage());
                synchronize();
            }
        }
        
        return members;
    }
    
    private InterfaceDeclStmt parseInterfaceDeclaration() {
        Token token = advance(); // Consume 'interface'
        
        String name = consume(TokenType.IDENTIFIER, "Expected interface name").getValue();
        
        List<String> extendsList = new ArrayList<>();
        if (match(TokenType.EXTENDS)) {
            do {
                extendsList.add(consume(TokenType.IDENTIFIER, "Expected interface name").getValue());
            } while (match(TokenType.COMMA));
        }
        
        consume(TokenType.LEFT_BRACE, "Expected '{' before interface body");
        
        List<InterfaceMember> members = parseInterfaceMembers();
        
        consume(TokenType.RIGHT_BRACE, "Expected '}' after interface body");
        
        return new InterfaceDeclStmt(name, extendsList, members, token.getLine(), token.getColumn());
    }
    
    private List<InterfaceMember> parseInterfaceMembers() {
        List<InterfaceMember> members = new ArrayList<>();
        
        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            try {
                Token memberToken = peek();
                
                // Parse readonly if present
                boolean isReadonly = match(TokenType.READONLY);
                
                // Parse member
                String name = consume(TokenType.IDENTIFIER, "Expected member name").getValue();
                
                if (check(TokenType.LEFT_PAREN)) {
                    // Method signature
                    consume(TokenType.LEFT_PAREN, "Expected '(' after method name");
                    List<Parameter> parameters = parseParameterList();
                    consume(TokenType.RIGHT_PAREN, "Expected ')' after parameters");
                    
                    consume(TokenType.COLON, "Expected return type for interface method");
                    TypeAnnotation returnType = parseTypeAnnotation();
                    
                    consume(TokenType.SEMICOLON, "Expected ';' after method signature");
                    
                    members.add(new InterfaceMethodMember(name, parameters, returnType, 
                                                      memberToken.getLine(), memberToken.getColumn()));
                } else {
                    // Property signature
                    consume(TokenType.COLON, "Expected type annotation for interface property");
                    TypeAnnotation type = parseTypeAnnotation();
                    
                    consume(TokenType.SEMICOLON, "Expected ';' after property signature");
                    
                    members.add(new InterfacePropertyMember(name, type, isReadonly, 
                                                        memberToken.getLine(), memberToken.getColumn()));
                }
            } catch (Exception e) {
                errorReporter.report(peek().getLine(), peek().getColumn(), 
                                    "Error parsing interface member: " + e.getMessage());
                synchronize();
            }
        }
        
        return members;
    }
    
    private IfStmt parseIfStatement() {
        Token token = advance(); // Consume 'if'
        
        consume(TokenType.LEFT_PAREN, "Expected '(' after 'if'");
        Expression condition = parseExpression();
        consume(TokenType.RIGHT_PAREN, "Expected ')' after if condition");
        
        Statement thenBranch = parseStatement();
        
        Statement elseBranch = null;
        if (match(TokenType.ELSE)) {
            elseBranch = parseStatement();
        }
        
        return new IfStmt(condition, thenBranch, elseBranch, token.getLine(), token.getColumn());
    }
    
    private WhileStmt parseWhileStatement() {
        Token token = advance(); // Consume 'while'
        
        consume(TokenType.LEFT_PAREN, "Expected '(' after 'while'");
        Expression condition = parseExpression();
        consume(TokenType.RIGHT_PAREN, "Expected ')' after while condition");
        
        Statement body = parseStatement();
        
        return new WhileStmt(condition, body, token.getLine(), token.getColumn());
    }
    
    private ForStmt parseForStatement() {
        Token token = advance(); // Consume 'for'
        
        consume(TokenType.LEFT_PAREN, "Expected '(' after 'for'");
        
        Statement initializer;
        if (match(TokenType.SEMICOLON)) {
            initializer = null;
        } else if (match(TokenType.LET) || match(TokenType.CONST) || match(TokenType.VAR)) {
            initializer = parseVariableDeclaration();
        } else {
            initializer = parseExpressionStatement();
        }
        
        Expression condition = null;
        if (!check(TokenType.SEMICOLON)) {
            condition = parseExpression();
        }
        consume(TokenType.SEMICOLON, "Expected ';' after loop condition");
        
        Expression increment = null;
        if (!check(TokenType.RIGHT_PAREN)) {
            increment = parseExpression();
        }
        consume(TokenType.RIGHT_PAREN, "Expected ')' after for clauses");
        
        Statement body = parseStatement();
        
        return new ForStmt(initializer, condition, increment, body, token.getLine(), token.getColumn());
    }
    
    private ReturnStmt parseReturnStatement() {
        Token token = advance(); // Consume 'return'
        
        Expression value = null;
        if (!check(TokenType.SEMICOLON)) {
            value = parseExpression();
        }
        
        consume(TokenType.SEMICOLON, "Expected ';' after return value");
        
        return new ReturnStmt(value, token.getLine(), token.getColumn());
    }
    
    private ImportStmt parseImportStatement() {
        Token token = advance(); // Consume 'import'
        
        List<ImportSpecifier> specifiers = new ArrayList<>();
        
        if (check(TokenType.LEFT_BRACE)) {
            // Named imports
            advance(); // Consume '{'
            
            if (!check(TokenType.RIGHT_BRACE)) {
                do {
                    String imported = consume(TokenType.IDENTIFIER, "Expected imported name").getValue();
                    String local = imported;
                    
                    if (match(TokenType.AS)) {
                        local = consume(TokenType.IDENTIFIER, "Expected local name").getValue();
                    }
                    
                    specifiers.add(new ImportSpecifier(imported, local, token.getLine(), token.getColumn()));
                } while (match(TokenType.COMMA));
            }
            
            consume(TokenType.RIGHT_BRACE, "Expected '}' after import specifiers");
        } else if (check(TokenType.IDENTIFIER)) {
            // Default import
            String local = consume(TokenType.IDENTIFIER, "Expected default import name").getValue();
            specifiers.add(new ImportSpecifier("default", local, token.getLine(), token.getColumn()));
            
            if (match(TokenType.COMMA)) {
                consume(TokenType.LEFT_BRACE, "Expected '{' after default import");
                
                if (!check(TokenType.RIGHT_BRACE)) {
                    do {
                        String imported = consume(TokenType.IDENTIFIER, "Expected imported name").getValue();
                        String namedLocal = imported;
                        
                        if (match(TokenType.AS)) {
                            namedLocal = consume(TokenType.IDENTIFIER, "Expected local name").getValue();
                        }
                        
                        specifiers.add(new ImportSpecifier(imported, namedLocal, token.getLine(), token.getColumn()));
                    } while (match(TokenType.COMMA));
                }
                
                consume(TokenType.RIGHT_BRACE, "Expected '}' after import specifiers");
            }
        } else if (check(TokenType.MULTIPLY)) {
            // Namespace import
            advance(); // Consume '*'
            consume(TokenType.AS, "Expected 'as' after '*'");
            String local = consume(TokenType.IDENTIFIER, "Expected namespace name").getValue();
            specifiers.add(new ImportSpecifier("*", local, token.getLine(), token.getColumn()));
        } else {
            throw error(peek(), "Expected import specifiers");
        }
        
        consume(TokenType.FROM, "Expected 'from' after import specifiers");
        String source = consume(TokenType.STRING_LITERAL, "Expected module source").getValue();
        
        consume(TokenType.SEMICOLON, "Expected ';' after import statement");
        
        return new ImportStmt(specifiers, source, token.getLine(), token.getColumn());
    }
    
    private ExportStmt parseExportStatement() {
        Token token = advance(); // Consume 'export'
        
        if (match(TokenType.DEFAULT)) {
            // Export default
            Expression expression = parseExpression();
            consume(TokenType.SEMICOLON, "Expected ';' after export default");
            
            return new ExportStmt(true, null, expression, token.getLine(), token.getColumn());
        } else if (match(TokenType.LEFT_BRACE)) {
            // Named exports
            List<ExportSpecifier> specifiers = new ArrayList<>();
            
            if (!check(TokenType.RIGHT_BRACE)) {
                do {
                    String local = consume(TokenType.IDENTIFIER, "Expected exported name").getValue();
                    String exported = local;
                    
                    if (match(TokenType.AS)) {
                        exported = consume(TokenType.IDENTIFIER, "Expected alias").getValue();
                    }
                    
                    specifiers.add(new ExportSpecifier(local, exported, token.getLine(), token.getColumn()));
                } while (match(TokenType.COMMA));
            }
            
            consume(TokenType.RIGHT_BRACE, "Expected '}' after export specifiers");
            
            String source = null;
            if (match(TokenType.FROM)) {
                source = consume(TokenType.STRING_LITERAL, "Expected module source").getValue();
            }
            
            consume(TokenType.SEMICOLON, "Expected ';' after export statement");
            
            return new ExportStmt(false, specifiers, null, source, token.getLine(), token.getColumn());
        } else {
            // Export declaration
            Statement declaration = parseStatement();
            
            return new ExportStmt(false, null, declaration, token.getLine(), token.getColumn());
        }
    }
    
    private BlockStmt parseBlockStatement() {
        Token token = advance(); // Consume '{'
        
        List<Statement> statements = new ArrayList<>();
        
        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            statements.add(parseStatement());
        }
        
        consume(TokenType.RIGHT_BRACE, "Expected '}' after block");
        
        return new BlockStmt(statements, token.getLine(), token.getColumn());
    }
    
    private ExpressionStmt parseExpressionStatement() {
        Token token = peek();
        Expression expr = parseExpression();
        consume(TokenType.SEMICOLON, "Expected ';' after expression");
        
        return new ExpressionStmt(expr, token.getLine(), token.getColumn());
    }
    
    private TypeAnnotation parseTypeAnnotation() {
        Token token = peek();
        
        // Basic types
        if (match(TokenType.IDENTIFIER)) {
            String typeName = previous().getValue();
            
            // Check for array type
            if (match(TokenType.LEFT_BRACKET)) {
                consume(TokenType.RIGHT_BRACKET, "Expected ']' after '['");
                
                return new ArrayType(
                    new TypeIdentifier(typeName, token.getLine(), token.getColumn()),
                    token.getLine(), token.getColumn()
                );
            }
            
            // Check for generic type
            if (match(TokenType.LESS_THAN)) {
                List<TypeAnnotation> typeArguments = new ArrayList<>();
                
                do {
                    typeArguments.add(parseTypeAnnotation());
                } while (match(TokenType.COMMA));
                
                consume(TokenType.GREATER_THAN, "Expected '>' after generic type arguments");
                
                return new GenericType(
                    new TypeIdentifier(typeName, token.getLine(), token.getColumn()),
                    typeArguments,
                    token.getLine(), token.getColumn()
                );
            }
            
            return new TypeIdentifier(typeName, token.getLine(), token.getColumn());
        } 
        // Function type
        else if (match(TokenType.LEFT_PAREN)) {
            List<Parameter> parameters = parseParameterList();
            consume(TokenType.RIGHT_PAREN, "Expected ')' after function parameters");
            
            consume(TokenType.ARROW, "Expected '=>' after function parameters");
            
            TypeAnnotation returnType = parseTypeAnnotation();
            
            return new FunctionType(parameters, returnType, token.getLine(), token.getColumn());
        } 
        // Object type
        else if (match(TokenType.LEFT_BRACE)) {
            List<ObjectTypeProperty> properties = new ArrayList<>();
            
            while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
                Token propToken = peek();
                String name = consume(TokenType.IDENTIFIER, "Expected property name").getValue();
                
                // Optional property
                boolean optional = match(TokenType.QUESTION_MARK);
                
                consume(TokenType.COLON, "Expected ':' after property name");
                
                TypeAnnotation type = parseTypeAnnotation();
                
                properties.add(new ObjectTypeProperty(name, type, optional, 
                                                   propToken.getLine(), propToken.getColumn()));
                
                if (!check(TokenType.RIGHT_BRACE)) {
                    consume(TokenType.SEMICOLON, "Expected ';' or '}' after property definition");
                }
            }
            
            consume(TokenType.RIGHT_BRACE, "Expected '}' after object type");
            
            return new ObjectType(properties, token.getLine(), token.getColumn());
        } 
        // Array type using Type[]
        else if (match(TokenType.ARRAY)) {
            consume(TokenType.LESS_THAN, "Expected '<' after 'Array'");
            TypeAnnotation elementType = parseTypeAnnotation();
            consume(TokenType.GREATER_THAN, "Expected '>' after array element type");
            
            return new ArrayType(elementType, token.getLine(), token.getColumn());
        } 
        // Union type (a | b)
        else if (check(TokenType.LEFT_PAREN)) {
            List<TypeAnnotation> types = new ArrayList<>();
            
            do {
                types.add(parseTypeAnnotation());
            } while (match(TokenType.BITWISE_OR));
            
            if (types.size() == 1) {
                return types.get(0);
            }
            
            return new UnionType(types, token.getLine(), token.getColumn());
        } else {
            throw error(peek(), "Expected type annotation");
        }
    }
    
    private Expression parseExpression() {
        return parseAssignment();
    }
    
    private Expression parseAssignment() {
        Expression expr = parseConditional();
        
        if (match(TokenType.ASSIGN) || 
            match(TokenType.PLUS_ASSIGN) || match(TokenType.MINUS_ASSIGN) ||
            match(TokenType.MULTIPLY_ASSIGN) || match(TokenType.DIVIDE_ASSIGN) || match(TokenType.MODULO_ASSIGN)) {
            Token operator = previous();
            Expression value = parseAssignment();
            
            if (expr instanceof IdentifierExpr || expr instanceof MemberExpr || expr instanceof IndexExpr) {
                return new AssignExpr(expr, value, operator.getType(), operator.getLine(), operator.getColumn());
            }
            
            throw error(operator, "Invalid assignment target");
        }
        
        return expr;
    }
    
    private Expression parseConditional() {
        Expression expr = parseLogicalOr();
        
        if (match(TokenType.QUESTION_MARK)) {
            Expression thenExpr = parseExpression();
            consume(TokenType.COLON, "Expected ':' in conditional expression");
            Expression elseExpr = parseConditional();
            
            return new ConditionalExpr(expr, thenExpr, elseExpr, expr.getLine(), expr.getColumn());
        }
        
        return expr;
    }
    
    private Expression parseLogicalOr() {
        Expression expr = parseLogicalAnd();
        
        while (match(TokenType.OR)) {
            Token operator = previous();
            Expression right = parseLogicalAnd();
            expr = new BinaryExpr(expr, right, BinaryExpr.Operator.OR, operator.getLine(), operator.getColumn());
        }
        
        return expr;
    }
    
    private Expression parseLogicalAnd() {
        Expression expr = parseEquality();
        
        while (match(TokenType.AND)) {
            Token operator = previous();
            Expression right = parseEquality();
            expr = new BinaryExpr(expr, right, BinaryExpr.Operator.AND, operator.getLine(), operator.getColumn());
        }
        
        return expr;
    }
    
    private Expression parseEquality() {
        Expression expr = parseComparison();
        
        while (match(TokenType.EQUAL) || match(TokenType.NOT_EQUAL) || 
               match(TokenType.STRICT_EQUAL) || match(TokenType.STRICT_NOT_EQUAL)) {
            Token operator = previous();
            Expression right = parseComparison();
            
            BinaryExpr.Operator op;
            switch (operator.getType()) {
                case EQUAL: op = BinaryExpr.Operator.EQUAL; break;
                case NOT_EQUAL: op = BinaryExpr.Operator.NOT_EQUAL; break;
                case STRICT_EQUAL: op = BinaryExpr.Operator.STRICT_EQUAL; break;
                case STRICT_NOT_EQUAL: op = BinaryExpr.Operator.STRICT_NOT_EQUAL; break;
                default: throw new RuntimeException("Unexpected equality operator");
            }
            
            expr = new BinaryExpr(expr, right, op, operator.getLine(), operator.getColumn());
        }
        
        return expr;
    }
    
    private Expression parseComparison() {
        Expression expr = parseBitwiseOr();
        
        while (match(TokenType.GREATER_THAN) || match(TokenType.GREATER_THAN_EQUAL) ||
               match(TokenType.LESS_THAN) || match(TokenType.LESS_THAN_EQUAL)) {
            Token operator = previous();
            Expression right = parseBitwiseOr();
            
            BinaryExpr.Operator op;
            switch (operator.getType()) {
                case GREATER_THAN: op = BinaryExpr.Operator.GREATER_THAN; break;
                case GREATER_THAN_EQUAL: op = BinaryExpr.Operator.GREATER_THAN_EQUAL; break;
                case LESS_THAN: op = BinaryExpr.Operator.LESS_THAN; break;
                case LESS_THAN_EQUAL: op = BinaryExpr.Operator.LESS_THAN_EQUAL; break;
                default: throw new RuntimeException("Unexpected comparison operator");
            }
            
            expr = new BinaryExpr(expr, right, op, operator.getLine(), operator.getColumn());
        }
        
        return expr;
    }
    
    private Expression parseBitwiseOr() {
        Expression expr = parseBitwiseXor();
        
        while (match(TokenType.BITWISE_OR)) {
            Token operator = previous();
            Expression right = parseBitwiseXor();
            expr = new BinaryExpr(expr, right, BinaryExpr.Operator.BITWISE_OR, operator.getLine(), operator.getColumn());
        }
        
        return expr;
    }
    
    private Expression parseBitwiseXor() {
        Expression expr = parseBitwiseAnd();
        
        while (match(TokenType.BITWISE_XOR)) {
            Token operator = previous();
            Expression right = parseBitwiseAnd();
            expr = new BinaryExpr(expr, right, BinaryExpr.Operator.BITWISE_XOR, operator.getLine(), operator.getColumn());
        }
        
        return expr;
    }
    
    private Expression parseBitwiseAnd() {
        Expression expr = parseShift();
        
        while (match(TokenType.BITWISE_AND)) {
            Token operator = previous();
            Expression right = parseShift();
            expr = new BinaryExpr(expr, right, BinaryExpr.Operator.BITWISE_AND, operator.getLine(), operator.getColumn());
        }
        
        return expr;
    }
    
    private Expression parseShift() {
        Expression expr = parseAdditive();
        
        while (match(TokenType.LEFT_SHIFT) || match(TokenType.RIGHT_SHIFT) || match(TokenType.UNSIGNED_RIGHT_SHIFT)) {
            Token operator = previous();
            Expression right = parseAdditive();
            
            BinaryExpr.Operator op;
            switch (operator.getType()) {
                case LEFT_SHIFT: op = BinaryExpr.Operator.LEFT_SHIFT; break;
                case RIGHT_SHIFT: op = BinaryExpr.Operator.RIGHT_SHIFT; break;
                case UNSIGNED_RIGHT_SHIFT: op = BinaryExpr.Operator.UNSIGNED_RIGHT_SHIFT; break;
                default: throw new RuntimeException("Unexpected shift operator");
            }
            
            expr = new BinaryExpr(expr, right, op, operator.getLine(), operator.getColumn());
        }
        
        return expr;
    }
    
    private Expression parseAdditive() {
        Expression expr = parseMultiplicative();
        
        while (match(TokenType.PLUS) || match(TokenType.MINUS)) {
            Token operator = previous();
            Expression right = parseMultiplicative();
            
            BinaryExpr.Operator op = operator.getType() == TokenType.PLUS ? 
                BinaryExpr.Operator.ADD : BinaryExpr.Operator.SUBTRACT;
            
            expr = new BinaryExpr(expr, right, op, operator.getLine(), operator.getColumn());
        }
        
        return expr;
    }
    
    private Expression parseMultiplicative() {
        Expression expr = parseUnary();
        
        while (match(TokenType.MULTIPLY) || match(TokenType.DIVIDE) || match(TokenType.MODULO)) {
            Token operator = previous();
            Expression right = parseUnary();
            
            BinaryExpr.Operator op;
            switch (operator.getType()) {
                case MULTIPLY: op = BinaryExpr.Operator.MULTIPLY; break;
                case DIVIDE: op = BinaryExpr.Operator.DIVIDE; break;
                case MODULO: op = BinaryExpr.Operator.MODULO; break;
                default: throw new RuntimeException("Unexpected multiplicative operator");
            }
            
            expr = new BinaryExpr(expr, right, op, operator.getLine(), operator.getColumn());
        }
        
        return expr;
    }
    
    private Expression parseUnary() {
        if (match(TokenType.NOT) || match(TokenType.MINUS) || match(TokenType.PLUS) || 
            match(TokenType.INCREMENT) || match(TokenType.DECREMENT) || match(TokenType.TYPEOF)) {
            Token operator = previous();
            Expression right = parseUnary();
            
            UnaryExpr.Operator op;
            switch (operator.getType()) {
                case NOT: op = UnaryExpr.Operator.NOT; break;
                case MINUS: op = UnaryExpr.Operator.NEGATE; break;
                case PLUS: op = UnaryExpr.Operator.PLUS; break;
                case INCREMENT: op = UnaryExpr.Operator.PREFIX_INCREMENT; break;
                case DECREMENT: op = UnaryExpr.Operator.PREFIX_DECREMENT; break;
                case TYPEOF: op = UnaryExpr.Operator.TYPEOF; break;
                default: throw new RuntimeException("Unexpected unary operator");
            }
            
            return new UnaryExpr(right, op, true, operator.getLine(), operator.getColumn());
        }
        
        return parsePostfix();
    }
    
    private Expression parsePostfix() {
        Expression expr = parseCall();
        
        if (match(TokenType.INCREMENT)) {
            Token operator = previous();
            return new UnaryExpr(expr, UnaryExpr.Operator.POSTFIX_INCREMENT, false, operator.getLine(), operator.getColumn());
        }
        
        if (match(TokenType.DECREMENT)) {
            Token operator = previous();
            return new UnaryExpr(expr, UnaryExpr.Operator.POSTFIX_DECREMENT, false, operator.getLine(), operator.getColumn());
        }
        
        return expr;
    }
    
    private Expression parseCall() {
        Expression expr = parsePrimary();
        
        while (true) {
            if (match(TokenType.LEFT_PAREN)) {
                expr = finishCall(expr);
            } else if (match(TokenType.DOT) || match(TokenType.OPTIONAL_CHAIN)) {
                Token operator = previous();
                String name = consume(TokenType.IDENTIFIER, "Expected property name after '.'").getValue();
                expr = new MemberExpr(expr, name, operator.getType() == TokenType.OPTIONAL_CHAIN, 
                                   operator.getLine(), operator.getColumn());
            } else if (match(TokenType.LEFT_BRACKET)) {
                Token operator = previous();
                Expression index = parseExpression();
                consume(TokenType.RIGHT_BRACKET, "Expected ']' after index");
                expr = new IndexExpr(expr, index, operator.getLine(), operator.getColumn());
            } else {
                break;
            }
        }
        
        return expr;
    }
    
    private Expression finishCall(Expression callee) {
        Token token = previous();
        List<Expression> arguments = new ArrayList<>();
        
        if (!check(TokenType.RIGHT_PAREN)) {
            do {
                if (arguments.size() >= 255) {
                    error(peek(), "Cannot have more than 255 arguments");
                }
                
                arguments.add(parseExpression());
            } while (match(TokenType.COMMA));
        }
        
        consume(TokenType.RIGHT_PAREN, "Expected ')' after arguments");
        
        return new CallExpr(callee, arguments, token.getLine(), token.getColumn());
    }
    
    private Expression parsePrimary() {
        Token token = peek();
        
        if (match(TokenType.FALSE)) {
            return new LiteralExpr(Boolean.FALSE, LiteralExpr.LiteralType.BOOLEAN, token.getLine(), token.getColumn());
        }
        
        if (match(TokenType.TRUE)) {
            return new LiteralExpr(Boolean.TRUE, LiteralExpr.LiteralType.BOOLEAN, token.getLine(), token.getColumn());
        }
        
        if (match(TokenType.NULL_LITERAL)) {
            return new LiteralExpr(null, LiteralExpr.LiteralType.NULL, token.getLine(), token.getColumn());
        }
        
        if (match(TokenType.UNDEFINED_LITERAL)) {
            return new LiteralExpr(null, LiteralExpr.LiteralType.UNDEFINED, token.getLine(), token.getColumn());
        }
        
        if (match(TokenType.NUMBER_LITERAL)) {
            double value = Double.parseDouble(previous().getValue());
            return new LiteralExpr(value, LiteralExpr.LiteralType.NUMBER, token.getLine(), token.getColumn());
        }
        
        if (match(TokenType.STRING_LITERAL)) {
            String value = previous().getValue();
            return new LiteralExpr(value, LiteralExpr.LiteralType.STRING, token.getLine(), token.getColumn());
        }
        
        if (match(TokenType.IDENTIFIER)) {
            return new IdentifierExpr(previous().getValue(), token.getLine(), token.getColumn());
        }
        
        if (match(TokenType.LEFT_PAREN)) {
            Expression expr = parseExpression();
            consume(TokenType.RIGHT_PAREN, "Expected ')' after expression");
            return expr;
        }
        
        if (match(TokenType.THIS)) {
            return new ThisExpr(token.getLine(), token.getColumn());
        }
        
        if (match(TokenType.NEW)) {
            Token newToken = previous();
            Expression constructor = parseCall(); // This should be an identifier or member expression
            
            consume(TokenType.LEFT_PAREN, "Expected '(' after class name");
            List<Expression> arguments = new ArrayList<>();
            
            if (!check(TokenType.RIGHT_PAREN)) {
                do {
                    arguments.add(parseExpression());
                } while (match(TokenType.COMMA));
            }
            
            consume(TokenType.RIGHT_PAREN, "Expected ')' after arguments");
            
            return new NewExpr(constructor, arguments, newToken.getLine(), newToken.getColumn());
        }
        
        if (match(TokenType.FUNCTION)) {
            Token functionToken = previous();
            
            // Optional function name
            String name = null;
            if (check(TokenType.IDENTIFIER)) {
                name = advance().getValue();
            }
            
            consume(TokenType.LEFT_PAREN, "Expected '(' after function name");
            List<Parameter> parameters = parseParameterList();
            consume(TokenType.RIGHT_PAREN, "Expected ')' after parameters");
            
            // Optional return type
            TypeAnnotation returnType = null;
            if (match(TokenType.COLON)) {
                returnType = parseTypeAnnotation();
            }
            
            BlockStmt body = parseBlockStatement();
            
            return new FunctionExpr(name, parameters, returnType, body, 
                                 functionToken.getLine(), functionToken.getColumn());
        }
        
        if (match(TokenType.LEFT_BRACE)) {
            Token leftBrace = previous();
            List<ObjectProperty> properties = new ArrayList<>();
            
            if (!check(TokenType.RIGHT_BRACE)) {
                do {
                    String key;
                    
                    // Property key can be an identifier or string literal
                    if (check(TokenType.IDENTIFIER)) {
                        key = advance().getValue();
                    } else if (check(TokenType.STRING_LITERAL)) {
                        key = advance().getValue();
                    } else {
                        throw error(peek(), "Expected property name");
                    }
                    
                    Expression value;
                    
                    // Shorthand property: { x } is equivalent to { x: x }
                    if (check(TokenType.COMMA) || check(TokenType.RIGHT_BRACE)) {
                        value = new IdentifierExpr(key, leftBrace.getLine(), leftBrace.getColumn());
                    } else {
                        consume(TokenType.COLON, "Expected ':' after property name");
                        value = parseExpression();
                    }
                    
                    properties.add(new ObjectProperty(key, value, leftBrace.getLine(), leftBrace.getColumn()));
                } while (match(TokenType.COMMA));
            }
            
            consume(TokenType.RIGHT_BRACE, "Expected '}' after object literal");
            
            return new ObjectExpr(properties, leftBrace.getLine(), leftBrace.getColumn());
        }
        
        if (match(TokenType.LEFT_BRACKET)) {
            Token leftBracket = previous();
            List<Expression> elements = new ArrayList<>();
            
            if (!check(TokenType.RIGHT_BRACKET)) {
                do {
                    // Handle trailing comma
                    if (check(TokenType.RIGHT_BRACKET)) {
                        break;
                    }
                    
                    elements.add(parseExpression());
                } while (match(TokenType.COMMA));
            }
            
            consume(TokenType.RIGHT_BRACKET, "Expected ']' after array literal");
            
            return new ArrayExpr(elements, leftBracket.getLine(), leftBracket.getColumn());
        }
        
        throw error(peek(), "Expected expression");
    }
    
    private boolean match(TokenType... types) {
        for (TokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }
        
        return false;
    }
    
    private boolean check(TokenType type) {
        if (isAtEnd()) {
            return false;
        }
        return peek().getType() == type;
    }
    
    private Token advance() {
        if (!isAtEnd()) {
            current++;
        }
        return previous();
    }
    
    private boolean isAtEnd() {
        return peek().getType() == TokenType.EOF;
    }
    
    private Token peek() {
        return tokens.get(current);
    }
    
    private Token peek(int offset) {
        if (current + offset >= tokens.size()) {
            return tokens.get(tokens.size() - 1);
        }
        return tokens.get(current + offset);
    }
    
    private Token previous() {
        return tokens.get(current - 1);
    }
    
    private Token consume(TokenType type, String message) {
        if (check(type)) {
            return advance();
        }
        
        throw error(peek(), message);
    }
    
    private Exception error(Token token, String message) {
        errorReporter.report(token.getLine(), token.getColumn(), message);
        return new ParseException(message, token);
    }
    
    private void synchronize() {
        advance();
        
        while (!isAtEnd()) {
            if (previous().getType() == TokenType.SEMICOLON) {
                return;
            }
            
            switch (peek().getType()) {
                case CLASS:
                case FUNCTION:
                case LET:
                case CONST:
                case VAR:
                case FOR:
                case IF:
                case WHILE:
                case RETURN:
                case IMPORT:
                case EXPORT:
                    return;
            }
            
            advance();
        }
    }
    
    private static class ParseException extends RuntimeException {
        private final Token token;
        
        public ParseException(String message, Token token) {
            super(message);
            this.token = token;
        }
    }
}