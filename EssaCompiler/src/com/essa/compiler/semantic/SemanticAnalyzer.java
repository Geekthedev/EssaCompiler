package com.essa.compiler.semantic;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import com.essa.compiler.parser.ast.*;
import com.essa.compiler.parser.ast.expr.*;
import com.essa.compiler.parser.ast.stmt.*;
import com.essa.compiler.parser.ast.type.*;
import com.essa.compiler.utils.ErrorReporter;

/**
 * Semantic analyzer for the Essa compiler
 * Performs type checking and builds symbol tables
 */
public class SemanticAnalyzer implements ASTVisitor<TypeAnnotation> {
    private final Program program;
    private final ErrorReporter errorReporter;
    private final boolean isTypeScript;
    
    private final SymbolTable symbolTable = new SymbolTable();
    private final Stack<FunctionDeclStmt> currentFunction = new Stack<>();
    private final Stack<ClassDeclStmt> currentClass = new Stack<>();
    
    public SemanticAnalyzer(Program program, ErrorReporter errorReporter, boolean isTypeScript) {
        this.program = program;
        this.errorReporter = errorReporter;
        this.isTypeScript = isTypeScript;
    }
    
    public void analyze() {
        // Initialize global scope
        symbolTable.enterScope();
        
        // Define built-in types and functions
        defineBuiltIns();
        
        // First pass: declare all top-level symbols
        for (Statement stmt : program.getStatements()) {
            if (stmt instanceof FunctionDeclStmt) {
                FunctionDeclStmt func = (FunctionDeclStmt) stmt;
                symbolTable.define(func.getName(), func);
            } else if (stmt instanceof ClassDeclStmt) {
                ClassDeclStmt cls = (ClassDeclStmt) stmt;
                symbolTable.define(cls.getName(), cls);
            } else if (stmt instanceof InterfaceDeclStmt) {
                InterfaceDeclStmt intf = (InterfaceDeclStmt) stmt;
                symbolTable.define(intf.getName(), intf);
            } else if (stmt instanceof VarDeclStmt) {
                VarDeclStmt var = (VarDeclStmt) stmt;
                symbolTable.define(var.getName(), var);
            }
        }
        
        // Second pass: visit all statements
        visit(program);
        
        // Leave global scope
        symbolTable.exitScope();
    }
    
    private void defineBuiltIns() {
        // Define built-in types
        symbolTable.define("any", new BuiltInType("any"));
        symbolTable.define("void", new BuiltInType("void"));
        symbolTable.define("number", new BuiltInType("number"));
        symbolTable.define("string", new BuiltInType("string"));
        symbolTable.define("boolean", new BuiltInType("boolean"));
        symbolTable.define("undefined", new BuiltInType("undefined"));
        symbolTable.define("null", new BuiltInType("null"));
        
        // Define built-in functions
        symbolTable.define("console", new BuiltInObject("console"));
        symbolTable.define("Math", new BuiltInObject("Math"));
        symbolTable.define("Date", new BuiltInConstructor("Date"));
        symbolTable.define("Array", new BuiltInConstructor("Array"));
        symbolTable.define("Object", new BuiltInConstructor("Object"));
        symbolTable.define("String", new BuiltInConstructor("String"));
        symbolTable.define("Number", new BuiltInConstructor("Number"));
        symbolTable.define("Boolean", new BuiltInConstructor("Boolean"));
    }
    
    @Override
    public TypeAnnotation visit(Program program) {
        for (Statement stmt : program.getStatements()) {
            stmt.accept(this);
        }
        return null;
    }
    
    @Override
    public TypeAnnotation visit(BlockStmt stmt) {
        symbolTable.enterScope();
        
        for (Statement s : stmt.getStatements()) {
            s.accept(this);
        }
        
        symbolTable.exitScope();
        return null;
    }
    
    @Override
    public TypeAnnotation visit(VarDeclStmt stmt) {
        // Check initializer
        if (stmt.getInitializer() != null) {
            TypeAnnotation initType = stmt.getInitializer().accept(this);
            
            // If there's a type annotation, check compatibility
            if (stmt.getType() != null) {
                TypeAnnotation declaredType = stmt.getType().accept(this);
                
                if (!isAssignable(declaredType, initType)) {
                    error(stmt, "Type '" + initType + "' is not assignable to type '" + declaredType + "'");
                }
                
                return declaredType;
            } else {
                // Infer type from initializer
                return initType;
            }
        } else {
            // No initializer
            if (stmt.getType() != null) {
                return stmt.getType().accept(this);
            } else {
                // If TypeScript, error: variable must have type or initializer
                if (isTypeScript) {
                    error(stmt, "Variable '" + stmt.getName() + "' has no type annotation and is not initialized");
                }
                
                // Default to 'any' in JavaScript
                return new TypeIdentifier("any", stmt.getLine(), stmt.getColumn());
            }
        }
    }
    
