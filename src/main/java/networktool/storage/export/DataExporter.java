package main.java.networktool.storage.export;

import main.java.networktool.model.HostResult;

import java.io.IOException;
import java.nio.file.Path;

/** Export gespeicherter Hosts in CSV / JSON / HTML. */
public final class DataExporter {

    private DataExporter() {}

    public static Path exportCsv(Path outDir) throws IOException {
        return DataExportFormatters.exportCsv(outDir);
    }

    public static Path exportJson(Path outDir) throws IOException {
        return DataExportFormatters.exportJson(outDir);
    }

    public static Path exportHtml(Path outDir) throws IOException {
        return DataExportFormatters.exportHtml(outDir);
    }

    @FunctionalInterface
    interface HostConsumer { void accept(HostResult h, String category); }

    static void forEachHost(HostConsumer consumer) {
        DataExportFormatters.forEachHost(consumer::accept);
    }

    static String csv(String s) { return DataExportFormatters.csv(s); }

    static String esc(String s) { return DataExportFormatters.esc(s); }
}