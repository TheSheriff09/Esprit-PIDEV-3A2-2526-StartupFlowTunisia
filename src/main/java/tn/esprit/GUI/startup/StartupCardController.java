package tn.esprit.GUI.startup;

import javafx.animation.ScaleTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import tn.esprit.entities.Startup;
import tn.esprit.Services.InvestmentScorer;
import tn.esprit.Services.StartupService;

import java.util.function.Consumer;

/**
 * Controller for a single startup card (startupcard.fxml).
 * The parent StartupViewController injects the Startup data
 * and callback lambdas after loading this FXML.
 */
public class StartupCardController {

    // ── FXML nodes ────────────────────────────────────────────
    @FXML
    private VBox cardRoot;
    @FXML
    private Label lblSector;
    @FXML
    private Label lblName;
    @FXML
    private Label lblDescription;
    @FXML
    private Label lblDate;
    @FXML
    private Label lblStatus;
    @FXML
    private Label lblFunding;
    @FXML
    private Label lblScore;
    @FXML
    private Label lblScoreBand;
    @FXML
    private Button btnMore;

    // ── State ─────────────────────────────────────────────
    private Startup startup;
    private Consumer<Startup> onEdit;
    private Consumer<Startup> onDelete;
    private Consumer<Startup> onViewPlans;
    private Consumer<Startup> onSimulate;
    private Consumer<Startup> onQR;
    private Consumer<Startup> onExport;
    private Consumer<Startup> onFunding;

    // ── Context menu built once per card ──────────────────────
    private ContextMenu contextMenu;

    // ── Service ───────────────────────────────────────────────
    private final StartupService startupService = new StartupService();

    // ── Injection from parent ─────────────────────────────────

    /**
     * Called by StartupViewController after FXMLLoader.load().
     */
    public void setData(Startup startup,
            Consumer<Startup> onEdit,
            Consumer<Startup> onDelete,
            Consumer<Startup> onViewPlans,
            Consumer<Startup> onSimulate,
            Consumer<Startup> onQR,
            Consumer<Startup> onExport,
            Consumer<Startup> onFunding) {
        this.startup = startup;
        this.onEdit = onEdit;
        this.onDelete = onDelete;
        this.onViewPlans = onViewPlans;
        this.onSimulate = onSimulate;
        this.onQR = onQR;
        this.onExport = onExport;
        this.onFunding = onFunding;
        buildContextMenu();
        populate();
        loadScore();
        attachHover();
    }

    /**
     * Backward-compatible overload (no QR / export callbacks).
     */
    public void setData(Startup startup,
            Consumer<Startup> onEdit,
            Consumer<Startup> onDelete,
            Consumer<Startup> onViewPlans,
            Consumer<Startup> onSimulate) {
        setData(startup, onEdit, onDelete, onViewPlans, onSimulate, s -> {
        }, s -> {
        }, s -> {
        });
    }

    public void setData(Startup startup,
            Consumer<Startup> onEdit,
            Consumer<Startup> onDelete,
            Consumer<Startup> onViewPlans) {
        setData(startup, onEdit, onDelete, onViewPlans, s -> {
        }, s -> {
        }, s -> {
        }, s -> {
        });
    }

    // ── Context menu ──────────────────────────────────────────

    private void buildContextMenu() {
        contextMenu = new ContextMenu();
        contextMenu.getStyleClass().add("card-context-menu");

        MenuItem editItem = new MenuItem("✏  Edit");
        editItem.getStyleClass().add("menu-item-card");
        editItem.setOnAction(e -> onEdit.accept(startup));

        MenuItem plansItem = new MenuItem("📋  Business Plans");
        plansItem.getStyleClass().add("menu-item-card");
        plansItem.setOnAction(e -> onViewPlans.accept(startup));

        MenuItem simulateItem = new MenuItem("💰  Simulate Funding");
        simulateItem.getStyleClass().add("menu-item-card");
        simulateItem.setOnAction(e -> onSimulate.accept(startup));

        MenuItem qrItem = new MenuItem("📱  Generate QR");
        qrItem.getStyleClass().add("menu-item-card");
        qrItem.setOnAction(e -> {
            if (onQR != null)
                onQR.accept(startup);
        });

        MenuItem exportItem = new MenuItem("📄  Export PDF");
        exportItem.getStyleClass().add("menu-item-card");
        exportItem.setOnAction(e -> {
            if (onExport != null)
                onExport.accept(startup);
        });

        MenuItem deleteItem = new MenuItem("🗑  Delete");
        deleteItem.getStyleClass().add("menu-item-delete");
        deleteItem.setOnAction(e -> onDelete.accept(startup));

        contextMenu.getItems().addAll(
                editItem, plansItem, simulateItem, qrItem, exportItem,
                new SeparatorMenuItem(), deleteItem);
    }

