package tn.esprit.services;

import tn.esprit.utils.MyDB;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class DashboardService {
    private final Connection cnx;

    public DashboardService() {
        cnx = MyDB.getInstance().getCnx();
    }

    /** For Mentor: Count sessions by status */
    public Map<String, Integer> getSessionStatusStatsForMentor(int mentorID) {
        Map<String, Integer> stats = new HashMap<>();
        String sql = "SELECT status, COUNT(*) as count FROM session WHERE mentorID = ? GROUP BY status";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, mentorID);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                stats.put(rs.getString("status"), rs.getInt("count"));
            }
        } catch (SQLException e) {
            System.err.println("DashboardService error: " + e.getMessage());
        }
        return stats;
    }

    /**
     * For Entrepreneur: Breakdown of sessions by mentor (or progress score average)
     */
    public Map<String, Double> getAverageProgressByMentorForEntrepreneur(int entrepreneurID) {
        Map<String, Double> stats = new HashMap<>();
        // Try fullName first
        String sql = "SELECT u.fullName, AVG(sf.progressScore) as avgScore " +
                "FROM session_feedback sf " +
                "JOIN session s ON sf.sessionID = s.sessionID " +
                "JOIN users u ON s.mentorID = u.id " +
                "WHERE s.entrepreneurID = ? " +
                "GROUP BY u.fullName";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, entrepreneurID);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                stats.put(rs.getString(1), rs.getDouble(2));
            }
            return stats;
        } catch (SQLException e) {
            // Fallback to 'name' column
            String sql2 = "SELECT u.name, AVG(sf.progressScore) as avgScore " +
                    "FROM session_feedback sf " +
                    "JOIN session s ON sf.sessionID = s.sessionID " +
                    "JOIN users u ON s.mentorID = u.id " +
                    "WHERE s.entrepreneurID = ? " +
                    "GROUP BY u.name";
            try (PreparedStatement ps2 = cnx.prepareStatement(sql2)) {
                ps2.setInt(1, entrepreneurID);
                ResultSet rs2 = ps2.executeQuery();
                while (rs2.next()) {
                    stats.put(rs2.getString(1), rs2.getDouble(2));
                }
            } catch (SQLException e2) {
                System.err.println("DashboardService fallback error: " + e2.getMessage());
            }
        }
        return stats;
    }

    public int getTotalSessions(int userID, boolean isMentor) {
        String column = isMentor ? "mentorID" : "entrepreneurID";
        String sql = "SELECT COUNT(*) FROM session WHERE " + column + " = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, userID);
            ResultSet rs = ps.executeQuery();
            if (rs.next())
                return rs.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public int getPendingBookings(int userID, boolean isMentor) {
        String column = isMentor ? "mentorID" : "entrepreneurID";
        String sql = "SELECT COUNT(*) FROM booking WHERE " + column + " = ? AND status = 'pending'";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, userID);
            ResultSet rs = ps.executeQuery();
            if (rs.next())
                return rs.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }
}
