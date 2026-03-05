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
import javafx.stage.Stage;
import tn.esprit.entities.User;

// IMPORTANT: adapt imports to your real classes:
import tn.esprit.entities.Application; // your funding application entity
import tn.esprit.Services.ApplicationService; // or tn.esprit.service.ApplicationService depending on your package

public class EvaluatorApplicationsController {

    @FXML
    private TextField searchField;

    @FXML
    private TableView<Application> tableApps;
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
    private TableColumn<Application, String> colReason;
    @FXML
    private TableColumn<Application, Integer> colProjectId;
    @FXML
    private TableColumn<Application, String> colSchedule;

    private final ApplicationService appService = new ApplicationService();

    private ObservableList<Application> masterData;
    private FilteredList<Application> filteredData;

    @FXML
    public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colEntrepreneurId.setCellValueFactory(new PropertyValueFactory<>("entrepreneurId"));
        colAmount.setCellValueFactory(new PropertyValueFactory<>("amount"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colSubmissionDate.setCellValueFactory(new PropertyValueFactory<>("submissionDate"));
        colReason.setCellValueFactory(new PropertyValueFactory<>("applicationReason"));
        colProjectId.setCellValueFactory(new PropertyValueFactory<>("projectId"));
        colSchedule.setCellValueFactory(new PropertyValueFactory<>("paymentSchedule"));

        loadApplications();
        setupSearch();
    }

    private void loadApplications() {
        // If your ApplicationService method is list(), use it:
        masterData = FXCollections.observableArrayList(appService.list());
        filteredData = new FilteredList<>(masterData, b -> true);

        SortedList<Application> sorted = new SortedList<>(filteredData);
        sorted.comparatorProperty().bind(tableApps.comparatorProperty());
        tableApps.setItems(sorted);
    }

    private void setupSearch() {
        if (searchField == null)
            return;

        searchField.textProperty().addListener((obs, oldV, newV) -> {
            filteredData.setPredicate(a -> {
                if (newV == null || newV.trim().isEmpty())
                    return true;
                String k = newV.toLowerCase();

                return String.valueOf(a.getId()).contains(k)
                        || String.valueOf(a.getEntrepreneurId()).contains(k)
                        || String.valueOf(a.getAmount()).contains(k)
                        || safeLower(a.getStatus()).contains(k)
                        || safeLower(a.getSubmissionDate()).contains(k)
                        || safeLower(a.getApplicationReason()).contains(k)
                        || String.valueOf(a.getProjectId()).contains(k)
                        || safeLower(a.getPaymentSchedule()).contains(k);
            });
        });
    }

    private String safeLower(String s) {
        return s == null ? "" : s.toLowerCase();
    }

    @FXML
    private void evaluateSelected(ActionEvent event) {
        Application selected = tableApps.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Warning", "Select an application first.");
            return;
        }

        User u = tn.esprit.utils.SessionManager.getUser();
        if (u == null) {
            showAlert("Error", "No logged-in evaluator found (SessionManager.user is null).");
            return;
        }

        int applicationId = selected.getId();
        int evaluatorId = u.getId(); // must exist in User

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/evaluation.fxml"));
            Parent root = loader.load();

            EvaluationController controller = loader.getController();
            controller.setContext(applicationId, evaluatorId);

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Cannot open evaluation page: " + e.getMessage());
        }
    }

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

    private void showAlert(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}
