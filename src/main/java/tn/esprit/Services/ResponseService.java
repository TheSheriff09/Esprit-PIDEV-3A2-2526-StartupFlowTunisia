package tn.esprit.Services;

import tn.esprit.entities.Response;
import tn.esprit.utils.MyDB;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ResponseService implements ICRUD<Response> {

    private final Connection cnx;

    public ResponseService() {
        cnx = MyDB.getInstance().getCnx();
    }

    @Override
    public Response add(Response r) {
        String sql = "INSERT INTO responses (content, reclamation_id, responder_user_id) VALUES (?, ?, ?)";

        try (PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, r.getContent());
            ps.setInt(2, r.getReclamationId());
            ps.setInt(3, r.getResponderUserId());

            ps.executeUpdate();

            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) r.setId(rs.getInt(1));

            return r;

        } catch (SQLException e) {
            System.out.println("addResponse error: " + e.getMessage());
            return null;
        }
    }

    @Override
    public List<Response> list() {
        List<Response> list = new ArrayList<>();
        String sql = "SELECT * FROM responses ORDER BY created_at DESC";

        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                list.add(map(rs));
            }

        } catch (SQLException e) {
            System.out.println("listResponses error: " + e.getMessage());
        }

        return list;
    }

    public List<Response> listByReclamationId(int reclamationId) {
        List<Response> list = new ArrayList<>();
        String sql = "SELECT * FROM responses WHERE reclamation_id=? ORDER BY created_at DESC";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, reclamationId);

            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(map(rs));

        } catch (SQLException e) {
            System.out.println("listByReclamationId error: " + e.getMessage());
        }

        return list;
    }

    @Override
    public void update(Response r) {
        String sql = "UPDATE responses SET content=? WHERE id=?";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, r.getContent());
            ps.setInt(2, r.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("updateResponse error: " + e.getMessage());
        }
    }

    @Override
    public void delete(Response r) {
        String sql = "DELETE FROM responses WHERE id=?";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, r.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("deleteResponse error: " + e.getMessage());
        }
    }

    private Response map(ResultSet rs) throws SQLException {
        return new Response(
                rs.getInt("id"),
                rs.getString("content"),
                rs.getTimestamp("created_at"),
                rs.getInt("reclamation_id"),
                rs.getInt("responder_user_id")
        );
    }
}