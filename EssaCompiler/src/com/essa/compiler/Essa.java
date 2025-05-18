package com.essa.compiler;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import com.essa.compiler.lexer.Lexer;
import com.essa.compiler.lexer.Token;
import com.essa.compiler.parser.Parser;
import com.essa.compiler.parser.ast.Program;
import com.essa.compiler.semantic.SemanticAnalyzer;
import com.essa.compiler.codegen.CodeGenerator;
import com.essa.compiler.utils.CompilationContext;
import com.essa.compiler.utils.ErrorReporter;

/**
 * Essa - A modular compiler for TypeScript and JavaScript
 * Main compiler class that orchestrates the compilation process
 */
public class Essa {
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java Essa <filename>");
            return;
        }

        String filename = args[0];
        String sourceCode;
        
        try {
            sourceCode = new String(Files.readAllBytes(Paths.get(filename)));
        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
            return;
        }

        boolean isTypeScript = filename.endsWith(".ts");
        compile(sourceCode, filename, isTypeScript);
    }

    public static void compile(String sourceCode, String filename, boolean isTypeScript) {
        CompilationContext context = new CompilationContext();
        ErrorReporter errorReporter = new ErrorReporter(sourceCode);
        context.setErrorReporter(errorReporter);

        // Lexical Analysis
        System.out.println("\u001B[36m[Essa Compiler] Starting lexical analysis...\u001B[0m");
        Lexer lexer = new Lexer(sourceCode, errorReporter);
        List<Token> tokens = lexer.tokenize();
        
        if (errorReporter.hasErrors()) {
            printErrors(errorReporter);
            return;
        }
        
        // Syntax Analysis
        System.out.println("\u001B[36m[Essa Compiler] Starting syntax analysis...\u001B[0m");
        Parser parser = new Parser(tokens, errorReporter);
        Program program = parser.parse();
        
        if (errorReporter.hasErrors()) {
            printErrors(errorReporter);
            return;
        }
        
        // Semantic Analysis
        System.out.println("\u001B[36m[Essa Compiler] Starting semantic analysis...\u001B[0m");
        SemanticAnalyzer semanticAnalyzer = new SemanticAnalyzer(program, errorReporter, isTypeScript);
        semanticAnalyzer.analyze();
        
        if (errorReporter.hasErrors()) {
            printErrors(errorReporter);
            return;
        }
        
        // Code Generation
        System.out.println("\u001B[36m[Essa Compiler] Starting code generation...\u001B[0m");
        CodeGenerator codeGenerator = new CodeGenerator(program, context);
        String generatedCode = codeGenerator.generate();
        
        // Write output file
        String outputFilename = filename.substring(0, filename.lastIndexOf('.')) + ".js";
        try {
            Files.write(Paths.get(outputFilename), generatedCode.getBytes());
            System.out.println("\u001B[32m[Essa Compiler] Compilation successful. Output written to " + outputFilename + "\u001B[0m");
        } catch (IOException e) {
            System.err.println("Error writing output file: " + e.getMessage());
        }
    }
    
    private static void printErrors(ErrorReporter errorReporter) {
        System.out.println("\u001B[31m[Essa Compiler] Compilation failed with errors:\u001B[0m");
        errorReporter.printErrors();
    }
}