    @Override
    public TypeAnnotation visit(FunctionDeclStmt stmt) {
        symbolTable.enterScope();
        currentFunction.push(stmt);
        
        // Define parameters in function scope
        for (Parameter param : stmt.getParameters()) {
            symbolTable.define(param.getName(), param);
            
            // In TypeScript, parameters must have types
            if (isTypeScript && param.getType() == null) {
                error(param, "Parameter '" + param.getName() + "' has no type annotation");
            }
        }
        
        // Check function body
        stmt.getBody().accept(this);
        
        currentFunction.pop();
        symbolTable.exitScope();
        
        // Return function type
        return stmt.getReturnType() != null ? 
            stmt.getReturnType().accept(this) : 
            new TypeIdentifier("any", stmt.getLine(), stmt.getColumn());
    }
    
    @Override
    public TypeAnnotation visit(ClassDeclStmt stmt) {
        symbolTable.enterScope();
        currentClass.push(stmt);
        
        // Check superclass
        if (stmt.getSuperClass() != null) {
            Object superClass = symbolTable.resolve(stmt.getSuperClass());
            
            if (superClass == null) {
                error(stmt, "Cannot find superclass '" + stmt.getSuperClass() + "'");
            } else if (!(superClass instanceof ClassDeclStmt)) {
                error(stmt, "'" + stmt.getSuperClass() + "' is not a class");
            }
        }
        
        // Check interfaces
        for (String interfaceName : stmt.getInterfaces()) {
            Object interfaceObj = symbolTable.resolve(interfaceName);
            
            if (interfaceObj == null) {
                error(stmt, "Cannot find interface '" + interfaceName + "'");
            } else if (!(interfaceObj instanceof InterfaceDeclStmt)) {
                error(stmt, "'" + interfaceName + "' is not an interface");
            }
        }
        
        // Check class members
        for (ClassMember member : stmt.getMembers()) {
            if (member instanceof PropertyMember) {
                PropertyMember prop = (PropertyMember) member;
                
                // Check property type
                if (prop.getType() != null) {
                    prop.getType().accept(this);
                } else if (isTypeScript && prop.getInitializer() == null) {
                    error(prop, "Property '" + prop.getName() + "' has no type annotation and is not initialized");
                }
                
                // Check property initializer
                if (prop.getInitializer() != null) {
                    TypeAnnotation initType = prop.getInitializer().accept(this);
                    
                    if (prop.getType() != null) {
                        TypeAnnotation propType = prop.getType().accept(this);
                        
                        if (!isAssignable(propType, initType)) {
                            error(prop, "Type '" + initType + "' is not assignable to type '" + propType + "'");
                        }
                    }
                }
            } else if (member instanceof MethodMember) {
                MethodMember method = (MethodMember) member;
                
                // Enter method scope
                symbolTable.enterScope();
                
                // Define parameters
                for (Parameter param : method.getParameters()) {
                    symbolTable.define(param.getName(), param);
                    
                    // In TypeScript, parameters must have types
                    if (isTypeScript && param.getType() == null) {
                        error(param, "Parameter '" + param.getName() + "' has no type annotation");
                    }
                }
                
                // Check method body
                method.getBody().accept(this);
                
                // Exit method scope
                symbolTable.exitScope();
            }
        }
        
        currentClass.pop();
        symbolTable.exitScope();
        
        return null;
    }
    
    @Override
    public TypeAnnotation visit(InterfaceDeclStmt stmt) {
        // Check extended interfaces
        for (String extendedName : stmt.getExtends()) {
            Object extended = symbolTable.resolve(extendedName);
            
            if (extended == null) {
                error(stmt, "Cannot find interface '" + extendedName + "'");
            } else if (!(extended instanceof InterfaceDeclStmt)) {
                error(stmt, "'" + extendedName + "' is not an interface");
            }
        }
        
        // Check interface members
        for (InterfaceMember member : stmt.getMembers()) {
            if (member instanceof InterfacePropertyMember) {
                InterfacePropertyMember prop = (InterfacePropertyMember) member;
                prop.getType().accept(this);
            } else if (member instanceof InterfaceMethodMember) {
                InterfaceMethodMember method = (InterfaceMethodMember) member;
                
                // Check parameter types
                for (Parameter param : method.getParameters()) {
                    if (param.getType() == null) {
                        error(param, "Parameter '" + param.getName() + "' must have a type annotation");
                    } else {
                        param.getType().accept(this);
                    }
                }
                
                // Check return type
                method.getReturnType().accept(this);
            }
        }
        
        return null;
    }
    
