package tn.esprit.GUI;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import tn.esprit.Services.StartupDAO;
import tn.esprit.entities.Startup;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.List;

public class SwotController {

    @FXML private ComboBox<Startup> startupCombo;
    @FXML private TextArea outputArea;
    @FXML private Label statusLabel;
  //  @FXML private MenuButton userMenuBtn;
  //  @FXML private MenuItem miHeader;

    private final StartupDAO startupDAO = new StartupDAO();


    private static final String PYTHON_EXE =
            "C:\\Users\\linaf\\OneDrive\\Bureau\\3A2PiDEV\\.venv\\Scripts\\python.exe";
    private static final String PY_SCRIPT = "ai/swot_generator.py";

    @FXML
    public void initialize() {

        loadStartups();
    }

    private void loadStartups() {
        try {
            List<Startup> startups = startupDAO.getAllStartups();
            startupCombo.setItems(FXCollections.observableArrayList(startups));
            statusLabel.setText("Select a startup to generate SWOT.");
        } catch (Exception e) {
            statusLabel.setText("Failed to load startups: " + e.getMessage());
        }
    }

    @FXML
    private void onGenerate() {
        Startup s = startupCombo.getValue();
        if (s == null) {
            statusLabel.setText("Please select a startup first.");
            return;
        }

        statusLabel.setText("Generating SWOT with AI...");
        outputArea.clear();

        try {
            ProcessBuilder pb = new ProcessBuilder(
                    PYTHON_EXE,
                    PY_SCRIPT,
                    safeArg(s.getName()),
                    safeArg(s.getDescription()),
                    safeArg(s.getSector())
            );

            pb.redirectErrorStream(false);

            Process p = pb.start();

            StringBuilder stdout = new StringBuilder();
            StringBuilder stderr = new StringBuilder();

            try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = br.readLine()) != null) {
                    stdout.append(line).append("\n");
                }
            }

            try (BufferedReader brErr = new BufferedReader(new InputStreamReader(p.getErrorStream()))) {
                String line;
                while ((line = brErr.readLine()) != null) {
                    stderr.append(line).append("\n");
                }
            }

            int code = p.waitFor();

            if (code == 0) {
                statusLabel.setText("SWOT generated.");
                outputArea.setText(stdout.toString().trim());
            } else {
                statusLabel.setText("AI failed (code " + code + ").");
                outputArea.setText(stderr.length() > 0 ? stderr.toString() : stdout.toString());
            }

        } catch (Exception e) {
            statusLabel.setText("Error running AI: " + e.getMessage());
        }
    }

    private String safeArg(String s) {
        if (s == null) return "";
        return s;
    }

    private void goTo(ActionEvent e, String fxmlPath) {
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
    @FXML
    private void onBack(ActionEvent e) {
        goTo(e,"/EntrepreneurDashboard.fxml");
    }

    @FXML
    private void onLogout() {
        statusLabel.setText("Logout clicked.");
    }
}
