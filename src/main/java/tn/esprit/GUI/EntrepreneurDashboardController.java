package tn.esprit.GUI;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import tn.esprit.entities.User;
import tn.esprit.utils.CurrentUserSession;
import tn.esprit.utils.NavContext;



public class EntrepreneurDashboardController {

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
            statusValueLabel.setText("Pending");
            statusValueLabel.getStyleClass().removeAll("stApproved","stPending","stBlocked");
            statusValueLabel.getStyleClass().add("stPending");
            miLogout.getStyleClass().add("logoutItem");
            return;
        }

        String fullName = (u.getFullName() == null) ? "User" : u.getFullName().trim();
        userMenuBtn.setText(fullName);
        miHeader.setText(fullName);

        String dbStatus = (u.getStatus() == null) ? "PENDING" : u.getStatus().trim();

        statusValueLabel.getStyleClass().removeAll("stApproved","stPending","stBlocked");

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

        miLogout.getStyleClass().add("logoutItem");
    }

    @FXML private void onDashboard() {    goTo("/EntrepreneurDashboard.fxml");
    }
    @FXML private void onStartup() {}
    @FXML private void onMentorship() {}
    @FXML private void onFunding() {}


    @FXML private void onManageProfile() {
        NavContext.setBack("/EntrepreneurDashboard.fxml");
        goTo("/ManageProfile.fxml");
    }
    private void goTo(String fxmlPath) {
        try {
            java.net.URL url = getClass().getResource(fxmlPath);
            System.out.println("FXML URL for " + fxmlPath + " => " + url);

            if (url == null) {
                System.out.println("❌ NOT FOUND: " + fxmlPath);
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

    @FXML private void onStartups() { System.out.println("Startups"); }
    @FXML private void onSettings() { System.out.println("Settings"); }

    @FXML private void onFindMentor() { goTo("/FindMentor.fxml"); }
    @FXML private void onSwot() {         goTo("/swot.fxml");
    }
@FXML private void onFourm(){}
    @FXML private void onSupport() {
        NavContext.setBack("/EntrepreneurDashboard.fxml");
        goTo("/ReclamationEntrepreneur.fxml");
    }
    @FXML
    private void onLogout() {
        CurrentUserSession.user = null;
        goTo("/Signup.fxml");
}

}