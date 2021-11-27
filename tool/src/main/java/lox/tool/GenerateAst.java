package lox.tool;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GenerateAst {
    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.err.println("Usage: generate_ast <output directory>");
            System.exit(64);
        }
        var outputDir = args[0];
        var types = Arrays.asList(
            "Binary     : Expr left, Token operator, Expr right",
            "Grouping   : Expr expression",
            "Literal    : Object value",
            "Unary      : Token operator, Expr right"
        );
        defineAst(outputDir, "Expr", types);
    }

    private static void defineAst(String outputDir, String baseName, List<String> types) throws IOException {
        var path = outputDir + "/lox/lang/" + baseName + ".java";
        try (var writer = new PrintWriter(path, StandardCharsets.UTF_8)) {
            writer.println("package lox.lang;");
            writer.println();
            writer.println("import java.util.List;");
            writer.println();
            writer.println("abstract class " + baseName + " {");

            defineVisitor(writer, baseName, types);

            // AST classes
            for (String type : types) {
                var split = type.split(":");
                var className = split[0].trim();
                var fields = split[1].trim();
                defineType(writer, baseName, className, fields);
            }

            // base accept() method
            writer.println();
            writer.println("  abstract <R> R accept(Visitor<R> visitor);");

            writer.println("}");
        }
    }

    private static void defineVisitor(PrintWriter writer, String baseName, List<String> types) {
        writer.println("  interface Visitor<R> {");

        for (var type : types) {
            var typeName = type.split(":")[0].trim();
            writer.println("    R visit" + typeName + baseName + "("
                    + typeName + " " + baseName.toLowerCase() + ");");
        }

        writer.println("  }");
    }

    private static void defineType(PrintWriter writer, String baseName, String className, String fieldList) {
        writer.println("  static class " + className + " extends " + baseName + " {");

        // constructor
        writer.println("    " + className + "(" + fieldList + ") {");

        var fields = Stream.of(fieldList.split(",")).map(String::trim).map(f -> f.split(" "))
                .collect(Collectors.toList());

        // store params in fields
        for (var field : fields) {
            var id = field[1];
            writer.println("      this." + id + " = " + id + ";");
        }

        writer.println("    }");

        // visitor pattern
        writer.println();
        writer.println("    <R> R accept(Visitor<R> visitor) {");
        writer.println("      return visitor.visit" + className + baseName + "(this);");
        writer.println("    }");

        // fields
        writer.println();
        for (var field : fields) {
            var type = field[0];
            var id = field[1];
            var idCap = id.substring(0, 1).toUpperCase() + id.substring(1);
            writer.println("    private final " + type + " " +id + ";");
            writer.println("    public " + type + " get" + idCap + "() { return " + id + "; }");
        }

        writer.println("  }");
    }
}
