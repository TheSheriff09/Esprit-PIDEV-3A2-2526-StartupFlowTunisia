package tn.esprit.GUI;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import tn.esprit.entities.ForumPost;
import tn.esprit.Services.ForumPostService;

import java.io.IOException;
import java.util.List;

import javafx.animation.TranslateTransition;
import javafx.animation.Animation;
import javafx.util.Duration;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.HBox;
import javafx.application.Platform;
import tn.esprit.entities.User;
import tn.esprit.utils.CurrentUserSession;
import tn.esprit.utils.NavContext;

public class ForumFeedController {

    @FXML
    private VBox postsContainer;

    @FXML
    private Label newsTickerLabel;

    @FXML
    private HBox newsTickerContainer;

    @FXML
    private javafx.scene.layout.Pane tickerPane;

    @FXML
    private Label navForumDashboard;

    @FXML
    private Label navUserManagement;

    @FXML
    private MenuButton userMenuBtn;

    @FXML
    private MenuItem miHeader;

    @FXML
    private MenuItem miLogout;

    private final ForumPostService postService = new ForumPostService();

    @FXML
    public void initialize() {
        setupUserDropdown();
        refreshFeed();
        initNewsTicker();
        setupAdminNav();
    }

    private void setupAdminNav() {
        if (navForumDashboard == null)
            return;

        User u = CurrentUserSession.user;
        boolean isAdmin = (u != null && "ADMIN".equalsIgnoreCase(u.getRole()));

        navForumDashboard.setVisible(isAdmin);
        navForumDashboard.setManaged(isAdmin);

        if (navUserManagement != null) {
            navUserManagement.setVisible(isAdmin);
            navUserManagement.setManaged(isAdmin);
        }
    }

    private void setupUserDropdown() {
        if (userMenuBtn == null)
            return;
        User u = CurrentUserSession.user;
        if (u == null) {
            userMenuBtn.setText("User");
            if (miHeader != null)
                miHeader.setText("USER");
            return;
        }

        String fullName = (u.getFullName() == null) ? "User" : u.getFullName().trim();
        userMenuBtn.setText(fullName);
        if (miHeader != null)
            miHeader.setText(fullName);
    }

    private void initNewsTicker() {
        ForumPost newestPost = postService.getNewestPost();
        if (newestPost != null) {
            String content = newestPost.getContent();
            // Truncate content if it's too long to keep the ticker readable
            if (content != null && content.length() > 50) {
                content = content.substring(0, 50) + "...";
            }

            newsTickerLabel.setText("BREAKING: " + newestPost.getTitle() + " - " + content + " ("
                    + newestPost.getCreatedAt().toString() + ")");

            // Allow the label to layout so we can get its width
            Platform.runLater(() -> {
                // Clips the text so it doesn't overlap with "LATEST POST"
                if (tickerPane != null) {
                    javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle();
                    clip.widthProperty().bind(tickerPane.widthProperty());
                    clip.heightProperty().bind(tickerPane.heightProperty());
                    tickerPane.setClip(clip);
                }

                double containerWidth = tickerPane != null ? tickerPane.getWidth() : newsTickerContainer.getWidth();
                double labelWidth = newsTickerLabel.getWidth();

                TranslateTransition transition = new TranslateTransition(Duration.seconds(15), newsTickerLabel);
                transition.setFromX(containerWidth);
                transition.setToX(-labelWidth - 50); // Move completely off screen
                transition.setCycleCount(Animation.INDEFINITE);
                transition.play();
            });
        } else {
            newsTickerLabel.setText("No news available.");
        }
    }

    public void refreshFeed() {
        postsContainer.getChildren().clear();
        List<ForumPost> posts = postService.list();

        // Sort by newest first
        posts.sort((p1, p2) -> p2.getCreatedAt().compareTo(p1.getCreatedAt()));

        for (ForumPost post : posts) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/PostItem.fxml"));
                Parent postItem = loader.load();

                PostItemController controller = loader.getController();
                controller.setData(post);

                postsContainer.getChildren().add(postItem);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void addPost() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/AddPost.fxml"));
            Parent root = loader.load();

            AddPostController controller = loader.getController();
            controller.setParentController(this);

            Stage stage = new Stage();
            stage.setTitle("Create Post");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Complete Navigation Implementation

    private void goTo(String fxmlPath) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            Stage stage = (Stage) postsContainer.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onDashboard() {
        User u = CurrentUserSession.user;
        if (u == null) {
            goTo("/Login.fxml");
            return;
        }

        String role = (u.getRole() == null) ? "" : u.getRole().trim().toUpperCase();

        if (role.equals("ADMIN")) {
            goTo("/DashboardAdmin.fxml");
        } else if (role.equals("ENTREPRENEUR")) {
            goTo("/EntrepreneurDashboard.fxml");
        } else if (role.equals("MENTOR")) {
            goTo("/MentorDashboard.fxml");
        } else if (role.equals("EVALUATOR")) {
            goTo("/EvaluatorDashboard.fxml");
        } else {
            // Default fallback
            goTo("/EntrepreneurDashboard.fxml");
        }
    }

    @FXML
    private void onStartup() {
        System.out.println("Navigating to Startup");
    }

    @FXML
    private void onMentorship() {
        System.out.println("Navigating to Mentorship");
    }

    @FXML
    private void onFunding() {
        System.out.println("Navigating to Funding");
    }

    @FXML
    private void onFourm() {
        /* Already on Forum */
    }

    @FXML
    private void onForumDashboard() {
        tn.esprit.GUI.ForumAdminController.showAnalyticsOnLoad = true;
        goTo("/ForumAdmin.fxml");
    }

    @FXML
    private void onUserManagement() {
        goTo("/UserManagement.fxml");
    }

    @FXML
    private void onManageProfile() {
        NavContext.setBack("/ForumFeed.fxml");
        goTo("/ManageProfile.fxml");
    }

    @FXML
    private void onStartups() {
        System.out.println("Startups");
    }

    @FXML
    private void onSettings() {
        System.out.println("Settings");
    }

    @FXML
    private void onLogout() {
        CurrentUserSession.user = null;
        goTo("/Signup.fxml");
    }
}
