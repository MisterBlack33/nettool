package main.java.networktool.storage.network;

/**
 * Migrationspfad für das Host-JSON-Schema (siehe {@link HostJsonBuilder#CURRENT_SCHEMA_VERSION}).
 *
 * Regel: Migrationen sind ausschließlich additiv. Vorhandene Felder werden nie entfernt
 * oder umbenannt; neue Versionen fügen nur neue, optionale Felder hinzu. Fehlende Felder
 * beim Parsen (siehe {@link HostJsonBuilder#parseHost}) werden bereits mit sinnvollen
 * Defaults behandelt, wodurch v0-Altbestand ohne Datenverlust weiterverwendet werden kann.
 */
final class HostSchemaMigration {

    private HostSchemaMigration() {}

    /** Ziel-Version der additiven Beispielmigration {@link #migrateHostV1ToV2}. */
    static final int SCHEMA_V2 = 2;

    static void logIfLegacy(String networkName, int fileVersion) {
        if (fileVersion < HostJsonBuilder.CURRENT_SCHEMA_VERSION) {
            System.out.println("[HostSchemaMigration] \"" + networkName + "\": v" + fileVersion
                    + " -> v" + HostJsonBuilder.CURRENT_SCHEMA_VERSION
                    + " (additiv, keine Datenänderung nötig)");
        }
    }

    /**
     * Beispiel-Migration v1 → v2: ergänzt additiv das optionale Feld
     * "lastSeenAt" in einem einzelnen Host-JSON-Objekt, falls es fehlt.
     * Bestehende Felder und deren Werte bleiben unverändert. Idempotent.
     */
    static String migrateHostV1ToV2(String hostJson) {
        if (hostJson == null || hostJson.contains("\"lastSeenAt\"")) return hostJson;
        int lastBrace = hostJson.lastIndexOf('}');
        if (lastBrace < 0) return hostJson;
        String head = hostJson.substring(0, lastBrace).stripTrailing();
        String sep  = head.endsWith(",") || head.endsWith("{") ? "" : ",";
        return head + sep + "\n      \"lastSeenAt\": \"\"\n    }";
    }
}