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

        String sql = "INSERT INTO session (mentorID, entrepreneurID, startupID, scheduleID, sessionDate, sessionType, status, notes) VALUES (?,?,?,?,?,?,?,?)";
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
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next())
                s.setSessionID(rs.getInt(1));
            return s;
        } catch (SQLException e) {
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

        String sql = "UPDATE session SET mentorID=?, entrepreneurID=?, startupID=?, scheduleID=?, sessionDate=?, sessionType=?, status=?, notes=? WHERE sessionID=?";
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
            ps.setInt(9, s.getSessionID());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error updating session: " + e.getMessage());
        }
    }

    @Override
    public void delete(Session s) {
        String sql = "DELETE FROM session WHERE sessionID=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, s.getSessionID());
            ps.executeUpdate();
        } catch (SQLException e) {
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
        return new Session(
                rs.getInt("sessionID"),
                rs.getInt("mentorID"),
                rs.getInt("entrepreneurID"),
                rs.getInt("startupID"),
                rs.wasNull() ? 0 : schID,
                rs.getDate("sessionDate").toLocalDate(),
                rs.getString("sessionType"),
                rs.getString("status"),
                rs.getString("notes"));
    }
}
