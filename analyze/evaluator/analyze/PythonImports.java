package evaluator.analyze;

import evaluator.analyze.TreeSitterAdapter.Source;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.treesitter.TSNode;

/**
 * A Python file's imports and where they lead. `import a.b as c` binds c (`a` without the alias);
 * `from m import x as y` binds y; `from m import *` binds nothing. A package's `__init__.py` imports
 * to re-export, so none of its names counts as unused.
 */
final class PythonImports {
    private static final Set<String> IMPORTS = Set.of("import_statement", "import_from_statement", "future_import_statement");

    private PythonImports() {}

    /** Each import statement's modules, names and line, and the bound names the file never uses. */
    static List<Import> of(TSNode root, Source source, String path) {
        Set<String> used = path.endsWith("__init__.py") ? null : TreeSitterAdapter.names(root, source, Set.of("identifier"), IMPORTS);
        List<Import> imports = new ArrayList<>();
        for (int i = 0; i < root.getNamedChildCount(); i++) {
            TSNode statement = root.getNamedChild(i);
            int line = statement.getStartPoint().getRow() + 1;
            if (statement.getType().equals("import_statement")) plain(statement, source, line, used, imports);
            else if (statement.getType().equals("import_from_statement")) from(statement, source, line, used, imports);
        }
        return imports;
    }

    /** `import a.b, c as d`: one import per module. */
    private static void plain(TSNode statement, Source source, int line, Set<String> used, List<Import> imports) {
        for (int i = 0; i < statement.getNamedChildCount(); i++) {
            TSNode part = statement.getNamedChild(i);
            TSNode dotted = part.getType().equals("aliased_import") ? TreeSitterAdapter.field(part, "name") : part;
            String module = source.text(dotted);
            String binds = part.getType().equals("aliased_import") ? source.text(TreeSitterAdapter.field(part, "alias")) : module.split("\\.")[0];
            imports.add(new Import(module, List.of(), line, used == null ? List.of() : TreeSitterAdapter.unused(List.of(binds), used)));
        }
    }

    /** `from m import x, y as z`: one import of m, naming x and y, binding x and z. */
    private static void from(TSNode statement, Source source, int line, Set<String> used, List<Import> imports) {
        String module = source.text(TreeSitterAdapter.field(statement, "module_name"));
        List<String> names = new ArrayList<>();
        List<String> bound = new ArrayList<>();
        for (int i = 0; i < statement.getChildCount(); i++) {
            if (!"name".equals(statement.getFieldNameForChild(i))) continue;
            TSNode part = statement.getChild(i);
            boolean aliased = part.getType().equals("aliased_import");
            String name = source.text(aliased ? TreeSitterAdapter.field(part, "name") : part);
            names.add(name);
            bound.add(aliased ? source.text(TreeSitterAdapter.field(part, "alias")) : name);
        }
        imports.add(new Import(module, names, line, used == null ? List.of() : TreeSitterAdapter.unused(bound, used)));
    }

    /**
     * Where an import leads: `from m import n` to module m.n when that is a file, else to m; a module
     * is `m/n.py` or `m/n/__init__.py`. Relative (`.util`, `..`) from the file's folder; absolute, any
     * file whose path ends so.
     */
    static List<String> resolve(Import imported, String from, Set<String> files) {
        Set<String> found = new LinkedHashSet<>();
        if (imported.names().isEmpty()) found.addAll(module(imported.module(), from, files));
        for (String name : imported.names()) {
            String dot = imported.module().endsWith(".") ? "" : ".";
            List<String> sub = module(imported.module() + dot + name, from, files);
            found.addAll(sub.isEmpty() ? module(imported.module(), from, files) : sub);
        }
        return List.copyOf(found);
    }

    /** One module's file: absolute, any path ending so; relative, from the file's folder, a dot past the first going up one, never above the root. */
    private static List<String> module(String module, String from, Set<String> files) {
        int dots = 0;
        while (dots < module.length() && module.charAt(dots) == '.') dots++;
        String rest = module.substring(dots).replace('.', '/');
        if (dots == 0) {
            List<String> found = new ArrayList<>(Adapter.endingWith(rest + ".py", files));
            found.addAll(Adapter.endingWith(rest + "/__init__.py", files));
            return found;
        }
        String base = TreeSitterAdapter.folder(from);
        for (int up = 1; up < dots; up++) {
            if (base.isEmpty()) return List.of();
            base = TreeSitterAdapter.folder(base);
        }
        String path = TreeSitterAdapter.normalize(base + "/" + rest);
        if (path == null) return List.of();
        List<String> found = new ArrayList<>();
        for (String candidate : List.of(path + ".py", path + "/__init__.py")) if (files.contains(TreeSitterAdapter.normalize(candidate))) found.add(TreeSitterAdapter.normalize(candidate));
        return found;
    }
}
