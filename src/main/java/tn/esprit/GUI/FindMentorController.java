package tn.esprit.GUI;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import tn.esprit.entities.MentorRecoRow;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class FindMentorController {

    @FXML
    private Label lblInfo;
    @FXML
    private Button btnFind;

    @FXML
    private TableView<MentorRecoRow> table;

    @FXML
    private TableColumn<MentorRecoRow, String> colBest;
    @FXML
    private TableColumn<MentorRecoRow, String> colName;
    @FXML
    private TableColumn<MentorRecoRow, String> colExpertise;
    @FXML
    private TableColumn<MentorRecoRow, String> colRating;
    @FXML
    private TableColumn<MentorRecoRow, String> colRecl;

    private final ObservableList<MentorRecoRow> data = FXCollections.observableArrayList();

    // ✅ Use ONE environment only (recommended: project .venv)
    private static final String PYTHON_EXE = "C:\\Users\\zaiem yousssef\\IdeaProjects\\Project3A2_StartUpFlow\\ai\\WORKSHOP\\venv\\Scripts\\python.exe";
    // ✅ Script relative to WORKDIR (project root)
    private static final String PY_SCRIPT = "ai\\predict_mentors_ml.py";

    @FXML
    private void initialize() {
        setupTable();
        table.setItems(data);

        if (lblInfo != null)
            lblInfo.setText("Click Refresh to load mentor recommendations.");
    }

    private void setupTable() {
        colBest.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().isBest() ? "BEST ✅" : ""));
        colName.setCellValueFactory(cd -> new SimpleStringProperty(ns(cd.getValue().getFullName())));
        colExpertise.setCellValueFactory(cd -> new SimpleStringProperty(ns(cd.getValue().getExpertise())));

        colRating.setCellValueFactory(cd -> {
            double pct = cd.getValue().getRatingPercent();
            String text = pct >= 0 ? String.format("%.1f%%", pct) : "—";
            return new SimpleStringProperty(text);
        });

        colRecl.setCellValueFactory(cd -> new SimpleStringProperty(String.valueOf(cd.getValue().getReclamations90d())));
    }

    @FXML
    private void findMentors() {
        try {
            if (lblInfo != null)
                lblInfo.setText("Loading recommendations...");

            ProcessBuilder pb = new ProcessBuilder(PYTHON_EXE, PY_SCRIPT);
            pb.directory(new File(System.getProperty("user.dir")));
            pb.redirectErrorStream(true);

            // Inherit the exact venv scripts folder into the PATH so native DLLs resolve
            // properly
            java.util.Map<String, String> env = pb.environment();
            String venvScripts = new File("venv", "Scripts").getAbsolutePath();
            env.put("PATH", venvScripts + File.pathSeparator + env.getOrDefault("PATH", ""));

            Process p = pb.start();

            StringBuilder sb = new StringBuilder();
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {

                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line).append("\n");
                }
            }

            int exit = p.waitFor();
            String output = sb.toString().trim();

            int start = output.indexOf('[');
            int end = output.lastIndexOf(']');
            if (start == -1 || end == -1 || end < start) {
                lblInfo.setText("Python did not return JSON array:\n" + output);
                return;
            }
            String json = output.substring(start, end + 1);
            if (exit != 0) {
                if (lblInfo != null)
                    lblInfo.setText("Python failed (exit=" + exit + "):\n" + output);
                return;
            }

            if (output.isEmpty()) {
                if (lblInfo != null)
                    lblInfo.setText("Python returned empty output.");
                return;
            }

            Gson gson = new Gson();
            Type listType = new TypeToken<List<MentorRecoRow>>() {
            }.getType();
            List<MentorRecoRow> rows;
            try {
                rows = gson.fromJson(json, listType);
            } catch (JsonSyntaxException ex) {
                ex.printStackTrace();
                if (lblInfo != null)
                    lblInfo.setText("Invalid response from recommendation service.");
                return;
            }

            data.setAll(rows);
            if (lblInfo != null)
                lblInfo.setText(rows.size() + " mentor(s) loaded.");

        } catch (Exception ex) {
            ex.printStackTrace();
            if (lblInfo != null)
                lblInfo.setText("Failed: " + ex.getMessage());
        }
    }

    private String ns(String s) {
        return (s == null) ? "" : s;
    }

    @FXML
    private void goBack() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/EntrepreneurDashboard.fxml"));
            javafx.scene.Parent root = loader.load();

            javafx.stage.Stage stage = (javafx.stage.Stage) table.getScene().getWindow();
            stage.setScene(new javafx.scene.Scene(root));
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            lblInfo.setText("Back failed: " + e.getMessage());
        }
    }
}
