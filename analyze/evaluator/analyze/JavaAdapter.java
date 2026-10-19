package evaluator.analyze;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.LineMap;
import com.sun.source.tree.MethodTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.TreeScanner;
import com.sun.source.util.Trees;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;

/**
 * Java, read with the JDK's own parser: parsed only, never compiled or annotation-processed (E-013).
 * Every method with a body is a function, including those of a class declared inside a method;
 * a constructor is named for its class. Counted as `scripts/Scan.java` counts paths.
 */
public final class JavaAdapter implements Adapter {
    @Override
    public boolean reads(String language) {
        return language.equals("Java");
    }

    @Override
    public String tool() {
        return "evaluator analyze java 1 (JDK " + Runtime.version().feature() + " parser)";
    }

    /** A class in this repository: `a.b.C` is a file ending `a/b/C.java`; a static import's member, or a nested class, drops a segment at a time. */
    @Override
    public List<String> resolve(Import imported, String from, Set<String> files) {
        String module = imported.module();
        if (module.endsWith(".*")) return JavaImports.packageFiles(module.substring(0, module.length() - 2), files);
        for (String name = module; name.contains("."); name = name.substring(0, name.lastIndexOf('.'))) {
            List<String> found = Adapter.endingWith(name.replace('.', '/') + ".java", files);
            if (!found.isEmpty()) return found;
        }
        return List.of();
    }

    /** The file's methods measured, or why it does not parse; javac's parser only, no compile, no annotation processing. */
    @Override
    public Parsed parse(String path, String text) {
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        var source = new SimpleJavaFileObject(URI.create("string:///source.java"), JavaFileObject.Kind.SOURCE) {
            @Override
            public CharSequence getCharContent(boolean ignore) {
                return text;
            }
        };
        JavacTask task = (JavacTask) ToolProvider.getSystemJavaCompiler().getTask(null, null, diagnostics, List.of("-proc:none"), null, List.of(source));
        try {
            List<Measure> found = new ArrayList<>();
            List<Import> imports = new ArrayList<>();
            SourcePositions positions = Trees.instance(task).getSourcePositions();
            for (CompilationUnitTree unit : task.parse()) {
                new Methods(unit, positions, found).scan(unit, null);
                imports.addAll(JavaImports.of(unit, positions));
            }
            for (Diagnostic<? extends JavaFileObject> d : diagnostics.getDiagnostics()) {
                if (d.getKind() == Diagnostic.Kind.ERROR) return Parsed.unread("does not parse: line " + d.getLineNumber());
            }
            return new Parsed(found, imports, null);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Walks the classes and their methods, keeping the class nesting for each method's name. */
    private static final class Methods extends TreeScanner<Void, Void> {
        private final CompilationUnitTree unit;
        private final SourcePositions positions;
        private final List<Measure> found;
        private final Deque<String> classes = new ArrayDeque<>();
        private final Map<String, Integer> seen = new HashMap<>();

        Methods(CompilationUnitTree unit, SourcePositions positions, List<Measure> found) {
            this.unit = unit;
            this.positions = positions;
            this.found = found;
        }

        @Override
        public Void visitClass(ClassTree tree, Void unused) {
            String name = tree.getSimpleName().toString();
            classes.addLast(name.isEmpty() ? "<anonymous>" : name);
            super.visitClass(tree, null);
            classes.removeLast();
            return null;
        }

        @Override
        public Void visitMethod(MethodTree tree, Void unused) {
            if (tree.getBody() != null) found.add(measure(tree));
            return super.visitMethod(tree, null);
        }

        /** One method's name (numbered when overloaded), its first line and its two complexities. */
        private Measure measure(MethodTree tree) {
            String name = tree.getName().contentEquals("<init>") ? classes.getLast() : tree.getName().toString();
            String qualified = String.join(".", classes) + "." + name;
            int count = seen.merge(qualified, 1, Integer::sum);
            LineMap lines = unit.getLineMap();
            int line = (int) lines.getLineNumber(positions.getStartPosition(unit, tree));
            return new Measure(qualified + (count > 1 ? "#" + count : ""), line, JavaComplexity.cyclomatic(tree), JavaComplexity.cognitive(tree));
        }
    }
}
