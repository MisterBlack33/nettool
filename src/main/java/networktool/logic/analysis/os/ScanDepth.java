package main.java.networktool.logic.analysis.os;

/**
 * Steuert wie gründlich {@link OsDetectionPipeline} nach einem OS-Treffer sucht.
 * Höhere Stufe = höhere Score-Schwellen = spätere/keine Früh-Abbrüche = mehr Präzision.
 */
public enum ScanDepth {
    SCHNELL,
    STANDARD,
    GRUENDLICH
}