package tn.esprit.Services;

import tn.esprit.entities.MentorEvaluation;
import tn.esprit.utils.MyDB;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MentorEvaluationService implements ICRUD<MentorEvaluation> {

    private final Connection cnx;

    public MentorEvaluationService() {
        cnx = MyDB.getInstance().getCnx();
        createTableIfNotExists();
    }

    private void createTableIfNotExists() {
        try (Statement st = cnx.createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS mentor_evaluations (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "entrepreneurID INT NOT NULL, " +
                    "mentorID INT NOT NULL, " +
                    "sessionID INT NOT NULL, " +
                    "rating INT NOT NULL CHECK (rating >= 1 AND rating <= 5), " +
                    "comment TEXT, " +
                    "createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public MentorEvaluation add(MentorEvaluation me) {
        String sql = "INSERT INTO mentor_evaluations (entrepreneurID, mentorID, sessionID, rating, comment) VALUES (?,?,?,?,?)";
        try (PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, me.getEntrepreneurID());
            ps.setInt(2, me.getMentorID());
            ps.setInt(3, me.getSessionID());
            ps.setInt(4, me.getRating());
            ps.setString(5, me.getComment());
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    me.setId(rs.getInt(1));
                }
            }
            return me;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public List<MentorEvaluation> list() {
        List<MentorEvaluation> list = new ArrayList<>();
        String sql = "SELECT * FROM mentor_evaluations";
        try (Statement st = cnx.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new MentorEvaluation(rs.getInt("id"), rs.getInt("entrepreneurID"), rs.getInt("mentorID"),
                        rs.getInt("sessionID"), rs.getInt("rating"), rs.getString("comment"),
                        rs.getTimestamp("createdAt")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<MentorEvaluation> listByMentor(int mentorID) {
        List<MentorEvaluation> list = new ArrayList<>();
        String sql = "SELECT * FROM mentor_evaluations WHERE mentorID = ? ORDER BY createdAt DESC";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, mentorID);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new MentorEvaluation(rs.getInt("id"), rs.getInt("entrepreneurID"), rs.getInt("mentorID"),
                            rs.getInt("sessionID"), rs.getInt("rating"), rs.getString("comment"),
                            rs.getTimestamp("createdAt")));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public MentorEvaluation getEvaluationForSession(int sessionID) {
        String sql = "SELECT * FROM mentor_evaluations WHERE sessionID = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, sessionID);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new MentorEvaluation(rs.getInt("id"), rs.getInt("entrepreneurID"), rs.getInt("mentorID"),
                            rs.getInt("sessionID"), rs.getInt("rating"), rs.getString("comment"),
                            rs.getTimestamp("createdAt"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public double getAverageRating(int mentorID) {
        String sql = "SELECT AVG(rating) FROM mentor_evaluations WHERE mentorID = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, mentorID);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble(1);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0.0;
    }

    @Override
    public void update(MentorEvaluation me) {
        String sql = "UPDATE mentor_evaluations SET rating=?, comment=? WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, me.getRating());
            ps.setString(2, me.getComment());
            ps.setInt(3, me.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void delete(MentorEvaluation me) {
        String sql = "DELETE FROM mentor_evaluations WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, me.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