    @Override
    public TypeAnnotation visit(ExpressionStmt stmt) {
        return stmt.getExpression().accept(this);
    }
    
    @Override
    public TypeAnnotation visit(ReturnStmt stmt) {
        if (currentFunction.isEmpty()) {
            error(stmt, "Return statement not allowed outside of function");
            return null;
        }
        
        FunctionDeclStmt function = currentFunction.peek();
        TypeAnnotation returnType = function.getReturnType();
        
        if (stmt.getValue() == null) {
            // Return with no value
            if (returnType != null && !isVoid(returnType)) {
                error(stmt, "Function with return type must return a value");
            }
            return new TypeIdentifier("void", stmt.getLine(), stmt.getColumn());
        } else {
            // Return with value
            TypeAnnotation valueType = stmt.getValue().accept(this);
            
            if (returnType != null) {
                if (isVoid(returnType) && !isVoid(valueType)) {
                    error(stmt, "Function with void return type cannot return a value");
                } else if (!isAssignable(returnType, valueType)) {
                    error(stmt, "Return type '" + valueType + "' is not assignable to function return type '" + returnType + "'");
                }
            }
            
            return valueType;
        }
    }
    
    @Override
    public TypeAnnotation visit(IfStmt stmt) {
        TypeAnnotation condType = stmt.getCondition().accept(this);
        
        // Condition must be convertible to boolean
        if (!isBoolean(condType) && !isAny(condType)) {
            error(stmt.getCondition(), "Condition must be a boolean expression");
        }
        
        stmt.getThenBranch().accept(this);
        
        if (stmt.getElseBranch() != null) {
            stmt.getElseBranch().accept(this);
        }
        
        return null;
    }
    
    @Override
    public TypeAnnotation visit(WhileStmt stmt) {
        TypeAnnotation condType = stmt.getCondition().accept(this);
        
        // Condition must be convertible to boolean
        if (!isBoolean(condType) && !isAny(condType)) {
            error(stmt.getCondition(), "Condition must be a boolean expression");
        }
        
        stmt.getBody().accept(this);
        
        return null;
    }
    
    @Override
    public TypeAnnotation visit(ForStmt stmt) {
        symbolTable.enterScope();
        
        if (stmt.getInitializer() != null) {
            stmt.getInitializer().accept(this);
        }
        
        if (stmt.getCondition() != null) {
            TypeAnnotation condType = stmt.getCondition().accept(this);
            
            // Condition must be convertible to boolean
            if (!isBoolean(condType) && !isAny(condType)) {
                error(stmt.getCondition(), "Condition must be a boolean expression");
            }
        }
        
        if (stmt.getIncrement() != null) {
            stmt.getIncrement().accept(this);
        }
        
        stmt.getBody().accept(this);
        
        symbolTable.exitScope();
        
        return null;
    }
    
    @Override
    public TypeAnnotation visit(ImportStmt stmt) {
        // For now, just record that there are imports
        return null;
    }
    
    @Override
    public TypeAnnotation visit(ExportStmt stmt) {
        // For now, just record that there are exports
        return null;
    }
    
