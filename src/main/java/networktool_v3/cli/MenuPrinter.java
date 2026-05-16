package main.java.networktool_v3.cli;

public final class MenuPrinter {

    private MenuPrinter() {}

    private static final String[][] MENU_ITEMS = {
            {"1",  "Minimale Netzwerkinfo"},
            {"2",  "Vollständige Netzwerkinfo"},
            {"3",  "Diagnose & Analyse  (Schnell / Voll)"},
            {"4",  "File-Server starten"},
            {"5",  "Datei senden"},
            {"6",  "CIDR-Scan + Filter + JSON-Export"},
            {"7",  "Netzwerk-Scan mit OS- und Hostname-Filter"},
            {"8",  "Nachricht an IP"},
            {"9",  "Fremdnetz-Scanner"},
            {"10", "Scheduler"},
            {"11", "Sicherheitsmonitor (ARP + Port-Monitor)"},
            {"12", "Dauerping"},
            {"13", "Bandwidth-Test"},
            {"14", "Export & Import"},
            {"0",  "Beenden"},
    };

    public static void print() {
        System.out.println("\n=== Netzwerk Tool ===");
        for (String[] item : MENU_ITEMS)
            System.out.println(item[0] + " - " + item[1]);
        System.out.print("Auswahl: ");
    }
}