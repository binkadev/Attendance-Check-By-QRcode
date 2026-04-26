package com.attendance.backend.attendance.support;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class SimpleXlsxWriter {

    private SimpleXlsxWriter() {
    }

    public static byte[] writeSheet(String sheetName, List<List<String>> rows) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            try (ZipOutputStream zip = new ZipOutputStream(out, StandardCharsets.UTF_8)) {
                add(zip, "[Content_Types].xml", contentTypes());
                add(zip, "_rels/.rels", rels());
                add(zip, "xl/workbook.xml", workbook(sheetName));
                add(zip, "xl/_rels/workbook.xml.rels", workbookRels());
                add(zip, "xl/worksheets/sheet1.xml", sheet(rows));
                add(zip, "xl/styles.xml", styles());
            }

            return out.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to create xlsx file", ex);
        }
    }

    private static void add(ZipOutputStream zip, String path, String content) throws Exception {
        zip.putNextEntry(new ZipEntry(path));
        zip.write(content.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private static String contentTypes() {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
                  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
                  <Default Extension="xml" ContentType="application/xml"/>
                  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
                  <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
                  <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
                </Types>
                """;
    }

    private static String rels() {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
                </Relationships>
                """;
    }

    private static String workbook(String sheetName) {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"
                          xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
                  <sheets>
                    <sheet name="%s" sheetId="1" r:id="rId1"/>
                  </sheets>
                </workbook>
                """.formatted(escape(sheetName));
    }

    private static String workbookRels() {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
                  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
                </Relationships>
                """;
    }

    private static String styles() {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
                  <fonts count="1"><font><sz val="11"/><name val="Calibri"/></font></fonts>
                  <fills count="1"><fill><patternFill patternType="none"/></fill></fills>
                  <borders count="1"><border/></borders>
                  <cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>
                  <cellXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/></cellXfs>
                </styleSheet>
                """;
    }

    private static String sheet(List<List<String>> rows) {
        StringBuilder sb = new StringBuilder();
        sb.append("""
                <?xml version="1.0" encoding="UTF-8"?>
                <worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
                  <sheetData>
                """);

        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
            int excelRow = rowIndex + 1;
            sb.append("<row r=\"").append(excelRow).append("\">");

            List<String> row = rows.get(rowIndex);
            for (int colIndex = 0; colIndex < row.size(); colIndex++) {
                String ref = columnName(colIndex + 1) + excelRow;
                sb.append("<c r=\"").append(ref).append("\" t=\"inlineStr\"><is><t>")
                        .append(escape(row.get(colIndex)))
                        .append("</t></is></c>");
            }

            sb.append("</row>");
        }

        sb.append("""
                  </sheetData>
                </worksheet>
                """);

        return sb.toString();
    }

    private static String columnName(int number) {
        StringBuilder sb = new StringBuilder();
        int n = number;

        while (n > 0) {
            int remainder = (n - 1) % 26;
            sb.insert(0, (char) ('A' + remainder));
            n = (n - 1) / 26;
        }

        return sb.toString();
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }

        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}