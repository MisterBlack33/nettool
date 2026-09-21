package main.java.networktool.logic.analysis.snmp;

import main.java.networktool.logging.DebugLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Durchläuft einen MIB-Teilbaum per wiederholtem GETNEXT. */
public final class SnmpWalker {

    static final int MAX_ENTRIES = 2000;
    private static final int FIRST_REQUEST_ID = 1;

    private final SnmpTransport transport;
    private final String community;

    public SnmpWalker(SnmpTransport transport, String community) {
        this.transport = transport;
        this.community = community;
    }

    public static SnmpWalker forHost(String host, String community) {
        return new SnmpWalker(new UdpSnmpTransport(host), community);
    }

    public List<SnmpVarBind> walk(SnmpOid root) {
        List<SnmpVarBind> entries = new ArrayList<>();
        SnmpOid current = root;
        int requestId = FIRST_REQUEST_ID;
        while (entries.size() < MAX_ENTRIES) {
            Optional<SnmpVarBind> next = fetchNext(current, requestId++);
            if (next.isEmpty() || !continuesWalk(next.get(), root, current)) break;
            entries.add(next.get());
            current = next.get().oid();
        }
        return List.copyOf(entries);
    }

    /** Verlässt die Schleife bei Subtree-Ende oder nicht aufsteigenden OIDs (Agent-Fehler). */
    private static boolean continuesWalk(SnmpVarBind next, SnmpOid root, SnmpOid current) {
        return next.oid().startsWith(root) && next.oid().compareTo(current) > 0;
    }

    private Optional<SnmpVarBind> fetchNext(SnmpOid oid, int requestId) {
        Optional<byte[]> response = transport.exchange(SnmpMessages.getNextRequest(community, requestId, oid));
        if (response.isEmpty()) return Optional.empty();
        try {
            return SnmpMessages.parseResponse(response.get(), requestId);
        } catch (SnmpProtocolException e) {
            DebugLogger.getInstance().log("FINE", "[SnmpWalker] Antwort verworfen: " + e.getMessage());
            return Optional.empty();
        }
    }
}
