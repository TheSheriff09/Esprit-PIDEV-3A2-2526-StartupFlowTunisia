package tn.esprit.GUI.admin;

import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;
import tn.esprit.utils.SessionManager;

/**
 * AdminDashboardController — Shell controller for the Admin Back-Office.
 *
 * Manages the sidebar navigation and dynamically loads sub-pages into the
 * content area with smooth fade transitions. Provides modal-layer injection
 * to child controllers that need popup support.
 */
public class AdminDashboardController implements Initializable {

    // ── FXML ─────────────────────────────────────────────────
    @FXML
    private BorderPane mainPane;
    @FXML
    private VBox contentArea;
    @FXML
    private StackPane modalLayer;
    @FXML
    private Label topbarTitle;
    @FXML
    private Label lblDate;

    // Sidebar nav buttons
    @FXML
    private Button btnDashboard;
    @FXML
    private Button btnStartups;
    @FXML
    private Button btnBusinessPlans;
    @FXML
    private Button btnUsers;
    @FXML
    private Button btnSettings;
    @FXML
    private Button btnReports;

    private Button activeBtn;

    // ── Init ─────────────────────────────────────────────────

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // ── ADMIN role guard ──
        if (!SessionManager.requireRole(btnDashboard, "ADMIN"))
            return;

        lblDate.setText(LocalDate.now()
                .format(DateTimeFormatter.ofPattern("MMM d, yyyy")));
        activeBtn = btnDashboard;
        showDashboard();
    }

    // ── Sidebar navigation ───────────────────────────────────

    @FXML
    private void showDashboard() {
        setActive(btnDashboard);
        topbarTitle.setText("Dashboard Overview");
        loadPage("/admin/adminhome.fxml");
    }

    @FXML
    private void showStartups() {
        setActive(btnStartups);
        topbarTitle.setText("Startups Management");
        loadPage("/admin/adminstartups.fxml");
    }

    @FXML
    private void showBusinessPlans() {
        setActive(btnBusinessPlans);
        topbarTitle.setText("Business Plans");
        loadPage("/admin/adminbusinessplans.fxml");
    }

    // ── Back to main app ─────────────────────────────────────

    @FXML
    private void goBack() {
        try {
            Stage stage = (Stage) mainPane.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/startupview.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root,
                    stage.getScene().getWidth(), stage.getScene().getHeight());
            stage.setScene(scene);
            stage.setTitle("StartupFlow — Startup Management");
        } catch (IOException e) {
            System.err.println("[AdminDashboardController.goBack] " + e.getMessage());
        }
    }

    // ── Universal App Navigation ─────────────────────────────

    @FXML
    private void goAppDashboard() {
        tn.esprit.utils.NavigationManager.navigateTo(btnDashboard, "/DashboardAdmin.fxml");
    }

    @FXML
    private void goUsers() {
        tn.esprit.utils.NavigationManager.navigateTo(btnDashboard, "/UserManagement.fxml");
    }

    @FXML
    private void goProjects() {
        showDashboard();
    }

    @FXML
    private Button btnMentorship;

    @FXML
    private void goMentorship() {
        setActive(btnMentorship);
        topbarTitle.setText("Mentorship Logs & Management");
        loadPage("/admin/adminmentorship.fxml");
    }

    @FXML
    private void goFunding() {
        System.out.println("Go Funding");
    }

    @FXML
    private void goDashboardForum() {
        tn.esprit.GUI.ForumAdminController.showAnalyticsOnLoad = true;
        tn.esprit.utils.NavigationManager.navigateTo(btnDashboard, "/ForumAdmin.fxml");
    }

    @FXML
    private void goForumBackOffice() {
        tn.esprit.GUI.ForumAdminController.showAnalyticsOnLoad = false;
        tn.esprit.utils.NavigationManager.navigateTo(btnDashboard, "/ForumAdmin.fxml");
    }

    @FXML
    private void goSettings() {
        System.out.println("Go Settings");
    }

    @FXML
    private void logout() {
        tn.esprit.utils.NavigationManager.logout(btnDashboard);
    }

    // ── Helpers ──────────────────────────────────────────────

    /**
     * Swap active sidebar button styling without touching other style classes.
     */
    private void setActive(Button btn) {
        if (activeBtn != null && activeBtn != btn) {
            activeBtn.getStyleClass().remove("active");
        }
        if (!btn.getStyleClass().contains("active")) {
            btn.getStyleClass().add("active");
        }
        activeBtn = btn;
    }

    /**
     * Load an FXML page, inject modal support, and fade it in.
     */
    private void loadPage(String fxmlPath) {
        try {
            URL resource = getClass().getResource(fxmlPath);
            if (resource == null) {
                System.err.println("[AdminDashboardController.loadPage] FXML not found: " + fxmlPath);
                return;
            }
            FXMLLoader loader = new FXMLLoader(resource);
            Node page = loader.load();

            // Inject modal bridge into child controllers that need it
            Object ctrl = loader.getController();
            if (ctrl instanceof AdminStartupsController asc) {
                asc.setModalBridge(modalLayer, mainPane);
            } else if (ctrl instanceof AdminBusinessPlansController abpc) {
                abpc.setModalBridge(modalLayer, mainPane);
            }

            contentArea.getChildren().setAll(page);
            VBox.setVgrow(page, javafx.scene.layout.Priority.ALWAYS);
            if (page instanceof javafx.scene.layout.Region r) {
                r.setMaxHeight(Double.MAX_VALUE);
                r.setMaxWidth(Double.MAX_VALUE);
            }

            // Fade in the page
            page.setOpacity(0);
            FadeTransition ft = new FadeTransition(Duration.millis(280), page);
            ft.setFromValue(0);
            ft.setToValue(1);
            ft.play();

        } catch (Exception e) {
            System.err.println("[AdminDashboardController.loadPage] "
                    + fxmlPath + " — " + e.getClass().getSimpleName() + ": " + e.getMessage());
            if (e.getCause() != null)
                System.err.println("  Caused by: " + e.getCause().getMessage());
        }
    }
}
