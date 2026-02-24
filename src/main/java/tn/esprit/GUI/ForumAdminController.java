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
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import tn.esprit.entities.ForumPost;
import tn.esprit.Services.ForumPostService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class ForumAdminController {

    public static boolean showAnalyticsOnLoad = false;

    @FXML
    private javafx.scene.control.Button btnDashboardForum;
    @FXML
    private javafx.scene.control.Button btnForumBackOffice;

    @FXML
    private TableView<ForumPost> postsTable;

    @FXML
    private TableColumn<ForumPost, String> colId;
    @FXML
    private TableColumn<ForumPost, String> colTitle;
    @FXML
    private TableColumn<ForumPost, String> colContent;
    @FXML
    private TableColumn<ForumPost, String> colImage;
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
    @FXML
    private TextField tfImageUrl;

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

        pnlDashboard.setVisible(showAnalyticsOnLoad);
        pnlForum.setVisible(!showAnalyticsOnLoad);

        if (btnDashboardForum != null && btnForumBackOffice != null) {
            btnDashboardForum.getStyleClass().remove("active");
            btnForumBackOffice.getStyleClass().remove("active");
            if (showAnalyticsOnLoad) {
                btnDashboardForum.getStyleClass().add("active");
            } else {
                btnForumBackOffice.getStyleClass().add("active");
            }
        }

        if (showAnalyticsOnLoad) {
            refreshDashboard();
        }
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
        colContent.setCellFactory(TextFieldTableCell.forTableColumn());
        colContent.setOnEditCommit(e -> {
            ForumPost p = e.getRowValue();
            p.setContent(e.getNewValue());
            p.setUpdatedAt(LocalDateTime.now());
            updateRow(p);
        });

        colImage.setCellValueFactory(cd -> new SimpleStringProperty(ns(cd.getValue().getImageUrl())));

        colCreatedAt.setCellValueFactory(cd -> new SimpleStringProperty(formatTs(cd.getValue().getCreatedAt())));
        colUpdatedAt.setCellValueFactory(cd -> new SimpleStringProperty(formatTs(cd.getValue().getUpdatedAt())));

        // ACTION ICONS: update + delete
        colActions.setCellFactory(tc -> new TableCell<>() {
            private final Button btnDelete = new Button("ðŸ—‘");

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
        String imageUrl = tfImageUrl.getText() == null ? "" : tfImageUrl.getText().trim(); // Basic text input for Admin

        if (title.isEmpty() || content.isEmpty()) {
            showAlert("Missing fields", "Title and Content are required.");
            return;
        }

        ForumPost p = new ForumPost();
        p.setTitle(title);
        p.setContent(content);
        p.setImageUrl(imageUrl);
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

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Comments for: " + selected.getTitle());
        dialog.setHeaderText("Manage Comments");

        // Load CSS
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/css/usermanagement.css").toExternalForm());

        ButtonType closeButtonType = new ButtonType("Close", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().add(closeButtonType);

        VBox content = new VBox(10);
        content.setPadding(new javafx.geometry.Insets(15));
        content.setPrefWidth(450);
        content.setPrefHeight(350);
        content.setStyle("-fx-background-color: #f8f9fa;");

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        tn.esprit.Services.CommentService commentService = new tn.esprit.Services.CommentService();
        Runnable loadComments = () -> {
            content.getChildren().clear();
            java.util.List<tn.esprit.entities.Comment> comments = commentService.getByPostId(selected.getId());

            if (comments.isEmpty()) {
                Label empty = new Label("No comments yet.");
                empty.setStyle("-fx-text-fill: #999; -fx-font-style: italic;");
                content.getChildren().add(empty);
            } else {
                for (tn.esprit.entities.Comment c : comments) {
                    HBox row = new HBox(10);
                    row.getStyleClass().add("comment-row");
                    row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

                    Label lbl = new Label(c.getContent());
                    lbl.setWrapText(true);
                    lbl.getStyleClass().add("comment-label");
                    HBox.setHgrow(lbl, javafx.scene.layout.Priority.ALWAYS);

                    Button btnDel = new Button("ðŸ—‘");
                    btnDel.getStyleClass().add("comment-delete-btn");
                    btnDel.setOnAction(e -> {
                        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Delete this comment?", ButtonType.YES,
                                ButtonType.NO);
                        confirm.getDialogPane().getStylesheets()
                                .add(getClass().getResource("/css/usermanagement.css").toExternalForm());

                        if (confirm.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
                            commentService.delete(c);
                            // Hide row
                            row.setVisible(false);
                            row.setManaged(false);
                        }
                    });

                    row.getChildren().addAll(lbl, btnDel);
                    content.getChildren().add(row);
                }
            }
        };

        loadComments.run();
        dialog.getDialogPane().setContent(scroll);
        dialog.showAndWait();
    }

    private void clearForm() {
        tfTitle.clear();
        taContent.clear();
        tfImageUrl.clear();
    }

    // Panels
    @FXML
    private javafx.scene.layout.VBox pnlDashboard;
    @FXML
    private javafx.scene.layout.AnchorPane pnlForum;

    // Charts
    @FXML
    private javafx.scene.chart.PieChart pieChart;
    @FXML
    private javafx.scene.chart.BarChart<String, Number> barChart;

    private ForumPostService ps = new ForumPostService();
    private tn.esprit.Services.InteractionService interactionService = new tn.esprit.Services.InteractionService();
    private ForumPost selected = null;

    @FXML
    private void goDashboard() {
        goTo("/DashboardAdmin.fxml");
    }

    @FXML
    private void goUsers() {
        goTo("/UserManagement.fxml");
    }

    @FXML
    private void goDashboardForum() {
        showAnalyticsOnLoad = true;
        pnlDashboard.setVisible(true);
        pnlForum.setVisible(false);
        refreshDashboard();
    }

    @FXML
    private void goForumBackOffice() {
        showAnalyticsOnLoad = false;
        pnlDashboard.setVisible(false);
        pnlForum.setVisible(true);
    }

    @FXML
    private void toggleAnalytics() {
        boolean showingForum = pnlForum.isVisible();
        pnlForum.setVisible(!showingForum);
        pnlDashboard.setVisible(showingForum);
        if (showingForum) {
            refreshDashboard();
        }
    }

    private void refreshDashboard() {
        // Pie Chart: Interactions
        pieChart.getData().clear();
        java.util.Map<String, Integer> stats = interactionService.getGlobalInteractionStats();

        if (!stats.isEmpty()) {
            for (java.util.Map.Entry<String, Integer> entry : stats.entrySet()) {
                pieChart.getData().add(new javafx.scene.chart.PieChart.Data(entry.getKey(), entry.getValue()));
            }
        }

        // Bar Chart: Posts
        barChart.getData().clear();
        int postCount = ps.list().size();

        javafx.scene.chart.XYChart.Series<String, Number> series = new javafx.scene.chart.XYChart.Series<>();
        series.setName("Content");
        series.getData().add(new javafx.scene.chart.XYChart.Data<>("Posts", postCount));
        barChart.getData().add(series);
    }

    @FXML
    private void goProjects() {
        System.out.println("Projects Clicked");
    }

    @FXML
    private void goMentorship() {
        System.out.println("Mentorship Clicked");
    }

    @FXML
    private void goFunding() {
        System.out.println("Funding Clicked");
    }

    @FXML
    private void goForumFeed() {
        goTo("/ForumFeed.fxml");
    }

    @FXML
    private void goSettings() {
        System.out.println("Settings Clicked");
    }

    @FXML
    private void logout() {
        try {
            // Return to MainFx (Role Selection)
            tn.esprit.MainFx main = new tn.esprit.MainFx();
            main.start((javafx.stage.Stage) postsTable.getScene().getWindow());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showAlert(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    // NAVIGATION
    /* The navigation methods below are replaced by the new ones above */

    private void goTo(String fxmlPath) {
        try {
            java.net.URL url = getClass().getResource(fxmlPath);
            Parent root = FXMLLoader.load(url);
            Stage stage = (Stage) postsTable.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String ns(String s) {
        return (s == null) ? "" : s;
    }

    private String formatTs(LocalDateTime ts) {
        return ts == null ? "" : formatter.format(ts);
    }
}
