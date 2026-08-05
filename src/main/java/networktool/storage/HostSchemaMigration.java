package main.java.networktool.storage;

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

    static void logIfLegacy(String networkName, int fileVersion) {
        if (fileVersion < HostJsonBuilder.CURRENT_SCHEMA_VERSION) {
            System.out.println("[HostSchemaMigration] \"" + networkName + "\": v" + fileVersion
                    + " -> v" + HostJsonBuilder.CURRENT_SCHEMA_VERSION
                    + " (additiv, keine Datenänderung nötig)");
        }
    }
}
