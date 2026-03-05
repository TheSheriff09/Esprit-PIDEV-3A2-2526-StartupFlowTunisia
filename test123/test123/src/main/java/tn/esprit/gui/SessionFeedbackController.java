package tn.esprit.gui;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import tn.esprit.entities.Session;
import tn.esprit.entities.SessionFeedback;
import tn.esprit.services.*;
import tn.esprit.services.SortingService.FeedbackSort;
import tn.esprit.utils.SessionContext;

import java.io.File;
import java.net.URL;
import java.time.LocalDate;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * SessionFeedbackController — upgraded with:
 * <ul>
 * <li><b>FilteredList</b>: real-time search across all text fields and
 * score.</li>
 * <li><b>Multi-criteria sort</b>: FeedbackSort ComboBox drives
 * SortingService.</li>
 * <li><b>Grok AI Summary</b>: async AI summarization of selected feedback.</li>
 * <li><b>CSV export</b>: FileChooser + CsvExportService with proper RFC 4180
 * escaping.</li>
 * <li><b>Sentiment column</b>: new column showing AI-analyzed sentiment.</li>
 * </ul>
 */
public class SessionFeedbackController implements Initializable {

    // ── Form ────────────────────────────────────────────────────────────────
    @FXML
    private ComboBox<Session> sessionCombo;
    @FXML
    private TextField mentorIDField;
    @FXML
    private Slider scoreSlider;
    @FXML
    private Label scoreLabel;
    @FXML
    private DatePicker feedbackDatePicker;
    @FXML
    private TextArea strengthsArea;
    @FXML
    private TextArea weaknessesArea;
    @FXML
    private TextArea recommendationsArea;
    @FXML
    private TextArea nextActionsArea;
    @FXML
    private Label msgLabel;
    @FXML
    private Label dateLabel;
    @FXML
    private HBox actionBox;
    @FXML
    private ToggleButton btnDictation;

    // ── Search & Sort ───────────────────────────────────────────────────────
    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<FeedbackSort> sortCombo;

    // ── AI Summary ──────────────────────────────────────────────────────────
    @FXML
    private VBox aiSummaryBox;
    @FXML
    private TextArea aiSummaryArea;

    // ── Table ───────────────────────────────────────────────────────────────
    @FXML
    private TableView<SessionFeedback> feedbackTable;
    @FXML
    private TableColumn<SessionFeedback, Integer> colID;
    @FXML
    private TableColumn<SessionFeedback, Integer> colSession;
    @FXML
    private TableColumn<SessionFeedback, Integer> colMentor;
    @FXML
    private TableColumn<SessionFeedback, Integer> colScore;
    @FXML
    private TableColumn<SessionFeedback, String> colDate;
    @FXML
    private TableColumn<SessionFeedback, String> colSentiment;
    @FXML
    private TableColumn<SessionFeedback, String> colStrengths;

    // ── Services ────────────────────────────────────────────────────────────
    private final SessionFeedbackService service = new SessionFeedbackService();
    private final SessionService sessionSvc = new SessionService();
    private final GrokService grokSvc = GrokService.getInstance();
    private tn.esprit.services.VoskService voskService;

    // ── Data ─────────────────────────────────────────────────────────────────
    private ObservableList<SessionFeedback> masterData;
    private FilteredList<SessionFeedback> filteredData;
    private SessionFeedback selected;

    // ── Init ─────────────────────────────────────────────────────────────────

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        try {
            voskService = new tn.esprit.services.VoskService();
        } catch (Exception e) {
            voskService = null;
        }

        setupTableColumns();
        setupSlider();
        setupSortCombo();

        int uid = SessionContext.getUserId();
        boolean isMentor = SessionContext.isMentor();

        feedbackDatePicker.setValue(LocalDate.now());

