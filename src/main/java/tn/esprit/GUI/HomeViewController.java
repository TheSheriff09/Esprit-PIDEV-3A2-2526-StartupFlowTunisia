package tn.esprit.GUI;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.*;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import tn.esprit.Services.DashboardService;
import tn.esprit.utils.SessionManager;

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
        int uid = SessionManager.getUser().getId();
        boolean isMentor = (SessionManager.getUser() != null
                && "mentor".equalsIgnoreCase(SessionManager.getUser().getRole()));

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

    @FXML
    private Label aiInsightsText;
    @FXML
    private javafx.scene.layout.VBox secondaryModuleContainer;
    @FXML
    private Label secondaryModuleTitle;
    @FXML
    private javafx.scene.layout.VBox secondaryModuleContent;

    private void setupMentorDashboard(int mentorID) {
        extraStatTitle.setText("Completed Sessions");
        Map<String, Integer> stats = dashboardService.getSessionStatusStatsForMentor(mentorID);
        int completed = stats.getOrDefault("completed", 0);
        extraStatLabel.setText(String.valueOf(completed));

        java.util.List<tn.esprit.entities.Session> sessions = new tn.esprit.Services.SessionService()
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

        // Fetch AI Mentor Advice
        tn.esprit.Services.MentorEvaluationService evalService = new tn.esprit.Services.MentorEvaluationService();
        double avgRating = evalService.getAverageRating(mentorID);

        secondaryModuleTitle.setText("My Mentor Badges");
        secondaryModuleContent.getChildren().clear();
        String badge = avgRating >= 4.5 ? "🏅 Master Mentor"
                : avgRating >= 3.5 ? "🥈 Dedicated Guide" : avgRating == 0.0 ? "🆕 Rising Mentor" : "🥉 Contributor";

        Label badgeLabel = new Label(badge + " (Avg Rating: " + String.format("%.1f", avgRating) + "⭐)");
        badgeLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #12a059;");
        secondaryModuleContent.getChildren().add(badgeLabel);

        aiInsightsText.setText("Generating advice...");
        new Thread(() -> {
            tn.esprit.Services.GeminiService gemini = new tn.esprit.Services.GeminiService();
            String advice = gemini.getMentorAdvice(SessionManager.getUser().getFullName(), completed, avgRating);
            javafx.application.Platform.runLater(() -> aiInsightsText.setText(advice));
        }).start();
    }

    private void setupEntrepreneurDashboard(int entrepreneurID) {
        extraStatTitle.setText("Avg Feedback Score");
        Map<String, Double> stats = dashboardService.getAverageProgressByMentorForEntrepreneur(entrepreneurID);

        double totalAvg = stats.values().stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        extraStatLabel.setText(String.format("%.1f", totalAvg));

        java.util.List<tn.esprit.entities.Session> sessions = new tn.esprit.Services.SessionService()
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

        // Fetch AI Recommended Mentors
        aiInsightsText.setText("Finding the best mentors for your startups...");
        new Thread(() -> {
            tn.esprit.Services.GeminiService gemini = new tn.esprit.Services.GeminiService();
            // In a real scenario, this fetches actual DB context strings:
            String startupsContext = "Tech startups, marketing focus";
            String mentorsContext = "John Doe (Marketing), Jane Doe (Finance)";
            String recommendation = gemini.rankMentorsForEntrepreneur(SessionManager.getUser().getFullName(),
                    startupsContext, mentorsContext);
            javafx.application.Platform.runLater(() -> aiInsightsText.setText(recommendation));
        }).start();

        // Build Favorites Panel
        secondaryModuleTitle.setText("Favorite Mentors");
        secondaryModuleContent.getChildren().clear();

        tn.esprit.Services.FavoriteService favService = new tn.esprit.Services.FavoriteService();
        tn.esprit.Services.UserService userService = new tn.esprit.Services.UserService();
        java.util.List<tn.esprit.entities.Favorite> favs = favService.listByEntrepreneur(entrepreneurID);

        if (favs.isEmpty()) {
            secondaryModuleContent.getChildren().add(new Label("No mentors favorited yet."));
        } else {
            for (tn.esprit.entities.Favorite fav : favs) {
                tn.esprit.entities.User mentor = userService.getById(fav.getMentorID());
                if (mentor != null) {
                    javafx.scene.layout.HBox favRow = new javafx.scene.layout.HBox(10);
                    favRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                    Label nameLbl = new Label("⭐ " + mentor.getFullName());
                    nameLbl.setStyle("-fx-font-size: 14px; -fx-text-fill: #333;");

                    javafx.scene.control.Button btnBook = new javafx.scene.control.Button("Book Session");
                    btnBook.setStyle(
                            "-fx-background-color: #5a4bd6; -fx-text-fill: white; -fx-cursor: hand; -fx-font-size: 11px;");
                    btnBook.setOnAction(e -> {
                        // Routing to calendar logic here
                        tn.esprit.utils.NavigationManager.navigateTo(btnBook, "/fxml/ScheduleView.fxml");
                    });

                    javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
                    javafx.scene.layout.HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

                    favRow.getChildren().addAll(nameLbl, spacer, btnBook);
                    secondaryModuleContent.getChildren().add(favRow);
                }
            }
        }
    }
}
