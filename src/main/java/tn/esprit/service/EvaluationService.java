package tn.esprit.service;

import tn.esprit.entity.Evaluation;
import tn.esprit.utils.MyDB;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EvaluationService implements ICrud<Evaluation> {

    private Connection conx;

    public EvaluationService() {
        conx = MyDB.getInstance().getConx();
    }

    @Override
    public void add(Evaluation e) {
        String query = "INSERT INTO fundingevaluation " +
                "(fundingApplicationId, score, decision, evaluationComments, evaluatorId, riskLevel, fundingCategory) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = conx.prepareStatement(query)) {
            ps.setInt(1, e.getFundingApplicationId());
            ps.setInt(2, e.getScore());
            ps.setString(3, e.getDecision());
            ps.setString(4, e.getEvaluationComments());
            ps.setInt(5, e.getEvaluatorId());
            ps.setString(6, e.getRiskLevel());
            ps.setString(7, e.getFundingCategory());

            ps.executeUpdate();
            System.out.println("Evaluation added successfully!");
        } catch (SQLException ex) {
            System.out.println("Error adding evaluation: " + ex.getMessage());
        }
    }

    @Override
    public List<Evaluation> getAll() {
        List<Evaluation> list = new ArrayList<>();
        String query = "SELECT * FROM fundingevaluation";

        try (Statement st = conx.createStatement();
             ResultSet rs = st.executeQuery(query)) {

            while (rs.next()) {
                Evaluation e = new Evaluation(
                        rs.getInt("id"),
                        rs.getInt("fundingApplicationId"),
                        rs.getInt("score"),
                        rs.getString("decision"),
                        rs.getString("evaluationComments"),
                        rs.getInt("evaluatorId"),
                        rs.getString("riskLevel"),
                        rs.getString("fundingCategory")
                );
                list.add(e);
            }

        } catch (SQLException ex) {
            System.out.println("Error fetching evaluations: " + ex.getMessage());
        }

        return list;
    }

    @Override
    public void update(Evaluation e) {
        String query = "UPDATE fundingevaluation SET " +
                "fundingApplicationId=?, score=?, decision=?, evaluationComments=?, evaluatorId=?, riskLevel=?, fundingCategory=? " +
                "WHERE id=?";

        try (PreparedStatement ps = conx.prepareStatement(query)) {
            ps.setInt(1, e.getFundingApplicationId());
            ps.setInt(2, e.getScore());
            ps.setString(3, e.getDecision());
            ps.setString(4, e.getEvaluationComments());
            ps.setInt(5, e.getEvaluatorId());
            ps.setString(6, e.getRiskLevel());
            ps.setString(7, e.getFundingCategory());
            ps.setInt(8, e.getId());

            ps.executeUpdate();
            System.out.println("Evaluation updated successfully!");
        } catch (SQLException ex) {
            System.out.println("Error updating evaluation: " + ex.getMessage());
        }
    }

    @Override
    public void delete(int id) {
        String query = "DELETE FROM fundingevaluation WHERE id=?";

        try (PreparedStatement ps = conx.prepareStatement(query)) {
            ps.setInt(1, id);
            ps.executeUpdate();
            System.out.println("Evaluation deleted successfully!");
        } catch (SQLException ex) {
            System.out.println("Error deleting evaluation: " + ex.getMessage());
        }
    }

    // Optional: keep your helper (same style, returns list)
    public List<Evaluation> getByApplicationId(int appId) {
        List<Evaluation> list = new ArrayList<>();
        String sql = "SELECT * FROM fundingevaluation WHERE fundingApplicationId=?";

        try (PreparedStatement ps = conx.prepareStatement(sql)) {
            ps.setInt(1, appId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Evaluation(
                            rs.getInt("id"),
                            rs.getInt("fundingApplicationId"),
                            rs.getInt("score"),
                            rs.getString("decision"),
                            rs.getString("evaluationComments"),
                            rs.getInt("evaluatorId"),
                            rs.getString("riskLevel"),
                            rs.getString("fundingCategory")
                    ));
                }
            }
        } catch (SQLException ex) {
            System.out.println("Error fetching evaluations by applicationId: " + ex.getMessage());
        }

        return list;
    }
}