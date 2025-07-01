package com.craftinginterpreters.tool;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

// This class is used for generating the Abstract Syntax Tree
public class GenerateAst {

    public static void main(String[] args) throws IOException {
        //Exit with error 64 (invalid number of inputs) if an extra input space is entered
        if (args.length != 1) {
            System.err.println("Usage: generate_ast <output_directory>");
            System.exit(64);
        }

        String outputDir = args[0];

        defineAst(outputDir, "Expr", Arrays.asList(
           "Assign   : Token name, Expr value",
           "Binary   : Expr left, Token operator, Expr right",
           "Call     : Expr callee, Token paren, List<Expr> arguments",
           "Grouping : Expr expression",
           "Literal  : Object value",
           "Logical  : Expr left, Token operator, Expr right",
           "Unary    : Token operator, Expr right",
           "Variable : Token name"
        ));

        //For parsing statements.
        defineAst(outputDir, "Stmt", Arrays.asList(
                "Block      : List<Stmt> statements",
                "Expression : Expr expression",
                "Function   : Token name, List<Token> params," +
                        " List<Stmt> body",
                "If         : Expr condition, Stmt thenBranch," + " Stmt elseBranch",
                "Print      : Expr expression ",
                "Return     : Token keyword, Expr value",
                "Var        : Token name, Expr initializer",
                "While      : Expr condition, Stmt body"
        ));

    }


    //Instead of manually creating a java class, we end up using this tool to
    // generate the requisite classes.
    private static void defineAst (
            String outputDir, String baseName, List<String> types) throws IOException {
        String path = outputDir + "/" + baseName + ".java";
        PrintWriter writer = new PrintWriter(path, "UTF-8");

        writer.println("package com.craftinginterpreters.pandi;");
        writer.println();
        writer.println("import java.util.List;");
        writer.println();
        writer.println("abstract class " + baseName + " {");

        defineVisitor(writer, baseName, types);

        //Make the AST classes
        for (String type : types) {
            //Split the string basis the semi-colon and the first element is the class name
            //trim any leading white spaces
            String className = type.split(":")[0].trim();

            //The fields are everything to the right of the colon
            String fields = type.split(":")[1].trim();


            defineType(writer, baseName, className, fields);
        }

        writer.println();
        writer.println("  abstract <R> R accept(Visitor<R> visitor);");

        writer.println("}");
        writer.close();
    }

    private static void defineVisitor(PrintWriter writer, String baseName, List<String> types) {
        writer.println("    interface Visitor<R> {");

        for (String type : types) {
            String typeName = type.split(":")[0].trim();
            writer.println("    R visit" + typeName + baseName +
                    "(" + typeName + " " + baseName.toLowerCase() + ");");
        }

        writer.println("    }");

    }


    private static void defineType ( PrintWriter writer, String baseName, String className, String fieldList){

        writer.println("    static class " + className + " extends " + baseName + " {");

        //constructor
        writer.println("    " + className + "(" + fieldList + ") {");

        //store parameters in fields
        String[] fields = fieldList.split(", ");
        for (String field : fields) {
            String name = field.split(" ")[1];

            //Initialize all fields
            writer.println("    this." + name + " = " + name + ";");
        }
        writer.println("    }");

        //Visitor pattern
        writer.println();
        writer.println("    @Override");
        writer.println("    <R> R accept(Visitor<R> visitor) {");
        writer.println("      return visitor.visit" +
                className + baseName + "(this);");
        writer.println("    }");
        //fields
        writer.println();

        for (String field : fields) {
            writer.println("    final " + field + ";");
        }

        writer.println("}");
    }
}