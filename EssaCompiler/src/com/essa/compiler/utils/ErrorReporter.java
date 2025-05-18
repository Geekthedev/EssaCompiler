package com.essa.compiler.utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for reporting and collecting errors during compilation
 */
public class ErrorReporter {
    private final String sourceCode;
    private final List<CompilationError> errors = new ArrayList<>();
    
    public ErrorReporter(String sourceCode) {
        this.sourceCode = sourceCode;
    }
    
    public void report(int line, int column, String message) {
        errors.add(new CompilationError(line, column, message));
    }
    
    public boolean hasErrors() {
        return !errors.isEmpty();
    }
    
    public List<CompilationError> getErrors() {
        return new ArrayList<>(errors);
    }
    
    public void printErrors() {
        String[] lines = sourceCode.split("\n");
        
        for (CompilationError error : errors) {
            System.err.println("\u001B[31mError at line " + error.line + ", column " + error.column + ": " + error.message + "\u001B[0m");
            
            if (error.line <= lines.length) {
                String errorLine = lines[error.line - 1];
                System.err.println(errorLine);
                
                StringBuilder pointer = new StringBuilder();
                for (int i = 0; i < error.column - 1; i++) {
                    pointer.append(" ");
                }
                pointer.append("^");
                System.err.println("\u001B[31m" + pointer.toString() + "\u001B[0m");
            }
        }
    }
    
    public static class CompilationError {
        public final int line;
        public final int column;
        public final String message;
        
        public CompilationError(int line, int column, String message) {
            this.line = line;
            this.column = column;
            this.message = message;
        }
    }
}