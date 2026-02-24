package tn.esprit.GUI;

import jakarta.mail.Session;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import tn.esprit.entities.User;
import tn.esprit.Services.UserService;
import tn.esprit.utils.CurrentUserSession;
import tn.esprit.utils.SignupSession;

import java.net.URL;

import static tn.esprit.utils.CurrentUserSession.user;

public class RoleChoiceController {

    @FXML
    private void onHome(   ActionEvent e) {
        goTo(e,"/landing.fxml"); // change if your landing file name is different
    }

    @FXML
    private void onLogin(   ActionEvent e) {
        System.out.println("Login page later...");
        // goTo("/login.fxml");
    }

    @FXML
    private void onEntrepreneur(ActionEvent e) {
        UserService us = new UserService();

        User u = new User(
                SignupSession.fullName,
                SignupSession.email,
                (SignupSession.passwordPlain == null ? null : SignupSession.passwordPlain),                "ENTREPRENEUR",
                "PENDING",
                null,
                null
        );

        User created = us.add(u);
        if (created != null) {
            CurrentUserSession.user = created;

            new Thread(() -> {
                tn.esprit.Services.EmailService mail = new tn.esprit.Services.EmailService();
                mail.sendWelcomeEmail(created.getEmail(), created.getFullName(),
                        "http://localhost:8080/startupflow/login");
            }).start();

            goTo(e, "/Login.fxml");
        }
    }

    @FXML
    private void onMentor(   ActionEvent e) {
        goTo(e,"/MentorExtra.fxml");
    }

    @FXML
    private void onEvaluator(   ActionEvent e) {

        goTo(e,"/EvaluatorExtra.fxml");

    }

    private void goTo(ActionEvent e,String fxmlPath) {
        try {
            URL url = getClass().getResource(fxmlPath);
            if (url == null) {
                System.out.println(" FXML not found on classpath: " + fxmlPath);
                return;
            }

            Parent root = FXMLLoader.load(url);

            Stage stage = (Stage) Stage.getWindows().filtered(w -> w.isShowing()).get(0);
            stage.setScene(new Scene(root));
            stage.show();

        } catch (Exception e1) {
            e1.printStackTrace();
        }
    }
}