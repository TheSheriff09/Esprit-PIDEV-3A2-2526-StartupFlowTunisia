package tn.esprit.GUI;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import tn.esprit.entities.User;
import tn.esprit.utils.NavigationManager;
import tn.esprit.utils.NavContext;
import tn.esprit.utils.SessionManager;
import tn.esprit.utils.TopBarHelper;

public class MentorDashboardController {

    @FXML
    private MenuButton userMenuBtn;
    @FXML
    private Label statusValueLabel;

    @FXML
    private void initialize() {
        // ── Session guard ──
        if (!SessionManager.requireLogin(userMenuBtn))
            return;

        // ── Top bar setup (reusable) ──
        TopBarHelper.setup(userMenuBtn, null, userMenuBtn);

        // ── Status display ──
        User u = SessionManager.getUser();
        String dbStatus = (u.getStatus() == null) ? "PENDING" : u.getStatus().trim();
        setStatus(dbStatus);
    }

    private void setStatus(String dbStatus) {
        statusValueLabel.getStyleClass().removeAll("stApproved", "stPending", "stBlocked");

        if ("ACTIVE".equalsIgnoreCase(dbStatus)) {
            statusValueLabel.setText("Approved");
            statusValueLabel.getStyleClass().add("stApproved");
        } else if ("PENDING".equalsIgnoreCase(dbStatus)) {
            statusValueLabel.setText("Pending");
            statusValueLabel.getStyleClass().add("stPending");
        } else if ("BLOCKED".equalsIgnoreCase(dbStatus)) {
            statusValueLabel.setText("Blocked");
            statusValueLabel.getStyleClass().add("stBlocked");
        } else {
            statusValueLabel.setText(dbStatus);
            statusValueLabel.getStyleClass().add("stPending");
        }
    }

    @FXML
    private void onDashboard() {
        NavigationManager.navigateTo(userMenuBtn, "/MentorDashboard.fxml");
    }

    @FXML
    private void onMentorship() {
        NavigationManager.navigateTo(userMenuBtn, "/fxml/MentorshipDashboard.fxml");
    }

    @FXML
    private void onSupport() {
        NavContext.setBack("/MentorDashboard.fxml");
        NavigationManager.navigateTo(userMenuBtn, "/ReclamationEntrepreneur.fxml");
    }

    @FXML
    private void onForum() {
        NavigationManager.navigateTo(userMenuBtn, "/ForumFeed.fxml");
    }

    @FXML
    private void onAssignedStartups() {
        System.out.println("Assigned startups page later");
    }

    @FXML
    private void onSessions() {
        System.out.println("Sessions page later");
    }

    @FXML
    private void onManageProfile() {
        NavContext.setBack("/MentorDashboard.fxml");
        NavigationManager.navigateTo(userMenuBtn, "/ManageProfile.fxml");
    }

    @FXML
    private void onSettings() {
        System.out.println("Settings page later");
    }

    @FXML
    private void onLogout() {
        NavigationManager.logout(userMenuBtn);
    }
}
