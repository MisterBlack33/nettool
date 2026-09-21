package main.java.networktool.storage.export;

import main.java.networktool.model.HostResult;

import java.util.ArrayList;
import java.util.List;

/** Wandelt gespeicherte Hosts in druckbare Textzeilen für {@link PdfReportBuilder}. */
public final class PdfHostLines {

    private static final int MAX_LINE_LENGTH = 95;

    private PdfHostLines() {}

    public static List<String> from(List<HostResult> hosts, String title) {
        List<String> lines = new ArrayList<>();
        lines.add(title);
        lines.add("Hosts: " + hosts.size());
        lines.add("");
        for (HostResult h : hosts) lines.add(clip(describe(h)));
        return lines;
    }

    private static String describe(HostResult h) {
        String ports = h.portsToString();
        return h.ip + "  " + nullToEmpty(h.hostname) + "  " + nullToEmpty(h.os)
                + (ports.isEmpty() ? "" : "  " + ports);
    }

    private static String clip(String s) {
        return s.length() <= MAX_LINE_LENGTH ? s : s.substring(0, MAX_LINE_LENGTH - 1) + "…";
    }

    private static String nullToEmpty(String s) { return s == null ? "" : s; }
}
