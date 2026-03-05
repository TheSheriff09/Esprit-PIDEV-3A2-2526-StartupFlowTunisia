package tn.esprit.GUI;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import tn.esprit.Services.UserService;
import tn.esprit.entities.User;
import tn.esprit.utils.NavigationManager;
import tn.esprit.utils.SessionManager;

import java.net.URL;
import java.util.ResourceBundle;

public class DashboardAdminController implements Initializable {

    @FXML
    private BarChart<String, Number> usersBarChart;

    @FXML
    private Label lblMentorsCount;
    @FXML
    private Label lblEvaluatorsCount;
    @FXML
    private Label lblEntrepreneursCount;

    @FXML
    private Label lblStatus;
    @FXML
    private VBox userDropdown;

    @FXML
    private Button btnDashboard;

    private final UserService userService = new UserService();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // ── ADMIN role guard ──
        if (!SessionManager.requireRole(btnDashboard, "ADMIN"))
            return;

        loadAdminHeader();

        int mentors = userService.countByRole("MENTOR");
        int evaluators = userService.countByRole("EVALUATOR");
        int entrepreneurs = userService.countByRole("ENTREPRENEUR");

        setCounts(mentors, evaluators, entrepreneurs);
        loadUsersChart(mentors, evaluators, entrepreneurs);

        if (userDropdown != null) {
            userDropdown.setVisible(false);
            userDropdown.setManaged(false);
        }
    }

    private void loadAdminHeader() {
        User admin = SessionManager.getUser();

        if (btnDashboard != null)
            btnDashboard.setText(SessionManager.getDisplayName());
        if (lblStatus != null)
            lblStatus.setText("Approved");

        if (admin == null)
            return;

        String dbStatus = (admin.getStatus() == null) ? "PENDING" : admin.getStatus().trim();

        if (lblStatus != null) {
            if (dbStatus.equalsIgnoreCase("ACTIVE"))
                lblStatus.setText("Approved");
            else if (dbStatus.equalsIgnoreCase("PENDING"))
                lblStatus.setText("Pending");
            else if (dbStatus.equalsIgnoreCase("BLOCKED"))
                lblStatus.setText("Blocked");
            else
                lblStatus.setText(dbStatus);
        }
    }

    private void setCounts(int mentors, int evaluators, int entrepreneurs) {
        if (lblMentorsCount != null)
            lblMentorsCount.setText(String.valueOf(mentors));
        if (lblEvaluatorsCount != null)
            lblEvaluatorsCount.setText(String.valueOf(evaluators));
        if (lblEntrepreneursCount != null)
            lblEntrepreneursCount.setText(String.valueOf(entrepreneurs));
    }

    private void loadUsersChart(int mentors, int evaluators, int entrepreneurs) {
        if (usersBarChart == null)
            return;

        usersBarChart.getData().clear();

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Users by role");

        series.getData().add(new XYChart.Data<>("Mentors", mentors));
        series.getData().add(new XYChart.Data<>("Evaluators", evaluators));
        series.getData().add(new XYChart.Data<>("Entrepreneurs", entrepreneurs));

        usersBarChart.getData().add(series);

        usersBarChart.setLegendVisible(false);
        usersBarChart.setAnimated(true);
    }

    @FXML
    private void toggleUserMenu() {
        if (userDropdown == null)
            return;

        boolean show = !userDropdown.isVisible();
        userDropdown.setVisible(show);
        userDropdown.setManaged(show);
    }

    @FXML
    private void goDashboard() {
    }

    @FXML
    private void goUsers(javafx.event.ActionEvent e) {
        NavigationManager.navigateTo(btnDashboard, "/UserManagement.fxml");
    }

    @FXML
    private void goProjects() {
        NavigationManager.navigateTo(btnDashboard, "/admindashboard.fxml");
    }

    @FXML
    private void goMentorship() {
        System.out.println("Go Mentorship");
    }

    @FXML
    private void goFunding() {
        NavigationManager.navigateTo(btnDashboard, "/AdminFundingDashboard.fxml");
    }

    @FXML
    private void goDashboardForum(javafx.event.ActionEvent e) {
        tn.esprit.GUI.ForumAdminController.showAnalyticsOnLoad = true;
        NavigationManager.navigateTo(btnDashboard, "/ForumAdmin.fxml");
    }

    @FXML
    private void goForumBackOffice(javafx.event.ActionEvent e) {
        tn.esprit.GUI.ForumAdminController.showAnalyticsOnLoad = false;
        NavigationManager.navigateTo(btnDashboard, "/ForumAdmin.fxml");
    }

    @FXML
    private void goSettings() {
        System.out.println("Go Settings");
    }

    @FXML
    private void openAddUser() {
        System.out.println("Open Add User");
    }

    @FXML
    private void openProfile() {
        tn.esprit.utils.NavContext.setBack("/DashboardAdmin.fxml");
        NavigationManager.navigateTo(btnDashboard, "/ManageProfile.fxml");
    }

    @FXML
    private void openMyStartups() {
        System.out.println("Admin: Startups list");
        NavigationManager.navigateTo(btnDashboard, "/admin/adminstartups.fxml");
    }

    @FXML
    private void logout(javafx.event.ActionEvent e) {
        NavigationManager.logout(btnDashboard);
    }
}