    @Override
    public TypeAnnotation visit(BinaryExpr expr) {
        TypeAnnotation leftType = expr.getLeft().accept(this);
        TypeAnnotation rightType = expr.getRight().accept(this);
        
        switch (expr.getOperator()) {
            case ADD:
                // Special case for string concatenation
                if (isString(leftType) || isString(rightType)) {
                    return new TypeIdentifier("string", expr.getLine(), expr.getColumn());
                }
                // Fall through for numeric addition
            case SUBTRACT:
            case MULTIPLY:
            case DIVIDE:
            case MODULO:
            case POWER:
                if ((!isNumber(leftType) && !isAny(leftType)) || 
                    (!isNumber(rightType) && !isAny(rightType))) {
                    error(expr, "Operator '" + expr.getOperator() + "' can only be applied to numbers");
                }
                return new TypeIdentifier("number", expr.getLine(), expr.getColumn());
                
            case EQUAL:
            case NOT_EQUAL:
            case STRICT_EQUAL:
            case STRICT_NOT_EQUAL:
            case GREATER_THAN:
            case LESS_THAN:
            case GREATER_THAN_EQUAL:
            case LESS_THAN_EQUAL:
                return new TypeIdentifier("boolean", expr.getLine(), expr.getColumn());
                
            case AND:
            case OR:
                if ((!isBoolean(leftType) && !isAny(leftType)) || 
                    (!isBoolean(rightType) && !isAny(rightType))) {
                    error(expr, "Operator '" + expr.getOperator() + "' can only be applied to booleans");
                }
                return new TypeIdentifier("boolean", expr.getLine(), expr.getColumn());
                
            case BITWISE_AND:
            case BITWISE_OR:
            case BITWISE_XOR:
            case LEFT_SHIFT:
            case RIGHT_SHIFT:
            case UNSIGNED_RIGHT_SHIFT:
                if ((!isNumber(leftType) && !isAny(leftType)) || 
                    (!isNumber(rightType) && !isAny(rightType))) {
                    error(expr, "Bitwise operator can only be applied to numbers");
                }
                return new TypeIdentifier("number", expr.getLine(), expr.getColumn());
                
            default:
                error(expr, "Unknown binary operator: " + expr.getOperator());
                return new TypeIdentifier("any", expr.getLine(), expr.getColumn());
        }
    }
    
    @Override
    public TypeAnnotation visit(UnaryExpr expr) {
        TypeAnnotation operandType = expr.getOperand().accept(this);
        
        switch (expr.getOperator()) {
            case NOT:
                return new TypeIdentifier("boolean", expr.getLine(), expr.getColumn());
                
            case NEGATE:
            case PLUS:
            case PREFIX_INCREMENT:
            case PREFIX_DECREMENT:
            case POSTFIX_INCREMENT:
            case POSTFIX_DECREMENT:
                if (!isNumber(operandType) && !isAny(operandType)) {
                    error(expr, "Operator '" + expr.getOperator() + "' can only be applied to numbers");
                }
                return new TypeIdentifier("number", expr.getLine(), expr.getColumn());
                
            case TYPEOF:
                return new TypeIdentifier("string", expr.getLine(), expr.getColumn());
                
            default:
                error(expr, "Unknown unary operator: " + expr.getOperator());
                return new TypeIdentifier("any", expr.getLine(), expr.getColumn());
        }
    }
    
    @Override
    public TypeAnnotation visit(CallExpr expr) {
        TypeAnnotation calleeType = expr.getCallee().accept(this);
        
        // Check if the callee is a function
        if (!(calleeType instanceof FunctionType) && !isAny(calleeType)) {
            error(expr, "Cannot call non-function type: " + calleeType);
            return new TypeIdentifier("any", expr.getLine(), expr.getColumn());
        }
        
        // Check arguments
        if (calleeType instanceof FunctionType) {
            FunctionType functionType = (FunctionType) calleeType;
            List<Parameter> parameters = functionType.getParameters();
            List<Expression> arguments = expr.getArguments();
            
            // Check argument count
            if (arguments.size() != parameters.size()) {
                error(expr, "Expected " + parameters.size() + " arguments, but got " + arguments.size());
            }
            
            // Check argument types
            int minCount = Math.min(arguments.size(), parameters.size());
            for (int i = 0; i < minCount; i++) {
                TypeAnnotation argType = arguments.get(i).accept(this);
                TypeAnnotation paramType = parameters.get(i).getType();
                
                if (paramType != null && !isAssignable(paramType, argType)) {
                    error(arguments.get(i), "Argument of type '" + argType + 
                        "' is not assignable to parameter of type '" + paramType + "'");
                }
            }
            
            // Return the function's return type
            return functionType.getReturnType();
        } else {
            // Any type, just check arguments
            for (Expression arg : expr.getArguments()) {
                arg.accept(this);
            }
            
            // Return "any" for calls to "any" type
            return new TypeIdentifier("any", expr.getLine(), expr.getColumn());
        }
    }
    
    @Override
    public TypeAnnotation visit(MemberExpr expr) {
        TypeAnnotation objectType = expr.getObject().accept(this);
        
        // For now, just allow any member access
        return new TypeIdentifier("any", expr.getLine(), expr.getColumn());
    }
    
