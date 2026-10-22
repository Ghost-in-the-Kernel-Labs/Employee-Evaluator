package evaluator.analyze;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.treesitter.TSLanguage;
import org.treesitter.TSNode;
import org.treesitter.TreeSitterTsx;
import org.treesitter.TreeSitterTypescript;

/**
 * TypeScript (and TSX), read with tree-sitter. A function measured on its own is a function
 * declaration, a method, or a function or arrow function given a name (`const f = () => ...`, a class
 * field); an unnamed one, a callback, counts in its function one level deeper, as a Java lambda does.
 * Decisions: if, loops, a case that is not default, catch, `?:`, `&&` and `||`.
 */
public class TypeScriptAdapter extends TreeSitterAdapter {
    private static final Set<String> BRACKETS = Set.of("parenthesized_expression");
    private static final Set<String> CALLABLES = Set.of("arrow_function", "function_expression", "generator_function");
    private static final Set<String> LOOPS = Set.of("for_statement", "for_in_statement", "while_statement", "do_statement");

    @Override
    public boolean reads(String language) {
        return language.equals("TypeScript");
    }

    @Override
    public String tool() {
        return "evaluator analyze typescript 1 (tree-sitter-typescript 0.23.2)";
    }

    @Override
    TSLanguage grammar(String path) {
        return path.endsWith(".tsx") ? new TreeSitterTsx() : new TreeSitterTypescript();
    }

    /** A declaration's or method's own name; an unnamed function takes the name of the variable or field holding it. */
    @Override
    String functionName(TSNode node, Source source) {
        String type = node.getType();
        if (type.equals("function_declaration") || type.equals("generator_function_declaration") || type.equals("method_definition")) {
            return source.text(field(node, "name"));
        }
        if (!CALLABLES.contains(type)) return null;
        TSNode parent = node.getParent();
        boolean named = parent != null && (parent.getType().equals("variable_declarator") || parent.getType().equals("public_field_definition"));
        return named && field(parent, "name") != null ? source.text(field(parent, "name")) : null;
    }

    @Override
    String className(TSNode node, Source source) {
        String type = node.getType();
        if (!type.equals("class_declaration") && !type.equals("abstract_class_declaration") && !type.equals("class")) return null;
        TSNode name = field(node, "name");
        return name == null ? "<anonymous>" : source.text(name);
    }

    @Override
    int decisions(TSNode node, Source source) {
        return switch (node.getType()) {
            case "if_statement", "for_statement", "for_in_statement", "while_statement", "do_statement", "switch_case",
                    "catch_clause", "ternary_expression" -> 1;
            case "binary_expression" -> logicalOperator(node) != null ? 1 : 0;
            default -> 0;
        };
    }

    /** `&&` or `||` when the node is one, else null; `??` is not a decision here. */
    private static String logicalOperator(TSNode node) {
        if (!node.getType().equals("binary_expression") || node.getChildCount() < 3) return null;
        String op = node.getChild(1).getType();
        return op.equals("&&") || op.equals("||") ? op : null;
    }

    @Override
    List<Import> imports(TSNode root, Source source, String path) {
        return TypeScriptImports.of(root, source);
    }

    @Override
    public List<String> resolve(Import imported, String from, java.util.Set<String> files) {
        return TypeScriptImports.resolve(imported, from, files);
    }

    @Override
    int cognitive(TSNode function, String name, Source source) {
        Cognitive walk = new Cognitive(name, source);
        walk.node(field(function, "body"), 0);
        return walk.total;
    }

    /** The cognitive walk over a function's body, its argument the nesting level (E-030). */
    // Decision: E-030
    private final class Cognitive {
        private final String name;
        private final Source source;
        int total;

        Cognitive(String name, Source source) {
            this.name = name;
            this.source = source;
        }

        /** One node: what it adds, and how deep its parts are walked. */
        void node(TSNode node, int nesting) {
            if (node == null || ownUnit(node, source)) return;
            String type = node.getType();
            if (type.equals("if_statement")) anIf(node, nesting);
            else if (LOOPS.contains(type)) nested(node, nesting);
            else if (type.equals("switch_statement") || type.equals("catch_clause") || type.equals("ternary_expression")) deeper(node, nesting);
            else if (CALLABLES.contains(type)) children(node, nesting + 1);
            else if (logicalOperator(node) != null) logical(node, nesting);
            else if ((type.equals("break_statement") || type.equals("continue_statement")) && field(node, "label") != null) total += 1;
            else if (type.equals("call_expression")) call(node, nesting);
            else children(node, nesting);
        }

        void children(TSNode node, int nesting) {
            if (node == null) return;
            for (int i = 0; i < node.getChildCount(); i++) node(node.getChild(i), nesting);
        }

        /** if: +1 and its nesting; each else if and the else +1 flat; their blocks one level deeper. */
        private void anIf(TSNode node, int nesting) {
            total += 1 + nesting;
            node(field(node, "condition"), nesting);
            node(field(node, "consequence"), nesting + 1);
            TSNode otherwise = field(node, "alternative");
            while (otherwise != null) {
                total += 1;
                TSNode inner = otherwise.getNamedChildCount() == 1 ? otherwise.getNamedChild(0) : null;
                if (inner == null || !inner.getType().equals("if_statement")) {
                    children(otherwise, nesting + 1);
                    return;
                }
                node(field(inner, "condition"), nesting);
                node(field(inner, "consequence"), nesting + 1);
                otherwise = field(inner, "alternative");
            }
        }

        /** A loop: +1 and its nesting; its body one level deeper, its header at this level. */
        private void nested(TSNode node, int nesting) {
            total += 1 + nesting;
            for (int i = 0; i < node.getChildCount(); i++) node(node.getChild(i), "body".equals(node.getFieldNameForChild(i)) ? nesting + 1 : nesting);
        }

        /** switch, catch and ?:: +1 and its nesting, everything inside one level deeper. */
        private void deeper(TSNode node, int nesting) {
            total += 1 + nesting;
            children(node, nesting + 1);
        }

        private void call(TSNode node, int nesting) {
            TSNode callee = field(node, "function");
            if (callee != null && callee.getType().equals("identifier") && source.text(callee).equals(name)) total += 1;
            children(node, nesting);
        }

        private void logical(TSNode node, int nesting) {
            List<String> operators = new ArrayList<>();
            List<TSNode> operands = new ArrayList<>();
            flatten(node, BRACKETS, TypeScriptAdapter::logicalOperator, operators, operands);
            total += runs(operators);
            for (TSNode operand : operands) node(operand, nesting);
        }
    }
}
