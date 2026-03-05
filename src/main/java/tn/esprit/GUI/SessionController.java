package tn.esprit.GUI;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import tn.esprit.entities.Session;
import tn.esprit.Services.CsvExportService;
import tn.esprit.Services.SessionService;
import tn.esprit.Services.SortingService;
import tn.esprit.Services.SortingService.SessionSort;
import tn.esprit.utils.SessionManager;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

/**
 * SessionController — upgraded with:
 * <ul>
 * <li><b>FilteredList</b>: real-time search on ObservableList (no
 * blocking).</li>
 * <li><b>Multi-criteria sort</b>: ComboBox drives SortingService Comparator
 * chains.</li>
 * <li><b>CSV export</b>: FileChooser + CsvExportService.</li>
 * </ul>
 *
 * <p>
 * <b>Search logic</b> (AND semantics):
 * Query is matched against session type, status, notes, and date string. The
 * predicate
 * is attached to a {@link FilteredList} wrapping the master
 * {@link ObservableList}, so
 * adding/updating records automatically re-applies the filter.
 */
public class SessionController implements Initializable {

    // ── Form ───────────────────────────────────────────────────────────────
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

    // ── To-Do List ─────────────────────────────────────────────────────────
    @FXML
    private javafx.scene.layout.VBox todoBox;
    @FXML
    private Label todoProgressLabel;
    @FXML
    private ListView<tn.esprit.entities.SessionTodo> todoList;
    @FXML
    private TextField newTodoField;

    private final tn.esprit.Services.SessionTodoService todoService = new tn.esprit.Services.SessionTodoService();

    // ── Search & Sort ──────────────────────────────────────────────────────
    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<SessionSort> sortCombo;

    // ── Table ──────────────────────────────────────────────────────────────
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
    private TableColumn<Session, String> colSuccess;
    @FXML
    private TableColumn<Session, String> colNotes;

    // ── Services ───────────────────────────────────────────────────────────
    private final SessionService service = new SessionService();

    // ── Data ───────────────────────────────────────────────────────────────
    private ObservableList<Session> masterData;
    private FilteredList<Session> filteredData;
    private Session selected;

    // ── Init ───────────────────────────────────────────────────────────────

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupTableColumns();
        setupForm();
        setupSortCombo();
        loadTable();

        // Role-based field locking
        mentorIDField.setEditable(false);
        entrepreneurIDField.setEditable(false);
        startupIDField.setEditable(false);

        if (!(SessionManager.getUser() != null && "mentor".equalsIgnoreCase(SessionManager.getUser().getRole()))) {
            sessionDatePicker.setDisable(true);
            sessionTypeCombo.setDisable(true);
            statusCombo.setDisable(true);
            notesArea.setEditable(false);
            scheduleIDField.setEditable(false);
            newTodoField.setVisible(false); // Entrepreneurs can only view todos, not add them
            newTodoField.setManaged(false);
        }

