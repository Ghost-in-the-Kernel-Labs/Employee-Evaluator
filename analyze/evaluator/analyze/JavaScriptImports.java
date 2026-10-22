package evaluator.analyze;

import evaluator.analyze.TreeSitterAdapter.Source;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.treesitter.TSNode;

/**
 * A JavaScript file's imports: `import` statements as TypeScript reads them, and each top-level
 * `const x = require("m")` or `const { a, b: c } = require("m")`, binding x, or a and c. A bare
 * `require("m")` binds nothing. A require's names are used when they appear outside its own declaration.
 */
final class JavaScriptImports {
    private static final Set<String> USES = Set.of("identifier", "shorthand_property_identifier");

    private JavaScriptImports() {}

    /** The imports and the requires, in file order. */
    static List<Import> of(TSNode root, Source source) {
        List<Import> imports = new ArrayList<>(TypeScriptImports.of(root, source));
        List<TSNode> declarators = new ArrayList<>();
        for (int i = 0; i < root.getNamedChildCount(); i++) requires(root.getNamedChild(i), source, declarators, imports);
        Set<String> used = new HashSet<>();
        uses(root, source, declarators, used);
        List<Import> resolved = new ArrayList<>();
        for (Import imported : imports) resolved.add(imported.names().isEmpty() ? imported
                : new Import(imported.module(), List.of(), imported.line(), TreeSitterAdapter.unused(imported.names(), used)));
        resolved.sort(java.util.Comparator.comparingInt(Import::line));
        return resolved;
    }

    /** A statement's requires: declarators whose value is `require("m")`, and a bare `require("m");`. Names bound are carried until their use is known. */
    private static void requires(TSNode statement, Source source, List<TSNode> declarators, List<Import> imports) {
        String type = statement.getType();
        if (type.equals("expression_statement")) {
            String module = required(statement.getNamedChild(0), source);
            if (module != null) imports.add(new Import(module, List.of(), statement.getStartPoint().getRow() + 1, List.of()));
            return;
        }
        if (!type.equals("lexical_declaration") && !type.equals("variable_declaration")) return;
        for (int i = 0; i < statement.getNamedChildCount(); i++) {
            TSNode declarator = statement.getNamedChild(i);
            String module = declarator.getType().equals("variable_declarator") ? required(TreeSitterAdapter.field(declarator, "value"), source) : null;
            if (module == null) continue;
            declarators.add(declarator);
            List<String> names = new ArrayList<>();
            bound(TreeSitterAdapter.field(declarator, "name"), source, names);
            imports.add(new Import(module, names, statement.getStartPoint().getRow() + 1, List.of()));
        }
    }

    /** The module of a `require("m")` call with one string argument, or null. */
    static String required(TSNode call, Source source) {
        if (call == null || !call.getType().equals("call_expression")) return null;
        TSNode function = TreeSitterAdapter.field(call, "function"), arguments = TreeSitterAdapter.field(call, "arguments");
        if (function == null || !source.text(function).equals("require") || arguments == null || arguments.getNamedChildCount() != 1) return null;
        TSNode argument = arguments.getNamedChild(0);
        return argument.getType().equals("string") ? source.text(argument).replaceAll("^[\"']|[\"']$", "") : null;
    }

    /** The names a binding pattern binds: an identifier, or each property of an object pattern (its alias when it has one). */
    private static void bound(TSNode pattern, Source source, List<String> names) {
        if (pattern == null) return;
        switch (pattern.getType()) {
            case "identifier", "shorthand_property_identifier_pattern" -> names.add(source.text(pattern));
            case "pair_pattern" -> bound(TreeSitterAdapter.field(pattern, "value"), source, names);
            default -> {
                for (int i = 0; i < pattern.getNamedChildCount(); i++) bound(pattern.getNamedChild(i), source, names);
            }
        }
    }

    /** Every name used in the file, the require declarations themselves left out. */
    private static void uses(TSNode node, Source source, List<TSNode> declarators, Set<String> used) {
        for (TSNode skip : declarators) if (skip.getStartByte() == node.getStartByte() && skip.getEndByte() == node.getEndByte()) return;
        if (USES.contains(node.getType())) used.add(source.text(node));
        for (int i = 0; i < node.getChildCount(); i++) uses(node.getChild(i), source, declarators, used);
    }
}
