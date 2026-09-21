package main.java.networktool.logic.analysis.snmp;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/** Objekt-Identifier in Punktnotation, z.B. {@code 1.3.6.1.2.1.2.2.1.2}. */
public record SnmpOid(List<Integer> arcs) implements Comparable<SnmpOid> {

    private static final int MIN_ARCS = 2;
    private static final int MAX_FIRST_ARC = 2;

    public SnmpOid {
        arcs = List.copyOf(arcs);
        if (arcs.size() < MIN_ARCS) throw new IllegalArgumentException("OID braucht mindestens 2 Arcs");
        if (arcs.stream().anyMatch(a -> a < 0)) throw new IllegalArgumentException("Negativer Arc in OID");
        if (arcs.getFirst() > MAX_FIRST_ARC) throw new IllegalArgumentException("Erster Arc muss 0-2 sein");
    }

    public static SnmpOid parse(String text) {
        if (text == null || text.isBlank()) throw new IllegalArgumentException("OID fehlt");
        String trimmed = text.trim();
        String body = trimmed.startsWith(".") ? trimmed.substring(1) : trimmed;
        List<Integer> arcs = new ArrayList<>();
        for (String part : body.split("\\.", -1)) arcs.add(parseArc(part, text));
        return new SnmpOid(arcs);
    }

    private static int parseArc(String part, String source) {
        try {
            return Integer.parseInt(part);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Ungültige OID: " + source, e);
        }
    }

    public boolean startsWith(SnmpOid prefix) {
        return arcs.size() >= prefix.arcs.size()
                && arcs.subList(0, prefix.arcs.size()).equals(prefix.arcs);
    }

    public int lastArc() {
        return arcs.getLast();
    }

    public SnmpOid child(int arc) {
        List<Integer> extended = new ArrayList<>(arcs);
        extended.add(arc);
        return new SnmpOid(extended);
    }

    @Override
    public int compareTo(SnmpOid other) {
        int shared = Math.min(arcs.size(), other.arcs.size());
        for (int i = 0; i < shared; i++) {
            int cmp = Integer.compare(arcs.get(i), other.arcs.get(i));
            if (cmp != 0) return cmp;
        }
        return Integer.compare(arcs.size(), other.arcs.size());
    }

    @Override
    public String toString() {
        return arcs.stream().map(String::valueOf).collect(Collectors.joining("."));
    }
}
