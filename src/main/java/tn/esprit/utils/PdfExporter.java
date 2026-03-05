package tn.esprit.utils;

import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class PdfExporter {

    private static final PDFont FONT = PDType1Font.HELVETICA;
    private static final PDFont FONT_BOLD = PDType1Font.HELVETICA_BOLD;

    public static <T> void exportTableView(TableView<T> table, File file, String title) throws Exception {
        try (PDDocument doc = new PDDocument()) {
            PDRectangle pageSize = PDRectangle.A4;
            float margin = 40;
            float pageW = pageSize.getWidth();
            float pageH = pageSize.getHeight();

            float titleSize = 16;
            float metaSize = 10;
            float headerSize = 10;
            float cellSize = 9;

            // Create first page/stream
            PDPage page = new PDPage(pageSize);
            doc.addPage(page);
            PDPageContentStream cs = new PDPageContentStream(doc, page);

            int pageNumber = 1;

            // ---------- Header (title + meta) ----------
            float y = pageH - margin;

            y = drawTitle(cs, margin, y, title, titleSize);
            y -= 10;

            String meta = "Generated: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            y = drawText(cs, margin, y, meta, metaSize, FONT);
            y -= 18;

            // ---------- Table setup ----------
            List<TableColumn<T, ?>> cols = (List<TableColumn<T, ?>>) (List<?>) table.getColumns();
            if (cols.isEmpty()) {
                cs.close();
                doc.save(file);
                return;
            }

            float tableWidth = pageW - 2 * margin;

            // Column widths: auto-ish based on header length (simple, stable)
            float[] colW = computeColumnWidths(cols, tableWidth);

            float headerH = 18;
            float rowPaddingV = 6;
            float lineSpacing = 1.2f;

            // draw header row
            y = ensureSpaceOrNewPage(doc, cs, pageSize, margin, y, headerH + 20,
                    () -> newPage(doc, pageSize), v -> {}, pageNumber);
            drawHeaderRow(cs, margin, y, colW, cols, headerH, headerSize);
            y -= headerH;

            // ---------- Rows ----------
            for (T item : table.getItems()) {

                // build wrapped cell lines and compute row height
                List<List<String>> wrapped = new ArrayList<>();
                int maxLines = 1;

                for (int c = 0; c < cols.size(); c++) {
                    Object val = cols.get(c).getCellData(item);
                    String text = safe(val);

                    // attachment column: show only filename if it looks like a path
                    if (looksLikePath(text)) text = new File(text).getName();

                    List<String> lines = wrapText(text, colW[c] - 8, cellSize, FONT); // 8 = inner padding
                    wrapped.add(lines);
                    maxLines = Math.max(maxLines, lines.size());
                }

                float rowH = Math.max(18, (maxLines * cellSize * lineSpacing) + rowPaddingV);

                // if not enough space: new page + redraw header
                if (y - rowH < margin + 40) {
                    // footer with page number
                    drawFooter(cs, pageW, margin, pageNumber, metaSize);

                    cs.close();

                    page = new PDPage(pageSize);
                    doc.addPage(page);
                    cs = new PDPageContentStream(doc, page);
                    pageNumber++;

                    y = pageH - margin;

                    // repeat meta line lightly
                    y = drawText(cs, margin, y, (title == null ? "Export" : title), 12, FONT_BOLD);
                    y -= 16;
                    y = drawText(cs, margin, y, meta, metaSize, FONT);
                    y -= 18;

                    drawHeaderRow(cs, margin, y, colW, cols, headerH, headerSize);
                    y -= headerH;
                }

                drawRow(cs, margin, y, colW, wrapped, rowH, cellSize, lineSpacing);
                y -= rowH;
            }

            // ---------- Attachments summary section (Applications only) ----------
            // If your table includes a column named "Attachment", list distinct filenames.
            List<String> attachments = extractAttachmentNames(table);
            if (!attachments.isEmpty()) {
                float sectionGap = 18;
                float sectionTitleH = 16;

                if (y - (sectionGap + sectionTitleH + 60) < margin + 40) {
                    drawFooter(cs, pageW, margin, pageNumber, metaSize);
                    cs.close();

                    page = new PDPage(pageSize);
                    doc.addPage(page);
                    cs = new PDPageContentStream(doc, page);
                    pageNumber++;

                    y = pageH - margin;
                }

                y -= sectionGap;
                y = drawText(cs, margin, y, "Attached Files", 12, FONT_BOLD);
                y -= 14;

                cs.setFont(FONT, metaSize);
                for (String a : attachments) {
                    if (y < margin + 40) {
                        drawFooter(cs, pageW, margin, pageNumber, metaSize);
                        cs.close();

                        page = new PDPage(pageSize);
                        doc.addPage(page);
                        cs = new PDPageContentStream(doc, page);
                        pageNumber++;

                        y = pageH - margin;
                        y = drawText(cs, margin, y, "Attached Files (continued)", 12, FONT_BOLD);
                        y -= 14;
                    }
                    y = drawText(cs, margin, y, "• " + a, metaSize, FONT);
                    y -= 12;
                }
            }

            // footer last page
            drawFooter(cs, pageW, margin, pageNumber, metaSize);

            cs.close();
            doc.save(file);
        }
    }

    // ---------------- helpers ----------------

    private static float drawTitle(PDPageContentStream cs, float x, float y, String title, float size) throws Exception {
        return drawText(cs, x, y, title == null ? "Export" : title, size, FONT_BOLD);
    }

    private static float drawText(PDPageContentStream cs, float x, float y, String text, float size, PDFont font) throws Exception {
        cs.beginText();
        cs.setFont(font, size);
        cs.newLineAtOffset(x, y);
        cs.showText(text == null ? "" : text);
        cs.endText();
        return y;
    }

    private static void drawFooter(PDPageContentStream cs, float pageW, float margin, int pageNumber, float size) throws Exception {
        String footer = "Page " + pageNumber;
        float textW = FONT.getStringWidth(footer) / 1000f * size;
        float x = pageW - margin - textW;
        float y = margin - 10;
        drawText(cs, x, y, footer, size, FONT);
    }

    private static void drawHeaderRow(PDPageContentStream cs, float x, float y,
                                      float[] colW, List<? extends TableColumn<?, ?>> cols,
                                      float h, float fontSize) throws Exception {

        // background not needed; draw borders + bold headers
        float curX = x;

        // outer header top line
        cs.moveTo(x, y);
        cs.lineTo(x + sum(colW), y);
        cs.stroke();

        // header cells
        for (int i = 0; i < cols.size(); i++) {
            // vertical lines
            cs.moveTo(curX, y);
            cs.lineTo(curX, y - h);
            cs.stroke();

            // header text
            String header = safe(cols.get(i).getText());
            cs.beginText();
            cs.setFont(FONT_BOLD, fontSize);
            cs.newLineAtOffset(curX + 4, y - 13);
            cs.showText(truncate(header, colW[i] - 8, fontSize, FONT_BOLD));
            cs.endText();

            curX += colW[i];
        }

        // last vertical
        cs.moveTo(x + sum(colW), y);
        cs.lineTo(x + sum(colW), y - h);
        cs.stroke();

        // header bottom line
        cs.moveTo(x, y - h);
        cs.lineTo(x + sum(colW), y - h);
        cs.stroke();
    }

    private static void drawRow(PDPageContentStream cs, float x, float y,
                                float[] colW, List<List<String>> wrapped,
                                float rowH, float fontSize, float lineSpacing) throws Exception {

        float curX = x;

        // top line
        cs.moveTo(x, y);
        cs.lineTo(x + sum(colW), y);
        cs.stroke();

        for (int i = 0; i < wrapped.size(); i++) {
            // vertical lines
            cs.moveTo(curX, y);
            cs.lineTo(curX, y - rowH);
            cs.stroke();

            float textY = y - 13; // start position inside cell
            cs.setFont(FONT, fontSize);

            for (String line : wrapped.get(i)) {
                cs.beginText();
                cs.newLineAtOffset(curX + 4, textY);
                cs.showText(line);
                cs.endText();
                textY -= (fontSize * lineSpacing);
                if (textY < y - rowH + 6) break;
            }

            curX += colW[i];
        }

        // last vertical
        cs.moveTo(x + sum(colW), y);
        cs.lineTo(x + sum(colW), y - rowH);
        cs.stroke();

        // bottom line
        cs.moveTo(x, y - rowH);
        cs.lineTo(x + sum(colW), y - rowH);
        cs.stroke();
    }

    private static float[] computeColumnWidths(List<? extends TableColumn<?, ?>> cols, float tableWidth) {
        // weights based on header length (stable)
        float totalWeight = 0;
        float[] weights = new float[cols.size()];

        for (int i = 0; i < cols.size(); i++) {
            String h = safe(cols.get(i).getText());
            float w = Math.max(6, h.length());
            weights[i] = w;
            totalWeight += w;
        }

        float[] out = new float[cols.size()];
        for (int i = 0; i < cols.size(); i++) {
            out[i] = tableWidth * (weights[i] / totalWeight);
            out[i] = Math.max(55, out[i]); // min width
        }

        // normalize again if we exceeded tableWidth due to min widths
        float sum = sum(out);
        if (sum > tableWidth) {
            float scale = tableWidth / sum;
            for (int i = 0; i < out.length; i++) out[i] *= scale;
        }
        return out;
    }

    private static List<String> wrapText(String text, float maxWidth, float fontSize, PDFont font) throws Exception {
        text = text == null ? "" : text.trim();
        if (text.isEmpty()) return Collections.singletonList("");

        List<String> lines = new ArrayList<>();
        String[] words = text.split("\\s+");
        StringBuilder line = new StringBuilder();

        for (String w : words) {
            String test = line.length() == 0 ? w : line + " " + w;
            float width = font.getStringWidth(test) / 1000f * fontSize;
            if (width <= maxWidth) {
                line.setLength(0);
                line.append(test);
            } else {
                if (line.length() > 0) lines.add(line.toString());
                line.setLength(0);
                // if single word too long -> hard cut
                if (font.getStringWidth(w) / 1000f * fontSize > maxWidth) {
                    lines.add(truncate(w, maxWidth, fontSize, font));
                } else {
                    line.append(w);
                }
            }
        }
        if (line.length() > 0) lines.add(line.toString());
        return lines;
    }

    private static String truncate(String text, float maxWidth, float fontSize, PDFont font) throws Exception {
        if (text == null) return "";
        String t = text;
        while (t.length() > 0) {
            float width = font.getStringWidth(t) / 1000f * fontSize;
            if (width <= maxWidth) return t;
            t = t.substring(0, t.length() - 1);
        }
        return "";
    }

    private static List<String> extractAttachmentNames(TableView<?> table) {
        // Finds a column with header "Attachment" (case-insensitive) and returns unique filenames.
        TableColumn<?, ?> attachmentCol = null;
        for (Object c : table.getColumns()) {
            TableColumn<?, ?> col = (TableColumn<?, ?>) c;
            if (col.getText() != null && col.getText().trim().equalsIgnoreCase("Attachment")) {
                attachmentCol = col;
                break;
            }
        }
        if (attachmentCol == null) return Collections.emptyList();

        Set<String> uniq = new LinkedHashSet<>();
        for (Object item : table.getItems()) {
            Object v = ((TableColumn) attachmentCol).getCellData(item);
            String path = safe(v);
            if (!path.isBlank() && looksLikePath(path)) uniq.add(new File(path).getName());
        }
        return new ArrayList<>(uniq);
    }

    private static boolean looksLikePath(String s) {
        if (s == null) return false;
        return s.contains("\\") || s.contains("/") || s.matches("(?i).*\\.(pdf|png|jpg|jpeg|doc|docx|xls|xlsx)$");
    }

    private static float sum(float[] arr) {
        float s = 0;
        for (float v : arr) s += v;
        return s;
    }

    private static String safe(Object o) {
        return o == null ? "" : String.valueOf(o);
    }

    // Just a placeholder for the earlier lambda signature (kept for clarity)
    private static float ensureSpaceOrNewPage(PDDocument doc, PDPageContentStream cs, PDRectangle pageSize,
                                              float margin, float y, float need,
                                              SupplierNewPage supplier, java.util.function.Consumer<Float> after,
                                              int pageNumber) {
        return y;
    }

    private interface SupplierNewPage {
        PDPage get();
    }

    private static PDPage newPage(PDDocument doc, PDRectangle pageSize) {
        PDPage p = new PDPage(pageSize);
        doc.addPage(p);
        return p;
    }
}