package tn.esprit.Services;

import tn.esprit.entities.Application;
import tn.esprit.utils.MyDB;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ApplicationService implements ICRUD<Application> {

    private final Connection cnx;

    public ApplicationService() {
        cnx = MyDB.getInstance().getCnx(); // IMPORTANT: getCnx() matches your MyDB
    }

    @Override
    public Application add(Application app) {
        String sql = "INSERT INTO fundingapplication " +
                "(entrepreneurId, amount, status, submissionDate, applicationReason, projectId, paymentSchedule, attachment) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, app.getEntrepreneurId());
            ps.setFloat(2, app.getAmount());
            ps.setString(3, app.getStatus());

            // DB column type is DATE. If you store date as String in Application, keep setString.
            // Ideally you'd parse to java.sql.Date, but this keeps your current entity unchanged.
            ps.setString(4, app.getSubmissionDate());

            ps.setString(5, app.getApplicationReason());
            ps.setInt(6, app.getProjectId());
            ps.setString(7, app.getPaymentSchedule());
            ps.setString(8, app.getAttachment());

            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    app.setId(keys.getInt(1));
                }
            }

            System.out.println("Application added successfully!");
            return app;

        } catch (SQLException e) {
            System.out.println("Error adding application: " + e.getMessage());
            return null;
        }
    }

    @Override
    public List<Application> list() {
        List<Application> apps = new ArrayList<>();
        String sql = "SELECT * FROM fundingapplication";

        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                Application app = new Application(
                        rs.getInt("Id"),              // your DB dump uses `Id`
                        rs.getInt("entrepreneurId"),
                        rs.getFloat("amount"),
                        rs.getString("status"),
                        rs.getString("submissionDate"),
                        rs.getString("applicationReason"),
                        rs.getInt("projectId"),
                        rs.getString("paymentSchedule"),
                        rs.getString("attachment")
                );
                apps.add(app);
            }

        } catch (SQLException e) {
            System.out.println("Error fetching applications: " + e.getMessage());
        }

        return apps;
    }

    @Override
    public void update(Application app) {
        String sql = "UPDATE fundingapplication SET " +
                "entrepreneurId=?, amount=?, status=?, submissionDate=?, applicationReason=?, projectId=?, paymentSchedule=?, attachment=? " +
                "WHERE Id=?";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {

            ps.setInt(1, app.getEntrepreneurId());
            ps.setFloat(2, app.getAmount());
            ps.setString(3, app.getStatus());
            ps.setString(4, app.getSubmissionDate());
            ps.setString(5, app.getApplicationReason());
            ps.setInt(6, app.getProjectId());
            ps.setString(7, app.getPaymentSchedule());
            ps.setString(8, app.getAttachment());
            ps.setInt(9, app.getId());

            ps.executeUpdate();
            System.out.println("Application updated successfully!");

        } catch (SQLException e) {
            System.out.println("Error updating application: " + e.getMessage());
        }
    }

    @Override
    public void delete(Application app) {
        if (app == null) return;

        String sql = "DELETE FROM fundingapplication WHERE Id=?";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, app.getId());
            ps.executeUpdate();
            System.out.println("Application deleted successfully!");
        } catch (SQLException e) {
            System.out.println("Error deleting application: " + e.getMessage());
        }
    }

    // Optional helper (not in ICRUD): list only this entrepreneur's applications
    public List<Application> listByEntrepreneurId(int entrepreneurId) {
        List<Application> apps = new ArrayList<>();
        String sql = "SELECT * FROM fundingapplication WHERE entrepreneurId=?";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, entrepreneurId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    apps.add(new Application(
                            rs.getInt("Id"),
                            rs.getInt("entrepreneurId"),
                            rs.getFloat("amount"),
                            rs.getString("status"),
                            rs.getString("submissionDate"),
                            rs.getString("applicationReason"),
                            rs.getInt("projectId"),
                            rs.getString("paymentSchedule"),
                            rs.getString("attachment")
                    ));
                }
            }
        } catch (SQLException e) {
            System.out.println("Error fetching entrepreneur applications: " + e.getMessage());
        }

        return apps;
    }
    public double getTotalFundingRequested() {
        double total = 0;
        for (Application app : list()) {
            total += app.getAmount();
        }
        return total;
    }
    public java.util.Map<String, Integer> getApplicationStatusCounts() {
        java.util.Map<String, Integer> counts = new java.util.HashMap<>();
        for (Application app : list()) {
            counts.put(app.getStatus(), counts.getOrDefault(app.getStatus(), 0) + 1);
        }
        return counts;
    }
}

