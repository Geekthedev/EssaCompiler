package com.essa.compiler.lexer;

/**
 * Enumeration of all token types supported by the Essa compiler
 */
public enum TokenType {
    // Keywords
    LET, CONST, VAR, FUNCTION, CLASS, EXTENDS, IMPLEMENTS, INTERFACE,
    IF, ELSE, FOR, WHILE, DO, RETURN, BREAK, CONTINUE,
    NEW, THIS, SUPER, IMPORT, EXPORT, FROM, AS, TYPEOF,
    PUBLIC, PRIVATE, PROTECTED, STATIC, READONLY,
    
    // Types
    TYPE, NUMBER, STRING, BOOLEAN, ANY, VOID, NULL, UNDEFINED,
    
    // Literals
    NUMBER_LITERAL, STRING_LITERAL, BOOLEAN_LITERAL, NULL_LITERAL, UNDEFINED_LITERAL,
    
    // Identifiers
    IDENTIFIER,
    
    // Operators
    PLUS, MINUS, MULTIPLY, DIVIDE, MODULO, POWER,
    ASSIGN, PLUS_ASSIGN, MINUS_ASSIGN, MULTIPLY_ASSIGN, DIVIDE_ASSIGN, MODULO_ASSIGN,
    EQUAL, NOT_EQUAL, STRICT_EQUAL, STRICT_NOT_EQUAL,
    GREATER_THAN, LESS_THAN, GREATER_THAN_EQUAL, LESS_THAN_EQUAL,
    AND, OR, NOT, BITWISE_AND, BITWISE_OR, BITWISE_XOR, BITWISE_NOT,
    LEFT_SHIFT, RIGHT_SHIFT, UNSIGNED_RIGHT_SHIFT,
    INCREMENT, DECREMENT, OPTIONAL_CHAIN,
    
    // Punctuation
    DOT, COMMA, COLON, SEMICOLON, QUESTION_MARK,
    LEFT_PAREN, RIGHT_PAREN, LEFT_BRACE, RIGHT_BRACE, LEFT_BRACKET, RIGHT_BRACKET,
    ARROW, SPREAD,
    
    // Other
    COMMENT, WHITESPACE, NEWLINE, 
    
    // End of file
    EOF;
    
    /**
     * Checks if the token type is a keyword
     */
    public boolean isKeyword() {
        return ordinal() >= LET.ordinal() && ordinal() <= READONLY.ordinal();
    }
    
    /**
     * Checks if the token type is a type
     */
    public boolean isType() {
        return ordinal() >= TYPE.ordinal() && ordinal() <= UNDEFINED.ordinal();
    }
    
    /**
     * Checks if the token type is a literal
     */
    public boolean isLiteral() {
        return ordinal() >= NUMBER_LITERAL.ordinal() && ordinal() <= UNDEFINED_LITERAL.ordinal();
    }
    
    /**
     * Checks if the token type is an operator
     */
    public boolean isOperator() {
        return ordinal() >= PLUS.ordinal() && ordinal() <= OPTIONAL_CHAIN.ordinal();
    }
    
    /**
     * Checks if the token type is punctuation
     */
    public boolean isPunctuation() {
        return ordinal() >= DOT.ordinal() && ordinal() <= SPREAD.ordinal();
    }
}