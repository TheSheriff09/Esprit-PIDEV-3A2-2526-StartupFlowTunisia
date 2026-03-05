package tn.esprit.Services;

import tn.esprit.entities.Session;
import tn.esprit.entities.SessionFeedback;
import tn.esprit.entities.SessionNote;
import tn.esprit.utils.AuditLogger;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * CsvExportService — Full CSV export with proper RFC 4180 escaping.
 *
 * <p>
 * Business justification: Stakeholders (mentors, admins, entrepreneurs) need
 * portable data exports for reporting, compliance archival, and offline
 * analysis
 * in tools like Excel or Google Sheets.
 *
 * <p>
 * Design decisions:
 * <ul>
 * <li>RFC 4180 escaping: fields with commas/quotes/newlines are double-quoted
 * with internal
 * quotes doubled — handles edge cases that naive String.format approaches
 * miss.</li>
 * <li>Accepts a {@code File} parameter from FileChooser — decouples service
 * from UI.</li>
 * <li>BufferedWriter for efficient I/O on large datasets.</li>
 * <li>Static methods: export is a pure function over data — no instance state
 * needed.</li>
 * </ul>
 */
public class CsvExportService {

    private static final String EXPORT_DIR = "exports";

    // ── Sessions Export ────────────────────────────────────────────────────

