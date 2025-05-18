// This is a sample TypeScript file to test the Essa compiler

// Variable declarations with types
let num: number = 42;
const str: string = "Hello, TypeScript!";
let flag: boolean = true;

// Function with typed parameters and return type
function add(a: number, b: number): number {
    return a + b;
}

// Class with properties and methods
class Person {
    private name: string;
    private age: number;
    
    constructor(name: string, age: number) {
        this.name = name;
        this.age = age;
    }
    
    public greet(): string {
        return `Hello, my name is ${this.name} and I am ${this.age} years old.`;
    }
    
    public static createPerson(name: string, age: number): Person {
        return new Person(name, age);
    }
}

// Interface
interface Shape {
    area(): number;
    perimeter(): number;
}

// Class implementing interface
class Circle implements Shape {
    private radius: number;
    
    constructor(radius: number) {
        this.radius = radius;
    }
    
    public area(): number {
        return Math.PI * this.radius * this.radius;
    }
    
    public perimeter(): number {
        return 2 * Math.PI * this.radius;
    }
}

// Create and use objects
const john = new Person("John", 30);
console.log(john.greet());

const circle = new Circle(5);
console.log(`Circle area: ${circle.area()}`);
console.log(`Circle perimeter: ${circle.perimeter()}`);

// Conditional logic
if (flag) {
    console.log("Flag is true");
} else {
    console.log("Flag is false");
}

// Loop
for (let i = 0; i < 5; i++) {
    console.log(`Iteration ${i}: ${add(i, 10)}`);
}