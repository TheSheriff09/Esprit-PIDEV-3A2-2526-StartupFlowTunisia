package tn.esprit.GUI;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.stage.Stage;
import tn.esprit.entities.User;
import tn.esprit.utils.CurrentUserSession;
import tn.esprit.utils.NavContext;



public class MentorDashboardController {

    @FXML private MenuButton userMenuBtn;
    @FXML private MenuItem miHeader;
    @FXML private MenuItem miLogout;
    @FXML private Label statusValueLabel;

    @FXML
    private void initialize() {
        User u = CurrentUserSession.user;

        if (u == null) {
            userMenuBtn.setText("User");
            miHeader.setText("USER");
            setStatus("PENDING");
            miLogout.getStyleClass().add("logoutItem");
            return;
        }

        String fullName = (u.getFullName() == null) ? "User" : u.getFullName().trim();
        userMenuBtn.setText(fullName);
        miHeader.setText(fullName);

        String dbStatus = (u.getStatus() == null) ? "PENDING" : u.getStatus().trim();
        setStatus(dbStatus);

        miLogout.getStyleClass().add("logoutItem");
    }

    private void setStatus(String dbStatus) {
        statusValueLabel.getStyleClass().removeAll("stApproved","stPending","stBlocked");

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

    @FXML private void onDashboard() { goTo("/MentorDashboard.fxml"); }
    @FXML private void onMentorship() { System.out.println("Mentorship page later"); }
    @FXML private void onSupport() {
        NavContext.setBack("/MentorDashboard.fxml");
        goTo("/ReclamationEntrepreneur.fxml");
    }
    @FXML private void onForum(){}
    @FXML private void onAssignedStartups() { System.out.println("Assigned startups page later"); }
    @FXML private void onSessions() { System.out.println("Sessions page later"); }

    @FXML private void onManageProfile() {
        NavContext.setBack("/MentorDashboard.fxml");
        goTo("/ManageProfile.fxml");
    }
    @FXML private void onSettings() { System.out.println("Settings page later"); }

    @FXML
    private void onLogout() {
        CurrentUserSession.user = null;
        goTo("/Signup.fxml");
    }

    private void goTo(String fxmlPath) {
        try {
            java.net.URL url = getClass().getResource(fxmlPath);
            if (url == null) {
                System.out.println(" NOT FOUND: " + fxmlPath);
                return;
            }
            Parent root = FXMLLoader.load(url);
            Stage stage = (Stage) userMenuBtn.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}