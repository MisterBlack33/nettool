package main.java.networktool.logic.analysis.snmp;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Liest die Interface-Tabelle (IF-MIB) eines Switches als Portliste. */
public final class SnmpPortTable {

    private static final SnmpOid IF_DESCR = SnmpOid.parse("1.3.6.1.2.1.2.2.1.2");
    private static final SnmpOid IF_SPEED = SnmpOid.parse("1.3.6.1.2.1.2.2.1.5");
    private static final SnmpOid IF_OPER_STATUS = SnmpOid.parse("1.3.6.1.2.1.2.2.1.8");
    private static final String STATUS_UP = "1";
    private static final long BITS_PER_KBIT = 1_000L;
    private static final long BITS_PER_MBIT = 1_000_000L;

    /** Ein Port; {@code speedBps} ist 0, wenn der Agent keine Geschwindigkeit meldet. */
    public record Row(int index, String name, boolean up, long speedBps) {

        public String speedLabel() {
            if (speedBps <= 0) return "–";
            if (speedBps >= BITS_PER_MBIT) return speedBps / BITS_PER_MBIT + " Mbit/s";
            return speedBps / BITS_PER_KBIT + " kbit/s";
        }

        public String toLine() {
            return String.format("  %-4d %-28s %-5s %s", index, name, up ? "UP" : "DOWN", speedLabel());
        }
    }

    private SnmpPortTable() {}

    public static List<Row> read(SnmpWalker walker) {
        return build(walker.walk(IF_DESCR), walker.walk(IF_OPER_STATUS), walker.walk(IF_SPEED));
    }

    static List<Row> build(List<SnmpVarBind> descr, List<SnmpVarBind> status, List<SnmpVarBind> speed) {
        Map<Integer, String> statusByIndex = byIndex(status);
        Map<Integer, String> speedByIndex = byIndex(speed);
        List<Row> rows = new ArrayList<>();
        for (SnmpVarBind entry : descr) {
            int index = entry.oid().lastArc();
            rows.add(new Row(index, entry.value(),
                    STATUS_UP.equals(statusByIndex.get(index)), parseSpeed(speedByIndex.get(index))));
        }
        rows.sort(Comparator.comparingInt(Row::index));
        return List.copyOf(rows);
    }

    private static Map<Integer, String> byIndex(List<SnmpVarBind> entries) {
        Map<Integer, String> map = new HashMap<>();
        for (SnmpVarBind entry : entries) map.put(entry.oid().lastArc(), entry.value());
        return map;
    }

    private static long parseSpeed(String value) {
        if (value == null) return 0;
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
