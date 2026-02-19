package tn.esprit.gui;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import tn.esprit.entities.Session;
import tn.esprit.services.SessionService;
import tn.esprit.utils.SessionContext;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class SessionController implements Initializable {

    @FXML
    private TextField mentorIDField;
    @FXML
    private TextField entrepreneurIDField;
    @FXML
    private TextField startupIDField;
    @FXML
    private DatePicker sessionDatePicker;
    @FXML
    private ComboBox<String> sessionTypeCombo;
    @FXML
    private ComboBox<String> statusCombo;
    @FXML
    private TextField scheduleIDField;
    @FXML
    private TextArea notesArea;
    @FXML
    private Label msgLabel;
    @FXML
    private TextField searchField;

    @FXML
    private TableView<Session> sessionTable;
    @FXML
    private TableColumn<Session, Integer> colID;
    @FXML
    private TableColumn<Session, Integer> colMentor;
    @FXML
    private TableColumn<Session, Integer> colEntrepreneur;
    @FXML
    private TableColumn<Session, Integer> colStartup;
    @FXML
    private TableColumn<Session, String> colDate;
    @FXML
    private TableColumn<Session, String> colType;
    @FXML
    private TableColumn<Session, String> colStatus;
    @FXML
    private TableColumn<Session, String> colNotes;

    private final SessionService service = new SessionService();
    private ObservableList<Session> allData;
    private Session selected;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupTableColumns();
        setupForm();
        loadTable();

        // Role-based field locking
        boolean isMentor = SessionContext.isMentor();
        mentorIDField.setEditable(false);
        entrepreneurIDField.setEditable(false);
        startupIDField.setEditable(false);

        if (!isMentor) {
            // Entrepreneurs can't change status or notes once session is created
            sessionDatePicker.setDisable(true);
            sessionTypeCombo.setDisable(true);
            statusCombo.setDisable(true);
            notesArea.setEditable(false);
            scheduleIDField.setEditable(false);
        }
    }

    private void setupTableColumns() {
        colID.setCellValueFactory(new PropertyValueFactory<>("sessionID"));
        colMentor.setCellValueFactory(new PropertyValueFactory<>("mentorID"));
        colEntrepreneur.setCellValueFactory(new PropertyValueFactory<>("entrepreneurID"));
        colStartup.setCellValueFactory(new PropertyValueFactory<>("startupID"));
        colDate.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getSessionDate().toString()));
        colType.setCellValueFactory(new PropertyValueFactory<>("sessionType"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colNotes.setCellValueFactory(new PropertyValueFactory<>("notes"));
    }

    private void setupForm() {
        sessionTypeCombo.setItems(FXCollections.observableArrayList("online", "in-person", "workshop", "review"));
        statusCombo.setItems(FXCollections.observableArrayList("planned", "ongoing", "completed", "cancelled"));
    }

    private void loadTable() {
        List<Session> data;
        if (SessionContext.isMentor()) {
            data = service.listByMentor(SessionContext.getUserId());
        } else {
            data = service.listByEvaluator(SessionContext.getUserId());
        }
        allData = FXCollections.observableArrayList(data);
        sessionTable.setItems(allData);
    }

    @FXML
    private void onUpdate() {
        if (selected == null) {
            showError("Select a session to update.");
            return;
        }
        if (!SessionContext.isMentor()) {
            showError("Only mentors can update session status.");
            return;
        }

        try {
            selected.setSessionDate(sessionDatePicker.getValue());
            selected.setSessionType(sessionTypeCombo.getValue());
            selected.setStatus(statusCombo.getValue());
            selected.setNotes(notesArea.getText());

            service.update(selected);
            showSuccess("Session status updated!");
            loadTable();
        } catch (Exception e) {
            showError("Update failed: " + e.getMessage());
        }
    }

    @FXML
    private void onDelete() {
        if (selected == null) {
            showError("Select a session to delete.");
            return;
        }
        if (!SessionContext.isMentor()) {
            showError("Only mentors can delete sessions.");
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Delete this session record?", ButtonType.YES,
                ButtonType.NO);
        alert.showAndWait().ifPresent(type -> {
            if (type == ButtonType.YES) {
                try {
                    service.delete(selected);
                    showSuccess("Session deleted.");
                    onClear();
                    loadTable();
                } catch (Exception e) {
                    showError(e.getMessage());
                }
            }
        });
    }

    @FXML
    private void onRowSelected() {
        Session s = sessionTable.getSelectionModel().getSelectedItem();
        if (s == null)
            return;
        selected = s;

        mentorIDField.setText(String.valueOf(s.getMentorID()));
        entrepreneurIDField.setText(String.valueOf(s.getEntrepreneurID()));
        startupIDField.setText(String.valueOf(s.getStartupID()));
        sessionDatePicker.setValue(s.getSessionDate());
        sessionTypeCombo.setValue(s.getSessionType());
        statusCombo.setValue(s.getStatus());
        scheduleIDField.setText(String.valueOf(s.getScheduleID()));
        notesArea.setText(s.getNotes());

        if ("completed".equalsIgnoreCase(s.getStatus()) && SessionContext.isMentor()) {
            showSuccess("Session completed! You can now add feedback in the Feedback module.");
        }
    }

    @FXML
    private void onClear() {
        selected = null;
        sessionTable.getSelectionModel().clearSelection();
        mentorIDField.clear();
        entrepreneurIDField.clear();
        startupIDField.clear();
        sessionDatePicker.setValue(null);
        sessionTypeCombo.setValue(null);
        statusCombo.setValue(null);
        scheduleIDField.clear();
        notesArea.clear();
        clearMsg();
    }

    @FXML
    private void onSearch() {
        String q = searchField.getText().toLowerCase();
        if (q.isEmpty()) {
            sessionTable.setItems(allData);
            return;
        }
        List<Session> filtered = allData.stream()
                .filter(s -> s.getSessionType().toLowerCase().contains(q)
                        || s.getStatus().toLowerCase().contains(q)
                        || (s.getNotes() != null && s.getNotes().toLowerCase().contains(q)))
                .collect(Collectors.toList());
        sessionTable.setItems(FXCollections.observableArrayList(filtered));
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

    @FXML
    private void onAdd() {
        showError("Sessions are auto-created from Bookings.");
    }
}
