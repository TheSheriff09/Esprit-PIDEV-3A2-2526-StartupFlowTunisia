package tn.esprit.GUI;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import tn.esprit.entities.Schedule;
import tn.esprit.entities.User;
import tn.esprit.Services.ScheduleService;
import tn.esprit.Services.UserService;
import tn.esprit.utils.SessionManager;

import java.net.URL;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

public class ScheduleController implements Initializable {

    @FXML
    private VBox evaluatorPickerBox;
    @FXML
    private ComboBox<User> mentorPickerCombo;
    @FXML
    private DatePicker datePicker;
    @FXML
    private Label msgLabel;
    @FXML
    private Label gridTitle;
    @FXML
    private Button btnSaveMentorSlots;
    @FXML
    private FlowPane timeslotGrid;

    private final ScheduleService service = new ScheduleService();
    private final UserService userSvc = new UserService();

    private boolean isMentor = false;
    private int activeMentorId = 0;

    // Tracks Mentor's active session state in the current calendar view
    private List<Schedule> currentDbSlots = new ArrayList<>();
    private final Set<LocalTime> selectedTimes = new HashSet<>();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        User u = SessionManager.getUser();
        if (u != null && "mentor".equals(u.getRole())) {
            isMentor = true;
            activeMentorId = u.getId();
            btnSaveMentorSlots.setVisible(true);
            btnSaveMentorSlots.setManaged(true);
        } else {
            evaluatorPickerBox.setVisible(true);
            evaluatorPickerBox.setManaged(true);
            mentorPickerCombo.setItems(FXCollections.observableArrayList(userSvc.listMentors()));

            mentorPickerCombo.setOnAction(e -> {
                User m = mentorPickerCombo.getValue();
                if (m != null) {
                    activeMentorId = m.getId();
                    if (datePicker.getValue() != null) {
                        onDateSelected();
                    }
                }
            });
        }
    }

    @FXML
    private void onDateSelected() {
        clearMsg();
        LocalDate date = datePicker.getValue();
        if (date == null)
            return;

        if (!isMentor && activeMentorId == 0) {
            showError("Please select a mentor first.");
            return;
        }

        gridTitle.setText("TIMESLOTS FOR " + date.toString());
        timeslotGrid.getChildren().clear();
        selectedTimes.clear();

        // Load existing slots for this mentor on this date
        currentDbSlots = service.listAvailableByMentor(activeMentorId).stream()
                .filter(s -> s.getAvailableDate().equals(date))
                .toList();

        if (isMentor) {
            buildMentorGrid();
        } else {
            buildEntrepreneurGrid();
        }
    }

    /**
     * Mentor Grid: Generates buttons from 08:00 to 18:00 (1 hour intervals).
     * Mentor can toggle which ones they want freely available.
     */
    private void buildMentorGrid() {
        for (int hour = 8; hour <= 18; hour++) {
            LocalTime time = LocalTime.of(hour, 0);
            ToggleButton btn = new ToggleButton(time.toString());
            btn.setPrefWidth(100);
            btn.setPrefHeight(45);
            btn.setStyle("-fx-font-size: 14px; -fx-cursor: hand;");

            // Check if already in DB
            Optional<Schedule> existing = currentDbSlots.stream()
                    .filter(s -> s.getStartTime().equals(time))
                    .findFirst();

            if (existing.isPresent()) {
                if (existing.get().isBooked()) {
                    btn.setText(time.toString() + "\n(Booked)");
                    btn.setDisable(true); // Can't remove a slot someone booked
                    btn.setStyle("-fx-background-color: #ff8b57; -fx-text-fill: white; -fx-opacity: 0.8;");
                } else {
                    btn.setSelected(true);
                    selectedTimes.add(time);
                }
            }

            btn.setOnAction(e -> {
                if (btn.isSelected()) {
                    selectedTimes.add(time);
                } else {
                    selectedTimes.remove(time);
                }
            });

            timeslotGrid.getChildren().add(btn);
        }
    }

    /**
     * Entrepreneur Grid: Only displays slots the mentor has set as strictly
     * available (not booked yet).
     */
    private void buildEntrepreneurGrid() {
        List<Schedule> freeSlots = currentDbSlots.stream()
                .filter(s -> !s.isBooked())
                .toList();

        if (freeSlots.isEmpty()) {
            Label lbl = new Label("No available timeslots on this date.");
            lbl.setStyle("-fx-font-size: 14px; -fx-text-fill: #999;");
            timeslotGrid.getChildren().add(lbl);
            return;
        }

        for (Schedule s : freeSlots) {
            Button btn = new Button(s.getStartTime().toString() + "\nClick to Book");
            btn.setPrefWidth(120);
            btn.setPrefHeight(55);
            btn.setStyle(
                    "-fx-background-color: #12a059; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");

            btn.setOnAction(e -> bookSlotAsEntrepreneur(s));
            timeslotGrid.getChildren().add(btn);
        }
    }

    @FXML
    private void onSaveMentorSlots() {
        LocalDate date = datePicker.getValue();
        if (date == null) {
            showError("Select a date first.");
            return;
        }

        // 1. Delete all non-booked slots for this date
        List<Schedule> unbookedDb = currentDbSlots.stream().filter(s -> !s.isBooked()).toList();
        for (Schedule old : unbookedDb) {
            service.delete(old);
        }

        // 2. Insert all selected toggle times
        int added = 0;
        for (LocalTime t : selectedTimes) {
            boolean alreadyBooked = currentDbSlots.stream()
                    .anyMatch(db -> db.getStartTime().equals(t) && db.isBooked());
            if (!alreadyBooked) {
                // Bookings are exactly 1 hour apart in this grid design
                Schedule s = new Schedule(activeMentorId, date, t, t.plusHours(1));
                service.add(s);
                added++;
            }
        }

        showSuccess("Saved " + added + " availability slots.");
        // Refetch state
        onDateSelected();
    }

    private void bookSlotAsEntrepreneur(Schedule s) {
        BookingController.prefillSlot(s);
        try {
            Stage stage = (Stage) timeslotGrid.getScene().getWindow();
            // Since this runs inside an Entrepreneur Dashboard context, route back to the
            // Entrepreneur's Dashboard loader
            // However, MentorshipDashboard.fxml acts as the shell for BOTH entrepreneur and
            // mentor mentorship tools.
            Parent dashboard = FXMLLoader.load(getClass().getResource("/fxml/MentorshipDashboard.fxml"));
            stage.setScene(new Scene(dashboard, 1280, 720));
        } catch (Exception e) {
            showError("Cannot navigate to bookings: " + e.getMessage());
        }
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
