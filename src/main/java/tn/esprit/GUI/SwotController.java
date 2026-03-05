package tn.esprit.GUI;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.concurrent.Task;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.Region;
import tn.esprit.Services.StartupService;
import tn.esprit.entities.Startup;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

public class SwotController {

    @FXML
    private ComboBox<Startup> startupCombo;
    @FXML
    private Button generateBtn;
    @FXML
    private ListView<String> strengthsList;
    @FXML
    private ListView<String> weaknessesList;
    @FXML
    private ListView<String> opportunitiesList;
    @FXML
    private ListView<String> threatsList;
    @FXML
    private Label statusLabel;
    @FXML
    private MenuButton userMenuBtn;
    @FXML
    private MenuItem miHeader;
    @FXML
    private MenuItem miLogout;

    private final StartupService startupService = new StartupService();
    private final Gson gson = new Gson();

    private static final String PYTHON_EXE = "C:\\Users\\zaiem yousssef\\IdeaProjects\\Project3A2_StartUpFlow\\ai\\WORKSHOP\\venv\\Scripts\\python.exe";
    private static final String PY_SCRIPT = "ai/swot_generator.py";

    @FXML
    public void initialize() {
        // ── Session guard ──
        if (!tn.esprit.utils.SessionManager.requireLogin(startupCombo))
            return;

        loadStartups();
        clearSwot();

        // Configure user menu via TopBarHelper
        if (userMenuBtn != null) {
            tn.esprit.utils.TopBarHelper.setup(userMenuBtn, miHeader, startupCombo);
        }
    }

    private void loadStartups() {
        try {
            List<Startup> startups = startupService.list();
            startupCombo.setItems(FXCollections.observableArrayList(startups));
            setStatus(true, "Select a startup to generate SWOT.");
        } catch (Exception e) {
            setStatus(false, "Failed to load startups: " + e.getMessage());
        }
    }

