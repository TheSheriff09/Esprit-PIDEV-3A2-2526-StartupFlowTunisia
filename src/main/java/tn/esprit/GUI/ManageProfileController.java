package tn.esprit.GUI;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import tn.esprit.Services.UserService;
import tn.esprit.entities.User;
import tn.esprit.utils.CurrentUserSession;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import tn.esprit.utils.NavContext;


public class ManageProfileController {

    @FXML private TextField fullNameField;
    @FXML private TextField emailField;

    @FXML private PasswordField oldPasswordField;
    @FXML private PasswordField newPasswordField;

    @FXML private TextField roleField;
    @FXML private TextField createdAtField;

    @FXML private Label msgLabel;
    @FXML private MenuButton userMenuBtn;
    @FXML private MenuItem miHeader;
    private final UserService userService = new UserService();

    @FXML
    private void initialize() {
        User sessionUser = CurrentUserSession.user;
        if (sessionUser == null) {
            msgLabel.setText("No user session.");
            return;
        }
        String fullName = (sessionUser.getFullName() == null) ? "User" : sessionUser.getFullName().trim();
        userMenuBtn.setText(fullName);
        miHeader.setText(fullName);
        User u = userService.getById(sessionUser.getId());
        if (u == null) {
            msgLabel.setText("User not found.");
            return;
        }

        CurrentUserSession.user = u;

        fullNameField.setText(u.getFullName() == null ? "" : u.getFullName());
        emailField.setText(u.getEmail() == null ? "" : u.getEmail());

        oldPasswordField.clear();
        newPasswordField.clear();

        // Fill read-only fields
        roleField.setText(u.getRole() == null ? "" : u.getRole());

        createdAtField.setText(formatTs(u.getCreatedAt()));

        roleField.setEditable(false);
        createdAtField.setEditable(false);
        roleField.setDisable(true);
        createdAtField.setDisable(true);
    }

    @FXML
    private void onSave() {
        User u = CurrentUserSession.user;
        if (u == null) return;

        String newName = fullNameField.getText();
        String newEmail = emailField.getText();

        String oldPass = oldPasswordField.getText();
        String newPass = newPasswordField.getText();
        if (newPass == null || newPass.trim().isEmpty()) {
            newPass = null;
            oldPass = null;
        }

        if (newName == null || newName.trim().isEmpty() || newEmail == null || newEmail.trim().isEmpty()) {
            msgLabel.setText("Full name and email are required.");
            return;
        }

        boolean wantsPasswordChange = newPass != null && !newPass.trim().isEmpty();
        if (wantsPasswordChange && (oldPass == null || oldPass.trim().isEmpty())) {
            msgLabel.setText("Enter your old password to set a new password.");
            return;
        }

        boolean ok = userService.updateProfile(u.getId(), newName, newEmail, oldPass, newPass);

        if (!ok) {
            msgLabel.setText("Update failed. Check old password or email already used.");
            return;
        }

        msgLabel.setText("Updated successfully ");

        User refreshed = userService.getById(u.getId());
        if (refreshed != null) CurrentUserSession.user = refreshed;

        oldPasswordField.clear();
        newPasswordField.clear();
    }

    @FXML

    private void goBack() {
        goTo(NavContext.backFxml);
    }
    @FXML
    private void onDashboard() {
        goTo("/EntrepreneurDashboard.fxml");
    }
    @FXML private void onStartups() { System.out.println("Startups"); }
    @FXML private void onSettings() { System.out.println("Settings"); }
    private void goTo(String fxmlPath) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            Stage stage = (Stage) fullNameField.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            msgLabel.setText("Navigation error: " + e.getMessage());
        }
    }

    private String formatTs(Timestamp ts) {
        if (ts == null) return "";
        return new SimpleDateFormat("yyyy-MM-dd HH:mm").format(ts);
    }
    @FXML
    private void onLogout() {
        CurrentUserSession.user = null;
        goTo("/Signup.fxml");
    }
}