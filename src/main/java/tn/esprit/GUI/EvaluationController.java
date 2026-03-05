package tn.esprit.GUI;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import tn.esprit.Services.EvaluationService;
import tn.esprit.Services.ICRUD;
import tn.esprit.entities.Evaluation;
import tn.esprit.utils.OcrService;
import tn.esprit.utils.PdfExporter;
import tn.esprit.utils.TranslationService;

import java.io.File;
import java.util.List;

public class EvaluationController {

    @FXML private TextField txtFundingApplicationId;
    @FXML private TextField txtScore;
    @FXML private TextField txtDecision;
    @FXML private TextField txtEvaluationComments;
    @FXML private TextField txtEvaluatorId;
    @FXML private TextField txtRiskLevel;
    @FXML private TextField txtFundingCategory;

    @FXML private TextField txtOcrFile;
    @FXML private TextField searchField;

    @FXML private TableView<Evaluation> tableEvaluations;
    @FXML private TableColumn<Evaluation, Integer> colId;
    @FXML private TableColumn<Evaluation, Integer> colFundingApplicationId;
    @FXML private TableColumn<Evaluation, Integer> colScore;
    @FXML private TableColumn<Evaluation, String> colDecision;
    @FXML private TableColumn<Evaluation, String> colEvaluationComments;
    @FXML private TableColumn<Evaluation, Integer> colEvaluatorId;
    @FXML private TableColumn<Evaluation, String> colRiskLevel;
    @FXML private TableColumn<Evaluation, String> colFundingCategory;

    @FXML private ComboBox<String> cmbLanguage;

    private final ICRUD<Evaluation> service = new EvaluationService();
    private final TranslationService translationService = new TranslationService();
    private final OcrService ocrService = new OcrService();

    private ObservableList<Evaluation> masterData;
    private FilteredList<Evaluation> filteredData;

    private Evaluation selectedEvaluation;

    // ✅ Context passed from applications list
    private Integer contextApplicationId = null;
    private Integer contextEvaluatorId = null;

