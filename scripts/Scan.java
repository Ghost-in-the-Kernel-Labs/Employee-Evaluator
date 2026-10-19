import com.sun.source.tree.CaseTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.DefaultCaseLabelTree;
import com.sun.source.tree.LineMap;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
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
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.tools.JavaCompiler;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;

/**
 * Every method in a Java source file, read with the JDK's own parser (no dependency): its key
 * ("path:Class.method", "#2" for a second of one name), its length, the paths through it, whether a
 * comment directly above says what it is for, the decision it cites (`// Decision: E-021`), the
 * methods it calls by name, and its body as text. Ported from Katana, itself from Suite Builder's scripts/kernel-scan.mjs.
 */
final class Scan {
    static final Pattern DECISION = Pattern.compile("^\\s*// Decision: (E-\\d{3})\\s*$");

    record Function(String key, String name, int line, int lines, int paths, boolean documented,
                    String decision, Set<String> calls, String body) {}

    /** Every function of a Java source, by javac's own parse, with its lines, paths and whether a comment says what it is for. */
    static List<Function> functions(String path, String text) {
        JavaCompiler javac = ToolProvider.getSystemJavaCompiler();
        var source = new SimpleJavaFileObject(URI.create("string:///" + path), SimpleJavaFileObject.Kind.SOURCE) {
            @Override
            public CharSequence getCharContent(boolean ignore) {
                return text;
            }
        };
        JavacTask task = (JavacTask) javac.getTask(null, null, d -> {}, List.of("-proc:none"), null, List.of(source));
        try {
            List<Function> found = new ArrayList<>();
            SourcePositions positions = Trees.instance(task).getSourcePositions();
            for (CompilationUnitTree unit : task.parse()) new Visitor(path, text, unit, positions, found).scan(unit, null);
            return found;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Walks the classes and their methods, keeping the class nesting for each method's key. */
    private static final class Visitor extends TreeScanner<Void, Void> {
        private final String path;
        private final String[] lines;
        private final CompilationUnitTree unit;
        private final SourcePositions positions;
        private final List<Function> found;
        private final Deque<String> classes = new ArrayDeque<>();
        private final Map<String, Integer> seen = new HashMap<>();
        private final String text;

        Visitor(String path, String text, CompilationUnitTree unit, SourcePositions positions, List<Function> found) {
            this.path = path;
            this.text = text;
            this.lines = text.split("\n", -1);
            this.unit = unit;
            this.positions = positions;
            this.found = found;
        }

        @Override
        public Void visitClass(ClassTree tree, Void unused) {
            classes.addLast(tree.getSimpleName().toString());
            super.visitClass(tree, null);
            classes.removeLast();
            return null;
        }

        @Override
        public Void visitMethod(MethodTree tree, Void unused) {
            if (tree.getBody() != null) found.add(function(tree));
            return super.visitMethod(tree, null);
        }

        /** One method as a Function: its qualified key (numbered when overloaded), its size, its paths, its comment. */
        private Function function(MethodTree tree) {
            String name = tree.getName().contentEquals("<init>") ? classes.getLast() : tree.getName().toString();
            String qualified = String.join(".", classes) + "." + name;
            int count = seen.merge(qualified, 1, Integer::sum);
            LineMap map = unit.getLineMap();
            int start = (int) map.getLineNumber(positions.getStartPosition(unit, tree));
            int end = (int) map.getLineNumber(positions.getEndPosition(unit, tree));
            long bodyStart = positions.getStartPosition(unit, tree.getBody());
            long bodyEnd = positions.getEndPosition(unit, tree.getBody());
            String body = text.substring((int) bodyStart, (int) bodyEnd).replaceAll("\\s+", " ");
            List<String> above = commentAbove(start);
            String decision = null;
            boolean documented = false;
            for (String line : above) {
                Matcher m = DECISION.matcher(line);
                if (m.matches()) decision = m.group(1);
                else documented = true;
            }
            return new Function(path + ":" + qualified + (count > 1 ? "#" + count : ""), name, start,
                    end - start + 1, paths(tree), documented, decision, calls(tree), body);
        }

        /** The comment lines directly above a line: blocks ending there and runs of `//` lines, in any order. */
        private List<String> commentAbove(int line) {
            List<String> comment = new ArrayList<>();
            int at = line - 2;
            while (at >= 0) {
                String trimmed = lines[at].trim();
                if (trimmed.startsWith("//")) {
                    comment.add(0, lines[at--]);
                } else if (trimmed.endsWith("*/")) {
                    while (at >= 0) {
                        comment.add(0, lines[at]);
                        if (lines[at--].trim().startsWith("/*")) break;
                    }
                } else {
                    break;
                }
            }
            return comment;
        }
    }

    /** Decision points plus one: if, loops, non-default cases, catch, ?:, && and ||. */
    static int paths(MethodTree method) {
        int[] count = {1};
        new TreeScanner<Void, Void>() {
            @Override
            public Void scan(Tree tree, Void unused) {
                if (tree == null) return null;
                switch (tree.getKind()) {
                    case IF, FOR_LOOP, ENHANCED_FOR_LOOP, WHILE_LOOP, DO_WHILE_LOOP, CATCH, CONDITIONAL_EXPRESSION,
                            CONDITIONAL_AND, CONDITIONAL_OR -> count[0]++;
                    case CASE -> {
                        if (((CaseTree) tree).getLabels().stream().noneMatch(l -> l instanceof DefaultCaseLabelTree)) count[0]++;
                    }
                    default -> {}
                }
                return super.scan(tree, null);
            }

            @Override
            public Void visitClass(ClassTree tree, Void unused) {
                return null; // a class inside a method has its own methods
            }
        }.scan(method.getBody(), null);
        return count[0];
    }

    /** The simple names of the methods a method invokes. */
    static Set<String> calls(MethodTree method) {
        Set<String> names = new TreeSet<>();
        new TreeScanner<Void, Void>() {
            @Override
            public Void visitMethodInvocation(MethodInvocationTree tree, Void unused) {
                String select = tree.getMethodSelect().toString();
                names.add(select.substring(select.lastIndexOf('.') + 1));
                return super.visitMethodInvocation(tree, null);
            }
        }.scan(method.getBody(), null);
        return names;
    }
}