    @Override
    public TypeAnnotation visit(IndexExpr expr) {
        TypeAnnotation objectType = expr.getObject().accept(this);
        TypeAnnotation indexType = expr.getIndex().accept(this);
        
        // Check if index is number or string
        if (!isNumber(indexType) && !isString(indexType) && !isAny(indexType)) {
            error(expr.getIndex(), "Index expression must be of type 'number' or 'string'");
        }
        
        // For now, just return "any" for indexed access
        return new TypeIdentifier("any", expr.getLine(), expr.getColumn());
    }
    
    @Override
    public TypeAnnotation visit(AssignExpr expr) {
        TypeAnnotation targetType = expr.getTarget().accept(this);
        TypeAnnotation valueType = expr.getValue().accept(this);
        
        // Check if the target is a valid lvalue
        if (!(expr.getTarget() instanceof IdentifierExpr || 
              expr.getTarget() instanceof MemberExpr || 
              expr.getTarget() instanceof IndexExpr)) {
            error(expr.getTarget(), "Invalid assignment target");
        }
        
        // For compound assignments (+=, -=, etc.), check the operation
        if (expr.getOperator() != TokenType.ASSIGN) {
            // Check that the operation is valid for the types
            switch (expr.getOperator()) {
                case PLUS_ASSIGN:
                    // Special case for string concatenation
                    if (!isString(targetType) && !isNumber(targetType) && !isAny(targetType)) {
                        error(expr, "Operator '+=' can only be applied to string or number");
                    }
                    break;
                    
                case MINUS_ASSIGN:
                case MULTIPLY_ASSIGN:
                case DIVIDE_ASSIGN:
                case MODULO_ASSIGN:
                    if (!isNumber(targetType) && !isAny(targetType)) {
                        error(expr, "Operator '" + expr.getOperator() + "' can only be applied to number");
                    }
                    break;
                    
                default:
                    error(expr, "Unknown compound assignment operator: " + expr.getOperator());
            }
        }
        
        // Check assignment compatibility
        if (!isAssignable(targetType, valueType)) {
            error(expr, "Type '" + valueType + "' is not assignable to type '" + targetType + "'");
        }
        
        return valueType;
    }
    
    @Override
    public TypeAnnotation visit(LiteralExpr expr) {
        switch (expr.getLiteralType()) {
            case NUMBER:
                return new TypeIdentifier("number", expr.getLine(), expr.getColumn());
                
            case STRING:
                return new TypeIdentifier("string", expr.getLine(), expr.getColumn());
                
            case BOOLEAN:
                return new TypeIdentifier("boolean", expr.getLine(), expr.getColumn());
                
            case NULL:
                return new TypeIdentifier("null", expr.getLine(), expr.getColumn());
                
            case UNDEFINED:
                return new TypeIdentifier("undefined", expr.getLine(), expr.getColumn());
                
            default:
                error(expr, "Unknown literal type: " + expr.getLiteralType());
                return new TypeIdentifier("any", expr.getLine(), expr.getColumn());
        }
    }
    
    @Override
    public TypeAnnotation visit(IdentifierExpr expr) {
        String name = expr.getName();
        Object resolvedSymbol = symbolTable.resolve(name);
        
        if (resolvedSymbol == null) {
            error(expr, "Cannot find name '" + name + "'");
            return new TypeIdentifier("any", expr.getLine(), expr.getColumn());
        }
        
        if (resolvedSymbol instanceof VarDeclStmt) {
            VarDeclStmt varDecl = (VarDeclStmt) resolvedSymbol;
            if (varDecl.getType() != null) {
                return varDecl.getType();
            } else if (varDecl.getInitializer() != null) {
                return varDecl.getInitializer().accept(this);
            } else {
                return new TypeIdentifier("any", expr.getLine(), expr.getColumn());
            }
        } else if (resolvedSymbol instanceof Parameter) {
            Parameter param = (Parameter) resolvedSymbol;
            if (param.getType() != null) {
                return param.getType();
            } else {
                return new TypeIdentifier("any", expr.getLine(), expr.getColumn());
            }
        } else if (resolvedSymbol instanceof FunctionDeclStmt) {
            FunctionDeclStmt funcDecl = (FunctionDeclStmt) resolvedSymbol;
            
            // Create function type
            TypeAnnotation returnType = funcDecl.getReturnType() != null ? 
                funcDecl.getReturnType() : 
                new TypeIdentifier("any", expr.getLine(), expr.getColumn());
                
            return new FunctionType(funcDecl.getParameters(), returnType, 
                                 expr.getLine(), expr.getColumn());
        } else if (resolvedSymbol instanceof ClassDeclStmt) {
            // Class used as constructor
            return new TypeIdentifier(name, expr.getLine(), expr.getColumn());
        } else if (resolvedSymbol instanceof InterfaceDeclStmt) {
            // Interface used as type
            return new TypeIdentifier(name, expr.getLine(), expr.getColumn());
        } else if (resolvedSymbol instanceof BuiltInType) {
            // Built-in type
            return new TypeIdentifier(((BuiltInType) resolvedSymbol).getName(), 
                                   expr.getLine(), expr.getColumn());
        } else if (resolvedSymbol instanceof BuiltInObject || 
                  resolvedSymbol instanceof BuiltInConstructor) {
            // Built-in object or constructor
            return new TypeIdentifier(((Symbol) resolvedSymbol).getName(), 
                                   expr.getLine(), expr.getColumn());
        } else {
            // Unknown symbol type
            return new TypeIdentifier("any", expr.getLine(), expr.getColumn());
        }
    }
    
