package main.java.networktool.storage.export;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Erzeugt ein mehrseitiges PDF ohne externe Bibliotheken.
 * Analog {@link HtmlReportBuilder}, aber Byte-basiert statt String.
 */
public final class PdfReportBuilder {

    static final int MAX_LINES_PER_PAGE = 50;

    private PdfReportBuilder() {}

    public static Path save(Path outDir, List<String> lines) throws IOException {
        return ExportFiles.writeUnique(outDir, "pdf", build(lines));
    }

    public static byte[] build(List<String> lines) {
        return PdfDocument.assemble(paginate(lines));
    }

    /** Liefert immer mindestens eine (ggf. leere) Seite. */
    static List<List<String>> paginate(List<String> lines) {
        List<List<String>> pages = new ArrayList<>();
        for (int from = 0; from < lines.size(); from += MAX_LINES_PER_PAGE) {
            pages.add(lines.subList(from, Math.min(from + MAX_LINES_PER_PAGE, lines.size())));
        }
        if (pages.isEmpty()) pages.add(List.of());
        return pages;
    }
}
