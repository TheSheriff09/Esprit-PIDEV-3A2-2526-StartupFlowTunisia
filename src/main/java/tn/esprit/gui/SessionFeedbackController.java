package tn.esprit.gui;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import tn.esprit.entities.Session;
import tn.esprit.entities.SessionFeedback;
import tn.esprit.services.SessionFeedbackService;
import tn.esprit.services.SessionService;
import tn.esprit.utils.SessionContext;

import java.net.URL;
import java.time.LocalDate;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class SessionFeedbackController implements Initializable {

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
    private TextField searchField;
    @FXML
    private HBox actionBox;
    @FXML
    private Label dateLabel;

    private final SessionFeedbackService service = new SessionFeedbackService();
    private final SessionService sessionSvc = new SessionService();
    private ObservableList<SessionFeedback> allData;
    private SessionFeedback selected;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupTableColumns();
        setupSlider();

        int uid = SessionContext.getUserId();
        boolean isMentor = SessionContext.isMentor();

        // Auto-set date to today
        feedbackDatePicker.setValue(LocalDate.now());

        if (isMentor) {
            mentorIDField.setText(String.valueOf(uid));
            mentorIDField.setDisable(true);

            // Only show COMPLETED sessions for feedback
            List<Session> completedSessions = sessionSvc.listByMentor(uid).stream()
                    .filter(s -> "completed".equalsIgnoreCase(s.getStatus()))
                    .collect(Collectors.toList());
            sessionCombo.setItems(FXCollections.observableArrayList(completedSessions));

            if (completedSessions.isEmpty()) {
                sessionCombo.setPromptText("No completed sessions yet.");
            }
        } else {
            // Entrepreneur View: Hide actions and make form read-only
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

        loadTable();
    }

    private void setupTableColumns() {
        colID.setCellValueFactory(new PropertyValueFactory<>("feedbackID"));
        colSession.setCellValueFactory(new PropertyValueFactory<>("sessionID"));
        colMentor.setCellValueFactory(new PropertyValueFactory<>("mentorID"));
        colScore.setCellValueFactory(new PropertyValueFactory<>("progressScore"));
        colDate.setCellValueFactory(cd -> new SimpleStringProperty(
                cd.getValue().getFeedbackDate() != null ? cd.getValue().getFeedbackDate().toString() : ""));
        colStrengths.setCellValueFactory(new PropertyValueFactory<>("strengths"));
    }

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
    private TableColumn<SessionFeedback, String> colStrengths;
    @FXML
    private TableView<SessionFeedback> feedbackTable;

    private void setupSlider() {
        scoreLabel.setText(String.valueOf((int) scoreSlider.getValue()));
        scoreSlider.valueProperty().addListener((obs, o, v) -> scoreLabel.setText(String.valueOf(v.intValue())));
    }

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
                s.getSessionID(),
                SessionContext.getUserId(),
                (int) scoreSlider.getValue(),
                strengths,
                weaknessesArea.getText().trim(),
                recommendationsArea.getText().trim(),
                nextActionsArea.getText().trim(),
                LocalDate.now() // Auto-set
        );
    }

    private void loadTable() {
        List<SessionFeedback> data;
        int uid = SessionContext.getUserId();
        if (SessionContext.isMentor()) {
            data = service.listByMentor(uid);
        } else {
            // Entrepreneurs see feedback related to their sessions
            data = service.list().stream()
                    .filter(f -> sessionSvc.listByEvaluator(uid).stream()
                            .anyMatch(s -> s.getSessionID() == f.getSessionID()))
                    .collect(Collectors.toList());
        }
        allData = FXCollections.observableArrayList(data);
        feedbackTable.setItems(allData);
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
    }

    @FXML
    private void onSearch() {
        String q = searchField.getText().toLowerCase();
        if (q.isEmpty()) {
            feedbackTable.setItems(allData);
            return;
        }
        List<SessionFeedback> f = allData.stream()
                .filter(fb -> fb.getStrengths().toLowerCase().contains(q)
                        || fb.getWeaknesses().toLowerCase().contains(q))
                .collect(Collectors.toList());
        feedbackTable.setItems(FXCollections.observableArrayList(f));
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
