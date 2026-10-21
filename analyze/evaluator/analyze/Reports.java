package evaluator.analyze;

import evaluator.rubric.Json;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * The reports a submission's test run writes, read as an attacker's files (E-037): JUnit XML (pytest,
 * Jest and Vitest's reporters, Maven's Surefire) for what passed, and Cobertura (coverage.py, c8) or
 * JaCoCo XML for line coverage, and Jest's JSON report. No DTD is loaded, no entity declared in the file is accepted, and a
 * report over the size cap is refused, so a report can neither read our files nor exhaust our memory.
 */
// Decision: E-037
public final class Reports {
    /** The most a report may hold; a larger one is refused, not read in part. */
    static final int MAX_BYTES = 20 * 1024 * 1024;
    /**
     * What a test's failure says when it reached for a network the sandbox does not have: a name that
     * would not resolve, or a network that is not there. A refused local connection is not one of them.
     */
    static final List<String> NETWORK_MARKS = List.of("temporary failure in name resolution", "name or service not known",
            "nodename nor servname", "getaddrinfo enotfound", "getaddrinfo eai_again", "unknownhostexception",
            "network is unreachable", "enetunreach", "could not resolve host", "failed to resolve");

    /** What the tests did: how many in each outcome, and the tests that reached for the network. */
    public record Outcome(int total, int passed, int failed, int skipped, List<String> networkAttempts) {
        public Outcome {
            networkAttempts = List.copyOf(networkAttempts);
        }
    }

    /** A report that could not be read: too large, not XML, or declaring what a report never needs. */
    public static final class Refused extends Exception {
        Refused(String why) {
            super(why);
        }
    }

    private Reports() {}

    /** Every test case of these JUnit XML reports, summed: failures and errors both count as failed. */
    public static Outcome junit(List<String> reports) throws Refused {
        int total = 0, passed = 0, failed = 0, skipped = 0;
        List<String> network = new ArrayList<>();
        for (String report : reports) {
            NodeList cases = parse(report).getElementsByTagName("testcase");
            for (int i = 0; i < cases.getLength(); i++) {
                Element test = (Element) cases.item(i);
                total++;
                String trouble = text(test, "failure") + text(test, "error");
                if (!trouble.isEmpty()) {
                    failed++;
                    if (reachedNetwork(trouble)) network.add(name(test));
                } else if (test.getElementsByTagName("skipped").getLength() > 0) {
                    skipped++;
                } else {
                    passed++;
                }
            }
        }
        return new Outcome(total, passed, failed, skipped, network);
    }

    /** Lines run and lines that could run, so coverage from several reports can be summed. */
    public record Lines(long covered, long valid) {}

    /** Line coverage from a Cobertura report (`<coverage>`, coverage.py and c8) or a JaCoCo one (`<report>`, its own LINE counter); empty when it counts nothing. */
    public static Optional<Lines> coverage(String report) throws Refused {
        Element root = parse(report).getDocumentElement();
        if (root.getTagName().equals("coverage")) {
            long valid = number(root.getAttribute("lines-valid")), covered = number(root.getAttribute("lines-covered"));
            return valid > 0 ? Optional.of(new Lines(covered, valid)) : Optional.empty();
        }
        if (root.getTagName().equals("report")) {
            for (Node n = root.getFirstChild(); n != null; n = n.getNextSibling()) {
                if (n instanceof Element counter && counter.getTagName().equals("counter") && counter.getAttribute("type").equals("LINE")) {
                    long missed = number(counter.getAttribute("missed")), covered = number(counter.getAttribute("covered"));
                    return missed + covered == 0 ? Optional.empty() : Optional.of(new Lines(covered, missed + covered));
                }
            }
        }
        return Optional.empty();
    }

    /** Jest's `--json` report: each assertion's status, a failed one's messages read for the network. */
    @SuppressWarnings("unchecked")
    public static Outcome jest(String report) throws Refused {
        if (report.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES) throw new Refused("over " + MAX_BYTES + " bytes");
        Object root;
        try {
            root = Json.parse(report);
        } catch (IllegalArgumentException e) {
            throw new Refused("not a report: " + e.getMessage());
        }
        int total = 0, passed = 0, failed = 0, skipped = 0;
        List<String> network = new ArrayList<>();
        for (Object file : list(root instanceof java.util.Map<?, ?> m ? m.get("testResults") : null)) {
            for (Object a : list(file instanceof java.util.Map<?, ?> f ? f.get("assertionResults") : null)) {
                if (!(a instanceof java.util.Map<?, ?> test)) continue;
                total++;
                switch (String.valueOf(test.get("status"))) {
                    case "passed" -> passed++;
                    case "failed" -> {
                        failed++;
                        if (reachedNetwork(String.valueOf(test.get("failureMessages")))) network.add(String.valueOf(test.get("fullName")));
                    }
                    default -> skipped++;
                }
            }
        }
        return new Outcome(total, passed, failed, skipped, network);
    }

    private static List<?> list(Object value) {
        return value instanceof List<?> l ? l : List.of();
    }

    /** Whether a failure's text is the sandbox's missing network, not the code's own fault. */
    static boolean reachedNetwork(String failure) {
        String lower = failure.toLowerCase(Locale.ROOT);
        for (String mark : NETWORK_MARKS) if (lower.contains(mark)) return true;
        return false;
    }

    /**
     * A report as a document, with every door an XML file can open shut: no external DTD or entity,
     * no XInclude, the JDK's secure processing limits, and any file that declares an entity refused.
     */
    static Document parse(String report) throws Refused {
        if (report.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES) throw new Refused("over " + MAX_BYTES + " bytes");
        if (report.contains("<!ENTITY")) throw new Refused("declares an entity, which no test report needs");
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            builder.setErrorHandler(null);
            return builder.parse(new ByteArrayInputStream(report.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new Refused("not a report: " + e.getMessage());
        }
    }

    /** The message and text of a test case's children of one tag, joined; "" when it has none. */
    private static String text(Element test, String tag) {
        NodeList found = test.getElementsByTagName(tag);
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < found.getLength(); i++) {
            Element e = (Element) found.item(i);
            text.append(e.getAttribute("message")).append(' ').append(e.getAttribute("type")).append(' ').append(e.getTextContent());
        }
        return text.toString().strip();
    }

    private static String name(Element test) {
        String cls = test.getAttribute("classname");
        return (cls.isEmpty() ? "" : cls + ".") + test.getAttribute("name");
    }

    private static long number(String text) {
        try {
            return text.isEmpty() ? 0 : Long.parseLong(text.strip());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
