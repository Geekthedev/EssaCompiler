package com.essa.compiler.lexer;

import com.essa.compiler.utils.ErrorReporter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Lexical analyzer for the Essa compiler
 * Converts source code into a list of tokens
 */
public class Lexer {
    private final String sourceCode;
    private final ErrorReporter errorReporter;
    private int position = 0;
    private int line = 1;
    private int column = 1;
    
    private static final Map<String, TokenType> KEYWORDS = new HashMap<>();
    
    static {
        // Initialize keywords map
        KEYWORDS.put("let", TokenType.LET);
        KEYWORDS.put("const", TokenType.CONST);
        KEYWORDS.put("var", TokenType.VAR);
        KEYWORDS.put("function", TokenType.FUNCTION);
        KEYWORDS.put("class", TokenType.CLASS);
        KEYWORDS.put("extends", TokenType.EXTENDS);
        KEYWORDS.put("implements", TokenType.IMPLEMENTS);
        KEYWORDS.put("interface", TokenType.INTERFACE);
        KEYWORDS.put("if", TokenType.IF);
        KEYWORDS.put("else", TokenType.ELSE);
        KEYWORDS.put("for", TokenType.FOR);
        KEYWORDS.put("while", TokenType.WHILE);
        KEYWORDS.put("do", TokenType.DO);
        KEYWORDS.put("return", TokenType.RETURN);
        KEYWORDS.put("break", TokenType.BREAK);
        KEYWORDS.put("continue", TokenType.CONTINUE);
        KEYWORDS.put("new", TokenType.NEW);
        KEYWORDS.put("this", TokenType.THIS);
        KEYWORDS.put("super", TokenType.SUPER);
        KEYWORDS.put("import", TokenType.IMPORT);
        KEYWORDS.put("export", TokenType.EXPORT);
        KEYWORDS.put("from", TokenType.FROM);
        KEYWORDS.put("as", TokenType.AS);
        KEYWORDS.put("typeof", TokenType.TYPEOF);
        KEYWORDS.put("public", TokenType.PUBLIC);
        KEYWORDS.put("private", TokenType.PRIVATE);
        KEYWORDS.put("protected", TokenType.PROTECTED);
        KEYWORDS.put("static", TokenType.STATIC);
        KEYWORDS.put("readonly", TokenType.READONLY);
        KEYWORDS.put("type", TokenType.TYPE);
        KEYWORDS.put("number", TokenType.NUMBER);
        KEYWORDS.put("string", TokenType.STRING);
        KEYWORDS.put("boolean", TokenType.BOOLEAN);
        KEYWORDS.put("any", TokenType.ANY);
        KEYWORDS.put("void", TokenType.VOID);
        KEYWORDS.put("null", TokenType.NULL_LITERAL);
        KEYWORDS.put("undefined", TokenType.UNDEFINED_LITERAL);
        KEYWORDS.put("true", TokenType.BOOLEAN_LITERAL);
        KEYWORDS.put("false", TokenType.BOOLEAN_LITERAL);
    }
    
    public Lexer(String sourceCode, ErrorReporter errorReporter) {
        this.sourceCode = sourceCode;
        this.errorReporter = errorReporter;
    }
    
    public List<Token> tokenize() {
        List<Token> tokens = new ArrayList<>();
        
        while (position < sourceCode.length()) {
            char currentChar = sourceCode.charAt(position);
            
            if (Character.isWhitespace(currentChar)) {
                consumeWhitespace();
            } else if (Character.isDigit(currentChar)) {
                tokens.add(tokenizeNumber());
            } else if (Character.isLetter(currentChar) || currentChar == '_' || currentChar == '$') {
                tokens.add(tokenizeIdentifier());
            } else if (currentChar == '"' || currentChar == '\'') {
                tokens.add(tokenizeString());
            } else if (currentChar == '/') {
                if (peekNext() == '/' || peekNext() == '*') {
                    consumeComment();
                } else {
                    tokens.add(tokenizeOperator());
                }
            } else if (isOperatorChar(currentChar)) {
                tokens.add(tokenizeOperator());
            } else if (isPunctuationChar(currentChar)) {
                tokens.add(tokenizePunctuation());
            } else {
                errorReporter.report(line, column, "Unexpected character: " + currentChar);
                // Skip the invalid character
                advance();
            }
        }
        
        // Add EOF token
        tokens.add(new Token(TokenType.EOF, "", line, column));
        
        return tokens;
    }
    
    private char advance() {
        char current = sourceCode.charAt(position++);
        if (current == '\n') {
            line++;
            column = 1;
        } else {
            column++;
        }
        return current;
    }
    
    private char peekNext() {
        if (position + 1 >= sourceCode.length()) {
            return '\0';
        }
        return sourceCode.charAt(position + 1);
    }
    
