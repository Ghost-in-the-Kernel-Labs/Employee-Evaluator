package evaluator.analyze;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.TreeScanner;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** A Java file's imports, and which of the names they bind its code never uses. */
final class JavaImports {
    private JavaImports() {}

    /** Each import with its line; a name it binds is unused when no identifier in the code outside the imports is that name. */
    // Unsure: a class named only in Javadoc (`{@link Foo}`) counts as unused here, though javac keeps the import.
    static List<Import> of(CompilationUnitTree unit, SourcePositions positions) {
        Set<String> used = identifiers(unit);
        List<Import> imports = new ArrayList<>();
        for (ImportTree imported : unit.getImports()) {
            MemberSelectTree name = (MemberSelectTree) imported.getQualifiedIdentifier();
            String module = name.toString();
            String binds = name.getIdentifier().toString();
            int line = (int) unit.getLineMap().getLineNumber(positions.getStartPosition(unit, imported));
            List<String> unused = binds.equals("*") || used.contains(binds) ? List.of() : List.of(binds);
            imports.add(new Import(module, List.of(), line, unused));
        }
        return imports;
    }

    /** The files of a package (`a.b.*`): those directly in a folder ending `a/b`. */
    static List<String> packageFiles(String pkg, Set<String> files) {
        String folder = pkg.replace('.', '/');
        List<String> found = new ArrayList<>();
        for (String file : files) {
            int slash = file.lastIndexOf('/');
            String dir = slash < 0 ? "" : file.substring(0, slash);
            if (file.endsWith(".java") && (dir.equals(folder) || dir.endsWith("/" + folder))) found.add(file);
        }
        return found;
    }

    /** Every simple name the file's code uses (a type, a call, an annotation), its import lines left out. */
    private static Set<String> identifiers(CompilationUnitTree unit) {
        Set<String> names = new HashSet<>();
        new TreeScanner<Void, Void>() {
            @Override
            public Void visitImport(ImportTree tree, Void unused) {
                return null;
            }

            @Override
            public Void visitIdentifier(IdentifierTree tree, Void unused) {
                names.add(tree.getName().toString());
                return null;
            }
        }.scan(unit, null);
        return names;
    }
}
