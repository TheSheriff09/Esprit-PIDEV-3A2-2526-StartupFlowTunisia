package tn.esprit.services;

import tn.esprit.entities.Session;
import tn.esprit.utils.MyDB;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SessionService implements ICRUD<Session> {

    private final Connection cnx;

    public SessionService() {
        cnx = MyDB.getInstance().getCnx();
    }

    @Override
    public Session add(Session s) {
        if (s.getMentorID() <= 0)
            throw new IllegalArgumentException("Mentor ID is required.");
        if (s.getEntrepreneurID() <= 0)
            throw new IllegalArgumentException("Entrepreneur ID is required.");
        if (s.getSessionDate() == null)
            throw new IllegalArgumentException("Session date is required.");
        if (s.getSessionType() == null || s.getSessionType().isEmpty())
            throw new IllegalArgumentException("Session type is required.");

        s.setSuccessProbability(calculateSuccessProbability(s.getMentorID(), s.getEntrepreneurID()));

        String sql = "INSERT INTO session (mentorID, entrepreneurID, startupID, scheduleID, sessionDate, sessionType, status, notes, successProbability) VALUES (?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, s.getMentorID());
            ps.setInt(2, s.getEntrepreneurID());
            ps.setInt(3, s.getStartupID());
            if (s.getScheduleID() > 0)
                ps.setInt(4, s.getScheduleID());
            else
                ps.setNull(4, Types.INTEGER);
            ps.setDate(5, Date.valueOf(s.getSessionDate()));
            ps.setString(6, s.getSessionType());
            ps.setString(7, s.getStatus() != null ? s.getStatus() : "planned");
            ps.setString(8, s.getNotes());
            ps.setDouble(9, s.getSuccessProbability());
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next())
                s.setSessionID(rs.getInt(1));
            tn.esprit.utils.AuditLogger.log("Created Session ID: " + s.getSessionID());
            return s;
        } catch (SQLException e) {
            tn.esprit.utils.AuditLogger.logWarning("Failed creating session: " + e.getMessage());
            throw new RuntimeException("Error adding session: " + e.getMessage());
        }
    }

    @Override
    public List<Session> list() {
        List<Session> list = new ArrayList<>();
        String sql = "SELECT * FROM session ORDER BY sessionDate DESC";
        try (Statement st = cnx.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next())
                list.add(mapRow(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Error listing sessions: " + e.getMessage());
        }
        return list;
    }

    @Override
    public void update(Session s) {
        if (s.getSessionDate() == null)
            throw new IllegalArgumentException("Session date is required.");
        if (s.getSessionType() == null || s.getSessionType().isEmpty())
            throw new IllegalArgumentException("Session type is required.");

        s.setSuccessProbability(calculateSuccessProbability(s.getMentorID(), s.getEntrepreneurID()));

        String sql = "UPDATE session SET mentorID=?, entrepreneurID=?, startupID=?, scheduleID=?, sessionDate=?, sessionType=?, status=?, notes=?, successProbability=? WHERE sessionID=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, s.getMentorID());
            ps.setInt(2, s.getEntrepreneurID());
            ps.setInt(3, s.getStartupID());
            if (s.getScheduleID() > 0)
                ps.setInt(4, s.getScheduleID());
            else
                ps.setNull(4, Types.INTEGER);
            ps.setDate(5, Date.valueOf(s.getSessionDate()));
            ps.setString(6, s.getSessionType());
            ps.setString(7, s.getStatus());
            ps.setString(8, s.getNotes());
            ps.setDouble(9, s.getSuccessProbability());
            ps.setInt(10, s.getSessionID());
            ps.executeUpdate();
            tn.esprit.utils.AuditLogger.log("Updated Session ID: " + s.getSessionID() + " to status: " + s.getStatus());
            if ("completed".equalsIgnoreCase(s.getStatus())) {
                tn.esprit.events.EventBus.getInstance().publish(new tn.esprit.events.SessionCompletedEvent(s));
            }
        } catch (SQLException e) {
            tn.esprit.utils.AuditLogger.logWarning("Failed updating session: " + e.getMessage());
            throw new RuntimeException("Error updating session: " + e.getMessage());
        }
    }

    @Override
    public void delete(Session s) {
        String sql = "DELETE FROM session WHERE sessionID=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, s.getSessionID());
            ps.executeUpdate();
            tn.esprit.utils.AuditLogger.log("Deleted Session ID: " + s.getSessionID());
        } catch (SQLException e) {
            tn.esprit.utils.AuditLogger.logWarning("Failed deleting session: " + e.getMessage());
            throw new RuntimeException("Error deleting session: " + e.getMessage());
        }
    }

    /** Sessions where this user is the mentor. */
    public List<Session> listByMentor(int mentorID) {
        return filterQuery("SELECT * FROM session WHERE mentorID=? ORDER BY sessionDate DESC", mentorID);
    }

    /** Sessions where this user is the evaluator/entrepreneur. */
    public List<Session> listByEvaluator(int entrepreneurID) {
        return filterQuery("SELECT * FROM session WHERE entrepreneurID=? ORDER BY sessionDate DESC", entrepreneurID);
    }

    private List<Session> filterQuery(String sql, int id) {
        List<Session> list = new ArrayList<>();
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            while (rs.next())
                list.add(mapRow(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Error filtering sessions: " + e.getMessage());
        }
        return list;
    }

    private Session mapRow(ResultSet rs) throws SQLException {
        int schID = rs.getInt("scheduleID");
        Session s = new Session(
                rs.getInt("sessionID"),
                rs.getInt("mentorID"),
                rs.getInt("entrepreneurID"),
                rs.getInt("startupID"),
                rs.wasNull() ? 0 : schID,
                rs.getDate("sessionDate").toLocalDate(),
                rs.getString("sessionType"),
                rs.getString("status"),
                rs.getString("notes"));
        try {
            s.setSuccessProbability(rs.getDouble("successProbability"));
        } catch (SQLException e) {
        }
        return s;
    }

    private double calculateSuccessProbability(int mentorID, int entrepreneurID) {
        double baseScore = 50.0;
        try {
            SessionFeedbackService fs = new SessionFeedbackService();
            List<tn.esprit.entities.SessionFeedback> mentorFeedbacks = fs.listByMentor(mentorID);
            if (!mentorFeedbacks.isEmpty()) {
                double avg = mentorFeedbacks.stream().mapToInt(tn.esprit.entities.SessionFeedback::getProgressScore)
                        .average().orElse(50.0);
                long pos = mentorFeedbacks.stream().filter(f -> "Positive".equalsIgnoreCase(f.getSentiment())).count();
                baseScore += (avg - 50) * 0.4;
                baseScore += (pos * 100.0 / mentorFeedbacks.size() - 50) * 0.2;
            }
            // Entrepreneur engagement factor
            int sessionCount = filterQuery("SELECT * FROM session WHERE entrepreneurID=?", entrepreneurID).size();
            baseScore += Math.min(sessionCount * 2.0, 20.0);
        } catch (Exception e) {
        }
        return Math.max(0.0, Math.min(100.0, baseScore));
    }
}