    @Override
    public TypeAnnotation visit(ObjectExpr expr) {
        Map<String, TypeAnnotation> propertyTypes = new HashMap<>();
        
        for (ObjectProperty property : expr.getProperties()) {
            String name = property.getKey();
            TypeAnnotation type = property.getValue().accept(this);
            propertyTypes.put(name, type);
        }
        
        // Create and return an object type with the inferred property types
        List<ObjectTypeProperty> properties = new ArrayList<>();
        
        for (Map.Entry<String, TypeAnnotation> entry : propertyTypes.entrySet()) {
            properties.add(new ObjectTypeProperty(
                entry.getKey(), entry.getValue(), false, 
                expr.getLine(), expr.getColumn()
            ));
        }
        
        return new ObjectType(properties, expr.getLine(), expr.getColumn());
    }
    
    @Override
    public TypeAnnotation visit(ArrayExpr expr) {
        // For simplicity, assume homogeneous arrays
        TypeAnnotation elementType = null;
        
        for (Expression element : expr.getElements()) {
            TypeAnnotation type = element.accept(this);
            
            if (elementType == null) {
                elementType = type;
            } else if (!isAssignable(elementType, type) && !isAssignable(type, elementType)) {
                // Try to find a common supertype or use any
                elementType = new TypeIdentifier("any", expr.getLine(), expr.getColumn());
                break;
            }
        }
        
        if (elementType == null) {
            // Empty array
            elementType = new TypeIdentifier("any", expr.getLine(), expr.getColumn());
        }
        
        return new ArrayType(elementType, expr.getLine(), expr.getColumn());
    }
    
    @Override
    public TypeAnnotation visit(NewExpr expr) {
        TypeAnnotation constructorType = expr.getConstructor().accept(this);
        
        // Check constructor arguments
        for (Expression arg : expr.getArguments()) {
            arg.accept(this);
        }
        
        // For now, return the constructor type as the instance type
        if (constructorType instanceof TypeIdentifier) {
            return constructorType;
        } else {
            error(expr, "Cannot instantiate non-class type: " + constructorType);
            return new TypeIdentifier("any", expr.getLine(), expr.getColumn());
        }
    }
    
    @Override
    public TypeAnnotation visit(FunctionExpr expr) {
        symbolTable.enterScope();
        
        // Define parameters in function scope
        for (Parameter param : expr.getParameters()) {
            symbolTable.define(param.getName(), param);
            
            // In TypeScript, parameters must have types
            if (isTypeScript && param.getType() == null) {
                error(param, "Parameter '" + param.getName() + "' has no type annotation");
            }
        }
        
        // Check function body
        expr.getBody().accept(this);
        
        symbolTable.exitScope();
        
        // Create and return function type
        TypeAnnotation returnType = expr.getReturnType() != null ? 
            expr.getReturnType().accept(this) : 
            new TypeIdentifier("any", expr.getLine(), expr.getColumn());
            
        return new FunctionType(expr.getParameters(), returnType, 
                             expr.getLine(), expr.getColumn());
    }
    
