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

public class ReclamationAdminController {

    @FXML private TableView<Reclamation> reclamationsTable;
    @FXML private TableColumn<Reclamation, String> colId;
    @FXML private TableColumn<Reclamation, String> colTitle;
    @FXML private TableColumn<Reclamation, String> colDescription;
    @FXML private TableColumn<Reclamation, String> colStatus;
    @FXML private TableColumn<Reclamation, String> colRequested;
    @FXML private TableColumn<Reclamation, String> colTarget;
    @FXML private TableColumn<Reclamation, String> colCreated;
    @FXML private TableColumn<Reclamation, Void> colActions;

    @FXML private TextField searchField;
    @FXML private Label lblInfo;

    @FXML private Label lblSelected;
    @FXML private ComboBox<String> cbStatus;

    @FXML private TextArea taResponse;

    @FXML private TableView<Response> responsesTable;
    @FXML private TableColumn<Response, String> colRespCreated;
    @FXML private TableColumn<Response, String> colRespContent;
    @FXML private Label lblRespInfo;

    private final ReclamationService reclamationService = new ReclamationService();
    private final ResponseService responseService = new ResponseService();

    private final ObservableList<Reclamation> master = FXCollections.observableArrayList();
    private FilteredList<Reclamation> filtered;

    private final ObservableList<Response> respMaster = FXCollections.observableArrayList();

    private final ObservableList<String> statuses =
            FXCollections.observableArrayList("OPEN","IN_PROGRESS","RESOLVED","REJECTED");

    private Reclamation selected;

    @FXML
    private void initialize() {

        cbStatus.setItems(statuses);

        setupTable();
        setupResponsesTable();

        loadAll();

        filtered = new FilteredList<>(master, r -> true);
        reclamationsTable.setItems(filtered);

        searchField.textProperty().addListener((obs, ov, nv) -> applyFilter(nv));

        reclamationsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, sel) -> {
            selected = sel;
            onSelect(sel);
        });
    }

    private void setupTable() {
        colId.setCellValueFactory(cd -> new SimpleStringProperty(String.valueOf(cd.getValue().getId())));
        colTitle.setCellValueFactory(cd -> new SimpleStringProperty(ns(cd.getValue().getTitle())));
        colDescription.setCellValueFactory(cd -> new SimpleStringProperty(ns(cd.getValue().getDescription())));
        colStatus.setCellValueFactory(cd -> new SimpleStringProperty(ns(cd.getValue().getStatus())));
        colRequested.setCellValueFactory(cd -> new SimpleStringProperty(String.valueOf(cd.getValue().getRequestedId())));
        colTarget.setCellValueFactory(cd -> new SimpleStringProperty(
                cd.getValue().getTargetId() == null ? "" : String.valueOf(cd.getValue().getTargetId())
        ));
        colCreated.setCellValueFactory(cd -> new SimpleStringProperty(formatTs(cd.getValue().getCreatedAt())));

        colActions.setCellFactory(tc -> new TableCell<>() {
            private final Button btnDelete = new Button("🗑");
            {
                btnDelete.getStyleClass().add("miniDanger");
                btnDelete.setOnAction(e -> {
                    Reclamation r = getTableView().getItems().get(getIndex());
                    deleteReclamation(r);
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : new HBox(6, btnDelete));
            }
        });
    }

    private void setupResponsesTable() {
        colRespCreated.setCellValueFactory(cd -> new SimpleStringProperty(formatTs(cd.getValue().getCreatedAt())));
        colRespContent.setCellValueFactory(cd -> new SimpleStringProperty(ns(cd.getValue().getContent())));
        responsesTable.setItems(respMaster);
    }

    private void loadAll() {
        master.clear();
        master.addAll(reclamationService.list());
        lblInfo.setText("Total: " + master.size());
        reclamationsTable.refresh();
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

    private void onSelect(Reclamation r) {
        respMaster.clear();

        if (r == null) {
            lblSelected.setText("Select a reclamation...");
            cbStatus.getSelectionModel().clearSelection();
            lblRespInfo.setText("Select a reclamation to view responses.");
            return;
        }

        lblSelected.setText("Selected: #" + r.getId() + "  (requested: " + r.getRequestedId() + ")");
        cbStatus.getSelectionModel().select(ns(r.getStatus()));

        loadResponses(r.getId());
    }

    private void loadResponses(int reclamationId) {
        respMaster.clear();
        respMaster.addAll(responseService.listByReclamationId(reclamationId));
        lblRespInfo.setText("Responses: " + respMaster.size());
        responsesTable.refresh();
    }

    @FXML
    private void refresh() {
        Reclamation keep = selected;
        loadAll();
        applyFilter(searchField.getText());
        if (keep != null) {
            // try to reselect
            for (Reclamation r : master) {
                if (r.getId() == keep.getId()) {
                    reclamationsTable.getSelectionModel().select(r);
                    break;
                }
            }
        }
    }

    @FXML
    private void saveStatus() {
        if (selected == null) {
            alert("Select one", "Select a reclamation first.");
            return;
        }

        String st = cbStatus.getValue();
        if (st == null || st.trim().isEmpty()) {
            alert("Missing", "Choose a status.");
            return;
        }

        boolean ok = reclamationService.updateStatus(selected.getId(), st);
        if (!ok) {
            alert("Failed", "Could not update status.");
            return;
        }

        selected.setStatus(st);
        reclamationsTable.refresh();
        toast("Status updated ");
    }

    @FXML
    private void sendResponse() {
        if (selected == null) {
            alert("Select one", "Select a reclamation first.");
            return;
        }

        String content = (taResponse.getText() == null) ? "" : taResponse.getText().trim();
        if (content.isEmpty()) {
            alert("Empty", "Response content cannot be empty.");
            return;
        }

        if (CurrentUserSession.user == null) {
            alert("Session", "No logged-in admin in session.");
            return;
        }

        int adminId = CurrentUserSession.user.getId();

        Response resp = new Response(content, selected.getId(), adminId);
        Response added = responseService.add(resp);

        if (added == null) {
            alert("Failed", "Could not send response.");
            return;
        }

        taResponse.clear();
        loadResponses(selected.getId());
        toast("Response sent");
    }

    private void deleteReclamation(Reclamation r) {
        if (r == null) return;

        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setTitle("Delete");
        a.setHeaderText("Delete reclamation #" + r.getId() + "?");
        a.setContentText("This will delete its responses too (CASCADE).");
        if (a.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;

        boolean ok = reclamationService.deleteByIdAdmin(r.getId());
        if (!ok) {
            alert("Failed", "Could not delete reclamation.");
            return;
        }

        if (selected != null && selected.getId() == r.getId()) {
            selected = null;
            respMaster.clear();
            onSelect(null);
        }

        refresh();
        toast("Deleted ");
    }

    @FXML
    private void goBack(ActionEvent e) {
        goTo(e, "/DashboardAdmin.fxml");
    }

    private void goTo(ActionEvent e, String fxmlPath) {
        try {
            URL url = getClass().getResource(fxmlPath);
            if (url == null) {
                System.out.println("FXML not found: " + fxmlPath);
                return;
            }
            Parent root = FXMLLoader.load(url);
            Stage stage = (Stage) ((Button) e.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
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
