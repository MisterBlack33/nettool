package main.java.networktool.logic.analysis;

/**
 * Loggt einzelne OS-Erkennungsschritte mit Erfolg/Fehlstatus.
 */
final class OsDetectionLogger {

    private OsDetectionLogger() {}

    static OsSignature tryStep(String method, OsSignature sig) {
        if (sig != null) {
            System.out.printf("  [OK]   %-20s → %s (Score: %d)%n", method, sig.os, sig.score);
        } else {
            System.out.printf("  [FAIL] %-20s%n", method);
        }
        return sig;
    }
}