    @Override
    public TypeAnnotation visit(ConditionalExpr expr) {
        TypeAnnotation condType = expr.getCondition().accept(this);
        
        // Condition must be convertible to boolean
        if (!isBoolean(condType) && !isAny(condType)) {
            error(expr.getCondition(), "Condition must be a boolean expression");
        }
        
        TypeAnnotation thenType = expr.getThenExpr().accept(this);
        TypeAnnotation elseType = expr.getElseExpr().accept(this);
        
        // If types are compatible, return the more specific one
        if (isAssignable(thenType, elseType)) {
            return elseType;
        } else if (isAssignable(elseType, thenType)) {
            return thenType;
        } else {
            // Types are incompatible, need a union type or any
            if (isTypeScript) {
                // In TypeScript we could return a union type
                return new UnionType(
                    List.of(thenType, elseType),
                    expr.getLine(), expr.getColumn()
                );
            } else {
                // In JavaScript just return any
                return new TypeIdentifier("any", expr.getLine(), expr.getColumn());
            }
        }
    }
    
    @Override
    public TypeAnnotation visit(TypeAnnotation type) {
        // This method should never be called directly
        return type;
    }
    
    @Override
    public TypeAnnotation visit(ObjectType type) {
        for (ObjectTypeProperty property : type.getProperties()) {
            property.getType().accept(this);
        }
        return type;
    }
    
    @Override
    public TypeAnnotation visit(ArrayType type) {
        type.getElementType().accept(this);
        return type;
    }
    
    @Override
    public TypeAnnotation visit(FunctionType type) {
        for (Parameter param : type.getParameters()) {
            if (param.getType() != null) {
                param.getType().accept(this);
            }
        }
        
        type.getReturnType().accept(this);
        return type;
    }
    
    @Override
    public TypeAnnotation visit(UnionType type) {
        for (TypeAnnotation subType : type.getTypes()) {
            subType.accept(this);
        }
        return type;
    }
    
    @Override
    public TypeAnnotation visit(IntersectionType type) {
        for (TypeAnnotation subType : type.getTypes()) {
            subType.accept(this);
        }
        return type;
    }
    
    @Override
    public TypeAnnotation visit(GenericType type) {
        type.getBaseType().accept(this);
        
        for (TypeAnnotation typeArg : type.getTypeArguments()) {
            typeArg.accept(this);
        }
        
        return type;
    }
    
    // Helper methods for type checking
    
    private boolean isAssignable(TypeAnnotation target, TypeAnnotation source) {
        // Any type can be assigned to any
        if (isAny(target)) {
            return true;
        }
        
        // Any source can be assigned from any
        if (isAny(source)) {
            return true;
        }
        
        // Null can be assigned to any object type
        if (isNull(source)) {
            return !isPrimitive(target);
        }
        
        // Undefined can be assigned to any type in JavaScript
        if (isUndefined(source) && !isTypeScript) {
            return true;
        }
        
        // Same type is always assignable
        if (isSameType(target, source)) {
            return true;
        }
        
        // Number literal to number
        if (isNumber(target) && isNumberLiteral(source)) {
            return true;
        }
        
        // String literal to string
        if (isString(target) && isStringLiteral(source)) {
            return true;
        }
        
        // Boolean literal to boolean
        if (isBoolean(target) && isBooleanLiteral(source)) {
            return true;
        }
        
        // Union type assignability
        if (target instanceof UnionType) {
            UnionType unionType = (UnionType) target;
            
            // Source is assignable if it's assignable to any part of the union
            for (TypeAnnotation unionMember : unionType.getTypes()) {
                if (isAssignable(unionMember, source)) {
                    return true;
                }
            }
            
            return false;
        }
        
        // Intersection type assignability
        if (source instanceof IntersectionType) {
            IntersectionType intersectionType = (IntersectionType) source;
            
            // Source is assignable if all its parts are assignable to target
            for (TypeAnnotation intersectionMember : intersectionType.getTypes()) {
                if (!isAssignable(target, intersectionMember)) {
                    return false;
                }
            }
            
            return true;
        }
        
        // Array type assignability
        if (target instanceof ArrayType && source instanceof ArrayType) {
            ArrayType targetArray = (ArrayType) target;
            ArrayType sourceArray = (ArrayType) source;
            
            return isAssignable(targetArray.getElementType(), sourceArray.getElementType());
        }
        
        // Object type assignability (structural typing)
        if (target instanceof ObjectType && source instanceof ObjectType) {
            ObjectType targetObj = (ObjectType) target;
            ObjectType sourceObj = (ObjectType) source;
            
            // Target is assignable from source if source has at least all properties of target
            // with compatible types
            for (ObjectTypeProperty targetProp : targetObj.getProperties()) {
                String name = targetProp.getName();
                boolean optional = targetProp.isOptional();
                
                // Find matching property in source
                ObjectTypeProperty sourceProp = null;
                for (ObjectTypeProperty prop : sourceObj.getProperties()) {
                    if (prop.getName().equals(name)) {
                        sourceProp = prop;
                        break;
                    }
                }
                
                // If property is required in target but not in source, not assignable
                if (!optional && sourceProp == null) {
                    return false;
                }
                
                // If property exists in both, check type compatibility
                if (sourceProp != null && !isAssignable(targetProp.getType(), sourceProp.getType())) {
                    return false;
                }
            }
            
            return true;
        }
        
        // Function type assignability
        if (target instanceof FunctionType && source instanceof FunctionType) {
            FunctionType targetFunc = (FunctionType) target;
            FunctionType sourceFunc = (FunctionType) source;
            
            // Check parameter count
            if (targetFunc.getParameters().size() != sourceFunc.getParameters().size()) {
                return false;
            }
            
            // Check parameter types (contravariant)
            for (int i = 0; i < targetFunc.getParameters().size(); i++) {
                Parameter targetParam = targetFunc.getParameters().get(i);
                Parameter sourceParam = sourceFunc.getParameters().get(i);
                
                // Skip parameters without types
                if (targetParam.getType() == null || sourceParam.getType() == null) {
                    continue;
                }
                
                if (!isAssignable(sourceParam.getType(), targetParam.getType())) {
                    return false;
                }
            }
            
            // Check return type (covariant)
            return isAssignable(targetFunc.getReturnType(), sourceFunc.getReturnType());
        }
        
        // Default: not assignable
        return false;
    }
    
