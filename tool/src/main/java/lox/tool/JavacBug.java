package lox.tool;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

public class JavacBug {
    public static void main(String[] args) throws IOException {
        var grammar = new String[] {
            "Binary     : Expr left, Token operator, Expr right",
            "Grouping   : Expr expression",
            "Literal    : Object value",
            "Unary      : Token operator, Expr right"
        };

        var types = parse(grammar);
        System.out.println(types);
    }

    private static <K, V> Map<K, V> tbl(Iterable<Map.Entry<K, V>> entries) {
        var map = new LinkedHashMap<K, V>();
        for (Map.Entry<K,V> entry : entries) {
            map.put(entry.getKey(), entry.getValue());
        }
        return map;
    }

    private static Map<String, Map<String, String>> parse(String... lines) {
        var esess = Stream.of(lines).map(line -> {
            var sTokens = line.split(":", 2);
            var cName = sTokens[0].trim();
            var fields = sTokens[1].trim().split(",");
            return Map.entry(cName, tbl(Stream.of(fields).map(f -> {
                var fTokens = f.trim().split(" ");
                var fName = fTokens[1].trim();
                var fType = fTokens[0].trim();
                return Map.entry(fName, fType);
            })::iterator));
        });
        return tbl(esess::iterator);
    }
}
