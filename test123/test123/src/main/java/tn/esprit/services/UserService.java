package tn.esprit.services;

import tn.esprit.entities.User;
import tn.esprit.utils.MyDB;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads User rows — no INSERT/UPDATE/DELETE needed for the mentorship module
 * (user management belongs to a different module).
 */
public class UserService {

    private final Connection cnx;

    public UserService() {
        cnx = MyDB.getInstance().getCnx();
    }

    public List<User> listAll() {
        return query("SELECT * FROM users");
    }

    /** Only users with role = 'mentor'. */
    public List<User> listMentors() {
        String cacheKey = "UserService_listMentors";
        if (tn.esprit.utils.CacheManager.contains(cacheKey)) {
            return (List<User>) tn.esprit.utils.CacheManager.get(cacheKey);
        }
        List<User> mentors = query("SELECT * FROM users WHERE role = 'mentor'");
        tn.esprit.utils.CacheManager.put(cacheKey, mentors);
        return mentors;
    }

    /**
     * Users who can book sessions (evaluator OR entrepreneur —
     * your DB may use either term).
     */
    public List<User> listEvaluators() {
        String cacheKey = "UserService_listEvaluators";
        if (tn.esprit.utils.CacheManager.contains(cacheKey)) {
            return (List<User>) tn.esprit.utils.CacheManager.get(cacheKey);
        }
        List<User> evaluators = query("SELECT * FROM users WHERE role IN ('evaluator','entrepreneur')");
        tn.esprit.utils.CacheManager.put(cacheKey, evaluators);
        return evaluators;
    }

    /** Find a single user by primary-key id. */
    public User findById(int id) {
        String cacheKey = "UserService_findById_" + id;
        if (tn.esprit.utils.CacheManager.contains(cacheKey)) {
            return (User) tn.esprit.utils.CacheManager.get(cacheKey);
        }
        String sql = "SELECT * FROM users WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                User u = mapRow(rs);
                tn.esprit.utils.CacheManager.put(cacheKey, u);
                return u;
            }
        } catch (SQLException e) {
            System.err.println("UserService.findById: " + e.getMessage());
        }
        return null;
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private List<User> query(String sql) {
        List<User> list = new ArrayList<>();
        try (Statement st = cnx.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next())
                list.add(mapRow(rs));
        } catch (SQLException e) {
            System.err.println("UserService.query: " + e.getMessage());
        }
        return list;
    }

    private User mapRow(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getInt(1)); // Assume first col is ID
        try {
            u.setFullName(rs.getString("fullName"));
        } catch (SQLException e) {
            try {
                u.setFullName(rs.getString("name"));
            } catch (SQLException e2) {
                u.setFullName("Unknown User");
            }
        }
        try {
            u.setEmail(rs.getString("email"));
        } catch (SQLException e) {
        }
        try {
            u.setRole(rs.getString("role"));
        } catch (SQLException e) {
        }
        return u;
    }
}
