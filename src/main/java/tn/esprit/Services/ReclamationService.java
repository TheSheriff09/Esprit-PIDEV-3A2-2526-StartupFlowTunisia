package tn.esprit.Services;

import tn.esprit.entities.Reclamation;
import tn.esprit.utils.MyDB;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ReclamationService implements ICRUD<Reclamation> {

    private final Connection cnx;

    public ReclamationService() {
        cnx = MyDB.getInstance().getCnx();
    }

    @Override
    public Reclamation add(Reclamation r) {
        String sql = "INSERT INTO reclamations (title, description, status, requested_id, target_id) " +
                "VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, r.getTitle());
            ps.setString(2, r.getDescription());

            String st = r.getStatus();
            if (st == null || st.trim().isEmpty()) st = "OPEN";
            ps.setString(3, st);

            ps.setInt(4, r.getRequestedId());

            if (r.getTargetId() == null) ps.setNull(5, Types.INTEGER);
            else ps.setInt(5, r.getTargetId());

            ps.executeUpdate();

            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) r.setId(rs.getInt(1));

            return r;

        } catch (SQLException e) {
            System.out.println("addReclamation error: " + e.getMessage());
            return null;
        }
    }

    @Override
    public List<Reclamation> list() {
        List<Reclamation> list = new ArrayList<>();
        String sql = "SELECT * FROM reclamations ORDER BY created_at DESC";

        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                Integer target = (rs.getObject("target_id") == null) ? null : rs.getInt("target_id");

                Reclamation r = new Reclamation(
                        rs.getInt("id"),
                        rs.getString("title"),
                        rs.getString("description"),
                        rs.getString("status"),
                        rs.getTimestamp("created_at"),
                        rs.getInt("requested_id"),
                        target
                );
                list.add(r);
            }

        } catch (SQLException e) {
            System.out.println("listReclamations error: " + e.getMessage());
        }

        return list;
    }

    public List<Reclamation> listByRequester(int requestedId) {
        List<Reclamation> list = new ArrayList<>();
        String sql = "SELECT * FROM reclamations WHERE requested_id=? ORDER BY created_at DESC";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, requestedId);

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Integer target = (rs.getObject("target_id") == null) ? null : rs.getInt("target_id");

                Reclamation r = new Reclamation(
                        rs.getInt("id"),
                        rs.getString("title"),
                        rs.getString("description"),
                        rs.getString("status"),
                        rs.getTimestamp("created_at"),
                        rs.getInt("requested_id"),
                        target
                );
                list.add(r);
            }

        } catch (SQLException e) {
            System.out.println("listByRequester error: " + e.getMessage());
        }

        return list;
    }

    @Override
    public void update(Reclamation r) {

        String sql = "UPDATE reclamations SET title=?, description=?, status=?, target_id=? WHERE id=?";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {

            ps.setString(1, r.getTitle());
            ps.setString(2, r.getDescription());
            ps.setString(3, r.getStatus());

            if (r.getTargetId() == null) ps.setNull(4, Types.INTEGER);
            else ps.setInt(4, r.getTargetId());

            ps.setInt(5, r.getId());

            int rows = ps.executeUpdate();
            if (rows > 0) System.out.println("Reclamation updated: " + r);
            else System.out.println("No reclamation found with id=" + r.getId());

        } catch (SQLException e) {
            System.out.println("updateReclamation error: " + e.getMessage());
        }
    }

    public boolean updateDescription(int reclamationId, int requestedId, String newDescription) {

        String sql = "UPDATE reclamations SET description=? WHERE id=? AND requested_id=?";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {

            ps.setString(1, newDescription);
            ps.setInt(2, reclamationId);
            ps.setInt(3, requestedId);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.out.println("updateDescription error: " + e.getMessage());
            return false;
        }
    }

    @Override
    public void delete(Reclamation r) {

        String sql = "DELETE FROM reclamations WHERE id=?";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, r.getId());

            int rows = ps.executeUpdate();
            if (rows > 0) System.out.println("Reclamation deleted: id=" + r.getId());
            else System.out.println("No reclamation found id=" + r.getId());

        } catch (SQLException e) {
            System.out.println("deleteReclamation error: " + e.getMessage());
        }
    }

    public boolean deleteById(int reclamationId, int requestedId) {

        String sql = "DELETE FROM reclamations WHERE id=? AND requested_id=?";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, reclamationId);
            ps.setInt(2, requestedId);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.out.println("deleteById error: " + e.getMessage());
            return false;
        }
    }
    public boolean updateStatus(int reclamationId, String newStatus) {
        String sql = "UPDATE reclamations SET status=? WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, newStatus);
            ps.setInt(2, reclamationId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("updateStatus error: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteByIdAdmin(int reclamationId) {
        String sql = "DELETE FROM reclamations WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, reclamationId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("deleteByIdAdmin error: " + e.getMessage());
            return false;
        }
    }
}