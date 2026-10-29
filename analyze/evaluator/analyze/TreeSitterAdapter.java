package evaluator.analyze;

import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.treesitter.TSInputEncoding;
import org.treesitter.TSLanguage;
import org.treesitter.TSNode;
import org.treesitter.TSParser;
import org.treesitter.TSReader;

/**
 * What the tree-sitter adapters share (E-028): the parse, a file that does not parse said with its
 * line, the walk that finds functions and names them by their enclosing classes and functions, and
 * the counting both complexities share. A language says which nodes are functions, classes and
 * decisions, and how its own cognitive walk goes. Counted as E-030 counts Java.
 */
abstract class TreeSitterAdapter implements Adapter {
    /** The file's bytes as UTF-8, which tree-sitter's offsets index. */
    record Source(byte[] bytes) {
        String text(TSNode node) {
            return new String(bytes, node.getStartByte(), node.getEndByte() - node.getStartByte(), StandardCharsets.UTF_8);
        }
    }

    /** The grammar for a file of this language (TypeScript and TSX differ). */
    abstract TSLanguage grammar(String path);

    /** The name of a function measured on its own, or null when the node is not one. */
    abstract String functionName(TSNode node, Source source);

    /** The name of a class, or null when the node is not one. */
    abstract String className(TSNode node, Source source);

    /** How many decision points this node is for cyclomatic complexity (most are 0 or 1). */
    abstract int decisions(TSNode node, Source source);

    /** The cognitive complexity of a function's own code, its name given for recursion. */
    abstract int cognitive(TSNode function, String name, Source source);

    /** The file's imports, each with the names it binds that the file never uses. */
    abstract List<Import> imports(TSNode root, Source source, String path);

    @Override
    public Parsed parse(String path, String text) {
        Source source = new Source(text.getBytes(StandardCharsets.UTF_8));
        TSParser parser = new TSParser();
        parser.setLanguage(grammar(path));
        TSNode root = parser.parse(source.bytes().clone(), null, reader(source.bytes()), TSInputEncoding.TSInputEncodingUTF8).getRootNode();
        if (root.hasError()) return Parsed.unread("does not parse: line " + (firstError(root).getStartPoint().getRow() + 1));
        List<Measure> found = new ArrayList<>();
        functions(root, source, new ArrayDeque<>(), new HashMap<>(), found);
        return new Parsed(found, imports(root, source, path), null);
    }

    /**
     * Reads the bytes handed to the parse. The binding's string entry passes the text through JNI as modified
     * UTF-8, where a character beyond the Basic Multilingual Plane (an emoji) is 6 bytes, not 4, so every offset
     * after one ran past the real bytes `Source` slices (J-131). Parsing the real UTF-8 bytes makes the two agree.
     * The binding hands the reader the very array it was given as its buffer, so the parse is given a copy: the
     * reader's writes into the buffer never reach the bytes `Source` slices.
     */
    private static TSReader reader(byte[] bytes) {
        return (buffer, offset, point) -> {
            if (offset >= bytes.length) return 0;
            int count = Math.min(buffer.length, bytes.length - offset);
            System.arraycopy(bytes, offset, buffer, 0, count);
            return count;
        };
    }

    // Unsure: the walks recurse; code nested thousands deep could exhaust the stack (the sandbox's limits, TODO.md 1.5).
    /** Every function under node, in source order, each named by the classes and functions around it. */
    private void functions(TSNode node, Source source, Deque<String> scopes, Map<String, Integer> seen, List<Measure> found) {
        String function = functionName(node, source);
        String scope = function != null ? function : className(node, source);
        if (function != null) {
            String qualified = String.join(".", scopes) + (scopes.isEmpty() ? "" : ".") + function;
            int count = seen.merge(qualified, 1, Integer::sum);
            found.add(new Measure(qualified + (count > 1 ? "#" + count : ""), node.getStartPoint().getRow() + 1,
                    1 + decisionsIn(node, source), cognitive(node, function, source), node.getEndPoint().getRow() + 1, source.text(node)));
        }
        if (scope != null) scopes.addLast(scope);
        for (int i = 0; i < node.getChildCount(); i++) functions(node.getChild(i), source, scopes, seen, found);
        if (scope != null) scopes.removeLast();
    }

