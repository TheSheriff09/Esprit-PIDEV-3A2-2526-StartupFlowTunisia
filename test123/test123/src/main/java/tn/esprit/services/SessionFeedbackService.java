package tn.esprit.services;

import tn.esprit.entities.SessionFeedback;
import tn.esprit.utils.MyDB;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SessionFeedbackService implements ICRUD<SessionFeedback> {

    private final Connection cnx;

    public SessionFeedbackService() {
        cnx = MyDB.getInstance().getCnx();
    }

    @Override
    public SessionFeedback add(SessionFeedback f) {
        if (f.getSessionID() <= 0)
            throw new IllegalArgumentException("Session ID is required.");
        if (f.getMentorID() <= 0)
            throw new IllegalArgumentException("Mentor ID is required.");
        if (f.getProgressScore() < 0 || f.getProgressScore() > 100)
            throw new IllegalArgumentException("Score must be between 0 and 100.");
        if (f.getStrengths() == null || f.getStrengths().trim().isEmpty())
            throw new IllegalArgumentException("Strengths field is required.");
        if (f.getFeedbackDate() == null)
            throw new IllegalArgumentException("Feedback date is required.");
        if (feedbackExistsForSession(f.getSessionID()))
            throw new IllegalStateException("Feedback already exists for this session.");

        if ("Neutral".equalsIgnoreCase(f.getSentiment())) {
            String combinedText = f.getStrengths() + " " + f.getWeaknesses() + " " + f.getRecommendations();
            f.setSentiment(SentimentAnalyzerService.getInstance().analyzeSentiment(combinedText));
        }

        String sql = "INSERT INTO session_feedback (sessionID, mentorID, progressScore, strengths, weaknesses, recommendations, nextActions, feedbackDate, sentiment) VALUES (?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, f.getSessionID());
            ps.setInt(2, f.getMentorID());
            ps.setInt(3, f.getProgressScore());
            ps.setString(4, tn.esprit.utils.AESUtil.encrypt(f.getStrengths()));
            ps.setString(5, tn.esprit.utils.AESUtil.encrypt(f.getWeaknesses()));
            ps.setString(6, tn.esprit.utils.AESUtil.encrypt(f.getRecommendations()));
            ps.setString(7, tn.esprit.utils.AESUtil.encrypt(f.getNextActions()));
            ps.setDate(8, Date.valueOf(f.getFeedbackDate()));
            ps.setString(9, f.getSentiment());
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next())
                f.setFeedbackID(rs.getInt(1));
            tn.esprit.events.EventBus.getInstance().publish(new tn.esprit.events.FeedbackSubmittedEvent(f));
            tn.esprit.utils.AuditLogger.log("Created Feedback for Session ID: " + f.getSessionID());
            return f;
        } catch (SQLException e) {
            tn.esprit.utils.AuditLogger.logWarning("Failed creating feedback: " + e.getMessage());
            throw new RuntimeException("Error adding feedback: " + e.getMessage());
        }
    }

    @Override
    public List<SessionFeedback> list() {
        List<SessionFeedback> list = new ArrayList<>();
        String sql = "SELECT * FROM session_feedback ORDER BY feedbackDate DESC";
        try (Statement st = cnx.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next())
                list.add(mapRow(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Error listing feedback: " + e.getMessage());
        }
        return list;
    }

    @Override
    public void update(SessionFeedback f) {
        if (f.getProgressScore() < 0 || f.getProgressScore() > 100)
            throw new IllegalArgumentException("Score must be between 0 and 100.");
        if (f.getStrengths() == null || f.getStrengths().trim().isEmpty())
            throw new IllegalArgumentException("Strengths field is required.");

        String sql = "UPDATE session_feedback SET progressScore=?, strengths=?, weaknesses=?, recommendations=?, nextActions=?, feedbackDate=? WHERE feedbackID=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, f.getProgressScore());
            ps.setString(2, tn.esprit.utils.AESUtil.encrypt(f.getStrengths()));
            ps.setString(3, tn.esprit.utils.AESUtil.encrypt(f.getWeaknesses()));
            ps.setString(4, tn.esprit.utils.AESUtil.encrypt(f.getRecommendations()));
            ps.setString(5, tn.esprit.utils.AESUtil.encrypt(f.getNextActions()));
            ps.setDate(6, Date.valueOf(f.getFeedbackDate()));
            ps.setInt(7, f.getFeedbackID());
            ps.executeUpdate();
            tn.esprit.utils.AuditLogger.log("Updated Feedback ID: " + f.getFeedbackID());
        } catch (SQLException e) {
            tn.esprit.utils.AuditLogger.logWarning("Failed updating feedback: " + e.getMessage());
            throw new RuntimeException("Error updating feedback: " + e.getMessage());
        }
    }

    @Override
    public void delete(SessionFeedback f) {
        String sql = "DELETE FROM session_feedback WHERE feedbackID=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, f.getFeedbackID());
            ps.executeUpdate();
            tn.esprit.utils.AuditLogger.log("Deleted Feedback ID: " + f.getFeedbackID());
        } catch (SQLException e) {
            tn.esprit.utils.AuditLogger.logWarning("Failed deleting feedback: " + e.getMessage());
            throw new RuntimeException("Error deleting feedback: " + e.getMessage());
        }
    }

    private boolean feedbackExistsForSession(int sessionID) {
        String sql = "SELECT COUNT(*) FROM session_feedback WHERE sessionID=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, sessionID);
            ResultSet rs = ps.executeQuery();
            if (rs.next())
                return rs.getInt(1) > 0;
        } catch (SQLException ignored) {
        }
        return false;
    }

    /** All feedback written by a specific mentor. */
    public List<SessionFeedback> listByMentor(int mentorID) {
        return filterQuery("SELECT * FROM session_feedback WHERE mentorID=? ORDER BY feedbackDate DESC", mentorID);
    }

    /** Feedback for one specific session. */
    public List<SessionFeedback> listBySession(int sessionID) {
        return filterQuery("SELECT * FROM session_feedback WHERE sessionID=? ORDER BY feedbackDate DESC", sessionID);
    }

    private List<SessionFeedback> filterQuery(String sql, int id) {
        List<SessionFeedback> list = new ArrayList<>();
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            while (rs.next())
                list.add(mapRow(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Error filtering feedback: " + e.getMessage());
        }
        return list;
    }

    private SessionFeedback mapRow(ResultSet rs) throws SQLException {
        SessionFeedback f = new SessionFeedback(
                rs.getInt("feedbackID"),
                rs.getInt("sessionID"),
                rs.getInt("mentorID"),
                rs.getInt("progressScore"),
                tn.esprit.utils.AESUtil.decrypt(rs.getString("strengths")),
                tn.esprit.utils.AESUtil.decrypt(rs.getString("weaknesses")),
                tn.esprit.utils.AESUtil.decrypt(rs.getString("recommendations")),
                tn.esprit.utils.AESUtil.decrypt(rs.getString("nextActions")),
                rs.getDate("feedbackDate").toLocalDate());
        try {
            f.setSentiment(rs.getString("sentiment"));
        } catch (SQLException e) {
        }
        return f;
    }
}
