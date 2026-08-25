package main.java.networktool.logic.analysis.os;

import java.util.function.Supplier;

/** Führt einen Erkennungsschritt fehlertolerant aus – Exceptions brechen die Pipeline nie ab. */
final class OsDetectionStepRunner {

    private OsDetectionStepRunner() {}

    static OsSignature safeCall(String name, Supplier<OsSignature> step) {
        try {
            return OsDetectionLogger.tryStep(name, step.get());
        } catch (Exception e) {
            OsDetectionLogger.tryStep(name + " [FEHLER: " + e.getClass().getSimpleName() + "]", null);
            return null;
        }
    }
}