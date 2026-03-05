package tn.esprit.GUI;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import tn.esprit.Services.UserService;
import tn.esprit.entities.User;
import tn.esprit.utils.NavigationManager;
import tn.esprit.utils.SessionManager;

public class LoginController {

    @FXML
    private TextField emailField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Label msgLabel;

    private final UserService userService = new UserService();

    @FXML
    private void onLogin() {
        String email = emailField.getText().trim();
        String pass = passwordField.getText();

        if (email.isEmpty() || pass.isEmpty()) {
            msgLabel.setText("Please fill email and password");
            return;
        }

        User u = userService.login(email, pass);

        if (u == null) {
            msgLabel.setText("Invalid email or password");
            return;
        }

        String st = (u.getStatus() == null) ? "PENDING" : u.getStatus().trim();

        if (st.equalsIgnoreCase("BLOCKED")) {
            msgLabel.setText("Your account is blocked");
            return;
        }

        // ── Unified session login ──
        SessionManager.login(u);

        // ── Role-aware navigation via NavigationManager ──
        NavigationManager.goToDashboard(emailField);
    }

    @FXML
    private void onGoSignup() {
        NavigationManager.navigateTo(emailField, "/Signup.fxml");
    }

    @FXML
    private void onHome() {
        NavigationManager.navigateTo(emailField, "/Landing.fxml");
    }

    @FXML
    private void onLinkedin() {
    }

    @FXML
    private void onGoogle() {
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

                    // ── Unified session login ──
                    SessionManager.login(existing);

                    javafx.application.Platform.runLater(() -> NavigationManager.goToDashboard(emailField));

                    return;
                }

                // New Google user -> send to RoleChoice to pick role
                tn.esprit.utils.SignupSession.fullName = g.getName();
                tn.esprit.utils.SignupSession.email = g.getEmail();
                tn.esprit.utils.SignupSession.passwordPlain = null;

                javafx.application.Platform.runLater(() -> {
                    msgLabel.setText("Choose your role to complete signup.");
                    NavigationManager.navigateTo(emailField, "/RoleChoice.fxml");
                });

            } catch (Exception ex) {
                ex.printStackTrace();
                javafx.application.Platform.runLater(() -> msgLabel.setText("Google login error: " + ex.getMessage()));
            }
        }).start();
    }

    @FXML
    private void onOther() {
    }

    @FXML
    private void togglePassword() {
    }
}
