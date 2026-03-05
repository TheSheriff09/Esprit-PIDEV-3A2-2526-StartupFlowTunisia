package tn.esprit.Services;

import tn.esprit.entities.Favorite;
import tn.esprit.utils.MyDB;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FavoriteService implements ICRUD<Favorite> {

    private final Connection cnx;

    public FavoriteService() {
        cnx = MyDB.getInstance().getCnx();
        createTableIfNotExists();
    }

    private void createTableIfNotExists() {
        try (Statement st = cnx.createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS mentor_favorites (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "entrepreneurID INT NOT NULL, " +
                    "mentorID INT NOT NULL, " +
                    "createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "UNIQUE KEY unique_fav (entrepreneurID, mentorID))");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Favorite add(Favorite fav) {
        String sql = "INSERT INTO mentor_favorites (entrepreneurID, mentorID) VALUES (?,?) ON DUPLICATE KEY UPDATE id=id";
        try (PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, fav.getEntrepreneurID());
            ps.setInt(2, fav.getMentorID());
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    fav.setId(rs.getInt(1));
                }
            }
            return fav;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public List<Favorite> list() {
        List<Favorite> list = new ArrayList<>();
        String sql = "SELECT * FROM mentor_favorites";
        try (Statement st = cnx.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new Favorite(rs.getInt("id"), rs.getInt("entrepreneurID"), rs.getInt("mentorID"),
                        rs.getTimestamp("createdAt")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<Favorite> listByEntrepreneur(int entrepreneurID) {
        List<Favorite> list = new ArrayList<>();
        String sql = "SELECT * FROM mentor_favorites WHERE entrepreneurID = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, entrepreneurID);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Favorite(rs.getInt("id"), rs.getInt("entrepreneurID"), rs.getInt("mentorID"),
                            rs.getTimestamp("createdAt")));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public boolean isFavorite(int entrepreneurID, int mentorID) {
        String sql = "SELECT COUNT(*) FROM mentor_favorites WHERE entrepreneurID = ? AND mentorID = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, entrepreneurID);
            ps.setInt(2, mentorID);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public void removeFavorite(int entrepreneurID, int mentorID) {
        String sql = "DELETE FROM mentor_favorites WHERE entrepreneurID = ? AND mentorID = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, entrepreneurID);
            ps.setInt(2, mentorID);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void update(Favorite fav) {
        // Not really needed for favorites
    }

    @Override
    public void delete(Favorite fav) {
        String sql = "DELETE FROM mentor_favorites WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, fav.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