        if (isMentor) {
            mentorIDField.setText(String.valueOf(uid));
            mentorIDField.setDisable(true);

            List<Session> completed = sessionSvc.listByMentor(uid).stream()
                    .filter(s -> "completed".equalsIgnoreCase(s.getStatus()))
                    .collect(Collectors.toList());
            sessionCombo.setItems(FXCollections.observableArrayList(completed));
            if (completed.isEmpty())
                sessionCombo.setPromptText("No completed sessions yet.");

        } else {
            // Entrepreneur: read-only view
            actionBox.setVisible(false);
            actionBox.setManaged(false);
            sessionCombo.setDisable(true);
            strengthsArea.setEditable(false);
            weaknessesArea.setEditable(false);
            recommendationsArea.setEditable(false);
            nextActionsArea.setEditable(false);
            scoreSlider.setDisable(true);
            dateLabel.setText("Date");
        }

        // Real-time search listener
        searchField.textProperty().addListener((obs, o, n) -> applyFilter(n));

        loadTable();
    }

    private void setupTableColumns() {
        colID.setCellValueFactory(new PropertyValueFactory<>("feedbackID"));
        colSession.setCellValueFactory(new PropertyValueFactory<>("sessionID"));
        colMentor.setCellValueFactory(new PropertyValueFactory<>("mentorID"));
        colScore.setCellValueFactory(new PropertyValueFactory<>("progressScore"));
        colDate.setCellValueFactory(cd -> new SimpleStringProperty(
                cd.getValue().getFeedbackDate() != null ? cd.getValue().getFeedbackDate().toString() : ""));
        if (colSentiment != null)
            colSentiment.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getSentiment()));
        colStrengths.setCellValueFactory(new PropertyValueFactory<>("strengths"));

        // Score colour-coding
        colScore.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Integer score, boolean empty) {
                super.updateItem(score, empty);
                if (empty || score == null) {
                    setText(null);
                    setStyle("");
                    return;
                }
                setText(String.valueOf(score));
                if (score >= 75)
                    setStyle("-fx-text-fill:#12a059;-fx-font-weight:700;");
                else if (score >= 50)
                    setStyle("-fx-text-fill:#e07c00;-fx-font-weight:700;");
                else
                    setStyle("-fx-text-fill:#e0134a;-fx-font-weight:700;");
            }
        });

        // Sentiment colour-coding
        if (colSentiment != null) {
            colSentiment.setCellFactory(col -> new TableCell<>() {
                @Override
                protected void updateItem(String s, boolean empty) {
                    super.updateItem(s, empty);
                    if (empty || s == null) {
                        setText(null);
                        setStyle("");
                        return;
                    }
                    setText(s);
                    switch (s.toLowerCase()) {
                        case "positive" -> setStyle("-fx-text-fill:#12a059;-fx-font-weight:700;");
                        case "negative" -> setStyle("-fx-text-fill:#e0134a;-fx-font-weight:700;");
                        default -> setStyle("-fx-text-fill:#5a4bd6;");
                    }
                }
            });
        }
    }

    private void setupSlider() {
        scoreLabel.setText(String.valueOf((int) scoreSlider.getValue()));
        scoreSlider.valueProperty().addListener((obs, o, v) -> scoreLabel.setText(String.valueOf(v.intValue())));
    }

    private void setupSortCombo() {
        if (sortCombo == null)
            return;
        sortCombo.setItems(FXCollections.observableArrayList(FeedbackSort.values()));
        sortCombo.setValue(FeedbackSort.DATE_DESC);
        sortCombo.setOnAction(e -> onSortChanged());
    }

    // ── Data ─────────────────────────────────────────────────────────────────

    private void loadTable() {
        List<SessionFeedback> raw;
        int uid = SessionContext.getUserId();
        if (SessionContext.isMentor()) {
            raw = service.listByMentor(uid);
        } else {
            raw = service.list().stream()
                    .filter(f -> sessionSvc.listByEvaluator(uid).stream()
                            .anyMatch(s -> s.getSessionID() == f.getSessionID()))
                    .collect(Collectors.toList());
        }

        raw = SortingService.defaultSortFeedback(raw);
        masterData = FXCollections.observableArrayList(raw);
        filteredData = new FilteredList<>(masterData, f -> true);
        feedbackTable.setItems(filteredData);
        applyFilter(searchField.getText());
    }

    // ── Search ────────────────────────────────────────────────────────────────

    private void applyFilter(String query) {
        if (filteredData == null)
            return;
        if (query == null || query.isBlank()) {
            filteredData.setPredicate(f -> true);
            return;
        }
        String q = query.toLowerCase().trim();
        filteredData.setPredicate(f -> (f.getStrengths() != null && f.getStrengths().toLowerCase().contains(q))
                || (f.getWeaknesses() != null && f.getWeaknesses().toLowerCase().contains(q))
                || (f.getRecommendations() != null && f.getRecommendations().toLowerCase().contains(q))
                || (f.getNextActions() != null && f.getNextActions().toLowerCase().contains(q))
                || (f.getSentiment() != null && f.getSentiment().toLowerCase().contains(q))
                || (f.getFeedbackDate() != null && f.getFeedbackDate().toString().contains(q))
                || String.valueOf(f.getProgressScore()).contains(q));
    }

    @FXML
    private void onSearch() {
        /* handled by listener */ }

    // ── Sort ──────────────────────────────────────────────────────────────────

    private void onSortChanged() {
        if (masterData == null || sortCombo == null)
            return;
        FeedbackSort sort = sortCombo.getValue();
        if (sort == null)
            return;
        List<SessionFeedback> sorted = SortingService.sortFeedback(masterData, sort);
        masterData.setAll(sorted);
    }

    // ── CSV Export ────────────────────────────────────────────────────────────

    @FXML
    private void onExportCSV() {
        if (masterData == null || masterData.isEmpty()) {
            showError("No feedback to export.");
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export Feedback to CSV");
        chooser.setInitialFileName("feedback_export.csv");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        File file = chooser.showSaveDialog(feedbackTable.getScene().getWindow());
        if (file == null)
            return;
        try {
            List<SessionFeedback> toExport = filteredData.stream().toList();
            CsvExportService.exportFeedbackToCSV(toExport, file);
            showSuccess("✓ Exported " + toExport.size() + " records → " + file.getName());
        } catch (Exception e) {
            showError("Export failed: " + e.getMessage());
        }
    }

    // ── AI Summary ────────────────────────────────────────────────────────────

    @FXML
    private void onAiSummarize() {
        if (selected == null) {
            showError("Select a feedback entry first.");
            return;
        }

        // Show the AI panel with loading state
        if (aiSummaryBox != null) {
            aiSummaryBox.setVisible(true);
            aiSummaryBox.setManaged(true);
        }
        if (aiSummaryArea != null)
            aiSummaryArea.setText("⏳ Asking Grok AI… (this may take a few seconds)");

        String combined = "Strengths: " + nullSafe(selected.getStrengths())
                + "\nWeaknesses: " + nullSafe(selected.getWeaknesses())
                + "\nRecommendations: " + nullSafe(selected.getRecommendations())
                + "\nNext Actions: " + nullSafe(selected.getNextActions());

        grokSvc.summarizeFeedbackAsync(combined)
                .thenAccept(summary -> Platform.runLater(() -> {
                    if (aiSummaryArea != null)
                        aiSummaryArea.setText(summary);
                    showSuccess("✓ AI summary ready");
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        if (aiSummaryArea != null)
                            aiSummaryArea.setText("[AI unavailable]");
                        showError("AI call failed.");
                    });
                    return null;
                });
    }

    // ── Dictation ─────────────────────────────────────────────────────────────

    @FXML
    private void onToggleDictation() {
        if (voskService == null || !voskService.isReady()) {
            showError("Speech script not found. Please check speech_listen.ps1 exists.");
            btnDictation.setSelected(false);
            return;
        }
        if (btnDictation.isSelected()) {
            btnDictation.setText("🛑 Stop Dictating");
            btnDictation.setStyle("-fx-background-color:#ff4c4c;-fx-text-fill:white;");
            voskService.startListening(text -> {
                if (strengthsArea.isFocused())
                    strengthsArea.appendText(text);
                else if (weaknessesArea.isFocused())
                    weaknessesArea.appendText(text);
                else if (recommendationsArea.isFocused())
                    recommendationsArea.appendText(text);
                else if (nextActionsArea.isFocused())
                    nextActionsArea.appendText(text);
                else
                    strengthsArea.appendText(text);
            });
        } else {
            btnDictation.setText("🎤 Dictate");
            btnDictation.setStyle("");
            voskService.stopListening();
        }
    }

    // ── CRUD ──────────────────────────────────────────────────────────────────

    @FXML
    private void onSubmit() {
        clearMsg();
        if (!SessionContext.isMentor())
            return;
        SessionFeedback fb = buildFromForm();
        if (fb == null)
            return;
        try {
            service.add(fb);
            showSuccess("Feedback submitted!");
            onClear();
            loadTable();
        } catch (Exception e) {
            showError("Submit failed: " + e.getMessage());
        }
    }

    @FXML
    private void onUpdate() {
        clearMsg();
        if (!SessionContext.isMentor())
            return;
        if (selected == null) {
            showError("Select an entry to update.");
            return;
        }
        SessionFeedback fb = buildFromForm();
        if (fb == null)
            return;
        fb.setFeedbackID(selected.getFeedbackID());
        try {
            service.update(fb);
            showSuccess("Updated!");
            onClear();
            loadTable();
        } catch (Exception e) {
            showError(e.getMessage());
        }
    }

    @FXML
    private void onDelete() {
        if (selected == null) {
            showError("Select feedback to delete.");
            return;
        }
        if (!SessionContext.isMentor())
            return;
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Delete this feedback?", ButtonType.YES, ButtonType.NO);
        alert.showAndWait().ifPresent(type -> {
            if (type == ButtonType.YES) {
                try {
                    service.delete(selected);
                    showSuccess("Deleted.");
                    onClear();
                    loadTable();
                } catch (Exception e) {
                    showError(e.getMessage());
                }
            }
        });
    }

    private SessionFeedback buildFromForm() {
        Session s = sessionCombo.getValue();
        if (s == null) {
            showError("Please select a session.");
            return null;
        }
        String strengths = strengthsArea.getText().trim();
        if (strengths.isEmpty()) {
            showError("Strengths are required.");
            return null;
        }
        return new SessionFeedback(
                s.getSessionID(), SessionContext.getUserId(), (int) scoreSlider.getValue(),
                strengths, weaknessesArea.getText().trim(),
                recommendationsArea.getText().trim(), nextActionsArea.getText().trim(),
                LocalDate.now());
    }

    @FXML
    private void onRowSelected() {
        SessionFeedback fb = feedbackTable.getSelectionModel().getSelectedItem();
        if (fb == null)
            return;
        selected = fb;

        sessionCombo.getItems().stream()
                .filter(s -> s.getSessionID() == fb.getSessionID())
                .findFirst().ifPresent(sessionCombo::setValue);

        scoreSlider.setValue(fb.getProgressScore());
        feedbackDatePicker.setValue(fb.getFeedbackDate());
        strengthsArea.setText(fb.getStrengths());
        weaknessesArea.setText(fb.getWeaknesses());
        recommendationsArea.setText(fb.getRecommendations());
        nextActionsArea.setText(fb.getNextActions());

        // Hide AI box when a new row is selected
        if (aiSummaryBox != null) {
            aiSummaryBox.setVisible(false);
            aiSummaryBox.setManaged(false);
        }
    }

    @FXML
    private void onClear() {
        sessionCombo.setValue(null);
        feedbackDatePicker.setValue(LocalDate.now());
        scoreSlider.setValue(50);
        strengthsArea.clear();
        weaknessesArea.clear();
        recommendationsArea.clear();
        nextActionsArea.clear();
        selected = null;
        feedbackTable.getSelectionModel().clearSelection();
        clearMsg();
        if (aiSummaryBox != null) {
            aiSummaryBox.setVisible(false);
            aiSummaryBox.setManaged(false);
        }
        if (btnDictation != null && btnDictation.isSelected()) {
            btnDictation.setSelected(false);
            onToggleDictation();
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String nullSafe(String s) {
        return s != null ? s : "";
    }

    private void showError(String msg) {
        msgLabel.setText("⚠ " + msg);
        msgLabel.setStyle("-fx-text-fill:#e0134a;");
    }

    private void showSuccess(String msg) {
        msgLabel.setText("✓ " + msg);
        msgLabel.setStyle("-fx-text-fill:#12a059;");
    }

    private void clearMsg() {
        msgLabel.setText("");
    }
}