    private boolean isSameType(TypeAnnotation type1, TypeAnnotation type2) {
        if (type1 instanceof TypeIdentifier && type2 instanceof TypeIdentifier) {
            TypeIdentifier id1 = (TypeIdentifier) type1;
            TypeIdentifier id2 = (TypeIdentifier) type2;
            
            return id1.getName().equals(id2.getName());
        }
        
        return false;
    }
    
    private boolean isAny(TypeAnnotation type) {
        return type instanceof TypeIdentifier && 
               ((TypeIdentifier) type).getName().equals("any");
    }
    
    private boolean isVoid(TypeAnnotation type) {
        return type instanceof TypeIdentifier && 
               ((TypeIdentifier) type).getName().equals("void");
    }
    
    private boolean isNumber(TypeAnnotation type) {
        return type instanceof TypeIdentifier && 
               ((TypeIdentifier) type).getName().equals("number");
    }
    
    private boolean isString(TypeAnnotation type) {
        return type instanceof TypeIdentifier && 
               ((TypeIdentifier) type).getName().equals("string");
    }
    
    private boolean isBoolean(TypeAnnotation type) {
        return type instanceof TypeIdentifier && 
               ((TypeIdentifier) type).getName().equals("boolean");
    }
    
    private boolean isNull(TypeAnnotation type) {
        return type instanceof TypeIdentifier && 
               ((TypeIdentifier) type).getName().equals("null");
    }
    
    private boolean isUndefined(TypeAnnotation type) {
        return type instanceof TypeIdentifier && 
               ((TypeIdentifier) type).getName().equals("undefined");
    }
    
    private boolean isPrimitive(TypeAnnotation type) {
        return isNumber(type) || isString(type) || isBoolean(type) || 
               isVoid(type) || isNull(type) || isUndefined(type);
    }
    
    private boolean isNumberLiteral(TypeAnnotation type) {
        // For now, we don't have literal types
        return isNumber(type);
    }
    
    private boolean isStringLiteral(TypeAnnotation type) {
        // For now, we don't have literal types
        return isString(type);
    }
    
    private boolean isBooleanLiteral(TypeAnnotation type) {
        // For now, we don't have literal types
        return isBoolean(type);
    }
    
    private void error(ASTNode node, String message) {
        errorReporter.report(node.getLine(), node.getColumn(), message);
    }
    
    // Symbol class for built-in types and objects
    private static abstract class Symbol {
        private final String name;
        
        public Symbol(String name) {
            this.name = name;
        }
        
        public String getName() {
            return name;
        }
    }
    
    private static class BuiltInType extends Symbol {
        public BuiltInType(String name) {
            super(name);
        }
    }
    
    private static class BuiltInObject extends Symbol {
        public BuiltInObject(String name) {
            super(name);
        }
    }
    
    private static class BuiltInConstructor extends Symbol {
        public BuiltInConstructor(String name) {
            super(name);
        }
    }
}