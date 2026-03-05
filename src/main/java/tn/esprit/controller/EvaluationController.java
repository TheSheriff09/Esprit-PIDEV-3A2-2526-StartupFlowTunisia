package tn.esprit.controller;

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
import tn.esprit.entity.Evaluation;
import tn.esprit.service.EvaluationService;
import tn.esprit.service.ICrud;
import tn.esprit.utils.PdfExporter;
import tn.esprit.utils.TranslationService;
import tn.esprit.utils.OcrService;

import java.io.File;

public class EvaluationController {

    @FXML private TextField txtFundingApplicationId;
    @FXML private TextField txtScore;
    @FXML private TextField txtDecision;
    @FXML private TextField txtEvaluationComments;
    @FXML private TextField txtEvaluatorId;
    @FXML private TextField txtRiskLevel;
    @FXML private TextField txtFundingCategory;

    @FXML private TextField txtOcrFile; // NEW
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

    private final ICrud<Evaluation> service = new EvaluationService();
    private final TranslationService translationService = new TranslationService();
    private final OcrService ocrService = new OcrService(); // NEW

    private ObservableList<Evaluation> masterData;
    private FilteredList<Evaluation> filteredData;

    private Evaluation selectedEvaluation;

    @FXML
    public void initialize() {

        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colFundingApplicationId.setCellValueFactory(new PropertyValueFactory<>("fundingApplicationId"));
        colScore.setCellValueFactory(new PropertyValueFactory<>("score"));
        colDecision.setCellValueFactory(new PropertyValueFactory<>("decision"));
        colEvaluationComments.setCellValueFactory(new PropertyValueFactory<>("evaluationComments"));
        colEvaluatorId.setCellValueFactory(new PropertyValueFactory<>("evaluatorId"));
        colRiskLevel.setCellValueFactory(new PropertyValueFactory<>("riskLevel"));
        colFundingCategory.setCellValueFactory(new PropertyValueFactory<>("fundingCategory"));

        limitNumericField(txtFundingApplicationId, 8);
        limitNumericField(txtScore, 8);
        limitNumericField(txtEvaluatorId, 8);

        limitTextField(txtDecision, 50);
        limitTextField(txtRiskLevel, 50);
        limitTextField(txtFundingCategory, 50);

        loadEvaluations();
        setupSearch();
        setupSelection();

        tableEvaluations.setOnMouseClicked(e -> fillFieldsFromSelection());

        cmbLanguage.getItems().setAll("en", "fr", "ar", "de", "es", "it");
        cmbLanguage.setValue("fr");
    }

    private void loadEvaluations() {
        masterData = FXCollections.observableArrayList(service.getAll());
        filteredData = new FilteredList<>(masterData, b -> true);

        SortedList<Evaluation> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(tableEvaluations.comparatorProperty());
        tableEvaluations.setItems(sortedData);
    }

    private void setupSearch() {
        searchField.textProperty().addListener((obs, oldValue, newValue) -> {
            filteredData.setPredicate(e -> {
                if (newValue == null || newValue.trim().isEmpty()) return true;

                String keyword = newValue.toLowerCase();
                return String.valueOf(e.getId()).contains(keyword)
                        || String.valueOf(e.getFundingApplicationId()).contains(keyword)
                        || String.valueOf(e.getScore()).contains(keyword)
                        || safeLower(e.getDecision()).contains(keyword)
                        || safeLower(e.getEvaluationComments()).contains(keyword)
                        || String.valueOf(e.getEvaluatorId()).contains(keyword)
                        || safeLower(e.getRiskLevel()).contains(keyword)
                        || safeLower(e.getFundingCategory()).contains(keyword);
            });
        });
    }

