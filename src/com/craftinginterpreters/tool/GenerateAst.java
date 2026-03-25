package com.craftinginterpreters.tool;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;  

public class GenerateAst {
    public static void main(String[] args) throws IOException{
        if (args.length != 1) {
            System.err.println("Usage: generate_ast <output director>");
            System.exit(64);
        }
        String outputDir = args[0]; //Output directoru equals to 1st parameter in terminal
        defineAst(outputDir, "Expr", Arrays.asList( 
            "Binary    : Expr left, Token operator, Expr right", //Arithmetic (+, -, *, /) and logic (==, !=, <, <=, >, >=)
            "Grouping   : Expr expression", //parantheses
            "Literal    : Object value",  //Numbers, strings, booleans, nil
            "Unary      : Token operator, Expr right" //Prefix ! for logical not and - to negate a number
        ));
    }

    //Generates the entire Expr.java
    private static void defineAst(String outputDir, String baseName, List<String> types) throws IOException {
        String path = outputDir + "/" + baseName + ".java";
        PrintWriter writer = new PrintWriter(path, "UTF-8");

        //Base Expr class
        writer.println("package com.craftinginterpreters.lox;");
        writer.println();
        writer.println("import java.util.List;");
        writer.println();
        writer.println("abstract class " + baseName + "{"); 

        defineVisitor(writer, baseName, types);

        //AST classes
        for (String type : types) {
            String className = type.split(":")[0].trim(); //String before :
            String fields = type.split(":")[1].trim(); //String after :
            defineType(writer, baseName, className, fields);

            //Base accept() method
            writer.println();
            writer.println("    abstract <R> R accept(Visitor<R> visitor);");
        }
        writer.println("}");
        writer.close();
    }

    //Generates subclass
    private static void defineType(PrintWriter writer, String baseName, String className, String fieldList) {
        //Class declaration
        writer.println("    static class " + className + " extends " + baseName + " {");

        //Fields or all variables
        String[] fields = fieldList.split(", ");
        for (String field : fields) {
            writer.println("        final " + field + ";");
        }

        //Constructor
        writer.println();
        writer.println("        " + className + "(" + fieldList + ") {");
        for (String field : fields) {
            String name = field.split(" ")[1];
            writer.println("            this." + name + " = " + name + ";");
        }
        writer.println("        }");

        //Visitor pattern
        writer.println();
        //Takes a Visitor object and calls the method passing itself (this)
        writer.println("        @Override");
        writer.println("        <R> R accept(Visitor<R> visitor) {");
        writer.println("            return visitor.visit" + className + baseName + "(this);");
        writer.println("        }");

        writer.println("    }");
    }
    
    //Generates the visitor interface for each type
    private static void defineVisitor(PrintWriter writer, String baseName, List<String>types) {
        writer.println("    interface Visitor<R> {");
        for (String type : types) {
            String typeName = type.split(":")[0].trim();
            writer.println("        R visit" + typeName + baseName + "(" + typeName + " " + baseName.toLowerCase() + ");");
        }
        writer.println("    }");
    }
}