    /**
     * Export a list of sessions to a CSV file chosen by the user via FileChooser.
     *
     * @param sessions   list of sessions to export
     * @param targetFile the File object obtained from FileChooser (never null)
     * @return absolute path of the written file
     */
    public static String exportSessionsToCSV(List<Session> sessions, File targetFile) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(targetFile))) {
            // Header row
            writer.write("SessionID,MentorID,EntrepreneurID,StartupID,ScheduleID,"
                    + "SessionDate,SessionType,Status,Notes,SuccessProbability");
            writer.newLine();

            for (Session s : sessions) {
                writer.write(
                        s.getSessionID() + ","
                                + s.getMentorID() + ","
                                + s.getEntrepreneurID() + ","
                                + s.getStartupID() + ","
                                + s.getScheduleID() + ","
                                + (s.getSessionDate() != null ? s.getSessionDate() : "") + ","
                                + csv(s.getSessionType()) + ","
                                + csv(s.getStatus()) + ","
                                + csv(s.getNotes()) + ","
                                + String.format("%.2f", s.getSuccessProbability()));
                writer.newLine();
            }

            AuditLogger.log("Exported " + sessions.size() + " sessions → " + targetFile.getName());
            return targetFile.getAbsolutePath();
        } catch (IOException e) {
            AuditLogger.logWarning("Session CSV export failed: " + e.getMessage());
            throw new RuntimeException("Session export failed: " + e.getMessage());
        }
    }

    // ── Feedback Export ────────────────────────────────────────────────────

    /**
     * Export feedback to a user-specified file (via FileChooser).
     * Decrypted text is exported (AESUtil already decrypts when loading from DB).
     */
    public static String exportFeedbackToCSV(List<SessionFeedback> feedbacks, File targetFile) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(targetFile))) {
            writer.write("FeedbackID,SessionID,MentorID,ProgressScore,Sentiment,"
                    + "Strengths,Weaknesses,Recommendations,NextActions,FeedbackDate");
            writer.newLine();

            for (SessionFeedback f : feedbacks) {
                writer.write(
                        f.getFeedbackID() + ","
                                + f.getSessionID() + ","
                                + f.getMentorID() + ","
                                + f.getProgressScore() + ","
                                + csv(f.getSentiment()) + ","
                                + csv(f.getStrengths()) + ","
                                + csv(f.getWeaknesses()) + ","
                                + csv(f.getRecommendations()) + ","
                                + csv(f.getNextActions()) + ","
                                + (f.getFeedbackDate() != null ? f.getFeedbackDate() : ""));
                writer.newLine();
            }

            AuditLogger.log("Exported " + feedbacks.size() + " feedback records → " + targetFile.getName());
            return targetFile.getAbsolutePath();
        } catch (IOException e) {
            AuditLogger.logWarning("Feedback CSV export failed: " + e.getMessage());
            throw new RuntimeException("Feedback export failed: " + e.getMessage());
        }
    }

    // ── Mentor Performance Export ──────────────────────────────────────────

    /**
     * Export the ranked mentor performance table produced by
     * {@link AnalyticsService}.
     */
    public static String exportMentorPerformanceToCSV(
            List<AnalyticsService.MentorStats> stats, File targetFile) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(targetFile))) {
            writer.write("Rank,MentorID,MentorName,MPI,AvgRating,SessionCount,FeedbackCount");
            writer.newLine();

            int rank = 1;
            for (AnalyticsService.MentorStats ms : stats) {
                writer.write(
                        rank + ","
                                + ms.getMentor().getId() + ","
                                + csv(ms.getMentor().getFullName()) + ","
                                + String.format("%.2f", ms.mpi()) + ","
                                + String.format("%.2f", ms.getAvgRating()) + ","
                                + ms.getSessionCount() + ","
                                + ms.getFeedbackCount());
                writer.newLine();
                rank++;
            }

            AuditLogger
                    .log("Exported mentor performance table (" + stats.size() + " mentors) → " + targetFile.getName());
            return targetFile.getAbsolutePath();
        } catch (IOException e) {
            AuditLogger.logWarning("Mentor performance CSV export failed: " + e.getMessage());
            throw new RuntimeException("Performance export failed: " + e.getMessage());
        }
    }

    // ── Notes Export (existing functionality preserved) ───────────────────

    public static String exportNotesToCSV(List<SessionNote> notes, File targetFile) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(targetFile))) {
            writer.write("NoteID,SessionID,EntrepreneurID,SatisfactionScore,Notes,NoteDate");
            writer.newLine();

            for (SessionNote n : notes) {
                writer.write(
                        n.getNoteID() + ","
                                + n.getSessionID() + ","
                                + n.getEntrepreneurID() + ","
                                + n.getSatisfactionScore() + ","
                                + csv(n.getNotes()) + ","
                                + (n.getNoteDate() != null ? n.getNoteDate() : ""));
                writer.newLine();
            }

            AuditLogger.log("Exported " + notes.size() + " notes → " + targetFile.getName());
            return targetFile.getAbsolutePath();
        } catch (IOException e) {
            throw new RuntimeException("Notes export failed: " + e.getMessage());
        }
    }

    // ── Auto-named export (legacy support for ExportService callers) ───────

    public static String exportFeedbackToCSV(List<SessionFeedback> feedbacks) {
        ensureExportDir();
        String filename = "feedback_" + timestamp() + ".csv";
        File file = new File(EXPORT_DIR, filename);
        return exportFeedbackToCSV(feedbacks, file);
    }

    public static String exportNotesToCSV(List<SessionNote> notes) {
        ensureExportDir();
        String filename = "notes_" + timestamp() + ".csv";
        File file = new File(EXPORT_DIR, filename);
        return exportNotesToCSV(notes, file);
    }

    public static String exportSessionsToCSV(List<Session> sessions) {
        ensureExportDir();
        String filename = "sessions_" + timestamp() + ".csv";
        File file = new File(EXPORT_DIR, filename);
        return exportSessionsToCSV(sessions, file);
    }

    // ── RFC 4180 CSV Escaping ─────────────────────────────────────────────

    /**
     * Properly escape a field value per RFC 4180:
     * - Wrap in double quotes if it contains comma, double-quote, or newline.
     * - Double any internal double-quote characters.
     */
    private static String csv(String value) {
        if (value == null)
            return "";
        boolean needsQuoting = value.contains(",") || value.contains("\"")
                || value.contains("\n") || value.contains("\r");
        String escaped = value.replace("\"", "\"\"");
        return needsQuoting ? "\"" + escaped + "\"" : escaped;
    }

    private static void ensureExportDir() {
        File dir = new File(EXPORT_DIR);
        if (!dir.exists())
            dir.mkdirs();
    }

    private static String timestamp() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
    }
}
