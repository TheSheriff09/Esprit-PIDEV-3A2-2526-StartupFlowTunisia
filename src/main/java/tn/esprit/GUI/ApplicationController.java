package tn.esprit.GUI;

import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import tn.esprit.entities.Application;
import tn.esprit.Services.ApplicationService;
import tn.esprit.utils.CurrencyConverterService;
import tn.esprit.utils.LanguageToolService;
import tn.esprit.utils.SessionManager;

import java.io.File;

public class ApplicationController {
    @FXML
    private TextField txtEntrepreneurId;
    @FXML
    private TextField txtAmount;
    @FXML
    private TextField txtStatus;
    @FXML
    private TextField txtSubmissionDate;
    @FXML
    private TextField txtApplicationReason;
    @FXML
    private TextField txtProjectId;
    @FXML
    private TextField txtPaymentSchedule;
    @FXML
    private TextField txtAttachment;

    // Currency conversion UI
    @FXML
    private ComboBox<String> cmbFromCurrency;
    @FXML
    private ComboBox<String> cmbToCurrency;
    @FXML
    private TextField txtConvertedAmount;

    @FXML
    private TextField searchField;

    @FXML
    private TableView<Application> tableApplications;
    @FXML
    private TableColumn<Application, Integer> colId;
    @FXML
    private TableColumn<Application, Integer> colEntrepreneurId;
    @FXML
    private TableColumn<Application, Float> colAmount;
    @FXML
    private TableColumn<Application, String> colStatus;
    @FXML
    private TableColumn<Application, String> colSubmissionDate;
    @FXML
    private TableColumn<Application, String> colApplicationReason;
    @FXML
    private TableColumn<Application, Integer> colProjectId;
    @FXML
    private TableColumn<Application, String> colPaymentSchedule;
    @FXML
    private TableColumn<Application, String> colAttachment;

    private final ApplicationService service = new ApplicationService();
    private ObservableList<Application> masterData;
    private FilteredList<Application> filteredData;

    private Application selectedApplication;

    // Default project ID for when opened from a Startup card
    // selectedApplication;

    // Services
    private final CurrencyConverterService currencyService = new CurrencyConverterService();
    private final LanguageToolService languageToolService = new LanguageToolService();

    private int currentProjectId = -1;

    public void setInitialData(int entrepreneurId, int projectId) {
        this.currentProjectId = projectId;

        if (txtEntrepreneurId != null) {
            txtEntrepreneurId.setText(String.valueOf(entrepreneurId));
            txtEntrepreneurId.setDisable(true);
        }
        if (txtProjectId != null) {
            txtProjectId.setText(String.valueOf(projectId));
            txtProjectId.setDisable(true);
        }
        updateFilterPredicate();
    }