    private void setupSelection() {
        tableEvaluations.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            selectedEvaluation = newSel;
            fillFieldsFromSelection();
        });
    }

    private void fillFieldsFromSelection() {
        Evaluation sel = tableEvaluations.getSelectionModel().getSelectedItem();
        selectedEvaluation = sel;
        if (sel == null) return;

        txtFundingApplicationId.setText(String.valueOf(sel.getFundingApplicationId()));
        txtScore.setText(String.valueOf(sel.getScore()));
        txtDecision.setText(nullToEmpty(sel.getDecision()));
        txtEvaluationComments.setText(nullToEmpty(sel.getEvaluationComments()));
        txtEvaluatorId.setText(String.valueOf(sel.getEvaluatorId()));
        txtRiskLevel.setText(nullToEmpty(sel.getRiskLevel()));
        txtFundingCategory.setText(nullToEmpty(sel.getFundingCategory()));
    }

    // -------- OCR: Browse file ----------
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

    // -------- OCR: Extract and put into comments ----------
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

            if (text.isBlank()) {
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

    // -------- Translation ----------
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

    // -------- CRUD ----------
    @FXML
    private void addEvaluation() {
        try {
            Evaluation e = new Evaluation(
                    0,
                    parseIntRequired(txtFundingApplicationId),
                    parseIntRequired(txtScore),
                    txtDecision.getText().trim(),
                    txtEvaluationComments.getText().trim(),
                    parseIntRequired(txtEvaluatorId),
                    txtRiskLevel.getText().trim(),
                    txtFundingCategory.getText().trim()
            );

            service.add(e);
            loadEvaluations();
            clearFields();
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
            selectedEvaluation.setFundingApplicationId(parseIntRequired(txtFundingApplicationId));
            selectedEvaluation.setScore(parseIntRequired(txtScore));
            selectedEvaluation.setDecision(txtDecision.getText().trim());
            selectedEvaluation.setEvaluationComments(txtEvaluationComments.getText().trim());
            selectedEvaluation.setEvaluatorId(parseIntRequired(txtEvaluatorId));
            selectedEvaluation.setRiskLevel(txtRiskLevel.getText().trim());
            selectedEvaluation.setFundingCategory(txtFundingCategory.getText().trim());

            service.update(selectedEvaluation);
            loadEvaluations();
            clearFields();
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

        service.delete(selectedEvaluation.getId());
        loadEvaluations();
        clearFields();
        showAlert("Success", "Evaluation deleted successfully!");
    }

    // -------- PDF ----------
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

    // -------- Navigation ----------
    @FXML
    private void goToDashboard(ActionEvent event) {
        switchScene(event, "/gui/dashboard.fxml", "Dashboard");
    }

    @FXML
    private void goToApplications(ActionEvent event) {
        switchScene(event, "/gui/application.fxml", "Application CRUD");
    }

    @FXML
    private void goToStats(ActionEvent event) {
        switchScene(event, "/gui/stats.fxml", "Statistics Dashboard");
    }

    private void switchScene(ActionEvent event, String path, String title) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(path));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setTitle(title);
            stage.setScene(new Scene(root, 1300, 850));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Cannot open page: " + e.getMessage());
        }
    }

    // -------- Helpers ----------
    private void limitTextField(TextField field, int maxLength) {
        field.setTextFormatter(new TextFormatter<String>(change ->
                change.getControlNewText().length() <= maxLength ? change : null));
    }

    private void limitNumericField(TextField field, int maxLength) {
        field.setTextFormatter(new TextFormatter<String>(change -> {
            String newText = change.getControlNewText();
            if (newText.matches("\\d*") && newText.length() <= maxLength) {
                return change;
            }
            return null;
        }));
    }

    private int parseIntRequired(TextField field) {
        if (field.getText() == null || field.getText().trim().isEmpty()) {
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

    private void clearFields() {
        txtFundingApplicationId.clear();
        txtScore.clear();
        txtDecision.clear();
        txtEvaluationComments.clear();
        txtEvaluatorId.clear();
        txtRiskLevel.clear();
        txtFundingCategory.clear();
        if (txtOcrFile != null) txtOcrFile.clear();
        selectedEvaluation = null;
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}