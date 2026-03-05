package tn.esprit.GUI;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.input.MouseEvent;
import tn.esprit.entities.User;
import tn.esprit.utils.NavigationManager;
import tn.esprit.utils.NavContext;
import tn.esprit.utils.SessionManager;
import tn.esprit.utils.TopBarHelper;

public class EvaluatorDashboardController {

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
        NavigationManager.navigateTo(userMenuBtn, "/EvaluatorDashboard.fxml");
    }

    @FXML
    private void onFunding(MouseEvent event) {
        NavigationManager.navigateTo((Node) event.getSource(), "/evaluator_applications.fxml");
    }

    @FXML
    private void onFunding() {
        NavigationManager.navigateTo(userMenuBtn, "/evaluation.fxml");
    }

    @FXML
    private void onSupport() {
        NavContext.setBack("/EvaluatorDashboard.fxml");
        NavigationManager.navigateTo(userMenuBtn, "/ReclamationEntrepreneur.fxml");
    }

    @FXML
    private void onAssignedApplications() {
        System.out.println("Assigned applications page later");
    }

    @FXML
    private void onSubmitEvaluation() {
        System.out.println("Submit evaluation page later");
    }

    @FXML
    private void onForum() {
        NavigationManager.navigateTo(userMenuBtn, "/ForumFeed.fxml");
    }

    @FXML
    private void onManageProfile() {
        NavContext.setBack("/EvaluatorDashboard.fxml");
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