    private boolean match(char expected) {
        if (position >= sourceCode.length() || sourceCode.charAt(position) != expected) {
            return false;
        }
        advance();
        return true;
    }
    
    private void consumeWhitespace() {
        while (position < sourceCode.length() && Character.isWhitespace(sourceCode.charAt(position))) {
            advance();
        }
    }
    
    private void consumeComment() {
        // Single-line comment
        if (match('/') && match('/')) {
            while (position < sourceCode.length() && sourceCode.charAt(position) != '\n') {
                advance();
            }
        } 
        // Multi-line comment
        else if (match('/') && match('*')) {
            boolean terminated = false;
            while (position < sourceCode.length() && !terminated) {
                if (match('*') && match('/')) {
                    terminated = true;
                } else {
                    advance();
                }
            }
            
            if (!terminated) {
                errorReporter.report(line, column, "Unterminated comment");
            }
        }
    }
    
    private Token tokenizeNumber() {
        int startLine = line;
        int startColumn = column;
        StringBuilder number = new StringBuilder();
        
        // Integer part
        while (position < sourceCode.length() && Character.isDigit(sourceCode.charAt(position))) {
            number.append(advance());
        }
        
        // Decimal part
        if (position < sourceCode.length() && sourceCode.charAt(position) == '.') {
            number.append(advance());
            
            while (position < sourceCode.length() && Character.isDigit(sourceCode.charAt(position))) {
                number.append(advance());
            }
        }
        
        // Exponent part
        if (position < sourceCode.length() && (sourceCode.charAt(position) == 'e' || sourceCode.charAt(position) == 'E')) {
            number.append(advance());
            
            if (position < sourceCode.length() && (sourceCode.charAt(position) == '+' || sourceCode.charAt(position) == '-')) {
                number.append(advance());
            }
            
            if (position < sourceCode.length() && Character.isDigit(sourceCode.charAt(position))) {
                while (position < sourceCode.length() && Character.isDigit(sourceCode.charAt(position))) {
                    number.append(advance());
                }
            } else {
                errorReporter.report(line, column, "Invalid number format: expected digits after exponent");
            }
        }
        
        return new Token(TokenType.NUMBER_LITERAL, number.toString(), startLine, startColumn);
    }
    
    private Token tokenizeIdentifier() {
        int startLine = line;
        int startColumn = column;
        StringBuilder identifier = new StringBuilder();
        
        while (position < sourceCode.length() && 
              (Character.isLetterOrDigit(sourceCode.charAt(position)) || 
               sourceCode.charAt(position) == '_' || 
               sourceCode.charAt(position) == '$')) {
            identifier.append(advance());
        }
        
        String value = identifier.toString();
        TokenType type = KEYWORDS.getOrDefault(value, TokenType.IDENTIFIER);
        
        return new Token(type, value, startLine, startColumn);
    }
    
    private Token tokenizeString() {
        int startLine = line;
        int startColumn = column;
        char quoteType = advance(); // Consume the opening quote
        StringBuilder string = new StringBuilder();
        
        while (position < sourceCode.length() && sourceCode.charAt(position) != quoteType) {
            // Handle escape sequences
            if (sourceCode.charAt(position) == '\\' && position + 1 < sourceCode.length()) {
                advance(); // Consume the backslash
                char escapedChar = advance();
                switch (escapedChar) {
                    case 'n': string.append('\n'); break;
                    case 'r': string.append('\r'); break;
                    case 't': string.append('\t'); break;
                    case '\'': string.append('\''); break;
                    case '"': string.append('"'); break;
                    case '\\': string.append('\\'); break;
                    default: 
                        string.append('\\');
                        string.append(escapedChar);
                }
            } else {
                string.append(advance());
            }
        }
        
        if (position >= sourceCode.length()) {
            errorReporter.report(line, column, "Unterminated string literal");
        } else {
            advance(); // Consume the closing quote
        }
        
        return new Token(TokenType.STRING_LITERAL, string.toString(), startLine, startColumn);
    }
    
