package tn.esprit.GUI;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import tn.esprit.entities.User;
import tn.esprit.utils.NavigationManager;
import tn.esprit.utils.NavContext;
import tn.esprit.utils.SessionManager;
import tn.esprit.utils.TopBarHelper;

public class EntrepreneurDashboardController {

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

        statusValueLabel.getStyleClass().removeAll("stApproved", "stPending", "stBlocked");

        if (dbStatus.equalsIgnoreCase("ACTIVE")) {
            statusValueLabel.setText("Approved");
            statusValueLabel.getStyleClass().add("stApproved");
        } else if (dbStatus.equalsIgnoreCase("PENDING")) {
            statusValueLabel.setText("Pending");
            statusValueLabel.getStyleClass().add("stPending");
        } else if (dbStatus.equalsIgnoreCase("BLOCKED")) {
            statusValueLabel.setText("Blocked");
            statusValueLabel.getStyleClass().add("stBlocked");
        } else {
            statusValueLabel.setText(dbStatus);
            statusValueLabel.getStyleClass().add("stPending");
        }
    }

    @FXML
    private void onFunding(MouseEvent event) {
        System.out.println("Navigating to Funding");

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/application.fxml"));
            Parent root = loader.load();

            User u = SessionManager.getUser();
            if (u == null) {
                System.out.println("❌ No logged-in user in session.");
            }

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onDashboard() {
        NavigationManager.navigateTo(userMenuBtn, "/EntrepreneurDashboard.fxml");
    }

    @FXML
    private void onStartup() {
        NavigationManager.navigateTo(userMenuBtn, "/startupview.fxml");
    }

    @FXML
    private void onMentorship() {
        NavigationManager.navigateTo(userMenuBtn, "/fxml/MentorshipDashboard.fxml");
    }

    @FXML
    private void onManageProfile() {
        NavContext.setBack("/EntrepreneurDashboard.fxml");
        NavigationManager.navigateTo(userMenuBtn, "/ManageProfile.fxml");
    }

    @FXML
    private void onStartups() {
        NavigationManager.navigateTo(userMenuBtn, "/startupview.fxml");
    }

    @FXML
    private void onSettings() {
        System.out.println("Settings");
    }

    @FXML
    private void onFindMentor() {
        NavigationManager.navigateTo(userMenuBtn, "/FindMentor.fxml");
    }

    @FXML
    private void onSwot() {
        NavigationManager.navigateTo(userMenuBtn, "/swot.fxml");
    }

    @FXML
    private void onFourm() {
        NavigationManager.navigateTo(userMenuBtn, "/ForumFeed.fxml");
    }

    @FXML
    private void onSupport() {
        NavContext.setBack("/EntrepreneurDashboard.fxml");
        NavigationManager.navigateTo(userMenuBtn, "/ReclamationEntrepreneur.fxml");
    }

    @FXML
    private void onLogout() {
        NavigationManager.logout(userMenuBtn);
    }
}
