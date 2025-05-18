package com.essa.compiler.utils;

import java.util.HashMap;
import java.util.Map;

/**
 * Context object that holds information about the compilation process
 * and is passed to different stages of the compiler
 */
public class CompilationContext {
    private ErrorReporter errorReporter;
    private boolean isTypeScript;
    private Map<String, Object> attributes = new HashMap<>();
    
    public ErrorReporter getErrorReporter() {
        return errorReporter;
    }
    
    public void setErrorReporter(ErrorReporter errorReporter) {
        this.errorReporter = errorReporter;
    }
    
    public boolean isTypeScript() {
        return isTypeScript;
    }
    
    public void setTypeScript(boolean isTypeScript) {
        this.isTypeScript = isTypeScript;
    }
    
    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }
    
    public Object getAttribute(String key) {
        return attributes.get(key);
    }
    
    public boolean hasAttribute(String key) {
        return attributes.containsKey(key);
    }
}