package tn.esprit.GUI;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import tn.esprit.entities.ForumPost;
import tn.esprit.Services.ForumPostService;
import tn.esprit.entities.User;
import tn.esprit.utils.AudioCapture;
import tn.esprit.utils.CurrentUserSession;
import org.vosk.Model;
import org.vosk.Recognizer;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.io.File;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import javafx.application.Platform;

public class AddPostController {

    @FXML
    private TextField tfTitle;
    @FXML
    private TextArea taContent;
    @FXML
    private Label lblImageName;
    @FXML
    private ImageView imgPreview;

    private File selectedImageFile;
    private ForumFeedController parentController;
    private final ForumPostService postService = new ForumPostService();

    public void setParentController(ForumFeedController parentController) {
        this.parentController = parentController;
    }

    @FXML
    private void uploadImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Image");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.gif"));
        selectedImageFile = fileChooser.showOpenDialog(tfTitle.getScene().getWindow());

        if (selectedImageFile != null) {
            lblImageName.setText(selectedImageFile.getName());
            imgPreview.setImage(new Image(selectedImageFile.toURI().toString()));
        }
    }

    @FXML
    private void submit() {
        String title = tfTitle.getText().trim();
        String content = taContent.getText().trim();

        if (title.isEmpty() || content.isEmpty()) {
            showAlert("Validation Error", "Title and Content are required.");
            return;
        }

        // Profanity Check
        if (tn.esprit.utils.ProfanityFilter.containsProfanity(title) ||
                tn.esprit.utils.ProfanityFilter.containsProfanity(content)) {
            showAlert("Content Warning", "Your post contains inappropriate language. Please revise.");
            return;
        }

        ForumPost post = new ForumPost();
        post.setTitle(title);
        post.setContent(content);
        post.setCreatedAt(LocalDateTime.now());

        User activeUser = CurrentUserSession.user;
        if (activeUser != null) {
            post.setUserId(activeUser.getId());
            // Admin Authorship logic
            if ("admin".equalsIgnoreCase(activeUser.getRole())) {
                post.setAuthorName("Manager");
            } else {
                post.setAuthorName(activeUser.getFullName() != null ? activeUser.getFullName().trim() : "Unknown User");
            }
        } else {
            post.setUserId(1);
            post.setAuthorName("Community User");
        }

        if (selectedImageFile != null) {
            post.setImageUrl(selectedImageFile.getAbsolutePath());
        }

        postService.add(post);

        if (parentController != null) {
            parentController.refreshFeed();
        }

        close();
    }

    @FXML
    private void cancel() {
        close();
    }

    private AudioCapture audioCapture;
    private boolean isDictating = false;

    @FXML
    private void onDictate() {
        if (isDictating) {
            // Stop dictation
            if (audioCapture != null) {
                audioCapture.stop();
            }
            isDictating = false;

            String text = taContent.getText();
            text = text.replace(" [Listening... Click Dictate again to stop]", "");
            text = text.replace("[Listening... Click Dictate again to stop]", "");
            taContent.setText(text.trim());

            showAlert("Dictation", "Microphone stopped.");
            return;
        }

        taContent.setText(taContent.getText() + (taContent.getText().isEmpty() ? "" : " ") + "...");

        CompletableFuture.runAsync(() -> {
            try {
                // Initialize Vosk Model (Note: Ideally this shouldn't be loaded on the fly
                // every time, but for demonstration it works)
                String modelPath = "C:\\Users\\zaiem yousssef\\IdeaProjects\\WORKSHOP\\src\\main\\resources\\assest\\vosk-model-small\\vosk-model-small-en-us-0.15";
                Model model = new Model(modelPath);
                Recognizer recognizer = new Recognizer(model, 16000.0f);

                audioCapture = new AudioCapture();
                isDictating = true;

                Platform.runLater(() -> {
                    taContent.setText(taContent.getText().replace("...", "").trim()
                            + " [Listening... Click Dictate again to stop]");
                });

                audioCapture.start((data, numBytes) -> {
                    if (recognizer.acceptWaveForm(data, numBytes)) {
                        String result = recognizer.getResult();
                        JsonObject jsonObject = JsonParser.parseString(result).getAsJsonObject();
                        String text = jsonObject.get("text").getAsString();

                        if (!text.isEmpty()) {
                            Platform.runLater(() -> {
                                String currentText = taContent.getText()
                                        .replace("[Listening... Click Dictate again to stop]", "").trim();
                                taContent.setText(currentText + (currentText.isEmpty() ? "" : " ") + text
                                        + " [Listening... Click Dictate again to stop]");
                            });
                        }
                    }
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    isDictating = false;
                    taContent.setText(taContent.getText().replace("[Listening... Click Dictate again to stop]", ""));
                    showAlert("Microphone Error", "Could not start microphone or load model. " + e.getMessage());
                });
            }
        });
    }

    @FXML
    private void onRephrase() {
        String content = taContent.getText().trim();
        if (content.isEmpty()) {
            showAlert("Rephrase", "Please enter some text to rephrase first.");
            return;
        }

        taContent.setText("⏳ Rephrasing content... Please wait.");

        CompletableFuture.runAsync(() -> {
            try {
                // Formatting prompt for the free AI endpoint
                String prompt = "Please rewrite the following text professionally and correct all grammar errors. Only return the corrected text, without any conversational filler or introductions: "
                        + content;
                String encodedPrompt = URLEncoder.encode(prompt, StandardCharsets.UTF_8.toString());
                String apiUrl = "https://text.pollinations.ai/prompt/" + encodedPrompt;

                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(apiUrl))
                        .GET()
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                String finalRephrased;
                if (response.statusCode() == 200 && response.body() != null && !response.body().isEmpty()) {
                    finalRephrased = response.body().trim();
                    // Remove quotes if the AI wraps the response in them
                    if (finalRephrased.startsWith("\"") && finalRephrased.endsWith("\"")) {
                        finalRephrased = finalRephrased.substring(1, finalRephrased.length() - 1);
                    }
                } else {
                    // Fallback formatting if the API fails
                    finalRephrased = content.substring(0, 1).toUpperCase() + content.substring(1);
                    if (!finalRephrased.endsWith(".") && !finalRephrased.endsWith("!")
                            && !finalRephrased.endsWith("?")) {
                        finalRephrased += ".";
                    }
                }

                String resultToDisplay = finalRephrased;
                Platform.runLater(() -> {
                    taContent.setText(resultToDisplay);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    taContent.setText(content);
                    showAlert("API Error", "Could not reach the rephrasing AI. Please check your internet connection.");
                });
            }
        });
    }

    private void close() {
        ((Stage) tfTitle.getScene().getWindow()).close();
    }

    private void showAlert(String title, String content) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