    private Token tokenizeOperator() {
        int startLine = line;
        int startColumn = column;
        
        char first = advance();
        TokenType type;
        StringBuilder operator = new StringBuilder().append(first);
        
        // Check for multi-character operators
        if (first == '+') {
            if (match('+')) {
                operator.append('+');
                type = TokenType.INCREMENT;
            } else if (match('=')) {
                operator.append('=');
                type = TokenType.PLUS_ASSIGN;
            } else {
                type = TokenType.PLUS;
            }
        } else if (first == '-') {
            if (match('-')) {
                operator.append('-');
                type = TokenType.DECREMENT;
            } else if (match('=')) {
                operator.append('=');
                type = TokenType.MINUS_ASSIGN;
            } else if (match('>')) {
                operator.append('>');
                type = TokenType.ARROW;
            } else {
                type = TokenType.MINUS;
            }
        } else if (first == '*') {
            if (match('=')) {
                operator.append('=');
                type = TokenType.MULTIPLY_ASSIGN;
            } else if (match('*')) {
                operator.append('*');
                type = TokenType.POWER;
            } else {
                type = TokenType.MULTIPLY;
            }
        } else if (first == '/') {
            if (match('=')) {
                operator.append('=');
                type = TokenType.DIVIDE_ASSIGN;
            } else {
                type = TokenType.DIVIDE;
            }
        } else if (first == '%') {
            if (match('=')) {
                operator.append('=');
                type = TokenType.MODULO_ASSIGN;
            } else {
                type = TokenType.MODULO;
            }
        } else if (first == '=') {
            if (match('=')) {
                operator.append('=');
                if (match('=')) {
                    operator.append('=');
                    type = TokenType.STRICT_EQUAL;
                } else {
                    type = TokenType.EQUAL;
                }
            } else if (match('>')) {
                operator.append('>');
                type = TokenType.ARROW;
            } else {
                type = TokenType.ASSIGN;
            }
        } else if (first == '!') {
            if (match('=')) {
                operator.append('=');
                if (match('=')) {
                    operator.append('=');
                    type = TokenType.STRICT_NOT_EQUAL;
                } else {
                    type = TokenType.NOT_EQUAL;
                }
            } else {
                type = TokenType.NOT;
            }
        } else if (first == '>') {
            if (match('=')) {
                operator.append('=');
                type = TokenType.GREATER_THAN_EQUAL;
            } else if (match('>')) {
                operator.append('>');
                if (match('>')) {
                    operator.append('>');
                    type = TokenType.UNSIGNED_RIGHT_SHIFT;
                } else {
                    type = TokenType.RIGHT_SHIFT;
                }
            } else {
                type = TokenType.GREATER_THAN;
            }
        } else if (first == '<') {
            if (match('=')) {
                operator.append('=');
                type = TokenType.LESS_THAN_EQUAL;
            } else if (match('<')) {
                operator.append('<');
                type = TokenType.LEFT_SHIFT;
            } else {
                type = TokenType.LESS_THAN;
            }
        } else if (first == '&') {
            if (match('&')) {
                operator.append('&');
                type = TokenType.AND;
            } else {
                type = TokenType.BITWISE_AND;
            }
        } else if (first == '|') {
            if (match('|')) {
                operator.append('|');
                type = TokenType.OR;
            } else {
                type = TokenType.BITWISE_OR;
            }
        } else if (first == '^') {
            type = TokenType.BITWISE_XOR;
        } else if (first == '~') {
            type = TokenType.BITWISE_NOT;
        } else if (first == '?') {
            if (match('.')) {
                operator.append('.');
                type = TokenType.OPTIONAL_CHAIN;
            } else {
                type = TokenType.QUESTION_MARK;
            }
        } else {
            // Unknown operator
            errorReporter.report(startLine, startColumn, "Unknown operator: " + first);
            type = TokenType.IDENTIFIER; // Fallback
        }
        
        return new Token(type, operator.toString(), startLine, startColumn);
    }
    
    private Token tokenizePunctuation() {
        int startLine = line;
        int startColumn = column;
        
        char c = advance();
        TokenType type;
        
        switch (c) {
            case '.': 
                if (match('.') && match('.')) {
                    type = TokenType.SPREAD;
                } else {
                    type = TokenType.DOT;
                }
                break;
            case ',': type = TokenType.COMMA; break;
            case ':': type = TokenType.COLON; break;
            case ';': type = TokenType.SEMICOLON; break;
            case '(': type = TokenType.LEFT_PAREN; break;
            case ')': type = TokenType.RIGHT_PAREN; break;
            case '{': type = TokenType.LEFT_BRACE; break;
            case '}': type = TokenType.RIGHT_BRACE; break;
            case '[': type = TokenType.LEFT_BRACKET; break;
            case ']': type = TokenType.RIGHT_BRACKET; break;
            default:
                // Unknown punctuation
                errorReporter.report(startLine, startColumn, "Unknown punctuation: " + c);
                type = TokenType.IDENTIFIER; // Fallback
                break;
        }
        
        return new Token(type, String.valueOf(c), startLine, startColumn);
    }
    
    private boolean isOperatorChar(char c) {
        return "+-*/%=!<>&|^~?".indexOf(c) != -1;
    }
    
    private boolean isPunctuationChar(char c) {
        return ".,;:(){}[]".indexOf(c) != -1;
    }
}