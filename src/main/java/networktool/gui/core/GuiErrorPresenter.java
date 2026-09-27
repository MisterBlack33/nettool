package main.java.networktool.gui.core;

import main.java.networktool.logic.scan.host.ScanErrorClassifier;

/** Einheitliche, nutzerverständliche Fehlermeldungen für GUI-Statuszeilen. Keine Stacktraces/Secrets. */
final class GuiErrorPresenter {

    private GuiErrorPresenter() {}

    static String userMessage(Throwable e) {
        return switch (ScanErrorClassifier.classify(e)) {
            case TIMEOUT      -> "Zeitüberschreitung – Ziel antwortet nicht.";
            case DNS          -> "Hostname konnte nicht aufgelöst werden.";
            case UNREACHABLE  -> "Ziel nicht erreichbar (Verbindung abgelehnt).";
            case PERMISSION   -> "Keine Berechtigung für diese Aktion.";
            case UNKNOWN      -> "Fehler: " + safeMessage(e);
        };
    }

    private static String safeMessage(Throwable e) {
        if (e == null) return "Unbekannter Fehler";
        String message = e.getMessage();
        return (message == null || message.isBlank()) ? e.getClass().getSimpleName() : message;
    }
}
