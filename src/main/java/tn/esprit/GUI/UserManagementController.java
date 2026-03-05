package tn.esprit.GUI;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.HBox;
import tn.esprit.Services.UserService;
import tn.esprit.entities.User;
import tn.esprit.utils.NavigationManager;
import tn.esprit.utils.SessionManager;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class UserManagementController {

    @FXML
    private TableView<User> usersTable;

    @FXML
    private TableColumn<User, String> colId;
    @FXML
    private TableColumn<User, String> colFullName;
    @FXML
    private TableColumn<User, String> colEmail;
    @FXML
    private TableColumn<User, String> colPassword; // always empty
    @FXML
    private TableColumn<User, String> colRole;
    @FXML
    private TableColumn<User, String> colStatus;
    @FXML
    private TableColumn<User, String> colMentorExp;
    @FXML
    private TableColumn<User, String> colEvalLevel;
    @FXML
    private TableColumn<User, String> colCreatedAt;
    @FXML
    private TableColumn<User, Void> colActions;

    @FXML
    private TextField searchField;
    @FXML
    private Label lblInfo;

    // Add form
    @FXML
    private TextField tfFullName;
    @FXML
    private TextField tfEmail;
    @FXML
    private PasswordField pfPassword;
    @FXML
    private ComboBox<String> cbRole;
    @FXML
    private ComboBox<String> cbStatus;
    @FXML
    private TextField tfMentorExpertise;
    @FXML
    private TextField tfEvaluatorLevel;

    private final UserService userService = new UserService();

    private final ObservableList<User> master = FXCollections.observableArrayList();
    private FilteredList<User> filtered;

    private final ObservableList<String> roles = FXCollections.observableArrayList("ENTREPRENEUR", "MENTOR",
            "EVALUATOR", "ADMIN");

    private final ObservableList<String> statuses = FXCollections.observableArrayList("PENDING", "ACTIVE", "BLOCKED");

    @FXML
    private void initialize() {
        // ── ADMIN role guard ──
        if (!SessionManager.requireRole(usersTable, "ADMIN"))
            return;

        cbRole.setItems(roles);
        cbStatus.setItems(statuses);
        cbRole.getSelectionModel().select("ENTREPRENEUR");
        cbStatus.getSelectionModel().select("PENDING");

        // Enable/disable extra fields depending on role chosen
        cbRole.valueProperty().addListener((obs, o, n) -> updateExtraFields(n));
        updateExtraFields(cbRole.getValue());

        setupTable();
        loadUsers();

        filtered = new FilteredList<>(master, u -> true);
        usersTable.setItems(filtered);

        searchField.textProperty().addListener((obs, ov, nv) -> applyFilter(nv));
    }

    private void updateExtraFields(String role) {
        boolean mentor = "MENTOR".equalsIgnoreCase(role);
        boolean eval = "EVALUATOR".equalsIgnoreCase(role);

        tfMentorExpertise.setDisable(!mentor);
        if (!mentor)
            tfMentorExpertise.clear();

        tfEvaluatorLevel.setDisable(!eval);
        if (!eval)
            tfEvaluatorLevel.clear();
    }

    private void setupTable() {
        usersTable.setEditable(true);

        colId.setCellValueFactory(cd -> new SimpleStringProperty(String.valueOf(cd.getValue().getId())));

        colFullName.setCellValueFactory(cd -> new SimpleStringProperty(ns(cd.getValue().getFullName())));
        colFullName.setCellFactory(TextFieldTableCell.forTableColumn());
        colFullName.setOnEditCommit(e -> e.getRowValue().setFullName(e.getNewValue()));

        colEmail.setCellValueFactory(cd -> new SimpleStringProperty(ns(cd.getValue().getEmail())));
        colEmail.setCellFactory(TextFieldTableCell.forTableColumn());
        colEmail.setOnEditCommit(e -> e.getRowValue().setEmail(e.getNewValue()));

        // password_hash column ALWAYS EMPTY
        colPassword.setCellValueFactory(cd -> new SimpleStringProperty(""));

        colRole.setCellValueFactory(cd -> new SimpleStringProperty(ns(cd.getValue().getRole())));
        colRole.setCellFactory(ComboBoxTableCell.forTableColumn(roles));
        colRole.setOnEditCommit(e -> {
            User u = e.getRowValue();
            u.setRole(e.getNewValue());

            // If role changed, clean irrelevant columns
            if (!"MENTOR".equalsIgnoreCase(u.getRole()))
                u.setMentorExpertise(null);
            if (!"EVALUATOR".equalsIgnoreCase(u.getRole()))
                u.setEvaluatorLevel(null);
        });

        colStatus.setCellValueFactory(cd -> new SimpleStringProperty(ns(cd.getValue().getStatus())));
        colStatus.setCellFactory(ComboBoxTableCell.forTableColumn(statuses));
        colStatus.setOnEditCommit(e -> e.getRowValue().setStatus(e.getNewValue()));

        colMentorExp.setCellValueFactory(cd -> new SimpleStringProperty(ns(cd.getValue().getMentorExpertise())));
        colMentorExp.setCellFactory(TextFieldTableCell.forTableColumn());
        colMentorExp.setOnEditCommit(e -> {
            User u = e.getRowValue();
            u.setMentorExpertise(emptyToNull(e.getNewValue()));
            if (!"MENTOR".equalsIgnoreCase(u.getRole()))
                u.setMentorExpertise(null);
        });

        colEvalLevel.setCellValueFactory(cd -> new SimpleStringProperty(ns(cd.getValue().getEvaluatorLevel())));
        colEvalLevel.setCellFactory(TextFieldTableCell.forTableColumn());
        colEvalLevel.setOnEditCommit(e -> {
            User u = e.getRowValue();
            u.setEvaluatorLevel(emptyToNull(e.getNewValue()));
            if (!"EVALUATOR".equalsIgnoreCase(u.getRole()))
                u.setEvaluatorLevel(null);
        });

        colCreatedAt.setCellValueFactory(cd -> new SimpleStringProperty(formatTs(cd.getValue().getCreatedAt())));

        colActions.setCellFactory(tc -> new TableCell<>() {

            private final Button btnUpdate = new Button("✏");
            private final Button btnDelete = new Button("🗑");

            {
                btnUpdate.getStyleClass().add("miniBtn");
                btnDelete.getStyleClass().add("miniDanger");

                btnUpdate.setOnAction(e -> {
                    User u = getTableView().getItems().get(getIndex());
                    updateRow(u);
                });

                btnDelete.setOnAction(e -> {
                    User u = getTableView().getItems().get(getIndex());
                    deleteRow(u);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    HBox box = new HBox(6, btnUpdate, btnDelete);
                    setGraphic(box);
                }
            }
        });
    }

    private void loadUsers() {
        master.clear();
        master.addAll(userService.list());
        if (lblInfo != null)
            lblInfo.setText("Total: " + master.size());
        usersTable.refresh();
    }

    private void applyFilter(String q) {
        String query = (q == null) ? "" : q.trim().toLowerCase(Locale.ROOT);

        filtered.setPredicate(u -> {
            if (query.isEmpty())
                return true;
            String name = ns(u.getFullName()).toLowerCase(Locale.ROOT);
            String mail = ns(u.getEmail()).toLowerCase(Locale.ROOT);
            return name.contains(query) || mail.contains(query);
        });

        if (lblInfo != null)
            lblInfo.setText("Showing: " + filtered.size());
    }

    @FXML
    private void refresh() {
        loadUsers();
        applyFilter(searchField.getText());
    }

    private void updateRow(User u) {

        userService.update(u);
        usersTable.refresh();
        toast("Updated user id=" + u.getId());
    }

    private void deleteRow(User u) {
        if (u == null)
            return;

        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setTitle("Delete user");
        a.setHeaderText("Delete: " + ns(u.getFullName()));
        a.setContentText("This cannot be undone.");
        if (a.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK)
            return;

        userService.deleteById(u.getId());
        refresh();
        toast("Deleted.");
    }

    @FXML
    private void addUser() {
        String fullName = tfFullName.getText() == null ? "" : tfFullName.getText().trim();
        String email = tfEmail.getText() == null ? "" : tfEmail.getText().trim();
        String pass = pfPassword.getText() == null ? "" : pfPassword.getText().trim();
        String role = cbRole.getValue() == null ? "" : cbRole.getValue().trim();
        String status = cbStatus.getValue() == null ? "PENDING" : cbStatus.getValue().trim();

        String mentorExp = tfMentorExpertise.getText() == null ? "" : tfMentorExpertise.getText().trim();
        String evalLevel = tfEvaluatorLevel.getText() == null ? "" : tfEvaluatorLevel.getText().trim();

        if (fullName.isEmpty() || email.isEmpty() || pass.isEmpty() || role.isEmpty()) {
            showAlert("Missing fields", "Full name, email, password, role are required.");
            return;
        }

        if (!role.equalsIgnoreCase("MENTOR"))
            mentorExp = null;
        if (!role.equalsIgnoreCase("EVALUATOR"))
            evalLevel = null;

        User u = new User();
        u.setFullName(fullName);
        u.setEmail(email);
        u.setPasswordHash(pass);
        u.setRole(role);
        u.setStatus(status);
        u.setMentorExpertise(mentorExp);
        u.setEvaluatorLevel(evalLevel);

        User added = userService.add(u);
        if (added == null) {
            showAlert("Add failed", "Could not add user. Email may already exist.");
            return;
        }

        refresh();
        clearForm();
    }

    private void showAlert(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    private void clearForm() {
        tfFullName.clear();
        tfEmail.clear();
        pfPassword.clear();
        cbRole.getSelectionModel().select("ENTREPRENEUR");
        cbStatus.getSelectionModel().select("PENDING");
        tfMentorExpertise.clear();
        tfEvaluatorLevel.clear();
        updateExtraFields(cbRole.getValue());
    }

    @FXML
    private void goDashboard() {
        NavigationManager.navigateTo(usersTable, "/DashboardAdmin.fxml");
    }

    @FXML
    private void goUsers() {
        /* already here */ }

    @FXML
    private void goProjects() {
        NavigationManager.navigateTo(usersTable, "/admindashboard.fxml");
    }

    @FXML
    private void goMentorship() {
        System.out.println("Mentorship later");
    }

    @FXML
    private void goFunding() {
        System.out.println("Funding later");
    }

    @FXML
    private void goDashboardForum() {
        ForumAdminController.showAnalyticsOnLoad = true;
        NavigationManager.navigateTo(usersTable, "/ForumAdmin.fxml");
    }

    @FXML
    private void goForumBackOffice() {
        ForumAdminController.showAnalyticsOnLoad = false;
        NavigationManager.navigateTo(usersTable, "/ForumAdmin.fxml");
    }

    @FXML
    private void goSettings() {
        System.out.println("Settings later");
    }

    @FXML
    private void goReclamations() {
        NavigationManager.navigateTo(usersTable, "/ReclamationAdmin.fxml");
    }

    @FXML
    private void logout() {
        NavigationManager.logout(usersTable);
    }

    private String ns(String s) {
        return (s == null) ? "" : s;
    }

    private String emptyToNull(String s) {
        if (s == null)
            return null;
        s = s.trim();
        return s.isEmpty() ? null : s;
    }

    private String formatTs(Timestamp ts) {
        if (ts == null)
            return "";
        return new SimpleDateFormat("yyyy-MM-dd HH:mm").format(ts);
    }

    private void toast(String msg) {
        if (lblInfo != null)
            lblInfo.setText(msg);
    }
}
