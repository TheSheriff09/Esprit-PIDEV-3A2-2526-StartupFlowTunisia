package tn.esprit.GUI.admin;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import tn.esprit.Services.SessionService;
import tn.esprit.Services.UserService;
import tn.esprit.entities.Session;
import tn.esprit.entities.User;

import java.net.URL;
import java.io.File;
import javafx.stage.FileChooser;
import java.util.List;
import java.util.ResourceBundle;

public class AdminMentorshipController implements Initializable {

    @FXML
    private TextField searchField;
    @FXML
    private TableView<Session> logsTable;
    @FXML
    private TableColumn<Session, Integer> colSessionID;
    @FXML
    private TableColumn<Session, String> colMentor;
    @FXML
    private TableColumn<Session, String> colEntrepreneur;
    @FXML
    private TableColumn<Session, String> colStatus;
    @FXML
    private TableColumn<Session, String> colDate;
    @FXML
    private TableColumn<Session, String> colTopic;

    @FXML
    private Label selectedLogLabel;
    @FXML
    private Button btnBanMentor;
    @FXML
    private Button btnBanEntrepreneur;
    @FXML
    private Label msgLabel;

    private final SessionService sessionService = new SessionService();
    private final UserService userService = new UserService();

    private ObservableList<Session> masterData;
    private FilteredList<Session> filteredData;
    private Session selectedSession;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTable();
        loadData();

        searchField.textProperty().addListener((obs, oldV, newV) -> filterData(newV));

        logsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            selectedSession = newV;
            if (newV != null) {
                User m = userService.getById(newV.getMentorID());
                User e = userService.getById(newV.getEntrepreneurID());

                String mName = (m != null) ? m.getFullName() : "Unknown";
                String eName = (e != null) ? e.getFullName() : "Unknown";

                selectedLogLabel
                        .setText("Selected Session #" + newV.getSessionID() + " (" + mName + " & " + eName + ")");
                btnBanMentor.setDisable(false);
                btnBanEntrepreneur.setDisable(false);

                if (m != null && "BLOCKED".equalsIgnoreCase(m.getStatus())) {
                    btnBanMentor.setText("Mentor Already Banned");
                    btnBanMentor.setDisable(true);
                } else {
                    btnBanMentor.setText("🚫 Ban Mentor (" + mName + ")");
                }

                if (e != null && "BLOCKED".equalsIgnoreCase(e.getStatus())) {
                    btnBanEntrepreneur.setText("Entrepreneur Already Banned");
                    btnBanEntrepreneur.setDisable(true);
                } else {
                    btnBanEntrepreneur.setText("🚫 Ban Entrepreneur (" + eName + ")");
                }
            } else {
                selectedLogLabel.setText("No session selected.");
                btnBanMentor.setDisable(true);
                btnBanEntrepreneur.setDisable(true);
                btnBanMentor.setText("🚫 Ban Mentor");
                btnBanEntrepreneur.setText("🚫 Ban Entrepreneur");
            }
        });
    }

    private void setupTable() {
        colSessionID.setCellValueFactory(new PropertyValueFactory<>("sessionID"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colDate.setCellValueFactory(cd -> new SimpleStringProperty(
                cd.getValue().getSessionDate() != null ? cd.getValue().getSessionDate().toString() : ""));
        colTopic.setCellValueFactory(new PropertyValueFactory<>("notes"));

        colMentor.setCellValueFactory(cd -> {
            User u = userService.getById(cd.getValue().getMentorID());
            return new SimpleStringProperty(u != null ? u.getFullName() : "ID:" + cd.getValue().getMentorID());
        });

        colEntrepreneur.setCellValueFactory(cd -> {
            User u = userService.getById(cd.getValue().getEntrepreneurID());
            return new SimpleStringProperty(u != null ? u.getFullName() : "ID:" + cd.getValue().getEntrepreneurID());
        });

        colStatus.setCellFactory(col -> new TableCell<Session, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if ("completed".equalsIgnoreCase(item))
                        setStyle("-fx-text-fill:#12a059;-fx-font-weight:bold;");
                    else if ("canceled".equalsIgnoreCase(item))
                        setStyle("-fx-text-fill:#e0134a;-fx-font-weight:bold;");
                    else
                        setStyle("-fx-text-fill:#e07c00;-fx-font-weight:bold;");
                }
            }
        });
    }

    private void loadData() {
        List<Session> all = sessionService.list();
        masterData = FXCollections.observableArrayList(all);
        filteredData = new FilteredList<>(masterData, p -> true);
        logsTable.setItems(filteredData);
    }

    private void filterData(String query) {
        if (query == null || query.isBlank()) {
            filteredData.setPredicate(p -> true);
            return;
        }
        String q = query.toLowerCase().trim();
        filteredData.setPredicate(s -> {
            User m = userService.getById(s.getMentorID());
            User e = userService.getById(s.getEntrepreneurID());
            String mName = (m != null) ? m.getFullName().toLowerCase() : "";
            String eName = (e != null) ? e.getFullName().toLowerCase() : "";

            return mName.contains(q)
                    || eName.contains(q)
                    || (s.getStatus() != null && s.getStatus().toLowerCase().contains(q))
                    || (s.getNotes() != null && s.getNotes().toLowerCase().contains(q))
                    || String.valueOf(s.getSessionID()).contains(q);
        });
    }

    @FXML
    private void onBanMentor() {
        if (selectedSession == null)
            return;
        banUser(selectedSession.getMentorID(), "Mentor");
    }

    @FXML
    private void onBanEntrepreneur() {
        if (selectedSession == null)
            return;
        banUser(selectedSession.getEntrepreneurID(), "Entrepreneur");
    }

    private void banUser(int userId, String type) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "Are you sure you want to block this " + type + "? They will not be able to log in.", ButtonType.YES,
                ButtonType.NO);
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                User u = userService.getById(userId);
                if (u != null) {
                    u.setStatus("BLOCKED");
                    userService.update(u);
                    msgLabel.setText(type + " " + u.getFullName() + " has been banned.");
                    msgLabel.setStyle("-fx-text-fill:#e0134a;");

                    // Trigger table refresh to update button states
                    int index = logsTable.getSelectionModel().getSelectedIndex();
                    logsTable.getSelectionModel().clearSelection();
                    logsTable.getSelectionModel().select(index);
                }
            }
        });
    }

    @FXML
    private void onExportCSV() {
        if (filteredData.isEmpty()) {
            msgLabel.setText("No data to export!");
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export Session Logs to CSV");
        chooser.setInitialFileName("mentorship_logs_export.csv");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        File file = chooser.showSaveDialog(logsTable.getScene().getWindow());
        if (file != null) {
            try {
                java.io.FileWriter writer = new java.io.FileWriter(file);
                writer.write("Session ID,Mentor Name,Entrepreneur Name,Status,Date,Notes\n");
                for (Session s : filteredData) {
                    User m = userService.getById(s.getMentorID());
                    User e = userService.getById(s.getEntrepreneurID());
                    String mName = (m != null) ? m.getFullName().replace(",", " ") : "Unknown";
                    String eName = (e != null) ? e.getFullName().replace(",", " ") : "Unknown";
                    String notes = (s.getNotes() != null) ? s.getNotes().replace(",", " ").replace("\n", " ") : "";

                    writer.write(String.format("%d,%s,%s,%s,%s,%s\n",
                            s.getSessionID(), mName, eName, s.getStatus(),
                            (s.getSessionDate() != null) ? s.getSessionDate().toString() : "",
                            notes));
                }
                writer.close();
                msgLabel.setText("Exported successfully to " + file.getName());
                msgLabel.setStyle("-fx-text-fill:#12a059;");
            } catch (Exception e) {
                msgLabel.setText("Failed to export: " + e.getMessage());
                msgLabel.setStyle("-fx-text-fill:#e0134a;");
            }
        }
    }
}
