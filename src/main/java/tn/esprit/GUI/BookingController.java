package tn.esprit.GUI;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import tn.esprit.entities.Booking;
import tn.esprit.entities.Schedule;
import tn.esprit.entities.User;
import tn.esprit.Services.BookingService;
import tn.esprit.Services.ScheduleService;
import tn.esprit.Services.UserService;
import tn.esprit.utils.SessionManager;

import java.net.URL;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class BookingController implements Initializable {

    @FXML
    private VBox evaluatorFormBox;
    @FXML
    private HBox mentorActionBox;

    @FXML
    private ComboBox<User> mentorCombo;
    @FXML
    private ComboBox<Schedule> slotCombo; // Refined: Slot selector
    @FXML
    private DatePicker requestedDatePicker;
    @FXML
    private TextField requestedTimeField;
    @FXML
    private TextField topicField;
    @FXML
    private Label msgLabel;
    @FXML
    private TextField searchField;

    @FXML
    private TableView<Booking> bookingTable;
    @FXML
    private TableColumn<Booking, Integer> colID;
    @FXML
    private TableColumn<Booking, Integer> colEntrepreneur;
    @FXML
    private TableColumn<Booking, Integer> colMentor;
    @FXML
    private TableColumn<Booking, Integer> colStartup;
    @FXML
    private TableColumn<Booking, String> colDate;
    @FXML
    private TableColumn<Booking, String> colTime;
    @FXML
    private TableColumn<Booking, String> colTopic;
    @FXML
    private TableColumn<Booking, String> colStatus;
    @FXML
    private TableColumn<Booking, String> colCreated;

    private final BookingService service = new BookingService();
    private final UserService userService = new UserService();
    private final ScheduleService scheduleService = new ScheduleService();
    private tn.esprit.Services.VoskService voskService;

    @FXML
    private ToggleButton btnDictation;

    private ObservableList<Booking> allData;
    private Booking selected;

    // Static bridge for pre-filling from ScheduleView if needed
    private static Schedule pendingSlotPrefill;

    public static void prefillSlot(Schedule slot) {
        pendingSlotPrefill = slot;
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        try {
            voskService = new tn.esprit.Services.VoskService();
        } catch (Exception e) {
            voskService = null;
        }
        setupTableColumns();

        boolean isMentor = (SessionManager.getUser() != null
                && "mentor".equalsIgnoreCase(SessionManager.getUser().getRole()));
        evaluatorFormBox.setVisible(!isMentor);
        evaluatorFormBox.setManaged(!isMentor);
        mentorActionBox.setVisible(isMentor);
        mentorActionBox.setManaged(isMentor);

        if (!isMentor) {
            setupEvaluatorForm();
        }

        loadTable();

        // Handle prefill if any
        if (pendingSlotPrefill != null && !isMentor) {
            handlePrefill();
        }
    }

    private void setupTableColumns() {
        colID.setCellValueFactory(new PropertyValueFactory<>("bookingID"));

        colEntrepreneur.setCellValueFactory(new PropertyValueFactory<>("entrepreneurID"));
        colMentor.setCellValueFactory(new PropertyValueFactory<>("mentorID"));
        colStartup.setCellValueFactory(new PropertyValueFactory<>("startupID"));
        colDate.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getRequestedDate().toString()));
        colTime.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getRequestedTime().toString()));
        colTopic.setCellValueFactory(new PropertyValueFactory<>("topic"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        colCreated.setCellValueFactory(cd -> new SimpleStringProperty(
                cd.getValue().getCreationDate() != null ? cd.getValue().getCreationDate().toString() : ""));
    }

    private void setupEvaluatorForm() {
        tn.esprit.Services.RecommendationService recService = new tn.esprit.Services.RecommendationService();
        List<User> topMentors = recService.getTopMentors(3);
        List<User> allMentors = userService.listMentors();

        for (User m : allMentors) {
            if (topMentors.stream().anyMatch(tm -> tm.getId() == m.getId())) {
                m.setFullName(m.getFullName() + " ⭐ (Recommended)");
            }
        }
        mentorCombo.setItems(FXCollections.observableArrayList(allMentors));

        // Use StringConverter to display only the mentor's name
        mentorCombo.setConverter(new javafx.util.StringConverter<User>() {
            @Override
            public String toString(User object) {
                return object == null ? "" : object.getFullName();
            }

            @Override
            public User fromString(String string) {
                return null; // Not needed
            }
        });

        // Listener: When Mentor is selected, load their available slots
        mentorCombo.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                loadAvailableSlots(newVal.getId());
            } else {
                slotCombo.setItems(FXCollections.observableArrayList());
            }
        });

        // Listener: When Slot is selected, auto-fill date and time
        slotCombo.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                requestedDatePicker.setValue(newVal.getAvailableDate());
                requestedTimeField.setText(newVal.getStartTime().toString());
            }
        });

        // Disable date/time manual entry to enforce slot selection
        requestedDatePicker.setEditable(false);
        requestedTimeField.setEditable(false);
    }

    private void loadAvailableSlots(int mentorId) {
        List<Schedule> slots = scheduleService.listAvailableByMentor(mentorId);
        slotCombo.setItems(FXCollections.observableArrayList(slots));
        if (slots.isEmpty()) {
            slotCombo.setPromptText("No available slots for this mentor.");
        } else {
            slotCombo.setPromptText("Select a slot...");
        }
    }

    private void handlePrefill() {
        User m = userService.getById(pendingSlotPrefill.getMentorID());
        if (m != null) {
            mentorCombo.setValue(m);
            loadAvailableSlots(m.getId());
            // find the slot in the combo to select it
            slotCombo.getItems().stream()
                    .filter(s -> s.getScheduleID() == pendingSlotPrefill.getScheduleID())
                    .findFirst()
                    .ifPresent(s -> slotCombo.setValue(s));
        }
        pendingSlotPrefill = null;
    }

    @FXML
    private void onToggleDictation() {
        if (!voskService.isReady()) {
            showError("Speech script not found at speech_listen.ps1.");
            if (btnDictation != null)
                btnDictation.setSelected(false);
            return;
        }
        if (btnDictation.isSelected()) {
            btnDictation.setText("🛑 Stop Dictating");
            btnDictation.setStyle("-fx-background-color: #ff4c4c; -fx-text-fill: white;");
            voskService.startListening(text -> {
                topicField.appendText(text);
            });
        } else {
            btnDictation.setText("🎤 Dictate Topic");
            btnDictation.setStyle("");
            voskService.stopListening();
        }
    }

    @FXML
    private void onAdd() {
        clearMsg();
        Booking b = buildFromForm();
        if (b == null)
            return;
        try {
            service.add(b);
            showSuccess("Booking requested! Waiting for mentor approval.");
            onClear();
            loadTable();
        } catch (Exception e) {
            showError(e.getMessage());
        }
    }

    @FXML
    private void onUpdate() {
        if (selected == null) {
            showError("Select a booking to update.");
            return;
        }
        if (!"pending".equalsIgnoreCase(selected.getStatus())) {
            showError("Only pending bookings can be edited.");
            return;
        }

        Booking b = buildFromForm();
        if (b == null)
            return;
        b.setBookingID(selected.getBookingID());
        try {
            service.update(b);
            showSuccess("Booking updated!");
            onClear();
            loadTable();
        } catch (Exception e) {
            showError(e.getMessage());
        }
    }

    @FXML
    private void onDelete() {
        if (selected == null) {
            showError("Select a booking to delete.");
            return;
        }
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Delete this booking?", ButtonType.YES, ButtonType.NO);
        alert.showAndWait().ifPresent(type -> {
            if (type == ButtonType.YES) {
                try {
                    service.delete(selected);
                    showSuccess("Booking deleted.");
                    onClear();
                    loadTable();
                } catch (Exception e) {
                    showError(e.getMessage());
                }
            }
        });
    }

    @FXML
    private void onApprove() {
        if (selected == null) {
            showError("Select a pending booking to approve.");
            return;
        }
        try {
            service.approveBooking(selected);
            showSuccess("Booking approved! Session created.");
            loadTable();
        } catch (Exception e) {
            showError(e.getMessage());
        }
    }

    @FXML
    private void onReject() {
        if (selected == null) {
            showError("Select a pending booking to reject.");
            return;
        }
        try {
            service.rejectBooking(selected);
            showSuccess("Booking rejected.");
            loadTable();
        } catch (Exception e) {
            showError(e.getMessage());
        }
    }

    @FXML
    private void onClear() {
        mentorCombo.setValue(null);
        slotCombo.setValue(null);
        requestedDatePicker.setValue(null);
        requestedTimeField.clear();
        topicField.clear();
        selected = null;
        bookingTable.getSelectionModel().clearSelection();
        clearMsg();
        if (btnDictation != null && btnDictation.isSelected()) {
            btnDictation.setSelected(false);
            onToggleDictation();
        }
    }

    @FXML
    private void onRowSelected() {
        Booking b = bookingTable.getSelectionModel().getSelectedItem();
        if (b == null)
            return;
        selected = b;

        if ((SessionManager.getUser() != null && "entrepreneur".equals(SessionManager.getUser().getRole()))) {
            // Fill form if pending
            if ("pending".equalsIgnoreCase(b.getStatus())) {
                User m = userService.getById(b.getMentorID());
                mentorCombo.setValue(m);
                requestedDatePicker.setValue(b.getRequestedDate());
                requestedTimeField.setText(b.getRequestedTime().toString());
                topicField.setText(b.getTopic());
            }
        }
    }

    @FXML
    private void onSearch() {
        String query = searchField.getText().toLowerCase();
        if (query.isEmpty()) {
            bookingTable.setItems(allData);
            return;
        }
        List<Booking> filtered = allData.stream()
                .filter(b -> b.getTopic().toLowerCase().contains(query) || b.getStatus().toLowerCase().contains(query))
                .collect(Collectors.toList());
        bookingTable.setItems(FXCollections.observableArrayList(filtered));
    }

    private Booking buildFromForm() {
        User m = mentorCombo.getValue();
        if (m == null) {
            showError("Please select a mentor.");
            return null;
        }

        LocalDate date = requestedDatePicker.getValue();
        if (date == null) {
            showError("Please choose a date (via slot selection).");
            return null;
        }

        String timeStr = requestedTimeField.getText().trim();
        if (timeStr.isEmpty()) {
            showError("Please choose a slot to fill the time.");
            return null;
        }

        String topic = topicField.getText().trim();
        if (topic.isEmpty()) {
            showError("Topic is required.");
            return null;
        }

        Booking b = new Booking();
        b.setMentorID(m.getId());
        b.setEntrepreneurID(SessionManager.getUser().getId());
        b.setStartupID(1); // Default for now
        b.setRequestedDate(date);
        b.setRequestedTime(LocalTime.parse(timeStr));
        b.setTopic(topic);
        return b;
    }

    private void loadTable() {
        List<Booking> data;
        if ((SessionManager.getUser() != null && "mentor".equalsIgnoreCase(SessionManager.getUser().getRole()))) {
            data = service.listByMentor(SessionManager.getUser().getId());
        } else {
            data = service.listByEvaluator(SessionManager.getUser().getId());
        }
        allData = FXCollections.observableArrayList(data);
        bookingTable.setItems(allData);
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