        // Real-time search: attach listener directly to text property
        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilter(newVal));

        setupTodoList();
    }

    private void setupTodoList() {
        todoList.setCellFactory(lv -> new ListCell<tn.esprit.entities.SessionTodo>() {
            @Override
            protected void updateItem(tn.esprit.entities.SessionTodo item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    CheckBox cb = new CheckBox(item.getTaskDescription());
                    cb.setSelected(item.isDone());
                    cb.setWrapText(true);

                    // Only mentors can check/uncheck items
                    if (!(SessionManager.getUser() != null
                            && "mentor".equalsIgnoreCase(SessionManager.getUser().getRole()))) {
                        cb.setDisable(true);
                    }

                    cb.setOnAction(e -> {
                        item.setDone(cb.isSelected());
                        try {
                            todoService.update(item);
                            refreshTodoProgress();
                        } catch (Exception ex) {
                            showError("Failed to update task: " + ex.getMessage());
                            cb.setSelected(!cb.isSelected()); // revert
                        }
                    });

                    // Mentors can delete tasks by double-clicking
                    if (SessionManager.getUser() != null
                            && "mentor".equalsIgnoreCase(SessionManager.getUser().getRole())) {
                        cb.setOnMouseClicked(e -> {
                            if (e.getClickCount() == 2) {
                                try {
                                    todoService.delete(item);
                                    loadTodos();
                                } catch (Exception ex) {
                                    showError(ex.getMessage());
                                }
                            }
                        });
                        cb.setTooltip(new Tooltip("Double-click to delete task"));
                    }

                    setGraphic(cb);
                }
            }
        });
    }

    private void loadTodos() {
        if (selected == null) {
            todoBox.setVisible(false);
            todoBox.setManaged(false);
            return;
        }

        todoBox.setVisible(true);
        todoBox.setManaged(true);

        List<tn.esprit.entities.SessionTodo> todos = todoService.listBySession(selected.getSessionID());
        todoList.setItems(FXCollections.observableArrayList(todos));
        refreshTodoProgress();
    }

    private void refreshTodoProgress() {
        if (todoList.getItems().isEmpty()) {
            todoProgressLabel.setText("0/0 Completed");
            return;
        }
        long completed = todoList.getItems().stream().filter(tn.esprit.entities.SessionTodo::isDone).count();
        todoProgressLabel.setText(completed + "/" + todoList.getItems().size() + " Completed");
    }

    @FXML
    private void onAddTodo() {
        if (selected == null) {
            showError("Select a session first.");
            return;
        }
        String desc = newTodoField.getText().trim();
        if (desc.isEmpty()) {
            showError("Task description cannot be empty.");
            return;
        }

        tn.esprit.entities.SessionTodo todo = new tn.esprit.entities.SessionTodo(selected.getSessionID(), desc, false);
        try {
            todoService.add(todo);
            newTodoField.clear();
            loadTodos();
        } catch (Exception e) {
            showError(e.getMessage());
        }
    }

    private void setupTableColumns() {
        colID.setCellValueFactory(new PropertyValueFactory<>("sessionID"));
        colMentor.setCellValueFactory(new PropertyValueFactory<>("mentorID"));
        colEntrepreneur.setCellValueFactory(new PropertyValueFactory<>("entrepreneurID"));
        colStartup.setCellValueFactory(new PropertyValueFactory<>("startupID"));
        colDate.setCellValueFactory(cd -> new SimpleStringProperty(
                cd.getValue().getSessionDate() != null ? cd.getValue().getSessionDate().toString() : ""));
        colType.setCellValueFactory(new PropertyValueFactory<>("sessionType"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        if (colSuccess != null)
            colSuccess.setCellValueFactory(
                    cd -> new SimpleStringProperty(String.format("%.1f%%", cd.getValue().getSuccessProbability())));
        colNotes.setCellValueFactory(new PropertyValueFactory<>("notes"));

        // Status colour coding
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setStyle("");
                    return;
                }
                setText(status);
                switch (status.toLowerCase()) {
                    case "completed" -> setStyle("-fx-text-fill:#12a059;-fx-font-weight:700;");
                    case "ongoing" -> setStyle("-fx-text-fill:#7357ff;-fx-font-weight:700;");
                    case "cancelled" -> setStyle("-fx-text-fill:#e0134a;-fx-font-weight:700;");
                    default -> setStyle("-fx-text-fill:#5a4bd6;");
                }
            }
        });
    }

    private void setupForm() {
        sessionTypeCombo.setItems(FXCollections.observableArrayList("online", "in-person", "workshop", "review"));
        statusCombo.setItems(FXCollections.observableArrayList("planned", "ongoing", "completed", "cancelled"));
    }

    private void setupSortCombo() {
        if (sortCombo == null)
            return;
        sortCombo.setItems(FXCollections.observableArrayList(SessionSort.values()));
        sortCombo.setValue(SessionSort.DATE_DESC);
        sortCombo.setOnAction(e -> onSortChanged());
    }

    // ── Data Loading ───────────────────────────────────────────────────────

    private void loadTable() {
        List<Session> raw;
        if ((SessionManager.getUser() != null && "mentor".equalsIgnoreCase(SessionManager.getUser().getRole())))
            raw = service.listByMentor(SessionManager.getUser().getId());
        else
            raw = service.listByEvaluator(SessionManager.getUser().getId());

        // Apply default sort before setting
        raw = SortingService.defaultSortSessions(raw);

        masterData = FXCollections.observableArrayList(raw);
        filteredData = new FilteredList<>(masterData, s -> true);
        sessionTable.setItems(filteredData);

        // Re-apply current filter after reload
        applyFilter(searchField.getText());
    }

    // ── Search (real-time) ─────────────────────────────────────────────────

    /**
     * Update the FilteredList predicate. Called automatically on every keystroke
     * via the textProperty listener — no button click needed.
     *
     * <p>
     * Search covers: date, type, status, notes. Case-insensitive substring match.
     */
    private void applyFilter(String query) {
        if (filteredData == null)
            return;
        if (query == null || query.isBlank()) {
            filteredData.setPredicate(s -> true);
            return;
        }
        String q = query.toLowerCase().trim();
        filteredData.setPredicate(s -> (s.getSessionDate() != null && s.getSessionDate().toString().contains(q))
                || (s.getSessionType() != null && s.getSessionType().toLowerCase().contains(q))
                || (s.getStatus() != null && s.getStatus().toLowerCase().contains(q))
                || (s.getNotes() != null && s.getNotes().toLowerCase().contains(q))
                || String.valueOf(s.getMentorID()).contains(q)
                || String.valueOf(s.getEntrepreneurID()).contains(q));
    }

    @FXML
    private void onSearch() {
        /* now handled by listener, kept for FXML compatibility */ }

    // ── Sort ───────────────────────────────────────────────────────────────

    private void onSortChanged() {
        if (masterData == null || sortCombo == null)
            return;
        SessionSort sort = sortCombo.getValue();
        if (sort == null)
            return;
        List<Session> sorted = SortingService.sortSessions(masterData, sort);
        masterData.setAll(sorted);
    }

    // ── CSV Export ─────────────────────────────────────────────────────────

    @FXML
    private void onExportCSV() {
        if (masterData == null || masterData.isEmpty()) {
            showError("No sessions to export.");
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export Sessions to CSV");
        chooser.setInitialFileName("sessions_export.csv");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("CSV Files", "*.csv"));

        File file = chooser.showSaveDialog(sessionTable.getScene().getWindow());
        if (file == null)
            return;

        try {
            // Export what is currently visible (filtered) for maximum relevance
            List<Session> toExport = filteredData.stream().toList();
            CsvExportService.exportSessionsToCSV(toExport, file);
            showSuccess("✓ Exported " + toExport.size() + " sessions → " + file.getName());
        } catch (Exception e) {
            showError("Export failed: " + e.getMessage());
        }
    }

    // ── CRUD Actions ───────────────────────────────────────────────────────

    @FXML
    private void onUpdate() {
        if (selected == null) {
            showError("Select a session to update.");
            return;
        }
        if (!(SessionManager.getUser() != null && "mentor".equalsIgnoreCase(SessionManager.getUser().getRole()))) {
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
        if (!(SessionManager.getUser() != null && "mentor".equalsIgnoreCase(SessionManager.getUser().getRole()))) {
            showError("Only mentors can delete sessions.");
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete this session record?", ButtonType.YES, ButtonType.NO);
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

        if ("completed".equalsIgnoreCase(s.getStatus())
                && (SessionManager.getUser() != null && "mentor".equalsIgnoreCase(SessionManager.getUser().getRole())))
            showSuccess("Session completed! Add feedback in the Feedback module.");

        loadTodos();
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

        todoBox.setVisible(false);
        todoBox.setManaged(false);
    }

    @FXML
    private void onAdd() {
        showError("Sessions are auto-created from Bookings.");
    }

    // ── Helpers ────────────────────────────────────────────────────────────

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
