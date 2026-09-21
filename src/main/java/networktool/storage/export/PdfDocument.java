package main.java.networktool.storage.export;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;

/** Baut aus Textseiten ein minimales PDF 1.4 (Helvetica, WinAnsi); Fassade: {@link PdfReportBuilder}. */
final class PdfDocument {

    private static final Charset WIN_ANSI = Charset.forName("windows-1252");
    private static final int FIXED_OBJECTS = 3;
    private static final int OBJECTS_PER_PAGE = 2;
    private static final int FONT_SIZE = 10;
    private static final int LINE_HEIGHT = 14;
    private static final int PAGE_WIDTH = 595;
    private static final int PAGE_HEIGHT = 842;
    private static final int MARGIN = 50;

    private PdfDocument() {}

    static byte[] assemble(List<List<String>> pages) {
        int objectCount = FIXED_OBJECTS + OBJECTS_PER_PAGE * pages.size();
        int[] offsets = new int[objectCount + 1];
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            write(out, "%PDF-1.4\n");
            offsets[1] = mark(out, "1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n");
            offsets[2] = mark(out, "2 0 obj\n<< /Type /Pages /Kids [" + kids(pages.size())
                    + "] /Count " + pages.size() + " >>\nendobj\n");
            offsets[3] = mark(out, "3 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica"
                    + " /Encoding /WinAnsiEncoding >>\nendobj\n");
            for (int i = 0; i < pages.size(); i++) writePage(out, offsets, i, pages.get(i));
            int xrefStart = out.size();
            writeXref(out, offsets);
            write(out, "trailer\n<< /Size " + (objectCount + 1) + " /Root 1 0 R >>\nstartxref\n"
                    + xrefStart + "\n%%EOF");
        } catch (IOException e) {
            throw new IllegalStateException("PDF-Erzeugung fehlgeschlagen", e);
        }
        return out.toByteArray();
    }

    static String escapeText(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)")
                .replace("\r", " ").replace("\n", " ");
    }

    private static int pageObject(int index)    { return FIXED_OBJECTS + 1 + OBJECTS_PER_PAGE * index; }
    private static int contentObject(int index) { return pageObject(index) + 1; }

    private static String kids(int pageCount) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < pageCount; i++) sb.append(i > 0 ? " " : "").append(pageObject(i)).append(" 0 R");
        return sb.toString();
    }

    private static void writePage(ByteArrayOutputStream out, int[] offsets, int index,
                                  List<String> lines) throws IOException {
        offsets[pageObject(index)] = mark(out, pageObject(index) + " 0 obj\n<< /Type /Page /Parent 2 0 R"
                + " /MediaBox [0 0 " + PAGE_WIDTH + " " + PAGE_HEIGHT + "] /Contents " + contentObject(index)
                + " 0 R /Resources << /Font << /F1 3 0 R >> >> >>\nendobj\n");
        byte[] content = contentStream(lines).getBytes(WIN_ANSI);
        offsets[contentObject(index)] = mark(out, contentObject(index) + " 0 obj\n<< /Length "
                + content.length + " >>\nstream\n");
        out.write(content);
        write(out, "\nendstream\nendobj\n");
    }

    private static String contentStream(List<String> lines) {
        StringBuilder sb = new StringBuilder();
        sb.append("BT /F1 ").append(FONT_SIZE).append(" Tf ")
          .append(MARGIN).append(' ').append(PAGE_HEIGHT - MARGIN).append(" Td\n");
        for (String line : lines) {
            sb.append('(').append(escapeText(line)).append(") Tj 0 -").append(LINE_HEIGHT).append(" Td\n");
        }
        return sb.append("ET").toString();
    }

    private static void writeXref(ByteArrayOutputStream out, int[] offsets) throws IOException {
        write(out, "xref\n0 " + offsets.length + "\n0000000000 65535 f \n");
        for (int i = 1; i < offsets.length; i++) write(out, String.format("%010d 00000 n \n", offsets[i]));
    }

    private static int mark(ByteArrayOutputStream out, String text) throws IOException {
        int offset = out.size();
        write(out, text);
        return offset;
    }

    private static void write(ByteArrayOutputStream out, String s) throws IOException {
        out.write(s.getBytes(StandardCharsets.ISO_8859_1));
    }
}
