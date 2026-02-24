package tn.esprit.GUI;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import tn.esprit.Services.UserService;
import tn.esprit.entities.User;
import tn.esprit.utils.CurrentUserSession;

import java.net.URL;
import java.util.ResourceBundle;

public class DashboardAdminController implements Initializable {

    @FXML private BarChart<String, Number> usersBarChart;

    @FXML private Label lblMentorsCount;
    @FXML private Label lblEvaluatorsCount;
    @FXML private Label lblEntrepreneursCount;

    @FXML private Label lblStatus;
    @FXML private VBox userDropdown;

    @FXML private Button btnUserMenu;

    private final UserService userService = new UserService();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

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
        User admin = CurrentUserSession.user;

        if (btnUserMenu != null) btnUserMenu.setText("Admin");
        if (lblStatus != null) lblStatus.setText("Approved");

        if (admin == null) return;

        String fullName = (admin.getFullName() == null) ? "Admin" : admin.getFullName().trim();
        if (btnUserMenu != null) btnUserMenu.setText(fullName);

        String dbStatus = (admin.getStatus() == null) ? "PENDING" : admin.getStatus().trim();

        if (lblStatus != null) {
            if (dbStatus.equalsIgnoreCase("ACTIVE")) lblStatus.setText("Approved");
            else if (dbStatus.equalsIgnoreCase("PENDING")) lblStatus.setText("Pending");
            else if (dbStatus.equalsIgnoreCase("BLOCKED")) lblStatus.setText("Blocked");
            else lblStatus.setText(dbStatus);
        }
    }

    private void setCounts(int mentors, int evaluators, int entrepreneurs) {
        if (lblMentorsCount != null) lblMentorsCount.setText(String.valueOf(mentors));
        if (lblEvaluatorsCount != null) lblEvaluatorsCount.setText(String.valueOf(evaluators));
        if (lblEntrepreneursCount != null) lblEntrepreneursCount.setText(String.valueOf(entrepreneurs));
    }

    private void loadUsersChart(int mentors, int evaluators, int entrepreneurs) {
        if (usersBarChart == null) return;

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
        if (userDropdown == null) return;

        boolean show = !userDropdown.isVisible();
        userDropdown.setVisible(show);
        userDropdown.setManaged(show);
    }

    private void goTo(javafx.event.ActionEvent e, String fxmlPath) {
        try {
            java.net.URL url = getClass().getResource(fxmlPath);
            System.out.println("FXML URL for " + fxmlPath + " => " + url);

            if (url == null) {
                System.out.println("FXML NOT FOUND: " + fxmlPath);
                return;
            }

            Parent root = FXMLLoader.load(url);

            javafx.scene.Node source = (javafx.scene.Node) e.getSource();
            Stage stage = (Stage) source.getScene().getWindow();

            stage.setScene(new Scene(root));
            stage.show();

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    @FXML private void goDashboard() {  }
    @FXML private void goUsers(javafx.event.ActionEvent e) {
        goTo(e, "/UserManagement.fxml");
    }
    @FXML private void goProjects() { System.out.println("Go Startup Projects"); }
    @FXML private void goMentorship() { System.out.println("Go Mentorship"); }
    @FXML private void goFunding() { System.out.println("Go Funding & Evaluation"); }
    @FXML private void goForum() { System.out.println("Go Forum"); }
    @FXML private void goSettings() { System.out.println("Go Settings"); }


    @FXML
    private void openAddUser() {
        System.out.println("Open Add User");
    }

    @FXML
    private void openProfile() {
        System.out.println("Open Profile (Admin)");
    }

    @FXML
    private void openMyStartups() {
        System.out.println("Admin: Startups list");
    }

    @FXML
    private void logout(javafx.event.ActionEvent e) {
        CurrentUserSession.user = null;
        goTo(e,"/Login.fxml");
    }
}