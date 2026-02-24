package tn.esprit.GUI;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import tn.esprit.entities.User;
import tn.esprit.Services.UserService;
import tn.esprit.utils.SignupSession;

import static tn.esprit.utils.CurrentUserSession.user;

public class EvaluatorExtraController {

    @FXML
    private javafx.scene.control.TextField levelField;

    @FXML
    private void finishEvaluator() {

        String level = levelField.getText();

        if (level.isEmpty()) return;

        UserService us = new UserService();

        User evaluator = new User(
                SignupSession.fullName,
                SignupSession.email,
                (SignupSession.passwordPlain == null ? null : SignupSession.passwordPlain),                "EVALUATOR",
                "ACTIVE",
                null,
                level
        );

        User created = us.add(evaluator);
        if (created == null) {
            System.out.println("Failed to create evaluator user.");
            return;

        }
        tn.esprit.utils.CurrentUserSession.user = created;
        new Thread(() -> {
            tn.esprit.Services.EmailService mail = new tn.esprit.Services.EmailService();
            mail.sendWelcomeEmail(user.getEmail(), user.getFullName(), "http://localhost:8080/startupflow/login");
        }).start();
        goTo("Login.fxml");
    }

    private void goTo(String fxml) {
        try {
            Stage stage = (Stage) levelField.getScene().getWindow();
            Parent root = FXMLLoader.load(getClass().getResource("/" + fxml));
            stage.setScene(new Scene(root));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}