    /** The decision points of a function's own code: a function or class inside it is measured on its own. */
    private int decisionsIn(TSNode function, Source source) {
        int count = 0;
        for (int i = 0; i < function.getChildCount(); i++) count += decisionsUnder(function.getChild(i), source);
        return count;
    }

    private int decisionsUnder(TSNode node, Source source) {
        if (ownUnit(node, source)) return 0;
        int count = decisions(node, source);
        for (int i = 0; i < node.getChildCount(); i++) count += decisionsUnder(node.getChild(i), source);
        return count;
    }

    /** Whether a node is measured on its own, so the function around it does not count it. */
    boolean ownUnit(TSNode node, Source source) {
        return functionName(node, source) != null || className(node, source) != null;
    }

    /** The first node tree-sitter could not parse or had to invent. */
    private static TSNode firstError(TSNode node) {
        if (node.isError() || node.isMissing()) return node;
        for (int i = 0; i < node.getChildCount(); i++) {
            TSNode child = node.getChild(i);
            if (child.hasError() || child.isMissing()) return firstError(child);
        }
        return node;
    }

    /** The text of every node of these types outside the nodes of the skipped types: the names a file uses. */
    static Set<String> names(TSNode node, Source source, Set<String> types, Set<String> skipped) {
        Set<String> found = new java.util.HashSet<>();
        collect(node, source, types, skipped, found);
        return found;
    }

    private static void collect(TSNode node, Source source, Set<String> types, Set<String> skipped, Set<String> found) {
        if (skipped.contains(node.getType())) return;
        if (types.contains(node.getType())) found.add(source.text(node));
        for (int i = 0; i < node.getChildCount(); i++) collect(node.getChild(i), source, types, skipped, found);
    }

    /** The bound names not in `used`, in order. */
    static List<String> unused(List<String> bound, Set<String> used) {
        return bound.stream().filter(name -> !used.contains(name)).toList();
    }

    /** A path with its `.` and `..` segments resolved, or null when it climbs out of the repository. */
    static String normalize(String path) {
        Deque<String> parts = new ArrayDeque<>();
        for (String part : path.split("/")) {
            if (part.isEmpty() || part.equals(".")) continue;
            if (!part.equals("..")) parts.addLast(part);
            else if (parts.pollLast() == null) return null;
        }
        return String.join("/", parts);
    }

    /** The folder a file is in, "" at the root. */
    static String folder(String path) {
        int slash = path.lastIndexOf('/');
        return slash < 0 ? "" : path.substring(0, slash);
    }

    /** A node's child by its field name, or null. */
    static TSNode field(TSNode node, String name) {
        TSNode child = node.getChildByFieldName(name);
        return child == null || child.isNull() ? null : child;
    }

    /**
     * One run of logical operators read left to right, brackets looked through: the operators, and the
     * operands that are not themselves part of the run. `operator` names a node's operator, or null when
     * the node is not a logical one.
     */
    static void flatten(TSNode node, Set<String> brackets, java.util.function.Function<TSNode, String> operator,
                        List<String> operators, List<TSNode> operands) {
        TSNode bare = node;
        while (brackets.contains(bare.getType()) && bare.getNamedChildCount() == 1) bare = bare.getNamedChild(0);
        String op = operator.apply(bare);
        if (op == null) {
            operands.add(node);
            return;
        }
        flatten(field(bare, "left"), brackets, operator, operators, operands);
        operators.add(op);
        flatten(field(bare, "right"), brackets, operator, operators, operands);
    }

    /** +1 for each change of operator along a run (`a && b && c` is 1, `a && b || c` is 2). */
    static int runs(List<String> operators) {
        int count = 0;
        for (int i = 0; i < operators.size(); i++) if (i == 0 || !operators.get(i).equals(operators.get(i - 1))) count++;
        return count;
    }
}
