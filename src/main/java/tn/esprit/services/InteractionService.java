package tn.esprit.services;

import tn.esprit.entities.Interaction;
import tn.esprit.utils.MyDB;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class InteractionService {
    private Connection cnx;

    public InteractionService() {
        cnx = MyDB.getInstance().getconx();
        ensureTableExists();
    }

    private void ensureTableExists() {
        String sql = "CREATE TABLE IF NOT EXISTS interactions (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "post_id INT NOT NULL, " +
                "user_id INT NOT NULL, " +
                "type VARCHAR(20) NOT NULL, " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "FOREIGN KEY (post_id) REFERENCES forum_posts(id) ON DELETE CASCADE" +
                ")";
        try (Statement st = cnx.createStatement()) {
            st.execute(sql);
        } catch (SQLException e) {
            System.out.println("Error creating interactions table: " + e.getMessage());
        }
    }

    public void add(Interaction interaction) {
        // Check if already reacted
        if (hasReacted(interaction.getPostId(), interaction.getUserId())) {
            updateReaction(interaction);
            return;
        }

        String sql = "INSERT INTO interactions (post_id, user_id, type, created_at) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, interaction.getPostId());
            ps.setInt(2, interaction.getUserId());
            ps.setString(3, interaction.getType());
            ps.setTimestamp(4, Timestamp.valueOf(interaction.getCreatedAt()));
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean hasReacted(int postId, int userId) {
        String sql = "SELECT count(*) FROM interactions WHERE post_id = ? AND user_id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, postId);
            ps.setInt(2, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public void updateReaction(Interaction interaction) {
        String sql = "UPDATE interactions SET type = ?, created_at = ? WHERE post_id = ? AND user_id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, interaction.getType());
            ps.setTimestamp(2, Timestamp.valueOf(interaction.getCreatedAt()));
            ps.setInt(3, interaction.getPostId());
            ps.setInt(4, interaction.getUserId());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public int getInteractionCount(int postId) {
        String sql = "SELECT count(*) FROM interactions WHERE post_id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, postId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    // Get count by type (optional for detailed stats)
    public int getCountByType(int postId, String type) {
        String sql = "SELECT count(*) FROM interactions WHERE post_id = ? AND type = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, postId);
            ps.setString(2, type);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public java.util.Map<String, Integer> getGlobalInteractionStats() {
        java.util.Map<String, Integer> stats = new java.util.HashMap<>();
        String sql = "SELECT type, count(*) FROM interactions GROUP BY type";
        try (Statement st = cnx.createStatement();
                ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                stats.put(rs.getString(1), rs.getInt(2));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return stats;
    }
}
