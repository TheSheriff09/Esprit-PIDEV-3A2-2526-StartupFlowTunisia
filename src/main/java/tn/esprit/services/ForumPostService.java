package tn.esprit.services;

import tn.esprit.entities.ForumPost;
import tn.esprit.utils.MyDB;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ForumPostService implements ICRUD<ForumPost> {

    private Connection cnx;

    public ForumPostService() {
        // Ensure connection is retrieved correctly
        this.cnx = MyDB.getInstance().getconx();
        if (this.cnx == null) {
            System.out.println("❌ Database connection is NULL in ForumPostService constructor!");
        } else {
            System.out.println("✅ Database connection established in ForumPostService.");
        }
    }

    @Override
    public ForumPost add(ForumPost post) {
        if (cnx == null) {
            System.out.println("❌ Cannot add post: Connection is null.");
            return null;
        }
        String sql = "INSERT INTO forum_posts (title, content, image_url, created_at, updated_at) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, post.getTitle());
            ps.setString(2, post.getContent());
            // Set default image if null or empty
            String img = post.getImageUrl();
            if (img == null || img.trim().isEmpty()) {
                img = "default_post.png"; // Or some placeholder
            }
            ps.setString(3, img);
            ps.setTimestamp(4, Timestamp.valueOf(post.getCreatedAt()));
            // Fix: updated_at cannot be null. Set it to created_at if null.
            Timestamp updatedTs = post.getUpdatedAt() != null ? Timestamp.valueOf(post.getUpdatedAt())
                    : Timestamp.valueOf(post.getCreatedAt());
            ps.setTimestamp(5, updatedTs);

            int rows = ps.executeUpdate();
            System.out.println("Rows affected: " + rows);

            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                post.setId(rs.getInt(1));
            }
            System.out.println("✅ Post added successfully: " + post);
            return post;
        } catch (SQLException e) {
            System.out.println("❌ addPost error: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public List<ForumPost> list() {
        List<ForumPost> posts = new ArrayList<>();
        if (cnx == null) {
            System.out.println("❌ Cannot list posts: Connection is null.");
            return posts;
        }
        String sql = "SELECT * FROM forum_posts";
        try (Statement st = cnx.createStatement();
                ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                ForumPost post = new ForumPost();
                post.setId(rs.getInt("id"));
                post.setTitle(rs.getString("title"));
                post.setContent(rs.getString("content"));
                post.setImageUrl(rs.getString("image_url"));

                Timestamp createdAt = rs.getTimestamp("created_at");
                if (createdAt != null)
                    post.setCreatedAt(createdAt.toLocalDateTime());

                Timestamp updatedAt = rs.getTimestamp("updated_at");
                if (updatedAt != null) {
                    post.setUpdatedAt(updatedAt.toLocalDateTime());
                }
                posts.add(post);
            }
            System.out.println("✅ Loaded " + posts.size() + " posts from DB.");
        } catch (SQLException e) {
            System.out.println("❌ listPosts error: " + e.getMessage());
            e.printStackTrace();
        }
        return posts;
    }

    @Override
    public void update(ForumPost post) {
        if (cnx == null)
            return;
        String sql = "UPDATE forum_posts SET title = ?, content = ?, image_url = ?, updated_at = ? WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, post.getTitle());
            ps.setString(2, post.getContent());
            String img = post.getImageUrl();
            if (img == null || img.trim().isEmpty())
                img = "default_post.png";
            ps.setString(3, img);
            ps.setTimestamp(4, Timestamp.valueOf(post.getUpdatedAt()));
            ps.setInt(5, post.getId());
            int rows = ps.executeUpdate();
            if (rows > 0)
                System.out.println("✏️ Post updated: " + post);
            else
                System.out.println("No post found with id=" + post.getId());
        } catch (SQLException e) {
            System.out.println("❌ updatePost error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void delete(ForumPost post) {
        deleteById(post.getId());
    }

    public void deleteById(int id) {
        if (cnx == null)
            return;
        String sql = "DELETE FROM forum_posts WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            int rows = ps.executeUpdate();
            if (rows > 0)
                System.out.println("🗑️ Post deleted: id=" + id);
            else
                System.out.println("No post found with id=" + id);
        } catch (SQLException e) {
            System.out.println("❌ deletePost error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public ForumPost getById(int id) {
        if (cnx == null)
            return null;
        String sql = "SELECT * FROM forum_posts WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    ForumPost post = new ForumPost();
                    post.setId(rs.getInt("id"));
                    post.setTitle(rs.getString("title"));
                    post.setContent(rs.getString("content"));
                    post.setImageUrl(rs.getString("image_url"));

                    Timestamp createdAt = rs.getTimestamp("created_at");
                    if (createdAt != null)
                        post.setCreatedAt(createdAt.toLocalDateTime());

                    Timestamp updatedAt = rs.getTimestamp("updated_at");
                    if (updatedAt != null) {
                        post.setUpdatedAt(updatedAt.toLocalDateTime());
                    }
                    return post;
                }
            }
        } catch (SQLException e) {
            System.out.println("❌ getById error: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    public ForumPost getNewestPost() {
        if (cnx == null)
            return null;
        // Order by created_at DESC and limit 1 to get the newest
        String sql = "SELECT * FROM forum_posts ORDER BY created_at DESC LIMIT 1";
        try (Statement st = cnx.createStatement();
                ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) {
                ForumPost post = new ForumPost();
                post.setId(rs.getInt("id"));
                post.setTitle(rs.getString("title"));
                post.setContent(rs.getString("content"));
                post.setImageUrl(rs.getString("image_url"));

                Timestamp createdAt = rs.getTimestamp("created_at");
                if (createdAt != null)
                    post.setCreatedAt(createdAt.toLocalDateTime());

                Timestamp updatedAt = rs.getTimestamp("updated_at");
                if (updatedAt != null) {
                    post.setUpdatedAt(updatedAt.toLocalDateTime());
                }
                return post;
            }
        } catch (SQLException e) {
            System.out.println("❌ getNewestPost error: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }
}
