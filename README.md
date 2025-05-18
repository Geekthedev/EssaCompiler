
# **Essa Compiler**

*A modular compiler for TypeScript and JavaScript, written in Java.*

> **Status:** Under Active Development

Essa Compiler is a robust, extensible compiler targeting TypeScript and JavaScript. Written entirely in Java, it supports both procedural and object-oriented programming paradigms. Essa focuses on correctness, modularity, and clear error diagnostics, making it suitable for both educational and real-world use cases.

---

## **🚀 Features**

* **Lexical Analysis** — Tokenizes TypeScript and JavaScript code with support for comments, literals, and keywords.
* **Parser & AST Generation** — Implements a recursive descent parser that builds a clean Abstract Syntax Tree (AST).
* **Semantic Analysis** — Performs type checking, manages symbol tables, and validates OOP relationships.
* **JavaScript Code Generation** — Translates the AST into optimized JavaScript, supporting type erasure and structural fidelity.
* **Error Reporting** — Provides precise diagnostics with line/column context, syntax highlighting, and fix suggestions.
* **Modular System** — Built with a modular structure to support future enhancements and plugin-based extensions.
* **Type Inference** — Offers basic inference capabilities for untyped TypeScript constructs.
* **Color-Coded Console Output** — Improves readability and debugging experience.

---

## **📁 Project Structure**

```
essa-compiler/
├── src/
│   └── com/
│       └── essa/
│           └── compiler/
│               ├── lexer/          # Lexical analysis
│               ├── parser/         # Syntax analysis and AST
│               │   └── ast/        
│               ├── semantic/       # Type checking and symbol tables
│               ├── codegen/        # JavaScript code generation
│               └── utils/          # Common utilities
├── bin/                           # Compiled class files
└── test/                          # Sample and unit test sources
```

---

## **🧩 Core Components**

### **Lexer**

Located in: `src/com/essa/compiler/lexer/`

* Tokenizes TypeScript and JavaScript source code.
* Detects and reports invalid characters and malformed tokens.
* Recognizes strings, numbers, keywords, identifiers, and comments.

### **Parser**

Located in: `src/com/essa/compiler/parser/`

* Builds an Abstract Syntax Tree (AST) using recursive descent parsing.
* Supports procedural and OOP constructs.
* Fully compliant with TypeScript's extended syntax.

### **Semantic Analyzer**

Located in: `src/com/essa/compiler/semantic/`

* Performs static type checking.
* Manages scoping via symbol tables.
* Supports inheritance, interfaces, generics, and access modifiers.

### **Code Generator**

Located in: `src/com/essa/compiler/codegen/`

* Outputs executable JavaScript code.
* Handles TypeScript-specific constructs via type erasure.
* Maintains source fidelity for traceable debugging.

---

## **🛠 Requirements**

* Java Development Kit (JDK) 11 or higher
* Node.js (only required to execute generated JavaScript, not for building)

---

## **🔧 Building the Compiler**

1. Compile all Java sources:

```bash
javac -d bin src/com/essa/compiler/**/*.java
```

2. Run the compiler with a `.ts` or `.js` file:

```bash
java -cp bin com.essa.compiler.Main path/to/input.ts
```

> Output JavaScript will be printed to the console or written to an output file, depending on configuration.

---

## **🎯 Language Features**

### **TypeScript**

* Type annotations
* Interfaces and generics
* Classes with access modifiers
* Union and intersection types
* Type inference and structural typing
* Module support

### **JavaScript (ES6+)**

* Classes and inheritance
* Arrow functions and async/await
* Destructuring and template literals
* Spread/rest operators
* Modules and lexical scoping

### **Object-Oriented Programming**

* Class and interface definitions
* Inheritance and polymorphism
* Encapsulation via access modifiers
* Static and abstract members

### **Procedural Programming**

* Function declarations
* Control flow: `if`, `switch`, loops
* Block scoping and variable declarations

---

## **🔍 Error Reporting**

The compiler provides clear, actionable diagnostics, including:

* Line and column information
* Highlighted error snippets
* Context-aware messages
* Suggested fixes

Example:

```
Error at line 5, column 10: Type 'string' is not assignable to type 'number'
let x: number = "hello";
         ^
```

---

## **📦 Testing**

Test files are located in the `test/` directory and can be compiled or executed manually using standard Java tooling. Unit testing integration (e.g., with JUnit) is planned for future releases.

---

## **📜 License**

MIT License — See [LICENSE](./LICENSE) for full details.

---

## **👥 Authors**

* Initial work — **Anointed Joseph (soem)**

---

## **🙏 Acknowledgments**

* TypeScript Compiler (`tsc`)
* Java Compiler API Documentation
* ANTLR Project (for parsing concepts and inspiration)

