package tn.esprit.service;

import tn.esprit.entity.Application;
import tn.esprit.utils.MyDB;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class DashboardService {

    private Connection conx;

    public DashboardService() {
        conx = MyDB.getInstance().getConx();
    }

    public Map<String, Integer> getApplicationsByStatus() {
        Map<String, Integer> statusCounts = new HashMap<>();
        String query = "SELECT status, COUNT(*) as count FROM fundingapplication GROUP BY status";

        try (Statement st = conx.createStatement();
             ResultSet rs = st.executeQuery(query)) {

            while (rs.next()) {
                statusCounts.put(rs.getString("status"), rs.getInt("count"));
            }

        } catch (SQLException e) {
            System.err.println("Error fetching status statistics: " + e.getMessage());
        }

        return statusCounts;
    }

    public Map<Integer, Double> getFundingAmountByProject() {
        Map<Integer, Double> projectAmounts = new HashMap<>();
        String query = "SELECT projectId, SUM(amount) as total FROM fundingapplication GROUP BY projectId";

        try (Statement st = conx.createStatement();
             ResultSet rs = st.executeQuery(query)) {

            while (rs.next()) {
                projectAmounts.put(rs.getInt("projectId"), rs.getDouble("total"));
            }

        } catch (SQLException e) {
            System.err.println("Error fetching project funding statistics: " + e.getMessage());
        }

        return projectAmounts;
    }

    public Map<String, Integer> getApplicationsByMonth() {
        Map<String, Integer> monthlyCounts = new HashMap<>();
        String query = "SELECT DATE_FORMAT(STR_TO_DATE(submissionDate, '%Y-%m-%d'), '%Y-%m') as month, COUNT(*) as count " +
                      "FROM fundingapplication GROUP BY month ORDER BY month";

        try (Statement st = conx.createStatement();
             ResultSet rs = st.executeQuery(query)) {

            while (rs.next()) {
                monthlyCounts.put(rs.getString("month"), rs.getInt("count"));
            }

        } catch (SQLException e) {
            System.err.println("Error fetching monthly statistics: " + e.getMessage());
        }

        return monthlyCounts;
    }

    public double getTotalFundingAmount() {
        String query = "SELECT SUM(amount) as total FROM fundingapplication";

        try (Statement st = conx.createStatement();
             ResultSet rs = st.executeQuery(query)) {

            if (rs.next()) {
                return rs.getDouble("total");
            }

        } catch (SQLException e) {
            System.err.println("Error fetching total funding: " + e.getMessage());
        }

        return 0.0;
    }

    public int getTotalApplications() {
        String query = "SELECT COUNT(*) as total FROM fundingapplication";

        try (Statement st = conx.createStatement();
             ResultSet rs = st.executeQuery(query)) {

            if (rs.next()) {
                return rs.getInt("total");
            }

        } catch (SQLException e) {
            System.err.println("Error fetching total applications: " + e.getMessage());
        }

        return 0;
    }
}