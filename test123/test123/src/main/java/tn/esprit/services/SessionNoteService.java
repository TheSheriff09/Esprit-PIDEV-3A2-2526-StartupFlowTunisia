package tn.esprit.services;

import tn.esprit.entities.SessionNote;
import tn.esprit.utils.MyDB;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SessionNoteService implements ICRUD<SessionNote> {

    private final Connection cnx;

    public SessionNoteService() {
        cnx = MyDB.getInstance().getCnx();
    }

    @Override
    public SessionNote add(SessionNote n) {
        String sql = "INSERT INTO session_notes (sessionID, entrepreneurID, satisfactionScore, notes, noteDate) VALUES (?,?,?,?,?)";
        try (PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, n.getSessionID());
            ps.setInt(2, n.getEntrepreneurID());
            ps.setInt(3, n.getSatisfactionScore());
            ps.setString(4, n.getNotes());
            ps.setDate(5, Date.valueOf(n.getNoteDate()));
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next())
                n.setNoteID(rs.getInt(1));
            tn.esprit.utils.AuditLogger.log("Entrepreneur Note added for Session ID: " + n.getSessionID());
            return n;
        } catch (SQLException e) {
            tn.esprit.utils.AuditLogger.logWarning("Failed adding note: " + e.getMessage());
            throw new RuntimeException("Error adding note: " + e.getMessage());
        }
    }

    @Override
    public List<SessionNote> list() {
        List<SessionNote> list = new ArrayList<>();
        String sql = "SELECT * FROM session_notes ORDER BY noteDate DESC";
        try (Statement st = cnx.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next())
                list.add(mapRow(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Error listing notes: " + e.getMessage());
        }
        return list;
    }

    @Override
    public void update(SessionNote n) {
        String sql = "UPDATE session_notes SET satisfactionScore=?, notes=?, noteDate=? WHERE noteID=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, n.getSatisfactionScore());
            ps.setString(2, n.getNotes());
            ps.setDate(3, Date.valueOf(n.getNoteDate()));
            ps.setInt(4, n.getNoteID());
            ps.executeUpdate();
            tn.esprit.utils.AuditLogger.log("Updated Note ID: " + n.getNoteID());
        } catch (SQLException e) {
            throw new RuntimeException("Error updating note: " + e.getMessage());
        }
    }

    @Override
    public void delete(SessionNote n) {
        String sql = "DELETE FROM session_notes WHERE noteID=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, n.getNoteID());
            ps.executeUpdate();
            tn.esprit.utils.AuditLogger.log("Deleted Note ID: " + n.getNoteID());
        } catch (SQLException e) {
            throw new RuntimeException("Error deleting note: " + e.getMessage());
        }
    }

    public List<SessionNote> listByEntrepreneur(int entrepreneurID) {
        List<SessionNote> list = new ArrayList<>();
        String sql = "SELECT * FROM session_notes WHERE entrepreneurID=? ORDER BY noteDate DESC";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, entrepreneurID);
            ResultSet rs = ps.executeQuery();
            while (rs.next())
                list.add(mapRow(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Error listing entrepreneur notes: " + e.getMessage());
        }
        return list;
    }

    public List<SessionNote> listBySession(int sessionID) {
        List<SessionNote> list = new ArrayList<>();
        String sql = "SELECT * FROM session_notes WHERE sessionID=? ORDER BY noteDate DESC";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, sessionID);
            ResultSet rs = ps.executeQuery();
            while (rs.next())
                list.add(mapRow(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Error listing session notes: " + e.getMessage());
        }
        return list;
    }

    private SessionNote mapRow(ResultSet rs) throws SQLException {
        return new SessionNote(
                rs.getInt("noteID"),
                rs.getInt("sessionID"),
                rs.getInt("entrepreneurID"),
                rs.getInt("satisfactionScore"),
                rs.getString("notes"),
                rs.getDate("noteDate").toLocalDate());
    }
}
