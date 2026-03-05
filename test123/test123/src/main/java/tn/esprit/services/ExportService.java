package tn.esprit.services;

import tn.esprit.entities.SessionFeedback;
import tn.esprit.entities.SessionNote;
import tn.esprit.utils.AuditLogger;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ExportService {

    private static final String EXPORT_DIR = "exports";

    public static String exportFeedbackToCSV(List<SessionFeedback> feedbacks) {
        ensureExportDir();
        String filename = "feedback_export_" +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")) + ".csv";
        File file = new File(EXPORT_DIR, filename);

        try (FileWriter fw = new FileWriter(file)) {
            fw.write(
                    "FeedbackID,SessionID,MentorID,ProgressScore,Sentiment,Strengths,Weaknesses,Recommendations,NextActions,FeedbackDate\n");
            for (SessionFeedback f : feedbacks) {
                fw.write(String.format("%d,%d,%d,%d,%s,\"%s\",\"%s\",\"%s\",\"%s\",%s\n",
                        f.getFeedbackID(), f.getSessionID(), f.getMentorID(),
                        f.getProgressScore(), safe(f.getSentiment()),
                        safe(f.getStrengths()), safe(f.getWeaknesses()),
                        safe(f.getRecommendations()), safe(f.getNextActions()),
                        f.getFeedbackDate()));
            }
            AuditLogger.log("Exported " + feedbacks.size() + " feedback records to " + filename);
            return file.getAbsolutePath();
        } catch (IOException e) {
            AuditLogger.logWarning("CSV export failed: " + e.getMessage());
            throw new RuntimeException("Export failed: " + e.getMessage());
        }
    }

    public static String exportNotesToCSV(List<SessionNote> notes) {
        ensureExportDir();
        String filename = "notes_export_" +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")) + ".csv";
        File file = new File(EXPORT_DIR, filename);

        try (FileWriter fw = new FileWriter(file)) {
            fw.write("NoteID,SessionID,EntrepreneurID,SatisfactionScore,Notes,NoteDate\n");
            for (SessionNote n : notes) {
                fw.write(String.format("%d,%d,%d,%d,\"%s\",%s\n",
                        n.getNoteID(), n.getSessionID(), n.getEntrepreneurID(),
                        n.getSatisfactionScore(), safe(n.getNotes()), n.getNoteDate()));
            }
            AuditLogger.log("Exported " + notes.size() + " notes to " + filename);
            return file.getAbsolutePath();
        } catch (IOException e) {
            throw new RuntimeException("Export failed: " + e.getMessage());
        }
    }

    private static void ensureExportDir() {
        File dir = new File(EXPORT_DIR);
        if (!dir.exists())
            dir.mkdirs();
    }

    private static String safe(String s) {
        return s == null ? "" : s.replace("\"", "\"\"");
    }
}
