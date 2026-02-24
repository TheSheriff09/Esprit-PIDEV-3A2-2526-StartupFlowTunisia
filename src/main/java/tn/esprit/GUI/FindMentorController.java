package tn.esprit.GUI;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import tn.esprit.entities.MentorRecoRow;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class FindMentorController {

    @FXML private TextArea needTextArea;
    @FXML private Button btnFind;
    @FXML private Label lblInfo;

    @FXML private TableView<MentorRecoRow> table;
    @FXML private TableColumn<MentorRecoRow, String> colBest;
    @FXML private TableColumn<MentorRecoRow, String> colName;
    @FXML private TableColumn<MentorRecoRow, String> colExpertise;
    @FXML private TableColumn<MentorRecoRow, String> colMatch;
    @FXML private TableColumn<MentorRecoRow, String> colRecl;
    @FXML private TableColumn<MentorRecoRow, String> colRisk;

    private final ObservableList<MentorRecoRow> data = FXCollections.observableArrayList();

    // Set your python exe like you did in SWOT
    private static final String PYTHON_EXE =
            "C:\\Users\\linaf\\Desktop\\chatBot_Like\\my_env\\Scripts\\python.exe";
    private static final String PY_SCRIPT = "ai/recommend_mentors.py";
    @FXML
    private void initialize() {
        setupTable();
        table.setItems(data);

        if (needTextArea != null && needTextArea.getText().trim().isEmpty()) {
            needTextArea.setText("Funding pitch deck fintech Tunisia marketing");
        }
    }

    private void setupTable() {
        colBest.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().isBest() ? "BEST ✅" : ""));
        colName.setCellValueFactory(cd -> new SimpleStringProperty(ns(cd.getValue().getFullName())));
        colExpertise.setCellValueFactory(cd -> new SimpleStringProperty(ns(cd.getValue().getExpertise())));
        colMatch.setCellValueFactory(cd -> new SimpleStringProperty(String.format("%.2f%%", cd.getValue().getMatchScore())));
        colRecl.setCellValueFactory(cd -> new SimpleStringProperty(String.valueOf(cd.getValue().getReclamations90d())));
        colRisk.setCellValueFactory(cd -> new SimpleStringProperty(ns(cd.getValue().getRisk())));
    }

    @FXML
    private void findMentors() {
        try {
            String need = (needTextArea.getText() == null) ? "" : needTextArea.getText().trim();
            if (need.isEmpty()) need = "startup mentor";

            ProcessBuilder pb = new ProcessBuilder(
                    PYTHON_EXE,
                    PY_SCRIPT,
                    need
            );

            pb.environment().put("DB_HOST", "127.0.0.1");
            pb.environment().put("DB_PORT", "3306");
            pb.environment().put("DB_NAME", "hamod");
            pb.environment().put("DB_USER", "root");
            pb.environment().put("DB_PASS", "");

            pb.redirectErrorStream(true);

            Process p = pb.start();

            StringBuilder sb = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
            }

            int exit = p.waitFor();
            if (exit != 0) {
                lblInfo.setText("Python error (exit=" + exit + ")");
                return;
            }

            String json = sb.toString().trim();
            Gson gson = new Gson();
            Type listType = new TypeToken<List<MentorRecoRow>>(){}.getType();
            List<MentorRecoRow> rows = gson.fromJson(json, listType);

            data.setAll(rows);
            lblInfo.setText("Mentors found: " + rows.size());

        } catch (Exception ex) {
            ex.printStackTrace();
            lblInfo.setText("Failed: " + ex.getMessage());
        }
    }

    private String ns(String s) { return (s == null) ? "" : s; }
}