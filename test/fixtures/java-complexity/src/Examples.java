package examples;

import java.util.List;

public class Examples {
    Examples() {
        int i = 0;
        do {
            i++;
        } while (i < 3);
    }

    int sumOfPrimes(int max) {
        int total = 0;
        OUT:
        for (int i = 1; i <= max; ++i) {
            for (int j = 2; j < i; ++j) {
                if (i % j == 0) {
                    continue OUT;
                }
            }
            total += i;
        }
        return total;
    }

    String getWords(int number) {
        switch (number) {
            case 1:
                return "one";
            case 2:
                return "a couple";
            case 3:
                return "a few";
            default:
                return "lots";
        }
    }

    String grade(int score) {
        if (score > 90) {
            return "A";
        } else if (score > 80) {
            return "B";
        } else {
            return "C";
        }
    }

    boolean logic(boolean a, boolean b, boolean c, boolean d) {
        return a && b && c || d && !(a || b);
    }

    int nested(List<Integer> xs) {
        int n = 0;
        for (int x : xs) {
            if (x > 0) {
                n += x > 10 ? 2 : 1;
            }
        }
        return n;
    }

    void tryIt() {
        try {
            run();
        } catch (IllegalStateException e) {
            if (e.getMessage() == null) {
                return;
            }
        }
    }

    void run() {}

    void run(int times) {}

    Runnable later(int x) {
        return () -> {
            if (x > 0) {
                System.out.println(x);
            }
        };
    }

    int fact(int n) {
        return n <= 1 ? 1 : n * fact(n - 1);
    }

    int kind(Object o) {
        return switch (o) {
            case Integer i -> 1;
            case String s -> 2;
            default -> 0;
        };
    }

    Runnable anon() {
        return new Runnable() {
            @Override
            public void run() {
                if (true) {}
            }
        };
    }

    static class Inner {
        void go(boolean a) {
            if (a) {
                return;
            }
        }
    }
}
