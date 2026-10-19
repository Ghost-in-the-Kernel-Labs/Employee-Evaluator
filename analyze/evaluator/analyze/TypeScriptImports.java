package evaluator.analyze;

import evaluator.analyze.TreeSitterAdapter.Source;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.treesitter.TSNode;

/**
 * A TypeScript file's imports and where they lead. `import d, { a as b } from "m"` binds d and b;
 * `import * as ns` binds ns; `import "m"` binds nothing (it is run for its effect); `export ... from
 * "m"` is a dependency that binds nothing. Only a relative path (`./`, `../`) leads into the
 * repository; a package name or a path alias leads out.
 */
final class TypeScriptImports {
    private static final Set<String> USES = Set.of("identifier", "type_identifier", "shorthand_property_identifier");
    private static final List<String> ENDINGS = List.of("", ".ts", ".tsx", ".d.ts", "/index.ts", "/index.tsx");

    private TypeScriptImports() {}

    /** Each import and re-export with its module and line, and the bound names the file never uses. */
    // Unsure: a path alias from tsconfig.json (`@/lib`) is read as outside the repository.
    static List<Import> of(TSNode root, Source source) {
        Set<String> used = TreeSitterAdapter.names(root, source, USES, Set.of("import_statement"));
        List<Import> imports = new ArrayList<>();
        for (int i = 0; i < root.getNamedChildCount(); i++) {
            TSNode statement = root.getNamedChild(i);
            TSNode from = TreeSitterAdapter.field(statement, "source");
            if (from == null) continue;
            String module = source.text(from).replaceAll("^[\"'`]|[\"'`]$", "");
            List<String> bound = statement.getType().equals("import_statement") ? bound(statement, source) : List.of();
            imports.add(new Import(module, List.of(), statement.getStartPoint().getRow() + 1, TreeSitterAdapter.unused(bound, used)));
        }
        return imports;
    }

    /** The names an import statement binds: its default, its namespace, and each named import (its alias when it has one). */
    private static List<String> bound(TSNode statement, Source source) {
        List<String> names = new ArrayList<>();
        for (int i = 0; i < statement.getNamedChildCount(); i++) {
            TSNode clause = statement.getNamedChild(i);
            if (!clause.getType().equals("import_clause")) continue;
            for (int j = 0; j < clause.getNamedChildCount(); j++) {
                TSNode part = clause.getNamedChild(j);
                switch (part.getType()) {
                    case "identifier" -> names.add(source.text(part));
                    case "namespace_import" -> names.add(source.text(part.getNamedChild(0)));
                    case "named_imports" -> specifiers(part, source, names);
                    default -> {}
                }
            }
        }
        return names;
    }

    private static void specifiers(TSNode named, Source source, List<String> names) {
        for (int k = 0; k < named.getNamedChildCount(); k++) {
            TSNode spec = named.getNamedChild(k);
            TSNode alias = TreeSitterAdapter.field(spec, "alias");
            names.add(source.text(alias != null ? alias : TreeSitterAdapter.field(spec, "name")));
        }
    }

    /** A relative module's file: the path as written, or with a TypeScript ending, or its folder's index. */
    static List<String> resolve(Import imported, String from, Set<String> files) {
        String module = imported.module();
        if (!module.startsWith("./") && !module.startsWith("../")) return List.of();
        String path = TreeSitterAdapter.normalize(TreeSitterAdapter.folder(from) + "/" + module);
        if (path == null) return List.of();
        for (String ending : ENDINGS) if (files.contains(path + ending)) return List.of(path + ending);
        return List.of();
    }
}
