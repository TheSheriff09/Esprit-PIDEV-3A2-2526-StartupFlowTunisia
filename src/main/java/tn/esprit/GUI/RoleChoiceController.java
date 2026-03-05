package tn.esprit.GUI;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import tn.esprit.entities.User;
import tn.esprit.Services.UserService;
import tn.esprit.utils.NavigationManager;
import tn.esprit.utils.SessionManager;
import tn.esprit.utils.SignupSession;

public class RoleChoiceController {

    @FXML
    private javafx.scene.control.Label msgLabel;

    @FXML
    private void onHome(ActionEvent e) {
        NavigationManager.navigateTo((javafx.scene.Node) e.getSource(), "/Landing.fxml");
    }

    @FXML
    private void onLogin(ActionEvent e) {
        NavigationManager.navigateTo((javafx.scene.Node) e.getSource(), "/Login.fxml");
    }

    @FXML
    private void onEntrepreneur(ActionEvent e) {
        UserService us = new UserService();

        User u = new User(
                SignupSession.fullName,
                SignupSession.email,
                (SignupSession.passwordPlain == null ? null : SignupSession.passwordPlain),
                "ENTREPRENEUR",
                "PENDING",
                null,
                null);

        User created = us.add(u);
        if (created != null) {
            SessionManager.login(created);

            new Thread(() -> {
                tn.esprit.Services.EmailService mail = new tn.esprit.Services.EmailService();
                mail.sendWelcomeEmail(created.getEmail(), created.getFullName(),
                        "http://localhost:8080/startupflow/login");
            }).start();

            NavigationManager.navigateTo((javafx.scene.Node) e.getSource(), "/Login.fxml");
        }
    }

    @FXML
    private void onMentor(ActionEvent e) {
        NavigationManager.navigateTo((javafx.scene.Node) e.getSource(), "/MentorExtra.fxml");
    }

    @FXML
    private void onEvaluator(ActionEvent e) {
        NavigationManager.navigateTo((javafx.scene.Node) e.getSource(), "/EvaluatorExtra.fxml");
    }
}
