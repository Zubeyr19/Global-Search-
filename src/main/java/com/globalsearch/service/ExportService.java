package com.globalsearch.service;

import com.opencsv.CSVWriter;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class ExportService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Export data to CSV format
     */
    public byte[] exportToCsv(List<String[]> data) throws IOException {
        log.info("Exporting {} rows to CSV", data.size());

        StringWriter stringWriter = new StringWriter();
        try (CSVWriter csvWriter = new CSVWriter(stringWriter)) {
            csvWriter.writeAll(data);
        }

        return stringWriter.toString().getBytes();
    }

    /**
     * Export data to Excel format
     */
    public byte[] exportToExcel(String sheetName, List<String> headers, List<List<Object>> rows) throws IOException {
        log.info("Exporting {} rows to Excel", rows.size());

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet(sheetName);

            // Create header row
            Row headerRow = sheet.createRow(0);
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            for (int i = 0; i < headers.size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers.get(i));
                cell.setCellStyle(headerStyle);
            }

            // Create data rows
            int rowNum = 1;
            for (List<Object> rowData : rows) {
                Row row = sheet.createRow(rowNum++);
                for (int i = 0; i < rowData.size(); i++) {
                    Cell cell = row.createCell(i);
                    Object value = rowData.get(i);

                    if (value instanceof String) {
                        cell.setCellValue((String) value);
                    } else if (value instanceof Number) {
                        cell.setCellValue(((Number) value).doubleValue());
                    } else if (value instanceof Boolean) {
                        cell.setCellValue((Boolean) value);
                    } else if (value instanceof LocalDateTime) {
                        cell.setCellValue(((LocalDateTime) value).format(DATE_FORMATTER));
                    } else if (value != null) {
                        cell.setCellValue(value.toString());
                    }
                }
            }

            // Auto-size columns
            for (int i = 0; i < headers.size(); i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    /**
     * Export data to PDF format
     */
    public byte[] exportToPdf(String title, List<String> headers, List<List<String>> rows) throws IOException {
        log.info("Exporting {} rows to PDF", rows.size());

        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            PDPage page = new PDPage();
            document.addPage(page);

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                // Set title
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 16);
                contentStream.beginText();
                contentStream.newLineAtOffset(50, 750);
                contentStream.showText(title);
                contentStream.endText();

                // Set header font
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 12);
                float yPosition = 720;
                float xPosition = 50;
                float columnWidth = 100;

                // Draw headers
                contentStream.beginText();
                contentStream.newLineAtOffset(xPosition, yPosition);
                for (String header : headers) {
                    contentStream.showText(header);
                    contentStream.newLineAtOffset(columnWidth, 0);
                }
                contentStream.endText();

                yPosition -= 20;

                // Set data font
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10);

                // Draw rows
                for (List<String> row : rows) {
                    if (yPosition < 50) {
                        // Need new page
                        contentStream.close();
                        page = new PDPage();
                        document.addPage(page);
                        PDPageContentStream newContentStream = new PDPageContentStream(document, page);
                        yPosition = 750;
                        return exportToPdf(title, headers, rows); // Restart with new page
                    }

                    contentStream.beginText();
                    contentStream.newLineAtOffset(xPosition, yPosition);
                    for (String cell : row) {
                        String cellValue = cell != null ? cell : "";
                        // Truncate if too long
                        if (cellValue.length() > 15) {
                            cellValue = cellValue.substring(0, 12) + "...";
                        }
                        contentStream.showText(cellValue);
                        contentStream.newLineAtOffset(columnWidth, 0);
                    }
                    contentStream.endText();
                    yPosition -= 15;
                }
            }

            document.save(outputStream);
            return outputStream.toByteArray();
        }
    }

    /**
     * Export search results to CSV
     */
    public byte[] exportSearchResultsToCsv(List<Map<String, Object>> results) throws IOException {
        log.info("Exporting {} search results to CSV", results.size());

        if (results.isEmpty()) {
            return "No data to export".getBytes();
        }

        // Get headers from first result
        Map<String, Object> firstResult = results.get(0);
        String[] headers = firstResult.keySet().toArray(new String[0]);

        // Convert results to String array
        List<String[]> csvData = new java.util.ArrayList<>();
        csvData.add(headers);

        for (Map<String, Object> result : results) {
            String[] row = new String[headers.length];
            for (int i = 0; i < headers.length; i++) {
                Object value = result.get(headers[i]);
                row[i] = value != null ? value.toString() : "";
            }
            csvData.add(row);
        }

        return exportToCsv(csvData);
    }
}
