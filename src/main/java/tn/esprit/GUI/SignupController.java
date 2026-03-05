package tn.esprit.GUI;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import tn.esprit.Services.UserService;
import tn.esprit.entities.User;
import tn.esprit.utils.NavigationManager;
import tn.esprit.utils.SessionManager;
import tn.esprit.utils.SignupSession;
import javafx.scene.control.Label;

public class SignupController {
    @FXML
    private Label msgLabel;
    @FXML
    private TextField fullNameField;
    @FXML
    private TextField emailField;
    @FXML
    private PasswordField passwordField;
    // @FXML private Label msgLabel;
    @FXML
    private Button togglePassBtn;
    @FXML
    private TextField visiblePasswordField;
    private boolean showingPassword = false;

    @FXML
    private void register() {
        String fullName = fullNameField.getText();
        String email = emailField.getText();
        String pass = passwordField.getText();
        String password = showingPassword
                ? visiblePasswordField.getText()
                : passwordField.getText();
        if (fullName.isEmpty() || email.isEmpty() || password.isEmpty()) {
            System.out.println("Please fill all fields");
            return;
        }

        SignupSession.fullName = fullName;
        SignupSession.email = email;
        SignupSession.passwordPlain = password;
        NavigationManager.navigateTo(fullNameField, "/RoleChoice.fxml");
    }

    @FXML
    private void togglePassword() {
        if (showingPassword) {
            passwordField.setText(visiblePasswordField.getText());

            passwordField.setVisible(true);
            passwordField.setManaged(true);

            visiblePasswordField.setVisible(false);
            visiblePasswordField.setManaged(false);

            togglePassBtn.setText("👁");

            showingPassword = false;

        } else {
            visiblePasswordField.setText(passwordField.getText());

            visiblePasswordField.setVisible(true);
            visiblePasswordField.setManaged(true);

            passwordField.setVisible(false);
            passwordField.setManaged(false);

            togglePassBtn.setText("🙈");

            showingPassword = true;
        }
    }

    @FXML
    private void socialFacebook() {
    }

    @FXML
    private void onGoogle2() {
        msgLabel.setText("Opening Google login...");

        new Thread(() -> {
            try {
                tn.esprit.auth.GoogleOAuthService svc = new tn.esprit.auth.GoogleOAuthService();
                tn.esprit.auth.GoogleOAuthService.GoogleUserInfo g = svc.loginAndGetUserInfo();

                UserService us = new UserService();
                User existing = us.getByEmail(g.getEmail());

                if (existing != null) {
                    String st = (existing.getStatus() == null) ? "PENDING" : existing.getStatus().trim();
                    if (st.equalsIgnoreCase("BLOCKED")) {
                        javafx.application.Platform.runLater(() -> msgLabel.setText("Your account is blocked"));
                        return;
                    }

                    SessionManager.login(existing);

                    javafx.application.Platform.runLater(() -> NavigationManager.goToDashboard(fullNameField));

                    return;
                }

                // New Google user -> send to RoleChoice to pick role
                tn.esprit.utils.SignupSession.fullName = g.getName();
                tn.esprit.utils.SignupSession.email = g.getEmail();
                tn.esprit.utils.SignupSession.passwordPlain = null;

                javafx.application.Platform.runLater(() -> {
                    msgLabel.setText("Choose your role to complete signup.");
                    NavigationManager.navigateTo(fullNameField, "/RoleChoice.fxml");
                });

            } catch (Exception ex) {
                ex.printStackTrace();
                javafx.application.Platform.runLater(() -> msgLabel.setText("Google login error: " + ex.getMessage()));
            }
        }).start();
    }

    @FXML
    private void socialGithub() {
    }

    @FXML
    private void goLogin() {
        NavigationManager.navigateTo(fullNameField, "/Login.fxml");
    }

    @FXML
    private void onHome() {
        NavigationManager.navigateTo(fullNameField, "/Landing.fxml");
    }
}
