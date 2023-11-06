package test_coupling;

public class Main {

    public static void main(String[] args) {
        // Create instances of ClassA, ClassB, ClassC, and ClassD
        ClassA classA = new ClassA();
        ClassB classB = new ClassB();
        ClassC classC = new ClassC();
        ClassD classD = new ClassD();

        // Demonstrate method calls to create coupling
        classA.methodA(); // ClassA calls ClassB's method
        classB.methodB(); // ClassB calls ClassA's method
        classC.methodC(); // ClassC calls ClassD's method
        classD.methodD(); // ClassD calls ClassC's method
        classA.anotherMethod(); // ClassA calls ClassC's method
        classB.anotherMethod(); // ClassB calls ClassD's method
        classC.anotherMethod(); // ClassC calls ClassA's method
        classD.anotherMethod(); // ClassD calls ClassB's method
    }
}

class ClassA {
    public void methodA() {
        System.out.println("ClassA's methodA is called.");
        ClassB classB = new ClassB();
        classB.methodB();
    }

    public void anotherMethod() {
        System.out.println("ClassA's another method is called.");
        ClassC classC = new ClassC();
        classC.methodC();
    }
}

class ClassB {
    public void methodB() {
        System.out.println("ClassB's methodB is called.");
        ClassA classA = new ClassA();
        classA.methodA();
    }

    public void anotherMethod() {
        System.out.println("ClassC's another method is called.");
        ClassA classA = new ClassA();
        classA.anotherMethod();
    }
}

class ClassC {
    public void methodC() {
        System.out.println("ClassC's methodC is called.");
        ClassD classD = new ClassD();
        classD.methodD();
    }
    
    public void anotherMethod() {
        System.out.println("ClassC's another method is called.");
        ClassA classA = new ClassA();
        classA.anotherMethod();
    }
}

class ClassD {
    public void methodD() {
        System.out.println("ClassD's methodD is called.");
        ClassC classC = new ClassC();
        classC.methodC();
    }

    public void anotherMethod() {
        System.out.println("ClassC's another method is called.");
        ClassA classA = new ClassA();
        classA.anotherMethod();
    }
}
