package tn.esprit.gui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.*;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import tn.esprit.services.DashboardService;
import tn.esprit.utils.SessionContext;

import java.net.URL;
import java.util.Map;
import java.util.ResourceBundle;

public class HomeViewController implements Initializable {

    @FXML
    private Label totalSessionsLabel;
    @FXML
    private Label pendingBookingsLabel;
    @FXML
    private Label extraStatTitle;
    @FXML
    private Label extraStatLabel;
    @FXML
    private Label chartTitle;
    @FXML
    private StackPane chartContainer;

    private final DashboardService dashboardService = new DashboardService();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        refreshDashboard();
        startAutoRefresh();
    }

    private void startAutoRefresh() {
        java.util.concurrent.ScheduledExecutorService scheduler = java.util.concurrent.Executors
                .newSingleThreadScheduledExecutor(r -> {
                    Thread t = new Thread(r);
                    t.setDaemon(true);
                    return t;
                });
        scheduler.scheduleAtFixedRate(() -> {
            javafx.application.Platform.runLater(this::refreshDashboard);
        }, 15, 15, java.util.concurrent.TimeUnit.SECONDS);
    }

    private void refreshDashboard() {
        int uid = SessionContext.getUserId();
        boolean isMentor = SessionContext.isMentor();

        // 1. Basic Stats
        int total = dashboardService.getTotalSessions(uid, isMentor);
        int pending = dashboardService.getPendingBookings(uid, isMentor);

        totalSessionsLabel.setText(String.valueOf(total));
        pendingBookingsLabel.setText(String.valueOf(pending));

        chartContainer.getChildren().clear();

        // 2. Role-specific stats & charts
        if (isMentor) {
            setupMentorDashboard(uid);
        } else {
            setupEntrepreneurDashboard(uid);
        }
    }

    private void setupMentorDashboard(int mentorID) {
        extraStatTitle.setText("Completed Sessions");
        Map<String, Integer> stats = dashboardService.getSessionStatusStatsForMentor(mentorID);
        int completed = stats.getOrDefault("completed", 0);
        extraStatLabel.setText(String.valueOf(completed));

        java.util.List<tn.esprit.entities.Session> sessions = new tn.esprit.services.SessionService()
                .listByMentor(mentorID);
        double avgSuccess = sessions.stream().mapToDouble(tn.esprit.entities.Session::getSuccessProbability).average()
                .orElse(0.0);
        chartTitle.setText(String.format("Session Status Distribution (Avg Prediction: %.1f%%)", avgSuccess));

        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Status");
        yAxis.setLabel("Count");

        BarChart<String, Number> barChart = new BarChart<>(xAxis, yAxis);
        barChart.setLegendVisible(false);
        XYChart.Series<String, Number> series = new XYChart.Series<>();

        for (Map.Entry<String, Integer> entry : stats.entrySet()) {
            series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
        }

        barChart.getData().add(series);
        chartContainer.getChildren().add(barChart);
    }

    private void setupEntrepreneurDashboard(int entrepreneurID) {
        extraStatTitle.setText("Avg Feedback Score");
        Map<String, Double> stats = dashboardService.getAverageProgressByMentorForEntrepreneur(entrepreneurID);

        double totalAvg = stats.values().stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        extraStatLabel.setText(String.format("%.1f", totalAvg));

        java.util.List<tn.esprit.entities.Session> sessions = new tn.esprit.services.SessionService()
                .listByEvaluator(entrepreneurID);
        double avgSuccess = sessions.stream().mapToDouble(tn.esprit.entities.Session::getSuccessProbability).average()
                .orElse(0.0);
        chartTitle.setText(String.format("Progress by Mentor (Avg Prediction: %.1f%%)", avgSuccess));

        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
        for (Map.Entry<String, Double> entry : stats.entrySet()) {
            pieData.add(new PieChart.Data(entry.getKey(), entry.getValue()));
        }

        PieChart pieChart = new PieChart(pieData);
        pieChart.setLabelsVisible(true);
        chartContainer.getChildren().add(pieChart);
    }
}
