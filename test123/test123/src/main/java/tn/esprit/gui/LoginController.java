package tn.esprit.gui;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import tn.esprit.entities.User;
import tn.esprit.utils.SessionContext;

import java.io.IOException;

public class LoginController {

    @FXML
    private Label msgLabel;

    @FXML
    private void onMentorLogin() {
        // Mock a Mentor User
        User mentor = new User();
        mentor.setId(1); // Standard test ID
        mentor.setFullName("Demo Mentor");
        mentor.setRole("mentor");

        SessionContext.setUser(mentor);
        navigateToDashboard();
    }

    @FXML
    private void onEvaluatorLogin() {
        // Mock an Evaluator User
        User evaluator = new User();
        evaluator.setId(2); // Standard test ID
        evaluator.setFullName("Demo Evaluator");
        evaluator.setRole("evaluator");

        SessionContext.setUser(evaluator);
        navigateToDashboard();
    }

    private void navigateToDashboard() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/MentorshipDashboard.fxml"));
            Stage stage = (Stage) msgLabel.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Mentorship Dashboard - StartupFlow");
            stage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
            msgLabel.setText("Navigation error: " + e.getMessage());
        }
    }
}
