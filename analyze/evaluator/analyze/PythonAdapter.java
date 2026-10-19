package evaluator.analyze;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.treesitter.TSLanguage;
import org.treesitter.TSNode;
import org.treesitter.TreeSitterPython;

/**
 * Python, read with tree-sitter. Every `def` is a function, a nested one included (a closure is
 * measured on its own, as a Java class inside a method is); a lambda counts in its function, one
 * level deeper. Decisions: if, elif, loops, except, `x if c else y`, and/or, a comprehension's for
 * and if, a match case that is not `case _`.
 */
public final class PythonAdapter extends TreeSitterAdapter {
    private static final Set<String> BRACKETS = Set.of("parenthesized_expression");

    @Override
    public boolean reads(String language) {
        return language.equals("Python");
    }

    @Override
    public String tool() {
        return "evaluator analyze python 1 (tree-sitter-python 0.25.0)";
    }

    @Override
    TSLanguage grammar(String path) {
        return new TreeSitterPython();
    }

    @Override
    String functionName(TSNode node, Source source) {
        return node.getType().equals("function_definition") ? source.text(field(node, "name")) : null;
    }

    @Override
    String className(TSNode node, Source source) {
        return node.getType().equals("class_definition") ? source.text(field(node, "name")) : null;
    }

    @Override
    int decisions(TSNode node, Source source) {
        return switch (node.getType()) {
            case "if_statement", "elif_clause", "for_statement", "while_statement", "except_clause", "conditional_expression",
                    "boolean_operator", "for_in_clause", "if_clause" -> 1;
            case "case_clause" -> wildcard(node) ? 0 : 1;
            default -> 0;
        };
    }

    /** `case _:`, the default of a match, which tree-sitter gives as an empty pattern. */
    private static boolean wildcard(TSNode caseClause) {
        TSNode pattern = caseClause.getNamedChildCount() > 0 ? caseClause.getNamedChild(0) : null;
        return pattern != null && pattern.getType().equals("case_pattern") && pattern.getNamedChildCount() == 0;
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
            if (ownUnit(node, source)) return;
            switch (node.getType()) {
                case "if_statement" -> anIf(node, nesting);
                case "for_statement", "while_statement" -> nested(node, nesting, "body");
                case "except_clause" -> {
                    total += 1 + nesting;
                    children(node, nesting + 1);
                }
                case "conditional_expression", "match_statement" -> {
                    total += 1 + nesting;
                    children(node, nesting + 1);
                }
                case "if_clause" -> {
                    total += 1;
                    children(node, nesting);
                }
                case "lambda" -> children(node, nesting + 1);
                case "boolean_operator" -> logical(node, nesting);
                case "call" -> {
                    TSNode callee = field(node, "function");
                    if (callee != null && callee.getType().equals("identifier") && source.text(callee).equals(name)) total += 1;
                    children(node, nesting);
                }
                default -> children(node, nesting);
            }
        }

        void children(TSNode node, int nesting) {
            for (int i = 0; i < node.getChildCount(); i++) node(node.getChild(i), nesting);
        }

        /** if: +1 and its nesting; each elif and the else +1 flat; their blocks one level deeper. */
        private void anIf(TSNode node, int nesting) {
            total += 1 + nesting;
            node(field(node, "condition"), nesting);
            children(field(node, "consequence"), nesting + 1);
            for (int i = 0; i < node.getNamedChildCount(); i++) {
                TSNode part = node.getNamedChild(i);
                if (part.getType().equals("elif_clause")) {
                    total += 1;
                    node(field(part, "condition"), nesting);
                    children(field(part, "consequence"), nesting + 1);
                } else if (part.getType().equals("else_clause")) {
                    total += 1;
                    children(part, nesting + 1);
                }
            }
        }

        /** A loop: +1 and its nesting; its body and any else one level deeper, its header at this level. */
        private void nested(TSNode node, int nesting, String body) {
            total += 1 + nesting;
            for (int i = 0; i < node.getChildCount(); i++) {
                TSNode part = node.getChild(i);
                boolean inner = body.equals(node.getFieldNameForChild(i)) || part.getType().equals("else_clause");
                node(part, inner ? nesting + 1 : nesting);
            }
        }

        private void logical(TSNode node, int nesting) {
            List<String> operators = new ArrayList<>();
            List<TSNode> operands = new ArrayList<>();
            flatten(node, BRACKETS, n -> n.getType().equals("boolean_operator") ? n.getChild(1).getType() : null, operators, operands);
            total += runs(operators);
            for (TSNode operand : operands) node(operand, nesting);
        }
    }
}
