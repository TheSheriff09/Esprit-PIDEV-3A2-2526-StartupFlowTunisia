package tn.esprit.GUI;

import javafx.event.ActionEvent;
import javafx.scene.Node;
import tn.esprit.entities.User;
import tn.esprit.Services.UserService;
import tn.esprit.utils.SignupSession;

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
                (SignupSession.passwordPlain == null ? null : SignupSession.passwordPlain), "MENTOR",
                "PENDING",
                expertise,
                null);

        User created = us.add(mentor);
        if (created == null) {
            errorLabel.setText("Failed to create mentor user.");
            return;
        }
        tn.esprit.utils.SessionManager.login(created);

        new Thread(() -> {
            tn.esprit.Services.EmailService mail = new tn.esprit.Services.EmailService();
            mail.sendWelcomeEmail(
                    created.getEmail(),
                    created.getFullName(),
                    "http://localhost:8080/startupflow/login");
        }).start();

        tn.esprit.utils.NavigationManager.navigateTo((Node) e.getSource(), "/Login.fxml");
    }

    public void goBack(ActionEvent e) {
        tn.esprit.utils.NavigationManager.navigateTo((Node) e.getSource(), "/RoleChoice.fxml");
    }
}
