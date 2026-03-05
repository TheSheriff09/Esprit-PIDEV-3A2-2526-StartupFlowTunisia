package tn.esprit.GUI;

import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import tn.esprit.Services.*;
import tn.esprit.Services.SortingService.MentorSort;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

/**
 * AiInsightsController — Integrates GrokService and AnalyticsService into
 * a unified AI Insights view with 3 Grok-powered features and a mentor ranking
 * table.
 *
 * <p>
 * Threading model:
 * <ul>
 * <li>All Grok API calls use {@link GrokService#...Async()} which returns
 * {@code CompletableFuture<String>}.</li>
 * <li>UI updates are posted to the JavaFX thread via
 * {@code Platform.runLater()}.</li>
 * <li>The UI never freezes — a status label shows loading state during the
 * call.</li>
 * </ul>
 */
public class AiInsightsController implements Initializable {

    // ── Table ──────────────────────────────────────────────────────────────
    @FXML
    private TableView<AnalyticsService.MentorStats> mentorTable;
    @FXML
    private TableColumn<AnalyticsService.MentorStats, Integer> colMentorRank;
    @FXML
    private TableColumn<AnalyticsService.MentorStats, String> colMentorName;
    @FXML
    private TableColumn<AnalyticsService.MentorStats, String> colMentorMPI;
    @FXML
    private TableColumn<AnalyticsService.MentorStats, Double> colMentorRating;
    @FXML
    private TableColumn<AnalyticsService.MentorStats, Integer> colMentorSessions;
    @FXML
    private ComboBox<MentorSort> mentorSortCombo;

    // ── AI Tab 1: Explain Recommendation ──────────────────────────────────
    @FXML
    private TextArea explainArea;
    @FXML
    private Label explainStatusLabel;

    // ── AI Tab 2: Mentorship Advice ────────────────────────────────────────
    @FXML
    private TextArea contextArea;
    @FXML
    private TextArea adviceArea;
    @FXML
    private Label adviceStatusLabel;

    // ── AI Tab 3: Feedback Summary ─────────────────────────────────────────
    @FXML
    private TextArea feedbackInputArea;
    @FXML
    private TextArea summaryArea;
    @FXML
    private Label summaryStatusLabel;

    // ── Status bar ─────────────────────────────────────────────────────────
    @FXML
    private Label statusLabel;

    // ── Services ───────────────────────────────────────────────────────────
    private final AnalyticsService analyticsService = new AnalyticsService();
    private final GrokService grokService = GrokService.getInstance();

    private ObservableList<AnalyticsService.MentorStats> mentorData;

