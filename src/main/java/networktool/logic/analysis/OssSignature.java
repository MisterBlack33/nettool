// src/main/java/networktool/logic/analysis/OsSignature.java
package main.java.networktool.logic.analysis;

/**
 * Gewichtetes OS-Erkennungsergebnis.
 * Mehrere Methoden können Hinweise liefern; der höchste Score gewinnt.
 */
final class OsSignature {

    final String os;
    final int    score;  // höher = zuverlässiger
    final String method;

    OsSignature(String os, int score, String method) {
        this.os = os; this.score = score; this.method = method;
    }

    static OsSignature of(String os, int score, String method) {
        return new OsSignature(os, score, method);
    }

    /** Gibt das Ergebnis mit dem höheren Score zurück. */
    static OsSignature best(OsSignature a, OsSignature b) {
        if (a == null) return b;
        if (b == null) return a;
        return a.score >= b.score ? a : b;
    }

    OsDetector.Confidence toConfidence() {
        if (score >= 80) return OsDetector.Confidence.HOCH;
        if (score >= 40) return OsDetector.Confidence.MITTEL;
        return OsDetector.Confidence.NIEDRIG;
    }
}