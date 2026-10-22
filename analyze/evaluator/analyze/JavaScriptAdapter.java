package evaluator.analyze;

import java.util.List;
import java.util.Set;
import org.treesitter.TSLanguage;
import org.treesitter.TSNode;
import org.treesitter.TreeSitterTsx;

/**
 * JavaScript (`.js`, `.mjs`, `.cjs`, `.jsx`), measured as TypeScript is and read with the vendored TSX
 * grammar (E-058): JavaScript is nearly TypeScript without its types, and TSX also takes JSX, which plain
 * `.js` files often hold. Its imports are `import` as TypeScript's and `require(...)` besides.
 */
// Decision: E-058
public final class JavaScriptAdapter extends TypeScriptAdapter {
    /** The endings a relative JavaScript import may leave off, and the index files a folder import means. */
    static final List<String> ENDINGS = List.of("", ".js", ".mjs", ".cjs", ".jsx", "/index.js", "/index.mjs", "/index.cjs", "/index.jsx");

    @Override
    public boolean reads(String language) {
        return language.equals("JavaScript");
    }

    @Override
    public String tool() {
        return "evaluator analyze javascript 1 (tree-sitter-typescript 0.23.2, TSX grammar)";
    }

    @Override
    TSLanguage grammar(String path) {
        return new TreeSitterTsx();
    }

    @Override
    List<Import> imports(TSNode root, Source source, String path) {
        return JavaScriptImports.of(root, source);
    }

    @Override
    public List<String> resolve(Import imported, String from, Set<String> files) {
        return TypeScriptImports.resolve(imported, from, files, ENDINGS);
    }
}
