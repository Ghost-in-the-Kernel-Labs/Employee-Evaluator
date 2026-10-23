package evaluator.analyze;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/** Strongly connected parts and a dependency order, both without recursion. */
final class Tarjan {
    private Tarjan() {}

    /** Each node's part: nodes that reach each other share one. */
    static Map<String, Integer> parts(Map<String, Set<String>> edges) {
        Map<String, Set<String>> graph = new TreeMap<>();
        edges.forEach((from, to) -> {
            graph.computeIfAbsent(from, k -> new TreeSet<>()).addAll(to);
            for (String t : to) graph.computeIfAbsent(t, k -> new TreeSet<>());
        });
        Map<String, Integer> index = new HashMap<>(), low = new HashMap<>(), part = new HashMap<>();
        Deque<String> stack = new ArrayDeque<>();
        Set<String> onStack = new java.util.HashSet<>();
        int[] counter = {0, 0};
        for (String root : graph.keySet()) if (!index.containsKey(root)) visit(root, graph, index, low, part, stack, onStack, counter);
        return part;
    }

    /** Tarjan's walk from one root, its recursion kept on an explicit stack of iterators. */
    private static void visit(String root, Map<String, Set<String>> graph, Map<String, Integer> index, Map<String, Integer> low,
                              Map<String, Integer> part, Deque<String> stack, Set<String> onStack, int[] counter) {
        Deque<Map.Entry<String, java.util.Iterator<String>>> work = new ArrayDeque<>();
        enter(root, graph, index, low, stack, onStack, counter, work);
        while (!work.isEmpty()) {
            var top = work.peek();
            String node = top.getKey();
            if (top.getValue().hasNext()) {
                String next = top.getValue().next();
                if (!index.containsKey(next)) enter(next, graph, index, low, stack, onStack, counter, work);
                else if (onStack.contains(next)) low.put(node, Math.min(low.get(node), index.get(next)));
                continue;
            }
            work.pop();
            if (!work.isEmpty()) low.put(work.peek().getKey(), Math.min(low.get(work.peek().getKey()), low.get(node)));
            if (low.get(node).equals(index.get(node))) {
                String member;
                do {
                    member = stack.pop();
                    onStack.remove(member);
                    part.put(member, counter[1]);
                } while (!member.equals(node));
                counter[1]++;
            }
        }
    }

    private static void enter(String node, Map<String, Set<String>> graph, Map<String, Integer> index, Map<String, Integer> low,
                              Deque<String> stack, Set<String> onStack, int[] counter, Deque<Map.Entry<String, java.util.Iterator<String>>> work) {
        index.put(node, counter[0]);
        low.put(node, counter[0]++);
        stack.push(node);
        onStack.add(node);
        work.push(Map.entry(node, graph.get(node).iterator()));
    }

    /** The parts in an order where every part comes after all it points to (they form no cycle). */
    static List<Integer> order(Map<Integer, Set<Integer>> next) {
        List<Integer> done = new ArrayList<>();
        Set<Integer> seen = new java.util.HashSet<>();
        for (int start : next.keySet()) {
            if (!seen.add(start)) continue;
            Deque<Map.Entry<Integer, java.util.Iterator<Integer>>> work = new ArrayDeque<>();
            work.push(Map.entry(start, next.get(start).iterator()));
            while (!work.isEmpty()) {
                var top = work.peek();
                if (top.getValue().hasNext()) {
                    int n = top.getValue().next();
                    if (seen.add(n)) work.push(Map.entry(n, next.get(n).iterator()));
                } else {
                    done.add(work.pop().getKey());
                }
            }
        }
        return done;
    }
}
