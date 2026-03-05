package tn.esprit.service;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import tn.esprit.entity.Application;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class PDFService {

    public String extractTextFromPDF(String pdfPath) throws IOException {
        try (PDDocument document = PDDocument.load(new File(pdfPath))) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    public void exportApplicationToPDF(Application app, String outputPath) throws IOException {
        PdfWriter writer = new PdfWriter(outputPath);
        PdfDocument pdfDoc = new PdfDocument(writer);
        Document document = new Document(pdfDoc);

        // Title
        document.add(new Paragraph("Funding Application Details")
                .setTextAlignment(TextAlignment.CENTER)
                .setFontSize(20));

        document.add(new Paragraph("\n"));

        // Application details table
        Table table = new Table(UnitValue.createPercentArray(new float[]{30, 70}));
        table.setWidth(UnitValue.createPercentValue(100));

        addTableRow(table, "Application ID:", String.valueOf(app.getId()));
        addTableRow(table, "Entrepreneur ID:", String.valueOf(app.getEntrepreneurId()));
        addTableRow(table, "Amount:", "$" + app.getAmount());
        addTableRow(table, "Status:", app.getStatus());
        addTableRow(table, "Submission Date:", app.getSubmissionDate());
        addTableRow(table, "Application Reason:", app.getApplicationReason());
        addTableRow(table, "Project ID:", String.valueOf(app.getProjectId()));
        addTableRow(table, "Payment Schedule:", app.getPaymentSchedule());
        addTableRow(table, "Attachment:", app.getAttachment());



        document.add(table);
        document.close();

        System.out.println("PDF exported successfully to: " + outputPath);
    }

    public void exportAllApplicationsToPDF(List<Application> applications, String outputPath) throws IOException {
        PdfWriter writer = new PdfWriter(outputPath);
        PdfDocument pdfDoc = new PdfDocument(writer);
        Document document = new Document(pdfDoc);

        // Title
        document.add(new Paragraph("All Funding Applications Report")
                .setTextAlignment(TextAlignment.CENTER)
                .setFontSize(20));

        document.add(new Paragraph("\n"));

        // Applications table
        Table table = new Table(UnitValue.createPercentArray(new float[]{8, 10, 8, 8, 12, 15, 8, 10, 8, 8, 5}));
        table.setWidth(UnitValue.createPercentValue(100));

        // Header
        table.addHeaderCell("ID");
        table.addHeaderCell("Ent ID");
        table.addHeaderCell("Amount");
        table.addHeaderCell("Status");
        table.addHeaderCell("Date");
        table.addHeaderCell("Reason");
        table.addHeaderCell("Proj ID");
        table.addHeaderCell("Payment");


        // Data rows
        for (Application app : applications) {
            table.addCell(String.valueOf(app.getId()));
            table.addCell(String.valueOf(app.getEntrepreneurId()));
            table.addCell("$" + app.getAmount());
            table.addCell(app.getStatus());
            table.addCell(app.getSubmissionDate());
            table.addCell(app.getApplicationReason());
            table.addCell(String.valueOf(app.getProjectId()));
            table.addCell(app.getPaymentSchedule());

        }

        document.add(table);
        document.close();

        System.out.println("Full applications PDF exported successfully to: " + outputPath);
    }

    private void addTableRow(Table table, String label, String value) {
        table.addCell(label);
        table.addCell(value);
    }
}