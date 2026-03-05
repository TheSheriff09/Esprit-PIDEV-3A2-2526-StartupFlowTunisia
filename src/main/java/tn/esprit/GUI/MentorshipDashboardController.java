package tn.esprit.GUI;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import tn.esprit.entities.User;
import tn.esprit.utils.SessionManager;
import tn.esprit.utils.ThemeManager;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * MentorshipDashboardController — Shell controller for the main layout.
 *
 * <p>
 * Changes from previous version:
 * <ul>
 * <li>Added {@link #showAI()} to navigate to the AI Insights view.</li>
 * <li>Added {@link #onToggleDarkMode()} wired to {@link ThemeManager}.</li>
 * <li>Dark mode icon updates live when toggled.</li>
 * </ul>
 */
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
    @FXML
    private Button btnAI;
    @FXML
    private Button btnDarkMode;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        User u = SessionManager.getUser();
        if (u != null) {
            userPill.setText(u.getFullName());
            roleBadge.setText(u.getRole().toUpperCase());
            roleBadge.setStyle(roleStyle(u.getRole()));

            // Role-based button visibility
            if ((SessionManager.getUser() != null && "entrepreneur".equals(SessionManager.getUser().getRole()))) {
                btnSchedule.setVisible(false);
                btnSchedule.setManaged(false);
            }
        }

        // Apply persisted theme on load
        syncDarkModeIcon();

        showHome();
    }

    // ── Navigation ─────────────────────────────────────────────────────────

    @FXML
    public void showHome() {
        setActive(btnHome);
        topTitle.setText("Mentorship — Overview");
        loadView("/fxml/HomeView.fxml");
    }

    @FXML
    public void showSchedule() {
        if (!(SessionManager.getUser() != null && "mentor".equalsIgnoreCase(SessionManager.getUser().getRole())))
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

    /**
     * Navigate to the new AI Insights view (Grok + AnalyticsService).
     * Available to all roles.
     */
    @FXML
    public void showAI() {
        setActive(btnAI);
        topTitle.setText("Mentorship — AI Insights ✨");
        loadView("/fxml/AiInsightsView.fxml");
    }

    // ── Dark Mode Toggle ───────────────────────────────────────────────────

    /**
     * Toggle dark mode and immediately re-apply theme to all open scenes.
     * ThemeManager persists the preference to config.properties.
     */
    @FXML
    public void onToggleDarkMode() {
        ThemeManager tm = ThemeManager.getInstance();
        tm.toggle();

        // Apply to the current scene
        Scene scene = mainContent.getScene();
        if (scene != null)
            tm.applyTo(scene);

        syncDarkModeIcon();
    }

    private void syncDarkModeIcon() {
        if (btnDarkMode == null)
            return;
        boolean dark = ThemeManager.getInstance().isDark();
        btnDarkMode.setText(dark ? "☀" : "🌙");
        btnDarkMode.setAccessibleText(dark ? "Switch to Light Mode" : "Switch to Dark Mode");
    }

    // ── Logout ─────────────────────────────────────────────────────────────

    @FXML
    public void onBackToDashboard() {
        tn.esprit.utils.NavigationManager.goToDashboard(mainContent);
    }

    @FXML
    public void onLogout() {
        tn.esprit.utils.NavigationManager.logout(mainContent);
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private void loadView(String fxml) {
        try {
            Node node = FXMLLoader.load(getClass().getResource(fxml));
            mainContent.getChildren().setAll(node);
            // Apply theme to the newly loaded sub-view
            ThemeManager.getInstance().applyTo(mainContent.getScene());
        } catch (IOException e) {
            System.err.println("Cannot load view " + fxml + ": " + e.getMessage());
        }
    }

    private void setActive(Button active) {
        for (Button b : new Button[] { btnHome, btnSchedule, btnBookings, btnSessions, btnFeedback, btnAI }) {
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
            case "evaluator", "entrepreneur" ->
                "-fx-background-color:#f0ecff;-fx-text-fill:#7357ff;-fx-background-radius:999;";
            default -> "-fx-background-color:#f7f5ff;-fx-text-fill:#5a4bd6;-fx-background-radius:999;";
        };
    }
}
