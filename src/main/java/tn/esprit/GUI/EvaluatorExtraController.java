package tn.esprit.GUI;

import javafx.fxml.FXML;
import tn.esprit.entities.User;
import tn.esprit.Services.UserService;
import tn.esprit.utils.SignupSession;

public class EvaluatorExtraController {

    @FXML
    private javafx.scene.control.TextField levelField;

    @FXML
    private void finishEvaluator() {

        String level = levelField.getText();

        if (level.isEmpty())
            return;

        UserService us = new UserService();

        User evaluator = new User(
                SignupSession.fullName,
                SignupSession.email,
                (SignupSession.passwordPlain == null ? null : SignupSession.passwordPlain), "EVALUATOR",
                "ACTIVE",
                null,
                level);

        User created = us.add(evaluator);
        if (created == null) {
            System.out.println("Failed to create evaluator user.");
            return;

        }
        tn.esprit.utils.SessionManager.login(created);
        new Thread(() -> {
            tn.esprit.Services.EmailService mail = new tn.esprit.Services.EmailService();
            mail.sendWelcomeEmail(created.getEmail(), created.getFullName(), "http://localhost:8080/startupflow/login");
        }).start();
        tn.esprit.utils.NavigationManager.navigateTo(levelField, "/Login.fxml");
    }
}