    @FXML
    private void onGenerate() {
        Startup s = startupCombo.getValue();
        if (s == null) {
            setStatus(false, "Please select a startup first.");
            return;
        }

        setStatus(true, "Generating SWOT with AI...");
        clearSwot();

        if (generateBtn != null) {
            generateBtn.setDisable(true);
        }
        if (startupCombo != null) {
            startupCombo.setDisable(true);
        }

        Task<SwotPayload> task = new Task<>() {
            @Override
            protected SwotPayload call() throws Exception {
                ProcessBuilder pb = new ProcessBuilder(
                        PYTHON_EXE,
                        PY_SCRIPT,
                        safeArg(s.getName()),
                        safeArg(s.getDescription()),
                        safeArg(s.getSector()));

                pb.redirectErrorStream(true);

                Process p = pb.start();

                StringBuilder out = new StringBuilder();
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        out.append(line).append("\n");
                    }
                }

                int code = p.waitFor();
                String raw = out.toString().trim();

                if (code == 0) {
                    SwotPayload swot = parseSwotPayload(raw);
                    if (swot == null) {
                        throw new RuntimeException("AI returned an unexpected response.");
                    }
                    if (swot.error != null && !swot.error.isBlank()) {
                        throw new RuntimeException(swot.error);
                    }

                    return swot;
                } else {
                    SwotPayload swot = parseSwotPayload(raw);
                    if (swot != null && swot.error != null && !swot.error.isBlank()) {
                        throw new RuntimeException(swot.error);
                    } else {
                        throw new RuntimeException("AI failed (code " + code + ").");
                    }
                }
            }
        };

        task.setOnSucceeded(ev -> {
            SwotPayload swot = task.getValue();
            render(swot);
            setStatus(true, "SWOT generated.");
            if (generateBtn != null) {
                generateBtn.setDisable(false);
            }
            if (startupCombo != null) {
                startupCombo.setDisable(false);
            }
        });

        task.setOnFailed(ev -> {
            Throwable ex = task.getException();
            String msg = (ex == null || ex.getMessage() == null || ex.getMessage().isBlank())
                    ? "Error running AI."
                    : ex.getMessage();
            setStatus(false, msg);
            if (generateBtn != null) {
                generateBtn.setDisable(false);
            }
            if (startupCombo != null) {
                startupCombo.setDisable(false);
            }
        });

        Thread t = new Thread(task, "swot-ai-task");
        t.setDaemon(true);
        t.start();
    }

    private String safeArg(String s) {
        if (s == null)
            return "";
        return s;
    }

    private void clearSwot() {
        if (strengthsList != null)
            strengthsList.setItems(FXCollections.observableArrayList());
        if (weaknessesList != null)
            weaknessesList.setItems(FXCollections.observableArrayList());
        if (opportunitiesList != null)
            opportunitiesList.setItems(FXCollections.observableArrayList());
        if (threatsList != null)
            threatsList.setItems(FXCollections.observableArrayList());
    }

    private void render(SwotPayload swot) {
        strengthsList.setItems(FXCollections.observableArrayList(nullToEmpty(swot.strengths)));
        weaknessesList.setItems(FXCollections.observableArrayList(nullToEmpty(swot.weaknesses)));
        opportunitiesList.setItems(FXCollections.observableArrayList(nullToEmpty(swot.opportunities)));
        threatsList.setItems(FXCollections.observableArrayList(nullToEmpty(swot.threats)));
    }

    private List<String> nullToEmpty(List<String> v) {
        return v == null ? Collections.emptyList() : v;
    }

    private void setStatus(boolean ok, String msg) {
        statusLabel.setText(msg == null ? "" : msg);
        statusLabel.getStyleClass().removeAll("msgOk", "msgError");
        statusLabel.getStyleClass().add(ok ? "msgOk" : "msgError");
    }

    private void showRawResponse(String title, String raw) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(raw == null || raw.isBlank() ? "(empty response)" : raw);
        alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        alert.showAndWait();
    }

    private SwotPayload parseSwotPayload(String rawText) {
        if (rawText == null)
            return null;
        String json = extractFirstJsonObject(rawText);
        if (json == null)
            return null;

        try {
            JsonElement el = JsonParser.parseString(json);
            if (!el.isJsonObject())
                return null;
            JsonObject obj = el.getAsJsonObject();

            // Sometimes the python script returns {"error": "..."}
            if (obj.has("error") && obj.get("error").isJsonPrimitive()) {
                SwotPayload p = new SwotPayload();
                p.error = obj.get("error").getAsString();
                return p;
            }

            return gson.fromJson(obj, SwotPayload.class);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Extracts the first top-level JSON object from text.
     * This avoids breaking when the model or runtime prints extra lines.
     */
    private String extractFirstJsonObject(String text) {
        int start = text.indexOf('{');
        if (start < 0)
            return null;

        int depth = 0;
        boolean inString = false;
        boolean escape = false;

        for (int i = start; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (inString) {
                if (escape) {
                    escape = false;
                } else if (ch == '\\') {
                    escape = true;
                } else if (ch == '"') {
                    inString = false;
                }
                continue;
            }

            if (ch == '"') {
                inString = true;
                continue;
            }

            if (ch == '{')
                depth++;
            else if (ch == '}') {
                depth--;
                if (depth == 0)
                    return text.substring(start, i + 1);
            }
        }
        return null;
    }

    private static class SwotPayload {
        List<String> strengths;
        List<String> weaknesses;
        List<String> opportunities;
        List<String> threats;
        String error;
    }

    @FXML
    private void onBack(ActionEvent e) {
        tn.esprit.utils.NavigationManager.goToDashboard(startupCombo);
    }

    @FXML
    private void onLogout() {
        tn.esprit.utils.NavigationManager.logout(startupCombo);
    }

    @FXML
    private void onManageProfile() {
        tn.esprit.utils.NavContext.setBack("/swot.fxml");
        tn.esprit.utils.NavigationManager.navigateTo(startupCombo, "/ManageProfile.fxml");
    }

    @FXML
    private void onStartups() {
        tn.esprit.utils.NavigationManager.navigateTo(startupCombo, "/startupview.fxml");
    }

    @FXML
    private void onSettings() {
        System.out.println("Settings");
    }
}
