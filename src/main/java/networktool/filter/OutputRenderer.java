package main.java.networktool.filter;

import main.java.networktool.model.HostResult;
import main.java.networktool.model.ScanResult;

import java.util.List;

/**
 * Abstraktion für die GUI-Ausgabe von Scan-/Host-Tabellen.
 * Löst die Compile-Abhängigkeit von {@code filter} auf {@code gui.core.GUI} auf.
 * Implementiert von {@code GUI}, registriert über {@link OutputRendererRegistry}.
 */
public interface OutputRenderer {

    boolean isActive();

    void showHostTable(List<HostResult> rows, String title);

    void showScanTable(List<ScanResult> rows);
}