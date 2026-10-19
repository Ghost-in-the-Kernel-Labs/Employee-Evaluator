package evaluator.analyze;

import java.util.List;

/**
 * One import as written: the module it names (`shop.money.Price`, `app.util`, `./view`), the names it
 * takes from it (Python's `from m import a, b`; empty otherwise), its line, and the names it binds that
 * the file never uses.
 */
public record Import(String module, List<String> names, int line, List<String> unused) {
    public Import {
        names = List.copyOf(names);
        unused = List.copyOf(unused);
    }
}
