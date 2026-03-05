package tn.esprit.Services;

import tn.esprit.entities.SessionTodo;
import tn.esprit.utils.MyDB;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SessionTodoService implements ICRUD<SessionTodo> {

    private final Connection cnx;

    public SessionTodoService() {
        cnx = MyDB.getInstance().getCnx();
        createTableIfNotExists();
    }

    private void createTableIfNotExists() {
        try (Statement st = cnx.createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS session_todos (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "sessionID INT NOT NULL, " +
                    "taskDescription VARCHAR(255) NOT NULL, " +
                    "isDone BOOLEAN DEFAULT FALSE, " +
                    "createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public SessionTodo add(SessionTodo todo) {
        String sql = "INSERT INTO session_todos (sessionID, taskDescription, isDone) VALUES (?,?,?)";
        try (PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, todo.getSessionID());
            ps.setString(2, todo.getTaskDescription());
            ps.setBoolean(3, todo.isDone());
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    todo.setId(rs.getInt(1));
                }
            }
            return todo;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public List<SessionTodo> list() {
        List<SessionTodo> list = new ArrayList<>();
        String sql = "SELECT * FROM session_todos";
        try (Statement st = cnx.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new SessionTodo(rs.getInt("id"), rs.getInt("sessionID"), rs.getString("taskDescription"),
                        rs.getBoolean("isDone"), rs.getTimestamp("createdAt")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<SessionTodo> listBySession(int sessionID) {
        List<SessionTodo> list = new ArrayList<>();
        String sql = "SELECT * FROM session_todos WHERE sessionID = ? ORDER BY createdAt ASC";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, sessionID);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new SessionTodo(rs.getInt("id"), rs.getInt("sessionID"), rs.getString("taskDescription"),
                            rs.getBoolean("isDone"), rs.getTimestamp("createdAt")));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    @Override
    public void update(SessionTodo todo) {
        String sql = "UPDATE session_todos SET taskDescription=?, isDone=? WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, todo.getTaskDescription());
            ps.setBoolean(2, todo.isDone());
            ps.setInt(3, todo.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void delete(SessionTodo todo) {
        String sql = "DELETE FROM session_todos WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, todo.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
