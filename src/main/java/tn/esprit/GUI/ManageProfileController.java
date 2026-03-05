package tn.esprit.GUI;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import tn.esprit.Services.UserService;
import tn.esprit.entities.User;
import tn.esprit.utils.NavigationManager;
import tn.esprit.utils.NavContext;
import tn.esprit.utils.SessionManager;
import tn.esprit.utils.TopBarHelper;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;

public class ManageProfileController {

    @FXML
    private TextField fullNameField;
    @FXML
    private TextField emailField;

    @FXML
    private PasswordField oldPasswordField;
    @FXML
    private PasswordField newPasswordField;

    @FXML
    private TextField roleField;
    @FXML
    private TextField createdAtField;

    @FXML
    private Label msgLabel;
    @FXML
    private MenuButton userMenuBtn;

    private final UserService userService = new UserService();

    @FXML
    private void initialize() {
        // ── Session guard ──
        if (!SessionManager.requireLogin(fullNameField))
            return;

        // ── Top bar setup (reusable) ──
        TopBarHelper.setup(userMenuBtn, null, fullNameField);

        User sessionUser = SessionManager.getUser();
        User u = userService.getById(sessionUser.getId());
        if (u == null) {
            msgLabel.setText("User not found.");
            return;
        }

        SessionManager.login(u); // refresh session

        fullNameField.setText(u.getFullName() == null ? "" : u.getFullName());
        emailField.setText(u.getEmail() == null ? "" : u.getEmail());

        oldPasswordField.clear();
        newPasswordField.clear();

        roleField.setText(u.getRole() == null ? "" : u.getRole());
        createdAtField.setText(formatTs(u.getCreatedAt()));

        roleField.setEditable(false);
        createdAtField.setEditable(false);
        roleField.setDisable(true);
        createdAtField.setDisable(true);
    }

    @FXML
    private void onSave() {
        User u = SessionManager.getUser();
        if (u == null)
            return;

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

        msgLabel.setText("Updated successfully ✅");

        User refreshed = userService.getById(u.getId());
        if (refreshed != null)
            SessionManager.login(refreshed);

        oldPasswordField.clear();
        newPasswordField.clear();
    }

    @FXML
    private void goBack() {
        NavigationManager.navigateTo(fullNameField, NavContext.backFxml);
    }

    @FXML
    private void onDashboard() {
        NavigationManager.goToDashboard(fullNameField);
    }

    @FXML
    private void onStartups() {
        NavigationManager.navigateTo(fullNameField, "/startupview.fxml");
    }

    @FXML
    private void onSettings() {
        System.out.println("Settings");
    }

    @FXML
    private void onLogout() {
        NavigationManager.logout(fullNameField);
    }

    private String formatTs(Timestamp ts) {
        if (ts == null)
            return "";
        return new SimpleDateFormat("yyyy-MM-dd HH:mm").format(ts);
    }
}