    // ── Populate labels ───────────────────────────────────────

    private void populate() {
        lblName.setText(startup.getName() != null ? startup.getName() : "—");

        String sector = startup.getSector() != null ? startup.getSector() : "General";
        lblSector.setText("• Sector: " + sector);

        String stage = startup.getStage() != null ? startup.getStage() : "Seed";
        lblDescription.setText("• Stage: " + stage);

        String mentor = (startup.getIncubatorProgram() != null && !startup.getIncubatorProgram().isBlank())
                ? startup.getIncubatorProgram()
                : "Not assigned";
        lblStatus.setText("• Mentor: " + mentor);

        String status = startup.getStatus() != null ? startup.getStatus() : "—";
        lblFunding.setText("• Status: " + status);

        lblDate.setText("");
    }

    /**
     * Calculates and renders the Investment Score badge.
     * Uses a background thread so the card renders instantly
     * and the score appears once the DB query finishes.
     */
    private void loadScore() {
        lblScore.setText("…");
        lblScore.setStyle("-fx-font-size:12px; -fx-font-weight:800; -fx-padding: 2 10 2 10;"
                + "-fx-background-radius:20; -fx-background-color:rgba(255,255,255,0.12);"
                + "-fx-text-fill:#a78bfa;");
        lblScoreBand.setText("");

        int id = startup.getStartupID();
        Thread t = new Thread(() -> {
            double score = startupService.calculateInvestmentScore(id);
            javafx.application.Platform.runLater(() -> renderScore(score));
        });
        t.setDaemon(true);
        t.start();
    }

    /** Applies color-coded styling to the score label. */
    private void renderScore(double score) {
        String hexColor = InvestmentScorer.hexColor(score);
        String band = InvestmentScorer.band(score);

        lblScore.setText(String.format("%.0f / 100", score));
        lblScore.setStyle(
                "-fx-font-size:12px; -fx-font-weight:800; -fx-padding: 2 10 2 10;"
                        + "-fx-background-radius:20;"
                        + "-fx-background-color:" + hexColor + "22;"
                        + "-fx-text-fill:" + hexColor + ";"
                        + "-fx-border-color:" + hexColor + "55;"
                        + "-fx-border-radius:20; -fx-border-width:1;");

        lblScoreBand.setText(band);
        lblScoreBand.setStyle("-fx-font-size:10.5px; -fx-opacity:0.8; -fx-text-fill:" + hexColor + ";");
    }

    // ── Hover animation ───────────────────────────────────────

    private void attachHover() {
        cardRoot.setOnMouseEntered(e -> scale(1.03));
        cardRoot.setOnMouseExited(e -> scale(1.00));

        // Clicking the card body (not the "⋯" button) → view plans
        cardRoot.setOnMouseClicked(e -> {
            javafx.scene.Node node = (javafx.scene.Node) e.getTarget();
            while (node != null && node != cardRoot) {
                if (node == btnMore)
                    return;
                node = node.getParent();
            }
            onViewPlans.accept(startup);
        });
    }

    private void scale(double factor) {
        ScaleTransition st = new ScaleTransition(Duration.millis(180), cardRoot);
        st.setToX(factor);
        st.setToY(factor);
        st.play();
    }

    // ── "More" button handler ─────────────────────────────────

    @FXML
    private void handleMore() {
        contextMenu.show(btnMore, javafx.geometry.Side.BOTTOM, 0, 4);
    }

    @FXML
    private void handleFunding() {
        if (onFunding != null && startup != null) {
            onFunding.accept(startup);
        }
    }
}
