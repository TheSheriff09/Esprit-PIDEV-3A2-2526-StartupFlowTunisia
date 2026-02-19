package tn.esprit.gui;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import tn.esprit.entities.User;
import tn.esprit.utils.SessionContext;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class MentorshipDashboardController implements Initializable {

    @FXML
    private StackPane mainContent;
    @FXML
    private Label topTitle;
    @FXML
    private Label userPill;
    @FXML
    private Label roleBadge;

    @FXML
    private Button btnHome;
    @FXML
    private Button btnSchedule;
    @FXML
    private Button btnBookings;
    @FXML
    private Button btnSessions;
    @FXML
    private Button btnFeedback;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        User u = SessionContext.getUser();
        if (u != null) {
            userPill.setText(u.getFullName());
            roleBadge.setText(u.getRole().toUpperCase());
            roleBadge.setStyle(roleStyle(u.getRole()));

            // Role-based button visibility
            if (SessionContext.isEntrepreneur()) {
                // Entrepreneurs don't manage slots, they book them
                btnSchedule.setVisible(false);
                btnSchedule.setManaged(false);
            }
        }
        showHome(); // Default view is now Overview
    }

    @FXML
    public void showHome() {
        setActive(btnHome);
        topTitle.setText("Mentorship — Overview");
        loadView("/fxml/HomeView.fxml");
    }

    @FXML
    public void showSchedule() {
        if (!SessionContext.isMentor())
            return;
        setActive(btnSchedule);
        topTitle.setText("Mentorship — Schedule");
        loadView("/fxml/ScheduleView.fxml");
    }

    @FXML
    public void showBookings() {
        setActive(btnBookings);
        topTitle.setText("Mentorship — Booking Requests");
        loadView("/fxml/BookingView.fxml");
    }

    @FXML
    public void showSessions() {
        setActive(btnSessions);
        topTitle.setText("Mentorship — Sessions");
        loadView("/fxml/SessionView.fxml");
    }

    @FXML
    public void showFeedback() {
        setActive(btnFeedback);
        topTitle.setText("Mentorship — Feedback");
        loadView("/fxml/FeedbackView.fxml");
    }

    @FXML
    public void onLogout() {
        SessionContext.clear();
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/LoginView.fxml"));
            Stage stage = (Stage) mainContent.getScene().getWindow();
            stage.setScene(new Scene(root, 1280, 720));
            stage.setTitle("StartupFlow Tunisia — Login");
        } catch (IOException e) {
            System.err.println("Logout error: " + e.getMessage());
        }
    }

    private void loadView(String fxml) {
        try {
            Node node = FXMLLoader.load(getClass().getResource(fxml));
            mainContent.getChildren().setAll(node);
        } catch (IOException e) {
            System.err.println("Cannot load view " + fxml + ": " + e.getMessage());
        }
    }

    private void setActive(Button active) {
        for (Button b : new Button[] { btnHome, btnSchedule, btnBookings, btnSessions, btnFeedback }) {
            if (b != null)
                b.getStyleClass().remove("active");
        }
        if (active != null)
            active.getStyleClass().add("active");
    }

    private String roleStyle(String role) {
        if (role == null)
            return "";
        return switch (role.toLowerCase()) {
            case "mentor" -> "-fx-background-color:#e8faf2;-fx-text-fill:#12a059;-fx-background-radius:999;";
            case "evaluator",
                    "entrepreneur" ->
                "-fx-background-color:#f0ecff;-fx-text-fill:#7357ff;-fx-background-radius:999;";
            default -> "-fx-background-color:#f7f5ff;-fx-text-fill:#5a4bd6;-fx-background-radius:999;";
        };
    }
}
