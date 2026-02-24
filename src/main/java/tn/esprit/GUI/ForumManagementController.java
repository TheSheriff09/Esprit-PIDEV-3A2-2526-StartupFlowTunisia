package tn.esprit.GUI;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import tn.esprit.entities.ForumPost;
import tn.esprit.services.ForumPostService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class ForumManagementController {

    @FXML
    private TableView<ForumPost> postsTable;

    @FXML
    private TableColumn<ForumPost, String> colId;
    @FXML
    private TableColumn<ForumPost, String> colTitle;
    @FXML
    private TableColumn<ForumPost, String> colContent;
    @FXML
    private TableColumn<ForumPost, String> colCreatedAt;
    @FXML
    private TableColumn<ForumPost, String> colUpdatedAt;
    @FXML
    private TableColumn<ForumPost, Void> colActions;

    @FXML
    private TextField searchField;
    @FXML
    private Label lblInfo;

    // Add form
    @FXML
    private TextField tfTitle;
    @FXML
    private TextArea taContent;

    private final ForumPostService postService = new ForumPostService();
    private final ObservableList<ForumPost> master = FXCollections.observableArrayList();
    private FilteredList<ForumPost> filtered;

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @FXML
    private void initialize() {
        setupTable();
        loadPosts();

        filtered = new FilteredList<>(master, p -> true);
        postsTable.setItems(filtered);

        searchField.textProperty().addListener((obs, ov, nv) -> applyFilter(nv));
    }

    private void setupTable() {
        postsTable.setEditable(true);

        colId.setCellValueFactory(cd -> new SimpleStringProperty(String.valueOf(cd.getValue().getId())));

        colTitle.setCellValueFactory(cd -> new SimpleStringProperty(ns(cd.getValue().getTitle())));
        colTitle.setCellFactory(TextFieldTableCell.forTableColumn());
        colTitle.setOnEditCommit(e -> {
            ForumPost p = e.getRowValue();
            p.setTitle(e.getNewValue());
            p.setUpdatedAt(LocalDateTime.now());
            updateRow(p);
        });

        colContent.setCellValueFactory(cd -> new SimpleStringProperty(ns(cd.getValue().getContent())));
        colContent.setCellFactory(TextFieldTableCell.forTableColumn()); // TextAreaTableCell would be better but
                                                                        // TextField is standard
        colContent.setOnEditCommit(e -> {
            ForumPost p = e.getRowValue();
            p.setContent(e.getNewValue());
            p.setUpdatedAt(LocalDateTime.now());
            updateRow(p);
        });

        colCreatedAt.setCellValueFactory(cd -> new SimpleStringProperty(formatTs(cd.getValue().getCreatedAt())));
        colUpdatedAt.setCellValueFactory(cd -> new SimpleStringProperty(formatTs(cd.getValue().getUpdatedAt())));

        // ACTION ICONS: update + delete
        colActions.setCellFactory(tc -> new TableCell<>() {
            private final Button btnDelete = new Button("🗑");

            {
                btnDelete.getStyleClass().add("miniDanger");
                btnDelete.setOnAction(e -> {
                    ForumPost p = getTableView().getItems().get(getIndex());
                    deleteRow(p);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    HBox box = new HBox(6, btnDelete);
                    setGraphic(box);
                }
            }
        });
    }

    private void loadPosts() {
        master.clear();
        master.addAll(postService.list());
        if (lblInfo != null)
            lblInfo.setText("Total: " + master.size());
        postsTable.refresh();
    }

    private void applyFilter(String q) {
        String query = (q == null) ? "" : q.trim().toLowerCase(Locale.ROOT);

        filtered.setPredicate(p -> {
            if (query.isEmpty())
                return true;
            String title = ns(p.getTitle()).toLowerCase(Locale.ROOT);
            String content = ns(p.getContent()).toLowerCase(Locale.ROOT);
            return title.contains(query) || content.contains(query);
        });

        if (lblInfo != null)
            lblInfo.setText("Showing: " + filtered.size());
    }

    @FXML
    private void refresh() {
        loadPosts();
        applyFilter(searchField.getText());
    }

    private void updateRow(ForumPost p) {
        postService.update(p);
        postsTable.refresh();
    }

    private void deleteRow(ForumPost p) {
        if (p == null)
            return;

        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setTitle("Delete Post");
        a.setHeaderText("Delete: " + ns(p.getTitle()));
        a.setContentText("This cannot be undone.");
        if (a.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK)
            return;

        postService.delete(p);
        refresh();
    }

    @FXML
    private void addPost() {
        String title = tfTitle.getText() == null ? "" : tfTitle.getText().trim();
        String content = taContent.getText() == null ? "" : taContent.getText().trim();

        if (title.isEmpty() || content.isEmpty()) {
            showAlert("Missing fields", "Title and Content are required.");
            return;
        }

        ForumPost p = new ForumPost();
        p.setTitle(title);
        p.setContent(content);
        p.setCreatedAt(LocalDateTime.now());
        p.setUpdatedAt(null);

        ForumPost added = postService.add(p);
        if (added == null) {
            showAlert("Add failed", "Could not add post.");
            return;
        }

        refresh();
        clearForm();
    }

    @FXML
    private void viewComments() {
        ForumPost selected = postsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("No Selection", "Please select a post to view comments.");
            return;
        }
        showAlert("Comments",
                "Viewing comments for: " + selected.getTitle() + "\n(Comment interface to be implemented)");
        // Logic to open Comment Management would go here
    }

    private void clearForm() {
        tfTitle.clear();
        taContent.clear();
    }

    private void showAlert(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    /* ---------------- NAVIGATION ---------------- */
    @FXML
    private void goDashboard() {
        System.out.println("Dashboard");
    }

    @FXML
    private void goUsers() {
        goTo("/UserManagement.fxml");
    } // Example navigation

    @FXML
    private void goProjects() {
        System.out.println("Projects");
    }

    @FXML
    private void goMentorship() {
        System.out.println("Mentorship");
    }

    @FXML
    private void goFunding() {
        System.out.println("Funding");
    }

    @FXML
    private void goForum() {
        /* Already here */ }

    @FXML
    private void goSettings() {
        System.out.println("Settings");
    }

    @FXML
    private void logout() {
        System.out.println("Logout");
    }

    private void goTo(String fxmlPath) {
        try {
            java.net.URL url = getClass().getResource(fxmlPath);
            if (url == null) {
                System.out.println("❌ FXML not found: " + fxmlPath);
                return;
            }
            Parent root = FXMLLoader.load(url);
            Stage stage = (Stage) postsTable.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* ---------------- Helpers ---------------- */

    private String ns(String s) {
        return (s == null) ? "" : s;
    }

    private String formatTs(LocalDateTime ts) {
        if (ts == null)
            return "";
        return formatter.format(ts);
    }
}
