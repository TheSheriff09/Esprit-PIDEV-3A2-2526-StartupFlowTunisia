package tn.esprit.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import tn.esprit.service.StatsService;

public class DashboardController {

    @FXML private Label lblTotalApps;
    @FXML private Label lblTotalEvals;
    @FXML private Label lblAvgScore;
    @FXML private Label lblTotalAmount;

    private final StatsService statsService = new StatsService();

    @FXML
    public void initialize() {
        loadKpis();
    }

    @FXML
    private void refresh() {
        loadKpis();
    }

    private void loadKpis() {
        int totalApps = statsService.getApplicationsCount();
        int totalEvals = statsService.getEvaluationsCount();
        double avgScore = statsService.getAverageScore();
        double totalAmount = statsService.getTotalFundingAmount();

        lblTotalApps.setText("Total Applications: " + totalApps);
        lblTotalEvals.setText("Total Evaluations: " + totalEvals);
        lblAvgScore.setText(String.format("Average Score: %.2f", avgScore));
        lblTotalAmount.setText(String.format("Total Funding Amount: %.2f", totalAmount));
    }

    @FXML
    private void goToApplications(ActionEvent event) {
        switchScene(event, "/gui/application.fxml", "Application CRUD");
    }

    @FXML
    private void goToEvaluations(ActionEvent event) {
        switchScene(event, "/gui/evaluation.fxml", "Evaluation CRUD");
    }

    @FXML
    private void goToStats(ActionEvent event) {
        switchScene(event, "/gui/stats.fxml", "Statistics Dashboard");
    }

    private void switchScene(ActionEvent event, String fxmlPath, String title) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setTitle(title);
            stage.setScene(new Scene(root, 1300, 850));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Cannot open page: " + e.getMessage());
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}