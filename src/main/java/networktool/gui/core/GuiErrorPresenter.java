package main.java.networktool.gui.core;

import main.java.networktool.logic.scan.host.ScanErrorClassifier;

/** Einheitliche, nutzerverständliche Fehlermeldungen für GUI-Statuszeilen. Keine Stacktraces/Secrets. */
final class GuiErrorPresenter {

    private GuiErrorPresenter() {}

    static String userMessage(Throwable e) {
        return switch (ScanErrorClassifier.classify(e)) {
            case TIMEOUT      -> "Zeitüberschreitung – Ziel antwortet nicht.";
            case HOST_OFFLINE -> "Ziel ist offline oder nicht erreichbar.";
            case DNS          -> "Hostname konnte nicht aufgelöst werden.";
            case CONNECTION_RESET -> "Verbindung zum Ziel wurde unterbrochen.";
            case CONNECTION_REFUSED, UNREACHABLE -> "Ziel nicht erreichbar (Verbindung abgelehnt).";
            case PERMISSION   -> "Keine Berechtigung für diese Aktion.";
            case UNKNOWN      -> "Ein unerwarteter Fehler ist aufgetreten.";
        };
    }
}
