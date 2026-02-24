package tn.esprit.services;

import tn.esprit.entities.Comment;
import tn.esprit.utils.MyDB;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CommentService implements ICRUD<Comment> {
    private final Connection cnx;

    public CommentService() {
        cnx = MyDB.getInstance().getconx();
    }

    @Override
    public Comment add(Comment comment) {
        String sql = "INSERT INTO comments (content, created_at, post_id) VALUES (?, ?, ?)";
        try (PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, comment.getContent());
            ps.setTimestamp(2, Timestamp.valueOf(comment.getCreatedAt()));
            ps.setInt(3, comment.getPostId());
            ps.executeUpdate();

            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                comment.setId(rs.getInt(1));
            }
            System.out.println("✅ Comment added: " + comment);
            return comment;
        } catch (SQLException e) {
            System.out.println("❌ addComment error: " + e.getMessage());
            return null;
        }
    }

    @Override
    public List<Comment> list() {
        List<Comment> comments = new ArrayList<>();
        String sql = "SELECT * FROM comments";
        try (Statement st = cnx.createStatement();
                ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Comment comment = new Comment(
                        rs.getInt("id"),
                        rs.getString("content"),
                        rs.getTimestamp("created_at").toLocalDateTime(),
                        rs.getInt("post_id"));
                comments.add(comment);
            }
        } catch (SQLException e) {
            System.out.println("❌ listComments error: " + e.getMessage());
        }
        return comments;
    }

    @Override
    public void update(Comment comment) {
        String sql = "UPDATE comments SET content=? WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, comment.getContent());
            ps.setInt(2, comment.getId());
            int rows = ps.executeUpdate();
            if (rows > 0)
                System.out.println("✏️ Comment updated: " + comment);
            else
                System.out.println("No comment found with id=" + comment.getId());
        } catch (SQLException e) {
            System.out.println("❌ updateComment error: " + e.getMessage());
        }
    }

    @Override
    public void delete(Comment comment) {
        deleteById(comment.getId());
    }

    public void deleteById(int id) {
        String sql = "DELETE FROM comments WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            int rows = ps.executeUpdate();
            if (rows > 0)
                System.out.println("🗑️ Comment deleted: id=" + id);
            else
                System.out.println("No comment found with id=" + id);
        } catch (SQLException e) {
            System.out.println("❌ deleteComment error: " + e.getMessage());
        }
    }

    public List<Comment> getByPostId(int postId) {
        List<Comment> comments = new ArrayList<>();
        String sql = "SELECT * FROM comments WHERE post_id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, postId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Comment comment = new Comment(
                            rs.getInt("id"),
                            rs.getString("content"),
                            rs.getTimestamp("created_at").toLocalDateTime(),
                            rs.getInt("post_id"));
                    comments.add(comment);
                }
            }
        } catch (SQLException e) {
            System.out.println("❌ getByPostId error: " + e.getMessage());
        }
        return comments;
    }
}
