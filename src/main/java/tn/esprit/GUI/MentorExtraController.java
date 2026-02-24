package tn.esprit.GUI;

import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.stage.Stage;
import tn.esprit.entities.User;
import tn.esprit.Services.UserService;
import tn.esprit.utils.SignupSession;

import java.io.IOException;

import static tn.esprit.utils.CurrentUserSession.user;

public class MentorExtraController {

    @javafx.fxml.FXML
    private javafx.scene.control.TextField expertiseField;

    @javafx.fxml.FXML
    private javafx.scene.control.Label errorLabel;

    public void finishMentor(ActionEvent e) {

        String expertise = expertiseField.getText();

        if (expertise == null || expertise.isEmpty()) {
            errorLabel.setText("Expertise required");
            return;
        }

        UserService us = new UserService();

        User mentor = new User(
                SignupSession.fullName,
                SignupSession.email,
                (SignupSession.passwordPlain == null ? null : SignupSession.passwordPlain),                "MENTOR",
                "PENDING",
                expertise,
                null
        );

        User created = us.add(mentor);
        if (created == null) {
            errorLabel.setText("Failed to create mentor user.");
            return;
        }
        tn.esprit.utils.CurrentUserSession.user = created;

        new Thread(() -> {
            tn.esprit.Services.EmailService mail = new tn.esprit.Services.EmailService();
            mail.sendWelcomeEmail(
                    created.getEmail(),
                    created.getFullName(),
                    "http://localhost:8080/startupflow/login"
            );
        }).start();

        goTo(e, "Login.fxml");
    }

    private void goTo(ActionEvent e, String fxml) {
        try {
            Stage stage = (Stage) ((Node) e.getSource()).getScene().getWindow();
            stage.setScene(new Scene(FXMLLoader.load(getClass().getResource("/" + fxml))));
            stage.show();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public void goBack(ActionEvent e) {
        goTo(e, "RoleChoice.fxml");
    }
}