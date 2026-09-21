package main.java.networktool.storage.export;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PdfReportBuilderTest {

    @TempDir Path tmp;

    private static String asText(byte[] pdf) { return new String(pdf, StandardCharsets.ISO_8859_1); }

    private static List<String> lines(int n) {
        List<String> l = new ArrayList<>();
        for (int i = 0; i < n; i++) l.add("line" + i);
        return l;
    }

    @Test void build_startsWithPdfHeader() {
        assertEquals("%PDF-1.4", asText(PdfReportBuilder.build(List.of("hello"))).substring(0, 8));
    }

    @Test void build_endsWithEof() {
        assertTrue(asText(PdfReportBuilder.build(List.of("x"))).trim().endsWith("%%EOF"));
    }

    @Test void build_containsContentText() {
        assertTrue(asText(PdfReportBuilder.build(List.of("10.0.0.1 Linux"))).contains("10.0.0.1 Linux"));
    }

    @Test void build_escapesParentheses() {
        assertTrue(asText(PdfReportBuilder.build(List.of("a(b)c"))).contains("a\\(b\\)c"));
    }

    @Test void build_emptyList_stillValidHeader() {
        String pdf = asText(PdfReportBuilder.build(List.of()));
        assertTrue(pdf.startsWith("%PDF-1.4"));
        assertTrue(pdf.contains("/Count 1"));
    }

    @Test void build_manyLines_spreadOverPages() {
        String pdf = asText(PdfReportBuilder.build(lines(80)));
        assertTrue(pdf.contains("line49"));
        assertTrue(pdf.contains("line50"));
        assertTrue(pdf.contains("line79"));
        assertTrue(pdf.contains("/Count 2"));
    }

    @Test void paginate_exactMultiple_noEmptyExtraPage() {
        assertEquals(2, PdfReportBuilder.paginate(lines(100)).size());
        assertEquals(3, PdfReportBuilder.paginate(lines(101)).size());
    }

    @Test void build_containsXrefAndTrailer() {
        String pdf = asText(PdfReportBuilder.build(List.of("x")));
        assertTrue(pdf.contains("xref"));
        assertTrue(pdf.contains("trailer"));
    }

    @Test void build_xrefOffsetsPointToObjects() {
        String pdf = asText(PdfReportBuilder.build(lines(60)));
        String xref = pdf.substring(pdf.indexOf("xref\n"));
        String[] rows = xref.split("\n");
        int objects = Integer.parseInt(rows[1].split(" ")[1]);
        for (int i = 1; i < objects; i++) {
            int offset = Integer.parseInt(rows[2 + i].substring(0, 10));
            assertTrue(pdf.startsWith(i + " 0 obj", offset), "Objekt " + i);
        }
    }

    @Test void build_declaresWinAnsiEncoding_andEncodesUmlauts() {
        byte[] pdf = PdfReportBuilder.build(List.of("Größe ä"));
        assertTrue(asText(pdf).contains("/WinAnsiEncoding"));
        assertTrue(indexOf(pdf, (byte) 0xE4) >= 0);
    }

    @Test void build_nullLine_treatedAsEmpty() {
        assertDoesNotThrow(() -> PdfReportBuilder.build(Arrays.asList("a", null, "b")));
    }

    @Test void build_newlineInLine_replacedBySpace() {
        assertTrue(asText(PdfReportBuilder.build(List.of("a\nb"))).contains("(a b)"));
    }

    @Test void save_createsFileWithPdfExtension() throws IOException {
        Path file = PdfReportBuilder.save(tmp, List.of("a"));
        assertTrue(Files.exists(file));
        assertTrue(file.getFileName().toString().endsWith(".pdf"));
    }

    @Test void save_fileContentStartsWithPdfHeader() throws IOException {
        byte[] bytes = Files.readAllBytes(PdfReportBuilder.save(tmp, List.of("a")));
        assertEquals("%PDF-1.4", asText(bytes).substring(0, 8));
    }

    @Test void save_twice_neverOverwrites() throws IOException {
        Path first = PdfReportBuilder.save(tmp, List.of("a"));
        Path second = PdfReportBuilder.save(tmp, List.of("b"));
        assertNotEquals(first, second);
        assertTrue(Files.exists(first));
    }

    private static int indexOf(byte[] data, byte value) {
        for (int i = 0; i < data.length; i++) if (data[i] == value) return i;
        return -1;
    }
}
