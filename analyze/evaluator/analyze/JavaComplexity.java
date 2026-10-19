package evaluator.analyze;

import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.BreakTree;
import com.sun.source.tree.CaseTree;
import com.sun.source.tree.CatchTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ConditionalExpressionTree;
import com.sun.source.tree.ContinueTree;
import com.sun.source.tree.DefaultCaseLabelTree;
import com.sun.source.tree.DoWhileLoopTree;
import com.sun.source.tree.EnhancedForLoopTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.ForLoopTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.IfTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ParenthesizedTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.SwitchExpressionTree;
import com.sun.source.tree.SwitchTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.WhileLoopTree;
import com.sun.source.util.TreeScanner;
import java.util.ArrayList;
import java.util.List;

/** A Java method's two complexities, from its parse tree. A class declared inside the method is not counted: its methods are functions of their own. */
final class JavaComplexity {
    private JavaComplexity() {}

    /** Decision points plus one: if, loops, a case that is not default, catch, ?:, && and ||. */
    static int cyclomatic(MethodTree method) {
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
                return null;
            }
        }.scan(method.getBody(), null);
        return count[0];
    }

    /** SonarSource's cognitive complexity (G. Ann Campbell, 2017), counted over the method's body. */
    static int cognitive(MethodTree method) {
        Cognitive counter = new Cognitive(method.getName().toString());
        counter.scan(method.getBody(), 0);
        return counter.total;
    }

    /**
     * The walk, its argument the nesting level. +1 for each break in linear flow (if, else if, else,
     * ?:, switch, loops, catch, a labelled break or continue, each run of like && or || operators, a
     * call to the method itself), plus the nesting level for those that nest (if, ?:, switch, loops,
     * catch). Their bodies, and a lambda's, are one level deeper.
     */
    // Decision: E-030
    private static final class Cognitive extends TreeScanner<Void, Integer> {
        private final String name;
        int total;

        Cognitive(String name) {
            this.name = name;
        }

        /** An if and its chain: +1 and its nesting for the if, +1 flat for each else if and the else. */
        @Override
        public Void visitIf(IfTree tree, Integer nesting) {
            total += 1 + nesting;
            scan(tree.getCondition(), nesting);
            scan(tree.getThenStatement(), nesting + 1);
            StatementTree otherwise = tree.getElseStatement();
            while (otherwise instanceof IfTree elseIf) {
                total += 1;
                scan(elseIf.getCondition(), nesting);
                scan(elseIf.getThenStatement(), nesting + 1);
                otherwise = elseIf.getElseStatement();
            }
            if (otherwise != null) {
                total += 1;
                scan(otherwise, nesting + 1);
            }
            return null;
        }

        @Override
        public Void visitConditionalExpression(ConditionalExpressionTree tree, Integer nesting) {
            total += 1 + nesting;
            scan(tree.getCondition(), nesting);
            scan(tree.getTrueExpression(), nesting + 1);
            scan(tree.getFalseExpression(), nesting + 1);
            return null;
        }

        @Override
        public Void visitSwitch(SwitchTree tree, Integer nesting) {
            total += 1 + nesting;
            scan(tree.getExpression(), nesting);
            scan(tree.getCases(), nesting + 1);
            return null;
        }

        @Override
        public Void visitSwitchExpression(SwitchExpressionTree tree, Integer nesting) {
            total += 1 + nesting;
            scan(tree.getExpression(), nesting);
            scan(tree.getCases(), nesting + 1);
            return null;
        }

        @Override
        public Void visitForLoop(ForLoopTree tree, Integer nesting) {
            total += 1 + nesting;
            scan(tree.getInitializer(), nesting);
            scan(tree.getCondition(), nesting);
            scan(tree.getUpdate(), nesting);
            scan(tree.getStatement(), nesting + 1);
            return null;
        }

        @Override
        public Void visitEnhancedForLoop(EnhancedForLoopTree tree, Integer nesting) {
            total += 1 + nesting;
            scan(tree.getExpression(), nesting);
            scan(tree.getStatement(), nesting + 1);
            return null;
        }

        @Override
        public Void visitWhileLoop(WhileLoopTree tree, Integer nesting) {
            total += 1 + nesting;
            scan(tree.getCondition(), nesting);
            scan(tree.getStatement(), nesting + 1);
            return null;
        }

        @Override
        public Void visitDoWhileLoop(DoWhileLoopTree tree, Integer nesting) {
            total += 1 + nesting;
            scan(tree.getStatement(), nesting + 1);
            scan(tree.getCondition(), nesting);
            return null;
        }

        @Override
        public Void visitCatch(CatchTree tree, Integer nesting) {
            total += 1 + nesting;
            scan(tree.getBlock(), nesting + 1);
            return null;
        }

        @Override
        public Void visitBreak(BreakTree tree, Integer nesting) {
            if (tree.getLabel() != null) total += 1;
            return null;
        }

        @Override
        public Void visitContinue(ContinueTree tree, Integer nesting) {
            if (tree.getLabel() != null) total += 1;
            return null;
        }

        @Override
        public Void visitLambdaExpression(LambdaExpressionTree tree, Integer nesting) {
            scan(tree.getBody(), nesting + 1);
            return null;
        }

        @Override
        public Void visitClass(ClassTree tree, Integer nesting) {
            return null;
        }

        // Unsure: a call is recursive when its simple name is the method's; an overload of the same name counts too.
        @Override
        public Void visitMethodInvocation(MethodInvocationTree tree, Integer nesting) {
            if (tree.getMethodSelect() instanceof IdentifierTree id && id.getName().contentEquals(name)) total += 1;
            return super.visitMethodInvocation(tree, nesting);
        }

        /** A run of && or || counts once per change of operator, read left to right; its operands are walked as usual. */
        @Override
        public Void visitBinary(BinaryTree tree, Integer nesting) {
            if (!logical(tree)) return super.visitBinary(tree, nesting);
            List<Tree.Kind> operators = new ArrayList<>();
            List<ExpressionTree> operands = new ArrayList<>();
            flatten(tree, operators, operands);
            for (int i = 0; i < operators.size(); i++) if (i == 0 || operators.get(i) != operators.get(i - 1)) total += 1;
            for (ExpressionTree operand : operands) scan(operand, nesting);
            return null;
        }

        private static boolean logical(Tree tree) {
            return tree.getKind() == Tree.Kind.CONDITIONAL_AND || tree.getKind() == Tree.Kind.CONDITIONAL_OR;
        }

        /** A logical expression as its operators and operands in source order, brackets looked through. */
        private static void flatten(ExpressionTree tree, List<Tree.Kind> operators, List<ExpressionTree> operands) {
            ExpressionTree bare = tree;
            while (bare instanceof ParenthesizedTree p) bare = p.getExpression();
            if (bare instanceof BinaryTree b && logical(b)) {
                flatten(b.getLeftOperand(), operators, operands);
                operators.add(b.getKind());
                flatten(b.getRightOperand(), operators, operands);
            } else {
                operands.add(tree);
            }
        }
    }
}
