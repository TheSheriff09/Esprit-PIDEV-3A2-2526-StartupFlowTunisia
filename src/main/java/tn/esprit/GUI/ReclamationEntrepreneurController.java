package tn.esprit.GUI;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import tn.esprit.Services.ReclamationService;
import tn.esprit.Services.ResponseService;
import tn.esprit.entities.Reclamation;
import tn.esprit.entities.Response;
import tn.esprit.utils.CurrentUserSession;

import java.net.URL;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Locale;
import tn.esprit.utils.NavContext;

public class ReclamationEntrepreneurController {

    @FXML private TableView<Reclamation> table;

    @FXML private TableColumn<Reclamation, String> colId;
    @FXML private TableColumn<Reclamation, String> colTitle;
    @FXML private TableColumn<Reclamation, String> colDescription;
    @FXML private TableColumn<Reclamation, String> colStatus;
    @FXML private TableColumn<Reclamation, String> colTarget;
    @FXML private TableColumn<Reclamation, String> colCreated;
    @FXML private TableColumn<Reclamation, Void> colActions;

    @FXML private TextField searchField;
    @FXML private Label lblInfo;

    @FXML private ComboBox<String> cbTitle;
    @FXML private TextArea taDescription;
    @FXML private TextField tfTargetId;

    // Responses
    @FXML private TableView<Response> responsesTable;
    @FXML private TableColumn<Response, String> colRespCreated;
    @FXML private TableColumn<Response, String> colRespContent;
    @FXML private Label lblRespInfo;

    private final ReclamationService service = new ReclamationService();
    private final ResponseService responseService = new ResponseService();

    private final ObservableList<Reclamation> master = FXCollections.observableArrayList();
    private final ObservableList<Response> respMaster = FXCollections.observableArrayList();

    private FilteredList<Reclamation> filtered;

    private final ObservableList<String> titles =
            FXCollections.observableArrayList("USER_PROBLEM","SYSTEM_PROBLEM","SELF_PROBLEM","OTHER");

