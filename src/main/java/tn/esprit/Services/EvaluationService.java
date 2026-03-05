package tn.esprit.Services;

import tn.esprit.entities.Evaluation;
import tn.esprit.utils.MyDB;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EvaluationService implements ICRUD<Evaluation> {

    private final Connection conx;

    public EvaluationService() {
        // ✅ MyDB you shared uses getCnx()
        conx = MyDB.getInstance().getCnx();
    }

    @Override
    public Evaluation add(Evaluation e) {
        String query = "INSERT INTO fundingevaluation " +
                "(fundingApplicationId, score, decision, evaluationComments, evaluatorId, riskLevel, fundingCategory) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = conx.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, e.getFundingApplicationId());
            ps.setInt(2, e.getScore());
            ps.setString(3, e.getDecision());
            ps.setString(4, e.getEvaluationComments());
            ps.setInt(5, e.getEvaluatorId());
            ps.setString(6, e.getRiskLevel());
            ps.setString(7, e.getFundingCategory());

            ps.executeUpdate();

            // ✅ set generated id back into object (if table uses auto_increment)
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    e.setId(keys.getInt(1));
                }
            }

            System.out.println("Evaluation added successfully!");
            return e;

        } catch (SQLException ex) {
            System.out.println("Error adding evaluation: " + ex.getMessage());
            return null;
        }
    }

    @Override
    public List<Evaluation> list() {
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
    public void delete(Evaluation e) {
        if (e == null) return;

        String query = "DELETE FROM fundingevaluation WHERE id=?";

        try (PreparedStatement ps = conx.prepareStatement(query)) {
            ps.setInt(1, e.getId());
            ps.executeUpdate();
            System.out.println("Evaluation deleted successfully!");
        } catch (SQLException ex) {
            System.out.println("Error deleting evaluation: " + ex.getMessage());
        }
    }

    // Optional helper if you still want delete by id sometimes
    public void deleteById(int id) {
        String query = "DELETE FROM fundingevaluation WHERE id=?";

        try (PreparedStatement ps = conx.prepareStatement(query)) {
            ps.setInt(1, id);
            ps.executeUpdate();
            System.out.println("Evaluation deleted successfully!");
        } catch (SQLException ex) {
            System.out.println("Error deleting evaluation: " + ex.getMessage());
        }
    }
    public List<Evaluation> listByApplicationId(int appId) {
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
    public double getAverageEvaluationScore() {
        java.util.List<Evaluation> evals = list();
        if (evals.isEmpty()) return 0;
        double sum = 0;
        for (Evaluation e : evals) {
            sum += e.getScore();
        }
        return sum / evals.size();
    }
    public java.util.Map<String, Integer> getEvaluationDecisionCounts() {
        java.util.Map<String, Integer> counts = new java.util.HashMap<>();
        for (Evaluation e : list()) {
            counts.put(e.getDecision(), counts.getOrDefault(e.getDecision(), 0) + 1);
        }
        return counts;
    }
}

