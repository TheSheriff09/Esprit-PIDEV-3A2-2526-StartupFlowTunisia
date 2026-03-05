package tn.esprit.GUI;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import tn.esprit.Services.ApplicationService;
import tn.esprit.Services.EvaluationService;
import tn.esprit.utils.NavigationManager;

import java.net.URL;
import java.text.DecimalFormat;
import java.util.Map;
import java.util.ResourceBundle;

public class AdminFundingDashboardController implements Initializable {

    @FXML
    private PieChart piechartApplications;
    @FXML
    private PieChart piechartEvaluations;
    @FXML
    private Label lblTotalApplications;
    @FXML
    private Label lblTotalFunding;
    @FXML
    private Label lblTotalEvaluations;
    @FXML
    private Label lblAvgScore;
    @FXML
    private Button btnDashboard;

    private final ApplicationService applicationService = new ApplicationService();
    private final EvaluationService evaluationService = new EvaluationService();
    private final DecimalFormat df = new DecimalFormat("#,##0.00");

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        loadApplicationStats();
        loadEvaluationStats();

        double totalRequested = applicationService.getTotalFundingRequested();
        if (lblTotalFunding != null) {
            lblTotalFunding.setText("$" + df.format(totalRequested));
        }

        double avgScore = evaluationService.getAverageEvaluationScore();
        if (lblAvgScore != null) {
            lblAvgScore.setText(df.format(avgScore));
        }
    }

    private void loadApplicationStats() {
        Map<String, Integer> stats = applicationService.getApplicationStatusCounts();

        int total = 0;
        ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList();

        for (Map.Entry<String, Integer> entry : stats.entrySet()) {
            pieChartData.add(new PieChart.Data(entry.getKey(), entry.getValue()));
            total += entry.getValue();
        }

        if (piechartApplications != null) {
            piechartApplications.setData(pieChartData);
        }
        if (lblTotalApplications != null) {
            lblTotalApplications.setText(String.valueOf(total));
        }
    }

    private void loadEvaluationStats() {
        Map<String, Integer> stats = evaluationService.getEvaluationDecisionCounts();

        int total = 0;
        ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList();

        for (Map.Entry<String, Integer> entry : stats.entrySet()) {
            pieChartData.add(new PieChart.Data(entry.getKey(), entry.getValue()));
            total += entry.getValue();
        }

        if (piechartEvaluations != null) {
            piechartEvaluations.setData(pieChartData);
        }
        if (lblTotalEvaluations != null) {
            lblTotalEvaluations.setText(String.valueOf(total));
        }
    }

    // --- Sidebar Navigation ---
    @FXML
    private void goDashboard() {
        NavigationManager.navigateTo(btnDashboard, "/DashboardAdmin.fxml");
    }

    @FXML
    private void goUsers() {
        NavigationManager.navigateTo(btnDashboard, "/UserManagement.fxml");
    }

    @FXML
    private void goProjects() {
        NavigationManager.navigateTo(btnDashboard, "/admindashboard.fxml");
    }

    @FXML
    private void goMentorship() {
        System.out.println("Mentorship feature is not connected yet.");
    }

    @FXML
    private void goFunding() {
        NavigationManager.navigateTo(btnDashboard, "/AdminFundingDashboard.fxml");
    }

    @FXML
    private void goDashboardForum() {
        tn.esprit.GUI.ForumAdminController.showAnalyticsOnLoad = true;
        NavigationManager.navigateTo(btnDashboard, "/ForumAdmin.fxml");
    }

    @FXML
    private void goForumBackOffice() {
        tn.esprit.GUI.ForumAdminController.showAnalyticsOnLoad = false;
        NavigationManager.navigateTo(btnDashboard, "/ForumAdmin.fxml");
    }

    @FXML
    private void goSettings() {
        System.out.println("Settings feature is not connected yet.");
    }

    @FXML
    private void logout() {
        NavigationManager.logout(btnDashboard);
    }
}