    @FXML
    private void initialize() {

        setupResponsesTable();

        cbTitle.setItems(titles);
        cbTitle.getSelectionModel().selectFirst();
        if (cbTitle.getValue() == null) cbTitle.setValue("SYSTEM_PROBLEM");

        cbTitle.valueProperty().addListener((obs, o, n) -> {
            boolean allow = "USER_PROBLEM".equalsIgnoreCase(n);
            tfTargetId.setDisable(!allow);
            if (!allow) tfTargetId.clear();
        });
        tfTargetId.setDisable(true);

        setupTable();
        loadMine();

        filtered = new FilteredList<>(master, r -> true);
        table.setItems(filtered);

        searchField.textProperty().addListener((obs, ov, nv) -> applyFilter(nv));

        table.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, sel) -> {
            if (sel == null) {
                respMaster.clear();
                if (lblRespInfo != null) lblRespInfo.setText("Select a reclamation to view replies.");
                return;
            }
            loadResponses(sel.getId());
        });
    }

    private void setupResponsesTable() {
        if (responsesTable == null) return;

        colRespCreated.setCellValueFactory(cd -> new SimpleStringProperty(formatTs(cd.getValue().getCreatedAt())));
        colRespContent.setCellValueFactory(cd -> new SimpleStringProperty(ns(cd.getValue().getContent())));

        responsesTable.setItems(respMaster);
        respMaster.clear();
    }

    private void loadResponses(int reclamationId) {
        respMaster.clear();
        respMaster.addAll(responseService.listByReclamationId(reclamationId));
        if (lblRespInfo != null) lblRespInfo.setText("Replies: " + respMaster.size());
        responsesTable.refresh();
    }

    private void setupTable() {
        colId.setCellValueFactory(cd -> new SimpleStringProperty(String.valueOf(cd.getValue().getId())));
        colTitle.setCellValueFactory(cd -> new SimpleStringProperty(ns(cd.getValue().getTitle())));
        colStatus.setCellValueFactory(cd -> new SimpleStringProperty(ns(cd.getValue().getStatus())));
        colTarget.setCellValueFactory(cd -> new SimpleStringProperty(
                cd.getValue().getTargetId() == null ? "" : String.valueOf(cd.getValue().getTargetId())
        ));
        colCreated.setCellValueFactory(cd -> new SimpleStringProperty(formatTs(cd.getValue().getCreatedAt())));

        colDescription.setCellValueFactory(cd -> new SimpleStringProperty(ns(cd.getValue().getDescription())));
        colDescription.setCellFactory(TextFieldTableCell.forTableColumn());
        colDescription.setOnEditCommit(e -> { /* ignore inline edit */ });

        colActions.setCellFactory(tc -> new TableCell<>() {

            private final Button btnEdit = new Button("✏");
            private final Button btnDelete = new Button("🗑");

            {
                btnEdit.getStyleClass().add("miniBtn");
                btnDelete.getStyleClass().add("miniDanger");

                btnEdit.setOnAction(e -> {
                    Reclamation r = getTableView().getItems().get(getIndex());
                    editDescription(r);
                });

                btnDelete.setOnAction(e -> {
                    Reclamation r = getTableView().getItems().get(getIndex());
                    deleteReclamation(r);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : new HBox(6, btnEdit, btnDelete));
            }
        });
    }

    private void loadMine() {
        master.clear();
        int myId = CurrentUserSession.user.getId();

        master.addAll(service.listByRequester(myId));
        lblInfo.setText("Total: " + master.size());
        table.refresh();
    }

    private void applyFilter(String q) {
        String query = (q == null) ? "" : q.trim().toLowerCase(Locale.ROOT);

        filtered.setPredicate(r -> {
            if (query.isEmpty()) return true;
            return ns(r.getTitle()).toLowerCase(Locale.ROOT).contains(query)
                    || ns(r.getDescription()).toLowerCase(Locale.ROOT).contains(query)
                    || ns(r.getStatus()).toLowerCase(Locale.ROOT).contains(query);
        });

        lblInfo.setText("Showing: " + filtered.size());
    }

    @FXML
    private void refresh() {
        Reclamation selected = table.getSelectionModel().getSelectedItem();
        loadMine();
        applyFilter(searchField.getText());

        if (selected != null) loadResponses(selected.getId());
    }

    @FXML
    private void submit() {

        String type = cbTitle.getValue();
        String desc = (taDescription.getText() == null) ? "" : taDescription.getText().trim();

        if (type == null || type.trim().isEmpty() || desc.isEmpty()) {
            type = "SYSTEM_PROBLEM";
            alert("Missing fields", "Type and description are required.");

            return;
        }

        System.out.println("cbTitle items=" + cbTitle.getItems());
        System.out.println("cbTitle value=" + cbTitle.getValue());
        Integer targetId = null;
        if ("USER_PROBLEM".equalsIgnoreCase(type)) {
            String t = (tfTargetId.getText() == null) ? "" : tfTargetId.getText().trim();
            if (t.isEmpty()) {
                alert("Target required", "For USER_PROBLEM you must enter target user id.");
                return;
            }
            try {
                targetId = Integer.parseInt(t);
            } catch (Exception ex) {
                alert("Invalid target", "Target ID must be a number.");
                return;
            }
        }

        int myId = CurrentUserSession.user.getId();

        Reclamation r = new Reclamation(type, desc, myId, targetId);
        System.out.println("Reclamation title before add = " + r.getTitle());
        Reclamation added = service.add(r);
        if (added == null) {
            alert("Failed", "Could not submit reclamation.");
            return;
        }

        taDescription.clear();
        tfTargetId.clear();
        cbTitle.getSelectionModel().select("SYSTEM_PROBLEM");

        refresh();
        toast("Submitted ");
    }

    private void editDescription(Reclamation r) {
        if (r == null) return;

        TextArea area = new TextArea(ns(r.getDescription()));
        area.setWrapText(true);
        area.setPrefHeight(220);

        Dialog<ButtonType> d = new Dialog<>();
        d.setTitle("Edit description");
        d.getDialogPane().setContent(area);
        d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        if (d.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;

        String newDesc = area.getText() == null ? "" : area.getText().trim();
        if (newDesc.isEmpty()) {
            alert("Empty", "Description cannot be empty.");
            return;
        }

        int myId = CurrentUserSession.user.getId();
        boolean ok = service.updateDescription(r.getId(), myId, newDesc);

        if (ok) {
            r.setDescription(newDesc);
            table.refresh();
            toast("Updated ");
        } else {
            alert("Failed", "Could not update (maybe not yours).");
        }
    }

    private void deleteReclamation(Reclamation r) {
        if (r == null) return;

        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setTitle("Delete");
        a.setHeaderText("Delete reclamation #" + r.getId() + "?");
        a.setContentText("This cannot be undone.");

        if (a.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;

        int myId = CurrentUserSession.user.getId();
        boolean ok = service.deleteById(r.getId(), myId);

        if (ok) {
            respMaster.clear();
            if (lblRespInfo != null) lblRespInfo.setText("Select a reclamation to view replies.");
            refresh();
            toast("Deleted");
        } else {
            alert("Failed", "Could not delete (maybe not yours).");
        }
    }

    private void goTo(ActionEvent e, String fxmlPath) {
        try {
            URL url = getClass().getResource(fxmlPath);
            if (url == null) {
                System.out.println("FXML not found on classpath: " + fxmlPath);
                return;
            }

            Parent root = FXMLLoader.load(url);

            Stage stage = (Stage) ((Button) e.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();

        } catch (Exception e1) {
            e1.printStackTrace();
        }
    }



    @FXML
    private void goBack(ActionEvent e) {
        goTo(e, NavContext.backFxml);
    }
    private void toast(String msg) { lblInfo.setText(msg); }

    private void alert(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    private String ns(String s) { return (s == null) ? "" : s; }

    private String formatTs(Timestamp ts) {
        if (ts == null) return "";
        return new SimpleDateFormat("yyyy-MM-dd HH:mm").format(ts);
    }
}