    @FXML
    private void backToDashboard(javafx.event.ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/EntrepreneurDashboard.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colEntrepreneurId.setCellValueFactory(new PropertyValueFactory<>("entrepreneurId"));
        colAmount.setCellValueFactory(new PropertyValueFactory<>("amount"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colSubmissionDate.setCellValueFactory(new PropertyValueFactory<>("submissionDate"));
        colApplicationReason.setCellValueFactory(new PropertyValueFactory<>("applicationReason"));
        colProjectId.setCellValueFactory(new PropertyValueFactory<>("projectId"));
        colPaymentSchedule.setCellValueFactory(new PropertyValueFactory<>("paymentSchedule"));
        colAttachment.setCellValueFactory(new PropertyValueFactory<>("attachment"));

        setupCurrencyUi();

        if (SessionManager.getUser() != null && txtEntrepreneurId != null && txtEntrepreneurId.getText().isEmpty()) {
            txtEntrepreneurId.setText(String.valueOf(SessionManager.getUser().getId()));
            txtEntrepreneurId.setDisable(true);
        }

        loadApplications();
        setupSearch();
        setupSelection();
    }

    // ===================== Currency =====================

    private void setupCurrencyUi() {
        if (cmbFromCurrency != null && cmbToCurrency != null) {
            cmbFromCurrency.setItems(FXCollections.observableArrayList("TND", "EUR", "USD", "GBP", "CAD", "JPY"));
            cmbToCurrency.setItems(FXCollections.observableArrayList("TND", "EUR", "USD", "GBP", "CAD", "JPY"));

            cmbFromCurrency.setValue("TND");
            cmbToCurrency.setValue("EUR");

            if (txtConvertedAmount != null) {
                txtConvertedAmount.setEditable(false);
            }

            cmbFromCurrency.valueProperty().addListener((obs, o, n) -> safeAutoConvert());
            cmbToCurrency.valueProperty().addListener((obs, o, n) -> safeAutoConvert());
            if (txtAmount != null) {
                txtAmount.textProperty().addListener((obs, o, n) -> safeAutoConvert());
            }
        }
    }

    private void safeAutoConvert() {
        if (txtAmount == null)
            return;
        String a = txtAmount.getText();
        if (a == null || a.isBlank()) {
            if (txtConvertedAmount != null)
                txtConvertedAmount.clear();
            return;
        }
        try {
            Double.parseDouble(a.trim().replace(",", "."));
            convertAmount();
        } catch (Exception ignored) {
            if (txtConvertedAmount != null)
                txtConvertedAmount.clear();
        }
    }

    @FXML
    private void convertAmount() {
        try {
            if (txtAmount.getText() == null || txtAmount.getText().isBlank()) {
                showAlert("Warning", "Enter an amount first.");
                return;
            }
            if (cmbFromCurrency == null || cmbToCurrency == null) {
                showAlert("Error", "Currency ComboBoxes are not linked in FXML.");
                return;
            }
            String from = cmbFromCurrency.getValue();
            String to = cmbToCurrency.getValue();

            double amount = Double.parseDouble(txtAmount.getText().trim().replace(",", "."));
            double converted = currencyService.convert(amount, from, to);

            if (txtConvertedAmount != null) {
                txtConvertedAmount.setText(String.format("%.2f", converted));
            }

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Currency conversion failed: " + e.getMessage());
        }
    }

    // ===================== LanguageTool: Improve Text =====================

    @FXML
    private void improveApplicationReason() {
        try {
            String original = txtApplicationReason.getText();
            if (original == null || original.trim().isEmpty()) {
                showAlert("Warning", "Write Application Reason first.");
                return;
            }

            String corrected = languageToolService.correctText(original);

            if (corrected == null || corrected.isBlank()) {
                showAlert("Info", "No correction returned.");
                return;
            }

            if (corrected.trim().equals(original.trim())) {
                showAlert("Info", "No corrections found (text already looks good).");
                return;
            }

            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("Improve Application Reason");
            dialog.setHeaderText("Corrected version (grammar + spelling)");

            TextArea area = new TextArea(corrected);
            area.setWrapText(true);
            area.setEditable(false);
            area.setPrefWidth(600);
            area.setPrefHeight(250);

            dialog.getDialogPane().setContent(area);

            ButtonType replaceBtn = new ButtonType("Replace", ButtonBar.ButtonData.OK_DONE);
            ButtonType cancelBtn = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
            dialog.getDialogPane().getButtonTypes().addAll(replaceBtn, cancelBtn);

            dialog.showAndWait().ifPresent(bt -> {
                if (bt == replaceBtn) {
                    txtApplicationReason.setText(corrected);
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Improve Text failed: " + e.getMessage());
        }
    }

    // ===================== CRUD + Table =====================

    private void loadApplications() {
        masterData = FXCollections.observableArrayList(service.list());
        filteredData = new FilteredList<>(masterData, b -> true);

        SortedList<Application> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(tableApplications.comparatorProperty());
        tableApplications.setItems(sortedData);
    }

    private void setupSearch() {
        if (searchField == null)
            return;

        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            updateFilterPredicate();
        });
    }

    private void updateFilterPredicate() {
        if (filteredData == null)
            return;

        filteredData.setPredicate(app -> {
            boolean matchesProject = (currentProjectId == -1) || (app.getProjectId() == currentProjectId);
            if (!matchesProject)
                return false;

            String searchVal = searchField != null ? searchField.getText() : "";
            if (searchVal == null || searchVal.isEmpty())
                return true;

            String keyword = searchVal.toLowerCase();
            return String.valueOf(app.getId()).contains(keyword)
                    || String.valueOf(app.getEntrepreneurId()).contains(keyword)
                    || String.valueOf(app.getAmount()).contains(keyword)
                    || safeLower(app.getStatus()).contains(keyword)
                    || safeLower(app.getSubmissionDate()).contains(keyword)
                    || safeLower(app.getApplicationReason()).contains(keyword)
                    || String.valueOf(app.getProjectId()).contains(keyword)
                    || safeLower(app.getPaymentSchedule()).contains(keyword)
                    || safeLower(app.getAttachment()).contains(keyword);
        });
    }

    private void setupSelection() {
        tableApplications.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldSelection, newSelection) -> {
                    selectedApplication = newSelection;

                    if (newSelection != null) {
                        txtEntrepreneurId.setText(String.valueOf(newSelection.getEntrepreneurId()));
                        txtAmount.setText(String.valueOf(newSelection.getAmount()));
                        txtStatus.setText(newSelection.getStatus());
                        txtSubmissionDate.setText(newSelection.getSubmissionDate());
                        txtApplicationReason.setText(newSelection.getApplicationReason());
                        txtProjectId.setText(String.valueOf(newSelection.getProjectId()));
                        txtPaymentSchedule.setText(newSelection.getPaymentSchedule());
                        txtAttachment.setText(newSelection.getAttachment());
                        safeAutoConvert();
                    }
                });
    }

    @FXML
    private void addApplication() {
        try {
            int projId = (currentProjectId != -1) ? currentProjectId : Integer.parseInt(txtProjectId.getText().trim());
            int entId = Integer.parseInt(txtEntrepreneurId.getText().trim());
            Application app = new Application(
                    0,
                    entId,
                    Float.parseFloat(txtAmount.getText().trim().replace(",", ".")),
                    txtStatus.getText(),
                    txtSubmissionDate.getText(),
                    txtApplicationReason.getText(),
                    projId,
                    txtPaymentSchedule.getText(),
                    txtAttachment.getText());
            Application inserted = service.add(app);
            if (inserted == null) {
                showAlert("Error", "Insert failed. Check DB constraints (entrepreneurId must exist in users).");
                return;
            }

            loadApplications();
            clearFields();

        } catch (Exception e) {
            showAlert("Error", "Invalid input: " + e.getMessage());
        }
    }

    @FXML
    private void updateApplication() {
        if (selectedApplication == null) {
            showAlert("Warning", "Select a row first.");
            return;
        }

        try {
            int projId = (currentProjectId != -1) ? currentProjectId : Integer.parseInt(txtProjectId.getText().trim());
            int entId = Integer.parseInt(txtEntrepreneurId.getText().trim());
            selectedApplication.setEntrepreneurId(entId);
            selectedApplication.setAmount(Float.parseFloat(txtAmount.getText().trim().replace(",", ".")));
            selectedApplication.setStatus(txtStatus.getText());
            selectedApplication.setSubmissionDate(txtSubmissionDate.getText());
            selectedApplication.setApplicationReason(txtApplicationReason.getText());
            selectedApplication.setProjectId(projId);
            selectedApplication.setPaymentSchedule(txtPaymentSchedule.getText());
            selectedApplication.setAttachment(txtAttachment.getText());

            service.update(selectedApplication);

            loadApplications();
            clearFields();

        } catch (Exception e) {
            showAlert("Error", "Invalid input: " + e.getMessage());
        }
    }

    @FXML
    private void deleteApplication() {
        if (selectedApplication == null) {
            showAlert("Warning", "Select a row first.");
            return;
        }

        service.delete(selectedApplication);
        loadApplications();
        clearFields();
    }

    @FXML
    private void clearForm() {
        clearFields();
    }

    private void clearFields() {
        if (txtEntrepreneurId != null && !txtEntrepreneurId.isDisabled())
            txtEntrepreneurId.clear();
        txtAmount.clear();
        txtStatus.clear();
        txtSubmissionDate.clear();
        txtApplicationReason.clear();
        if (txtProjectId != null && !txtProjectId.isDisabled())
            txtProjectId.clear();
        txtPaymentSchedule.clear();
        txtAttachment.clear();
        if (txtConvertedAmount != null)
            txtConvertedAmount.clear();
        selectedApplication = null;
        tableApplications.getSelectionModel().clearSelection();
    }

    private String safeLower(String s) {
        return s == null ? "" : s.toLowerCase();
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // ===================== Navigation =====================

    @FXML
    private void goToEvaluation(ActionEvent event) {
        switchScene(event, "/evaluation.fxml", "Evaluation CRUD");
    }

    @FXML
    private void goToStats(ActionEvent event) {
        switchScene(event, "/stats.fxml", "Statistics Dashboard");
    }

    private void switchScene(ActionEvent event, String fxmlPath, String title) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setTitle(title);
            stage.setScene(new Scene(root, 1300, 850));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Cannot open page: " + e.getMessage());
        }
    }

    // ===================== Attachment =====================

    @FXML
    private void browseAttachment() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Attachment");

        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("PDF Files", "*.pdf"),
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg"),
                new FileChooser.ExtensionFilter("All Files", "*.*"));

        File selectedFile = fileChooser.showOpenDialog(txtAttachment.getScene().getWindow());
        if (selectedFile != null) {
            txtAttachment.setText(selectedFile.getAbsolutePath());
        }
    }
}
