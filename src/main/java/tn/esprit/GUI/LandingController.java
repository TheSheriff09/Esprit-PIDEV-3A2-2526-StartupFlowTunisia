package tn.esprit.GUI;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class LandingController {

    @FXML
    private void onHome() {
        System.out.println("Home clicked");

    }

    @FXML
    private void onAbout() {
        System.out.println("About clicked");
        // goTo("About.fxml");
    }

    @FXML
    private void onContent() {
        System.out.println("Content clicked");
        // goTo("Content.fxml");
    }

    @FXML
    private void onLogin() {
        goTo("Login.fxml");
    }

    @FXML
    private void onSignup() {
        System.out.println("Signup clicked");
        goTo("signup.fxml"); // you already have signup.fxml
    }

    @FXML
    private void onGetStarted() {
        System.out.println("GET STARTED clicked -> go signup");
        goTo("signup.fxml");
    }

    @FXML
    private void onGetIncorporated() {
        goTo("signup.fxml");
    }

    // ========== HELPER ==========
    private void goTo(String fxml) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/" + fxml));
            Parent root = loader.load();

            Stage stage = (Stage) Stage.getWindows().filtered(w -> w.isShowing()).get(0);
            stage.setScene(new Scene(root));
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