    @FXML
    public void initialize() {

        // Table columns
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colFundingApplicationId.setCellValueFactory(new PropertyValueFactory<>("fundingApplicationId"));
        colScore.setCellValueFactory(new PropertyValueFactory<>("score"));
        colDecision.setCellValueFactory(new PropertyValueFactory<>("decision"));
        colEvaluationComments.setCellValueFactory(new PropertyValueFactory<>("evaluationComments"));
        colEvaluatorId.setCellValueFactory(new PropertyValueFactory<>("evaluatorId"));
        colRiskLevel.setCellValueFactory(new PropertyValueFactory<>("riskLevel"));
        colFundingCategory.setCellValueFactory(new PropertyValueFactory<>("fundingCategory"));

        // Input constraints
        limitNumericField(txtFundingApplicationId, 10);
        limitNumericField(txtScore, 10);
        limitNumericField(txtEvaluatorId, 10);

        limitTextField(txtDecision, 50);
        limitTextField(txtRiskLevel, 50);
        limitTextField(txtFundingCategory, 50);

        // Load all by default (will be replaced by filtered after setContext)
        loadEvaluationsAll();

        setupSearch();
        setupSelection();

        if (cmbLanguage != null) {
            cmbLanguage.getItems().setAll("en", "fr", "ar", "de", "es", "it");
            cmbLanguage.setValue("fr");
        }
    }
    @FXML
    private void goToApplications(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/evaluator_applications.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Cannot open applications list: " + e.getMessage());
        }
    }
    @FXML
    private void goToDashboard(ActionEvent event) {
        backToDashboard(event);
    }

    @FXML
    private void goToDashboard(javafx.scene.input.MouseEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/EvaluatorDashboard.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Cannot go back: " + e.getMessage());
        }
    }
    /**
     * ✅ Called from EvaluatorApplicationsController after loading evaluation.fxml
     */
    public void setContext(int applicationId, int evaluatorId) {
        this.contextApplicationId = applicationId;
        this.contextEvaluatorId = evaluatorId;

        // Autofill and lock fields
        if (txtFundingApplicationId != null) {
            txtFundingApplicationId.setText(String.valueOf(applicationId));
            txtFundingApplicationId.setEditable(false);
            txtFundingApplicationId.setDisable(true);
        }
        if (txtEvaluatorId != null) {
            txtEvaluatorId.setText(String.valueOf(evaluatorId));
            txtEvaluatorId.setEditable(false);
            txtEvaluatorId.setDisable(true);
        }

        // Load only evaluations related to this application (recommended)
        loadEvaluationsFilteredByApplication();
    }

    // ===================== Load data =====================

    private void loadEvaluationsAll() {
        masterData = FXCollections.observableArrayList(service.list());
        bindTable(masterData);
    }

    private void loadEvaluationsFilteredByApplication() {
        if (contextApplicationId == null) {
            loadEvaluationsAll();
            return;
        }

        // We need the concrete method listByApplicationId => cast to EvaluationService
        EvaluationService es = (EvaluationService) service;
        List<Evaluation> list = es.listByApplicationId(contextApplicationId);

        masterData = FXCollections.observableArrayList(list);
        bindTable(masterData);
    }

    private void bindTable(ObservableList<Evaluation> data) {
        filteredData = new FilteredList<>(data, b -> true);
        SortedList<Evaluation> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(tableEvaluations.comparatorProperty());
        tableEvaluations.setItems(sortedData);
    }

    // ===================== Search =====================

    private void setupSearch() {
        if (searchField == null) return;

        searchField.textProperty().addListener((obs, oldValue, newValue) -> {
            filteredData.setPredicate(e -> {
                if (newValue == null || newValue.trim().isEmpty()) return true;
                String k = newValue.toLowerCase();

                return String.valueOf(e.getId()).contains(k)
                        || String.valueOf(e.getFundingApplicationId()).contains(k)
                        || String.valueOf(e.getScore()).contains(k)
                        || safeLower(e.getDecision()).contains(k)
                        || safeLower(e.getEvaluationComments()).contains(k)
                        || String.valueOf(e.getEvaluatorId()).contains(k)
                        || safeLower(e.getRiskLevel()).contains(k)
                        || safeLower(e.getFundingCategory()).contains(k);
            });
        });
    }

    // ===================== Selection =====================

    private void setupSelection() {
        tableEvaluations.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            selectedEvaluation = newSel;
            fillFieldsFromSelection();
        });
    }

    private void fillFieldsFromSelection() {
        if (selectedEvaluation == null) return;

        // keep locked values if context exists
        if (contextApplicationId == null) {
            txtFundingApplicationId.setText(String.valueOf(selectedEvaluation.getFundingApplicationId()));
        }
        txtScore.setText(String.valueOf(selectedEvaluation.getScore()));
        txtDecision.setText(nullToEmpty(selectedEvaluation.getDecision()));
        txtEvaluationComments.setText(nullToEmpty(selectedEvaluation.getEvaluationComments()));

        if (contextEvaluatorId == null) {
            txtEvaluatorId.setText(String.valueOf(selectedEvaluation.getEvaluatorId()));
        }

        txtRiskLevel.setText(nullToEmpty(selectedEvaluation.getRiskLevel()));
        txtFundingCategory.setText(nullToEmpty(selectedEvaluation.getFundingCategory()));
    }

    // ===================== CRUD =====================

    @FXML
    private void addEvaluation() {
        try {
            int appId = (contextApplicationId != null) ? contextApplicationId : parseIntRequired(txtFundingApplicationId);
            int evaluatorId = (contextEvaluatorId != null) ? contextEvaluatorId : parseIntRequired(txtEvaluatorId);

            Evaluation e = new Evaluation(
                    0,
                    appId,
                    parseIntRequired(txtScore),
                    txtDecision.getText().trim(),
                    txtEvaluationComments.getText().trim(),
                    evaluatorId,
                    txtRiskLevel.getText().trim(),
                    txtFundingCategory.getText().trim()
            );

            Evaluation added = service.add(e);
            if (added == null) {
                showAlert("Error", "Insert failed. Check foreign keys and input.");
                return;
            }

            loadEvaluationsFilteredByApplication();
            clearFieldsButKeepLocked();
            showAlert("Success", "Evaluation added successfully!");

        } catch (Exception ex) {
            ex.printStackTrace();
            showAlert("Error", ex.getMessage());
        }
    }

    @FXML
    private void updateEvaluation() {
        if (selectedEvaluation == null) {
            showAlert("Warning", "Select a row first.");
            return;
        }

        try {
            int appId = (contextApplicationId != null) ? contextApplicationId : parseIntRequired(txtFundingApplicationId);
            int evaluatorId = (contextEvaluatorId != null) ? contextEvaluatorId : parseIntRequired(txtEvaluatorId);

            selectedEvaluation.setFundingApplicationId(appId);
            selectedEvaluation.setScore(parseIntRequired(txtScore));
            selectedEvaluation.setDecision(txtDecision.getText().trim());
            selectedEvaluation.setEvaluationComments(txtEvaluationComments.getText().trim());
            selectedEvaluation.setEvaluatorId(evaluatorId);
            selectedEvaluation.setRiskLevel(txtRiskLevel.getText().trim());
            selectedEvaluation.setFundingCategory(txtFundingCategory.getText().trim());

            service.update(selectedEvaluation);

            loadEvaluationsFilteredByApplication();
            clearFieldsButKeepLocked();
            showAlert("Success", "Evaluation updated successfully!");

        } catch (Exception ex) {
            ex.printStackTrace();
            showAlert("Error", ex.getMessage());
        }
    }

    @FXML
    private void deleteEvaluation() {
        if (selectedEvaluation == null) {
            showAlert("Warning", "Select a row first.");
            return;
        }

        service.delete(selectedEvaluation);

        loadEvaluationsFilteredByApplication();
        clearFieldsButKeepLocked();
        showAlert("Success", "Evaluation deleted successfully!");
    }

    private void clearFieldsButKeepLocked() {
        txtScore.clear();
        txtDecision.clear();
        txtEvaluationComments.clear();
        txtRiskLevel.clear();
        txtFundingCategory.clear();
        if (txtOcrFile != null) txtOcrFile.clear();
        selectedEvaluation = null;
        if (tableEvaluations != null) tableEvaluations.getSelectionModel().clearSelection();

        // re-set locked ids if context exists
        if (contextApplicationId != null && txtFundingApplicationId != null) {
            txtFundingApplicationId.setText(String.valueOf(contextApplicationId));
        }
        if (contextEvaluatorId != null && txtEvaluatorId != null) {
            txtEvaluatorId.setText(String.valueOf(contextEvaluatorId));
        }
    }

    // ===================== OCR =====================

    @FXML
    private void browseOcrFile() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Select PDF/Image for OCR");
        fc.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("PDF Files", "*.pdf"),
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.bmp"),
                new FileChooser.ExtensionFilter("All Files", "*.*")
        );

        File file = fc.showOpenDialog(txtOcrFile.getScene().getWindow());
        if (file != null) {
            txtOcrFile.setText(file.getAbsolutePath());
        }
    }

    @FXML
    private void ocrExtractToComments() {
        try {
            String path = txtOcrFile.getText();
            if (path == null || path.isBlank()) {
                showAlert("Warning", "Please choose a PDF/Image file first.");
                return;
            }
            File f = new File(path);
            if (!f.exists()) {
                showAlert("Error", "File not found.");
                return;
            }

            String text = ocrService.extractText(f);

            if (text == null || text.isBlank()) {
                showAlert("Info", "OCR finished but no text was detected.");
                return;
            }

            txtEvaluationComments.setText(text);
            showAlert("Success", "OCR text extracted into Evaluation Comments.");

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "OCR failed: " + e.getMessage());
        }
    }

    // ===================== Translation =====================

    @FXML
    private void translateComments() {
        try {
            String targetLang = (cmbLanguage == null) ? null : cmbLanguage.getValue();
            if (targetLang == null || targetLang.isBlank()) {
                showAlert("Warning", "Select a language first.");
                return;
            }

            String original = txtEvaluationComments.getText();
            if (original == null || original.isBlank()) {
                showAlert("Warning", "Write comments first.");
                return;
            }

            String translated = translationService.translate(original, targetLang);
            txtEvaluationComments.setText(translated);

            showAlert("Success", "Translated to " + targetLang);

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Translation failed: " + e.getMessage());
        }
    }

    // ===================== PDF =====================

    @FXML
    private void exportPdf() {
        try {
            FileChooser fc = new FileChooser();
            fc.setTitle("Save Evaluations PDF");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
            fc.setInitialFileName("evaluations.pdf");

            File file = fc.showSaveDialog(tableEvaluations.getScene().getWindow());
            if (file != null) {
                PdfExporter.exportTableView(tableEvaluations, file, "Funding Evaluations");
                showAlert("Success", "PDF exported:\n" + file.getAbsolutePath());
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "PDF export failed: " + e.getMessage());
        }
    }

    // ===================== Navigation (optional) =====================

    @FXML
    private void backToDashboard(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/EvaluatorDashboard.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Cannot go back: " + e.getMessage());
        }
    }

    // ===================== Helpers =====================

    private void limitTextField(TextField field, int maxLength) {
        if (field == null) return;
        field.setTextFormatter(new TextFormatter<String>(change ->
                change.getControlNewText().length() <= maxLength ? change : null));
    }

    private void limitNumericField(TextField field, int maxLength) {
        if (field == null) return;
        field.setTextFormatter(new TextFormatter<String>(change -> {
            String newText = change.getControlNewText();
            if (newText.matches("\\d*") && newText.length() <= maxLength) {
                return change;
            }
            return null;
        }));
    }

    private int parseIntRequired(TextField field) {
        if (field == null || field.getText() == null || field.getText().trim().isEmpty()) {
            throw new IllegalArgumentException("All numeric fields are required.");
        }
        return Integer.parseInt(field.getText().trim());
    }

    private String safeLower(String s) {
        return s == null ? "" : s.toLowerCase();
    }

    private String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