    // ── Init ───────────────────────────────────────────────────────────────

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupTable();
        setupSortCombo();
        loadRankings();
    }

    private void setupTable() {
        colMentorRank
                .setCellValueFactory(cd -> new SimpleIntegerProperty(mentorData.indexOf(cd.getValue()) + 1).asObject());
        colMentorName.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getMentor().getFullName()));
        colMentorMPI.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getMpiDisplay()));
        colMentorRating.setCellValueFactory(cd -> new SimpleDoubleProperty(cd.getValue().getAvgRating()).asObject());
        colMentorSessions
                .setCellValueFactory(cd -> new SimpleIntegerProperty(cd.getValue().getSessionCount()).asObject());

        // Colour-code MPI cells: green ≥70, orange ≥50, red <50
        colMentorMPI.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String mpi, boolean empty) {
                super.updateItem(mpi, empty);
                if (empty || mpi == null) {
                    setText(null);
                    setStyle("");
                    return;
                }
                setText(mpi);
                double val = Double.parseDouble(mpi);
                if (val >= 70)
                    setStyle("-fx-text-fill:#12a059;-fx-font-weight:700;");
                else if (val >= 50)
                    setStyle("-fx-text-fill:#e07c00;-fx-font-weight:700;");
                else
                    setStyle("-fx-text-fill:#e0134a;-fx-font-weight:700;");
            }
        });
    }

    private void setupSortCombo() {
        mentorSortCombo.setItems(FXCollections.observableArrayList(MentorSort.values()));
        mentorSortCombo.setValue(MentorSort.MPI_DESC);
    }

    // ── Data Loading ───────────────────────────────────────────────────────

    private void loadRankings() {
        setStatus("⏳ Loading mentor rankings…", false);
        // Load in background to avoid blocking JavaFX thread for DB calls
        Thread t = new Thread(() -> {
            try {
                List<AnalyticsService.MentorStats> ranked = analyticsService.getRankedMentors();
                Platform.runLater(() -> {
                    mentorData = FXCollections.observableArrayList(ranked);
                    mentorTable.setItems(mentorData);
                    setStatus("✓ " + ranked.size() + " mentors ranked by MPI", false);
                });
            } catch (Exception e) {
                Platform.runLater(() -> setStatus("⚠ Failed to load rankings: " + e.getMessage(), true));
            }
        });
        t.setDaemon(true);
        t.start();
    }

    @FXML
    private void onRefreshRankings() {
        loadRankings();
    }

    @FXML
    private void onMentorSortChanged() {
        if (mentorData == null || mentorData.isEmpty())
            return;
        MentorSort sort = mentorSortCombo.getValue();
        if (sort == null)
            return;
        List<AnalyticsService.MentorStats> sorted = SortingService.sortMentors(mentorData, sort);
        mentorData.setAll(sorted);
    }

    // ── AI Feature 1: Explain Recommendation ──────────────────────────────

    @FXML
    private void onExplainMentor() {
        AnalyticsService.MentorStats selected = mentorTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            explainStatusLabel.setText("⚠ Please select a mentor from the table first.");
            return;
        }

        explainArea.setText("");
        explainStatusLabel.setText("⏳ Asking Grok AI… (this may take a few seconds)");

        String name = selected.getMentor().getFullName();
        String expertise = selected.getMentor().getMentorExpertise();
        double mpi = selected.mpi();

        grokService.explainRecommendationAsync(name, expertise != null ? expertise : "General", mpi)
                .thenAccept(response -> Platform.runLater(() -> {
                    explainArea.setText(response);
                    explainStatusLabel.setText("✓ AI explanation ready");
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> explainStatusLabel.setText("⚠ AI call failed"));
                    return null;
                });
    }

    // ── AI Feature 2: Mentorship Advice ───────────────────────────────────

    @FXML
    private void onGenerateAdvice() {
        String context = contextArea.getText().trim();
        if (context.isEmpty()) {
            adviceStatusLabel.setText("⚠ Please describe your business context first.");
            return;
        }

        adviceArea.setText("");
        adviceStatusLabel.setText("⏳ Generating advice… (this may take a few seconds)");

        grokService.generateMentorshipAdviceAsync(context)
                .thenAccept(response -> Platform.runLater(() -> {
                    adviceArea.setText(response);
                    adviceStatusLabel.setText("✓ Advice generated");
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> adviceStatusLabel.setText("⚠ AI call failed"));
                    return null;
                });
    }

    // ── AI Feature 3: Feedback Summarization ──────────────────────────────

    @FXML
    private void onSummarizeFeedback() {
        String text = feedbackInputArea.getText().trim();
        if (text.isEmpty()) {
            summaryStatusLabel.setText("⚠ Please paste feedback text first.");
            return;
        }

        summaryArea.setText("");
        summaryStatusLabel.setText("⏳ Summarizing… (this may take a few seconds)");

        grokService.summarizeFeedbackAsync(text)
                .thenAccept(response -> Platform.runLater(() -> {
                    summaryArea.setText(response);
                    summaryStatusLabel.setText("✓ Summary ready");
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> summaryStatusLabel.setText("⚠ AI call failed"));
                    return null;
                });
    }

    // ── Export ─────────────────────────────────────────────────────────────

    @FXML
    private void onExportMentorCSV() {
        if (mentorData == null || mentorData.isEmpty()) {
            setStatus("⚠ No data to export. Please load rankings first.", true);
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save Mentor Performance CSV");
        chooser.setInitialFileName("mentor_performance.csv");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("CSV Files", "*.csv"));

        File file = chooser.showSaveDialog(mentorTable.getScene().getWindow());
        if (file == null)
            return;

        try {
            CsvExportService.exportMentorPerformanceToCSV(mentorData, file);
            setStatus("✓ Exported to: " + file.getName(), false);
        } catch (Exception e) {
            setStatus("⚠ Export failed: " + e.getMessage(), true);
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private void setStatus(String msg, boolean error) {
        if (statusLabel == null)
            return;
        statusLabel.setText(msg);
        statusLabel.setStyle(error ? "-fx-text-fill:#e0134a;" : "-fx-text-fill:#5a4bd6;");
    }
}
