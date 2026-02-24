package tn.esprit.Services;

import tn.esprit.entities.User;
import tn.esprit.utils.MyDB;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserService implements ICRUD<User> {

    private final Connection cnx;
   // public String sha256Public;

    public UserService() {
        cnx = MyDB.getInstance().getCnx();
    }

    @Override
    public User add(User u) {
        String sql = "INSERT INTO users (full_name, email, password_hash, role, status, mentor_expertise, evaluator_level) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, u.getFullName());
            ps.setString(2, u.getEmail());
            ps.setString(3, u.getPasswordHash());
            ps.setString(4, u.getRole());
            ps.setString(5, u.getStatus());
            ps.setString(6, u.getMentorExpertise());
            ps.setString(7, u.getEvaluatorLevel());

            ps.executeUpdate();

            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                u.setId(rs.getInt(1));
            }

            System.out.println(" User added: " + u);
            return u;

        } catch (SQLException e) {
            System.out.println(" addUser error: " + e.getMessage());
            return null;
        }
    }

    @Override
    public List<User> list() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM users";

        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                User u = new User(
                        rs.getInt("id"),
                        rs.getString("full_name"),
                        rs.getString("email"),
                        rs.getString("password_hash"),
                        rs.getString("role"),
                        rs.getString("status"),
                        rs.getString("mentor_expertise"),
                        rs.getString("evaluator_level"),
                        rs.getTimestamp("created_at")
                );
                users.add(u);
            }

        } catch (SQLException e) {
            System.out.println(" listUsers error: " + e.getMessage());
        }

        return users;
    }

    @Override
    public void update(User u) {
       String sql = "UPDATE users SET full_name=?, email=?, password_hash=?, role=?, status=?, mentor_expertise=?, evaluator_level=? " +
                "WHERE id=?";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {

            ps.setString(1, u.getFullName());
            ps.setString(2, u.getEmail());
            ps.setString(3, u.getPasswordHash());
            ps.setString(4, u.getRole());
            ps.setString(5, u.getStatus());

            ps.setString(6, u.getMentorExpertise()); // can be null
            ps.setString(7, u.getEvaluatorLevel());  // can be null

            ps.setInt(8, u.getId());

            int rows = ps.executeUpdate();
            if (rows > 0) System.out.println(" User updated: " + u);
            else System.out.println(" No user found with id=" + u.getId());

        } catch (SQLException e) {
            System.out.println(" error: " + e.getMessage());
        }
    }

    @Override
    public void delete(User u) {
        deleteById(u.getId());
    }
    public void deleteById(int id) {
        String sql = "DELETE FROM users WHERE id=?";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);

            int rows = ps.executeUpdate();
            if (rows > 0) {
                System.out.println(" User deleted: id=" + id);
            } else {
                System.out.println(" No user found with id=" + id);
            }

        } catch (SQLException e) {
            System.out.println("deleteUser error: " + e.getMessage());
        }
    }

    public User getByEmail(String email) {
        String sql = "SELECT * FROM users WHERE email=?";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, email);

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new User(
                        rs.getInt("id"),
                        rs.getString("full_name"),
                        rs.getString("email"),
                        rs.getString("password_hash"),
                        rs.getString("role"),
                        rs.getString("status"),
                        rs.getString("mentor_expertise"),
                        rs.getString("evaluator_level"),
                        rs.getTimestamp("created_at")
                );
            }

        } catch (SQLException e) {
            System.out.println(" getByEmail error: " + e.getMessage());
        }

        return null;
    }
    public User login(String email, String passwordPlain) {
        User u = getByEmail(email);
        if (u == null) return null;

        String dbPass = (u.getPasswordHash() == null) ? "" : u.getPasswordHash().trim();

        if (!passwordPlain.equals(dbPass)) return null;

        return u;
    }

    /*private boolean isSha256Hex(String s) {
        if (s == null) return false;
        s = s.trim();
        if (s.length() != 64) return false;
        for (int i = 0; i < s.length(); i++) {
            char c = Character.toLowerCase(s.charAt(i));
            boolean hex = (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f');
            if (!hex) return false;
        }
        return true;
    }

   /* private void updatePasswordHashOnly(int id, String newHash) {
        String sql = "UPDATE users SET password_hash=? WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, newHash);
            ps.setInt(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("updatePasswordHashOnly error: " + e.getMessage());
        }
    }
    /*private static String sha256(String input) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
   /* public String sha256Public(String input) {
        return sha256(input);
    }*/
    public User getById(int id) {
        String sql = "SELECT * FROM users WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new User(
                        rs.getInt("id"),
                        rs.getString("full_name"),
                        rs.getString("email"),
                        rs.getString("password_hash"),
                        rs.getString("role"),
                        rs.getString("status"),
                        rs.getString("mentor_expertise"),
                        rs.getString("evaluator_level"),
                        rs.getTimestamp("created_at")
                );
            }
        } catch (SQLException e) {
            System.out.println("getById error: " + e.getMessage());
        }
        return null;
    }
    public boolean updateProfile(int userId, String newFullName, String newEmail,
                                 String oldPasswordPlain, String newPasswordPlain) {

        User dbUser = getById(userId);
        if (dbUser == null) return false;

        newFullName = (newFullName == null) ? "" : newFullName.trim();
        newEmail = (newEmail == null) ? "" : newEmail.trim();

        if (newFullName.isEmpty() || newEmail.isEmpty()) return false;

        User emailOwner = getByEmail(newEmail);
        if (emailOwner != null && emailOwner.getId() != userId) {
            System.out.println(" email already used");
            return false;
        }

        boolean wantsPasswordChange = (newPasswordPlain != null && !newPasswordPlain.trim().isEmpty());

        if (wantsPasswordChange) {
            if (oldPasswordPlain == null || oldPasswordPlain.trim().isEmpty()) return false;

            String dbPass = (dbUser.getPasswordHash() == null) ? "" : dbUser.getPasswordHash().trim();
            if (!oldPasswordPlain.equals(dbPass)) {
                System.out.println("old password incorrect");
                return false;
            }
        }

        String sql;
        if (wantsPasswordChange) {
            sql = "UPDATE users SET full_name=?, email=?, password_hash=? WHERE id=?";
        } else {
            sql = "UPDATE users SET full_name=?, email=? WHERE id=?";
        }

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, newFullName);
            ps.setString(2, newEmail);

            if (wantsPasswordChange) {
                ps.setString(3, newPasswordPlain.trim());
                ps.setInt(4, userId);
            } else {
                ps.setInt(3, userId);
            }

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.out.println("updateProfile error: " + e.getMessage());
            return false;
        }
    }

   /* private boolean passwordMatches(String dbStored, String typedPlain) {
        String dbPass = (dbStored == null) ? "" : dbStored.trim();

        // if DB stored sha256
        if (isSha256Hex(dbPass)) {
            String typedHash = sha256(typedPlain);
            return typedHash.equalsIgnoreCase(dbPass);
        }

        // db stored plain
        return typedPlain.equals(dbPass);
    }*/
    public int countByRole(String role) {
        String sql = "SELECT COUNT(*) FROM users WHERE role=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, role);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            System.out.println("countByRole error: " + e.getMessage());
        }
        return 0;
    }
    public User addGoogleUser(String fullName, String email, String role) {
        try {
            // Password is null/empty because Google handles authentication
            String sql = "INSERT INTO user (fullName, email, passwordHash, role, status, mentorExpertise, evaluatorLevel, createdAt) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, NOW())";

            PreparedStatement ps = MyDB.getInstance().getCnx().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, fullName);
            ps.setString(2, email);
            ps.setString(3, null);
            ps.setString(4, role);
            ps.setString(5, "ACTIVE");
            ps.setString(6, null);
            ps.setString(7, null);

            int ok = ps.executeUpdate();
            if (ok == 0) return null;

            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) {
                int id = keys.getInt(1);
                return getByEmail(email); // simplest
            }
            return getByEmail(email);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
