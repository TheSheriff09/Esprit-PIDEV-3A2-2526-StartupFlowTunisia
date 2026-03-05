package tn.esprit.GUI.startup;

import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.input.MouseEvent;
import javafx.event.ActionEvent;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import tn.esprit.entities.Startup;
import tn.esprit.GUI.businessplan.BusinessPlanViewController;
import tn.esprit.GUI.popup.DialogStyler;
import tn.esprit.GUI.popup.CurrencyController;
import tn.esprit.GUI.popup.FinancialForecastController;
import tn.esprit.GUI.popup.FundingSimulationController;
import tn.esprit.GUI.popup.PopupManager;
import tn.esprit.Services.QRCodeService;
import tn.esprit.Services.StartupPDFExporter;
import tn.esprit.Services.StartupService;
import tn.esprit.Services.BusinessPlanService;
import tn.esprit.Services.InvestmentScorer;
import tn.esprit.Services.StartupWebServer;
import tn.esprit.utils.AlertUtil;
import tn.esprit.utils.DesignTokens;
import tn.esprit.utils.FormValidator;
import tn.esprit.utils.ThemeManager;
import tn.esprit.utils.ValidationUtil;
import tn.esprit.utils.SessionManager;
import tn.esprit.utils.NavigationManager;
import tn.esprit.utils.TopBarHelper;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class StartupViewController implements Initializable {

    // ── FXML nodes ────────────────────────────────────────────
    @FXML
    private BorderPane mainContent;
    @FXML
    private StackPane modalLayer;
    @FXML
    private FlowPane cardsContainer;
    @FXML
    private TextField searchField;
    @FXML
    private Label lblCount;
    @FXML
    private Button fabBtn;
    @FXML
    private Button themeToggle;
    @FXML
    private ComboBox<String> sortCombo;
    @FXML
    private Button btnAdmin;
    @FXML
    private MenuButton userMenuBtn;

    // ── Services ──────────────────────────────────────────────
    private final StartupService service = new StartupService();

    // ── State ─────────────────────────────────────────────────
    private List<Startup> allStartups;

    // ── Init ──────────────────────────────────────────────────

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // ── Session guard ──
        if (!SessionManager.requireLogin(cardsContainer))
            return;

        // ── TopBar Setup ──
        if (userMenuBtn != null) {
            TopBarHelper.setup(userMenuBtn, null, userMenuBtn);
        }
        if (mainContent != null) {
            mainContent.getStylesheets()
                    .add(getClass().getResource("/css/entrepreneur_dashboard.css").toExternalForm());
        }

        if (btnAdmin != null) {
            btnAdmin.setVisible(SessionManager.isAdmin());
            btnAdmin.setManaged(SessionManager.isAdmin());
        }

        searchField.textProperty().addListener((obs, oldVal, newVal) -> filterCards(newVal));
        // Populate sort options
        if (sortCombo != null) {
            sortCombo.getItems().addAll(
                    "Name (A → Z)", "Name (Z → A)",
                    "Funding (High → Low)", "Funding (Low → High)",
                    "Score (High → Low)", "Score (Low → High)",
                    "Date (Newest)", "Date (Oldest)");
        }
        loadAll();
        // Set initial theme toggle icon
        if (themeToggle != null)
            themeToggle.setText(ThemeManager.getInstance().isDark() ? "☀" : "🌙");
        // Start FAB pulse animation after scene is ready
        javafx.application.Platform.runLater(this::breatheFAB);
    }

    /** Subtle idle "breathing" glow on the FAB. */
    private void breatheFAB() {
        if (fabBtn == null)
            return;
        Timeline pulse = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(fabBtn.scaleXProperty(), 1.0),
                        new KeyValue(fabBtn.scaleYProperty(), 1.0)),
                new KeyFrame(Duration.millis(1100),
                        new KeyValue(fabBtn.scaleXProperty(), 1.07),
                        new KeyValue(fabBtn.scaleYProperty(), 1.07)),
                new KeyFrame(Duration.millis(2200),
                        new KeyValue(fabBtn.scaleXProperty(), 1.0),
                        new KeyValue(fabBtn.scaleYProperty(), 1.0)));
        pulse.setCycleCount(Timeline.INDEFINITE);
        pulse.play();
    }

    @FXML
    private void toggleTheme() {
        ThemeManager tm = ThemeManager.getInstance();
        tm.toggle();
        tm.applyTo(themeToggle.getScene());
        themeToggle.setText(tm.isDark() ? "☀" : "🌙");
        // Re-render cards so inline styles update
        if (allStartups != null)
            renderCards(allStartups);
    }

    // ── Navigation ────────────────────────────────────────────

    @FXML
    private void goDashboard(MouseEvent e) {
        NavigationManager.goToDashboard(cardsContainer);
    }

    @FXML
    private void goStartup(MouseEvent e) {
    }

    @FXML
    private void goMentorship(MouseEvent e) {
        NavigationManager.navigateTo(cardsContainer, "/Mentorship.fxml");
    }

    @FXML
    private void goFunding(MouseEvent e) {
        NavigationManager.navigateTo(cardsContainer, "/application.fxml");
    }

    @FXML
    private void goForum(MouseEvent e) {
        NavigationManager.navigateTo(cardsContainer, "/ForumFeed.fxml");
    }

    @FXML
    private void onManageProfile(javafx.event.ActionEvent e) {
        NavigationManager.navigateTo(cardsContainer, "/ManageProfile.fxml");
    }

    @FXML
    private void onStartups(javafx.event.ActionEvent e) {
        NavigationManager.navigateTo(cardsContainer, "/startupview.fxml");
    }

    @FXML
    private void onSettings(javafx.event.ActionEvent e) {
    }

    @FXML
    private void onLogout(javafx.event.ActionEvent e) {
        NavigationManager.logout(cardsContainer);
    }

    // ── Data ──────────────────────────────────────────────────

    private void loadAll() {
        allStartups = service.list().stream()
                .sorted(Comparator.comparingInt(Startup::getStartupID).reversed())
                .collect(Collectors.toList());
        renderCards(allStartups);
    }

    private void filterCards(String query) {
        if (allStartups == null || allStartups.isEmpty()) {
            renderCards(List.of());
            return;
        }
        if (query == null || query.isBlank()) {
            renderCards(allStartups);
            return;
        }
        String q = query.toLowerCase().trim();
        List<Startup> filtered = allStartups.stream()
                .filter(s -> (s.getName() != null && s.getName().toLowerCase().contains(q)) ||
                        (s.getSector() != null && s.getSector().toLowerCase().contains(q)) ||
                        (s.getDescription() != null && s.getDescription().toLowerCase().contains(q)) ||
                        (s.getStage() != null && s.getStage().toLowerCase().contains(q)))
                .collect(Collectors.toList());

        renderCards(filtered);
    }

    // ── Card rendering ────────────────────────────────────────

    private void renderCards(List<Startup> startups) {
        cardsContainer.getChildren().clear();
        lblCount.setText(startups.size() + " startup" + (startups.size() != 1 ? "s" : ""));

        if (startups.isEmpty()) {
            Label empty = new Label("No startups found. Click + to add your first startup.");
            empty.getStyleClass().add("activity-meta");
            cardsContainer.getChildren().add(empty);
            return;
        }

        for (int i = 0; i < startups.size(); i++) {
            Startup s = startups.get(i);
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/startupcard.fxml"));
                Parent card = loader.load();
                StartupCardController ctrl = loader.getController();

                ctrl.setData(
                        s,
                        this::openEditDialog,
                        this::confirmDelete,
                        this::navigateToBusinessPlans,
                        this::openFundingSimulation,
                        this::handleCardQR,
                        this::handleCardExport,
                        this::navigateToFunding);

                animateIn(card, i * 55L);
                cardsContainer.getChildren().add(card);

            } catch (IOException e) {
                System.err.println("[StartupViewController] Card load error: " + e.getMessage());
            }
        }
    }

    private void animateIn(Node node, long delayMs) {
        node.setOpacity(0);
        node.setScaleX(0.88);
        node.setScaleY(0.88);
        node.setTranslateY(18);

        FadeTransition ft = new FadeTransition(Duration.millis(420), node);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.setDelay(Duration.millis(delayMs));

        ScaleTransition st = new ScaleTransition(Duration.millis(420), node);
        st.setFromX(0.88);
        st.setToX(1.0);
        st.setFromY(0.88);
        st.setToY(1.0);
        st.setDelay(Duration.millis(delayMs));

        TranslateTransition tt = new TranslateTransition(Duration.millis(420), node);
        tt.setFromY(18);
        tt.setToY(0);
        tt.setDelay(Duration.millis(delayMs));

        ParallelTransition pt = new ParallelTransition(ft, st, tt);
        pt.play();
    }

    // ── FAB: open modern popup overlay ──────────────────────────

    @FXML
    private void openAddDialog() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/addstartupdialog.fxml"));
            Node overlay = loader.load();
            AddStartupController ctrl = loader.getController();

            // Wire save → persist + success alert + refresh
            ctrl.setOnSave(startup -> {
                service.add(startup);
                loadAll();
                javafx.application.Platform.runLater(() -> {
                    AlertUtil.showSuccess("\u2705  Startup Created",
                            "\"" + startup.getName() + "\" has been added successfully.",
                            cardsContainer.getScene().getWindow());
                });
            });

            // Wire close → remove overlay, restore blur
            ctrl.setOnClose(() -> {
                modalLayer.getChildren().clear();
                modalLayer.setVisible(false);
                modalLayer.setManaged(false);
                if (mainContent != null)
                    mainContent.setEffect(null);
            });

            // Inject overlay, blur background, animate in
            modalLayer.getChildren().setAll(overlay);
            modalLayer.setVisible(true);
            modalLayer.setManaged(true);
            if (mainContent != null)
                mainContent.setEffect(new GaussianBlur(7));
            ctrl.animateOpen();

        } catch (IOException e) {
            showError("Cannot open Add Startup dialog: " + e.getMessage());
        }
    }

    // ── Callbacks from cards ──────────────────────────────────

    private void openEditDialog(Startup s) {
        openStartupDialog(s);
    }

    private void confirmDelete(Startup s) {
        PopupManager.showDelete(
                modalLayer, mainContent,
                "Delete Startup",
                "Delete \"" + s.getName() + "\"? All linked business plans will also be deleted.",
                () -> {
                    service.delete(s);
                    loadAll();
                    AlertUtil.showSuccess("🗑  Startup Deleted",
                            "\"" + s.getName() + "\" has been removed.",
                            cardsContainer.getScene().getWindow());
                });
    }

    private void navigateToBusinessPlans(Startup s) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/businessplanview.fxml"));
            Parent root = loader.load();
            BusinessPlanViewController ctrl = loader.getController();
            ctrl.initWithStartup(s);

            Stage stage = (Stage) cardsContainer.getScene().getWindow();
            stage.setTitle("Business Plans — " + s.getName());
            Scene sc = new Scene(root, stage.getScene().getWidth(), stage.getScene().getHeight());
            ThemeManager.getInstance().applyTo(sc);
            stage.setScene(sc);
        } catch (IOException e) {
            showError("Cannot open Business Plans: " + e.getMessage());
        }
    }

    /** Opens the Funding Simulation popup for the given startup. */
    private void openFundingSimulation(Startup s) {
        FundingSimulationController.show(modalLayer, mainContent);
    }

    /** Navigates to the full detail page for a startup. */
    private void navigateToDetail(Startup s) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/startupdetail.fxml"));
            Parent root = loader.load();
            StartupDetailController ctrl = loader.getController();
            ctrl.initWithStartup(s);

            Stage stage = (Stage) cardsContainer.getScene().getWindow();
            stage.setTitle("Startup Detail — " + s.getName());
            Scene sc = new Scene(root, stage.getScene().getWidth(), stage.getScene().getHeight());
            ThemeManager.getInstance().applyTo(sc);
            stage.setScene(sc);
        } catch (IOException e) {
            showError("Cannot open Startup Detail: " + e.getMessage());
        }
    }

    /** Navigates to the funding application view directly for this startup. */
    private void navigateToFunding(Startup s) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/application.fxml"));
            Parent root = loader.load();

            tn.esprit.GUI.ApplicationController ctrl = loader.getController();
            int userId = SessionManager.getUser() != null ? SessionManager.getUser().getId() : -1;
            ctrl.setInitialData(userId, s.getStartupID());

            Stage stage = (Stage) cardsContainer.getScene().getWindow();
            stage.setTitle("Funding Applications — " + s.getName());
            Scene sc = new Scene(root, stage.getScene().getWidth(), stage.getScene().getHeight());
            ThemeManager.getInstance().applyTo(sc);
            stage.setScene(sc);
        } catch (IOException e) {
            showError("Cannot open Funding Application: " + e.getMessage());
        }
    }

    /** Shows a QR code dialog for a startup directly from the card. */
    private void handleCardQR(Startup s) {
        // Load plans and score, then start web server so phone camera opens a browser
        // page
        BusinessPlanService bpSvc = new BusinessPlanService();
        java.util.List<tn.esprit.entities.BusinessPlan> plans = bpSvc.getByStartup(s.getStartupID());
        double score = service.calculateInvestmentScore(s.getStartupID());

        StartupWebServer webServer = new StartupWebServer(s, plans, score);
        try {
            webServer.start();
        } catch (java.io.IOException ioEx) {
            showError("Could not start QR web server: " + ioEx.getMessage());
            return;
        }

        String qrUrl = webServer.getUrl();

        try {
            javafx.scene.image.Image qrImg = QRCodeService.generateQRImageFromUrl(qrUrl, 250);
            javafx.scene.image.ImageView iv = new javafx.scene.image.ImageView(qrImg);
            iv.setFitWidth(250);
            iv.setFitHeight(250);

            javafx.scene.control.Label lblHint = new javafx.scene.control.Label(
                    "📱  Scan — opens full details in browser");
            lblHint.setStyle("-fx-font-size:11px;-fx-text-fill:#6d28d9;-fx-font-weight:700;"
                    + "-fx-padding:4 12;-fx-background-color:rgba(109,40,217,0.09);"
                    + "-fx-background-radius:10;");

            javafx.scene.control.Label lblUrl = new javafx.scene.control.Label(qrUrl);
            lblUrl.setStyle("-fx-font-size:10px;-fx-text-fill:#a78bfa;-fx-font-family:monospace;");
            lblUrl.setWrapText(true);
            lblUrl.setMaxWidth(300);

            javafx.scene.control.Dialog<Void> dlg = new javafx.scene.control.Dialog<>();
            dlg.setTitle("QR Code — " + s.getName());
            dlg.setHeaderText(null);

            javafx.scene.layout.VBox box = new javafx.scene.layout.VBox(12, iv, lblHint, lblUrl);
            box.setAlignment(javafx.geometry.Pos.CENTER);
            box.setPadding(new javafx.geometry.Insets(16));

            Button saveBtn = new Button("💾  Save PNG");
            saveBtn.getStyleClass().add("button-primary");
            saveBtn.setOnAction(evt -> {
                FileChooser fc = new FileChooser();
                fc.setTitle("Save QR Code");
                fc.setInitialFileName(s.getName().replaceAll("\\s+", "_") + "_QR.png");
                fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG", "*.png"));
                File file = fc.showSaveDialog(dlg.getDialogPane().getScene().getWindow());
                if (file != null) {
                    try {
                        QRCodeService.saveQRToPNGFromUrl(qrUrl, 400, file);
                        AlertUtil.showSuccess("QR code saved to " + file.getName(),
                                dlg.getDialogPane().getScene().getWindow());
                    } catch (Exception ex) {
                        showError("Could not save QR: " + ex.getMessage());
                    }
                }
            });
            box.getChildren().add(saveBtn);

            dlg.getDialogPane().setContent(box);
            dlg.getDialogPane().getButtonTypes().add(javafx.scene.control.ButtonType.CLOSE);
            dlg.getDialogPane().getStylesheets().addAll(cardsContainer.getScene().getStylesheets());
            dlg.initOwner(cardsContainer.getScene().getWindow());
            dlg.setOnHidden(e -> webServer.stop());
            dlg.showAndWait();
        } catch (Exception e) {
            webServer.stop();
            showError("QR generation failed: " + e.getMessage());
        }
    }

    /** Exports a startup to PDF directly from the card. */
    private void handleCardExport(Startup s) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Export Startup PDF");
        fc.setInitialFileName(s.getName().replaceAll("\\s+", "_") + "_Report.pdf");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        File file = fc.showSaveDialog(cardsContainer.getScene().getWindow());
        if (file == null)
            return;
        try {
            BusinessPlanService bpService = new BusinessPlanService();
            java.util.List<tn.esprit.entities.BusinessPlan> plans = bpService.getByStartup(s.getStartupID());
            double score = service.calculateInvestmentScore(s.getStartupID());
            StartupPDFExporter.exportStartupToPDF(s, plans, score, file);
            AlertUtil.showSuccess("PDF exported to " + file.getName(),
                    cardsContainer.getScene().getWindow());
        } catch (Exception e) {
            showError("PDF export failed: " + e.getMessage());
        }
    }

    // ── Sort handler ──────────────────────────────────────────

    @FXML
    private void onSortChanged() {
        if (sortCombo == null || sortCombo.getValue() == null || allStartups == null)
            return;
        String sort = sortCombo.getValue();
        Comparator<Startup> cmp = switch (sort) {
            case "Name (A → Z)" -> Comparator.comparing(s -> s.getName() != null ? s.getName().toLowerCase() : "");
            case "Name (Z → A)" ->
                Comparator.comparing((Startup s) -> s.getName() != null ? s.getName().toLowerCase() : "").reversed();
            case "Funding (High → Low)" ->
                Comparator.comparing((Startup s) -> s.getFundingAmount() != null ? s.getFundingAmount() : 0.0,
                        Comparator.reverseOrder());
            case "Funding (Low → High)" ->
                Comparator.comparing((Startup s) -> s.getFundingAmount() != null ? s.getFundingAmount() : 0.0);
            case "Score (High → Low)" -> Comparator.comparing(
                    (Startup s) -> service.calculateInvestmentScore(s.getStartupID()), Comparator.reverseOrder());
            case "Score (Low → High)" ->
                Comparator.comparing((Startup s) -> service.calculateInvestmentScore(s.getStartupID()));
            case "Date (Newest)" -> Comparator.comparing(
                    (Startup s) -> s.getCreationDate() != null ? s.getCreationDate() : java.time.LocalDate.MIN,
                    Comparator.reverseOrder());
            case "Date (Oldest)" -> Comparator.comparing(
                    (Startup s) -> s.getCreationDate() != null ? s.getCreationDate() : java.time.LocalDate.MIN);
            default -> null;
        };
        if (cmp != null) {
            allStartups.sort(cmp);
            renderCards(allStartups);
        }
    }

    // ── QR scan handler ───────────────────────────────────────

    @FXML
    private void handleScanQR() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Select QR Code Image");
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images (PNG, JPG)", "*.png", "*.jpg", "*.jpeg"));
        File file = fc.showOpenDialog(cardsContainer.getScene().getWindow());
        if (file == null)
            return;

        try {
            int startupID = QRCodeService.decodeStartupID(file);
            if (startupID <= 0) {
                AlertUtil.showValidationErrors(
                        List.of("Invalid QR code. Not a StartupFlow QR."),
                        cardsContainer.getScene().getWindow());
                return;
            }
            Startup s = service.getById(startupID);
            if (s == null) {
                AlertUtil.showValidationErrors(
                        List.of("Startup with ID " + startupID + " not found in database."),
                        cardsContainer.getScene().getWindow());
                return;
            }
            navigateToDetail(s);
        } catch (Exception e) {
            AlertUtil.showValidationErrors(
                    List.of("Could not read QR image: " + e.getMessage()),
                    cardsContainer.getScene().getWindow());
        }
    }

    /** Opens the AI Financial Forecast popup from the navbar button. */
    @FXML
    private void openFinancialForecast() {
        FinancialForecastController.show(modalLayer, mainContent);
    }

    /** Opens the Currency Exchange popup from the navbar button. */
    @FXML
    private void openCurrencyConverter() {
        CurrencyController.show(modalLayer, mainContent);
    }

    // ── Add / Edit dialog (programmatic, no extra FXML needed) ─

    private void openStartupDialog(Startup existing) {
        boolean isEdit = existing != null;

        // ── Form fields ──
        TextField tfName = styledField("e.g. EcoTech Solutions");
        TextField tfSector = styledField("e.g. FinTech / HealthTech");
        TextArea taDesc = styledArea("Brief description…");
        DatePicker dpCreation = styledDatePicker();
        TextField tfStage = styledField("e.g. Seed / Growth / Mature");
        ComboBox<String> cbStatus = styledCombo("Active", "Inactive", "Under Review", "Approved", "Scaling");
        TextField tfFunding = styledField("e.g. 50000");
        TextField tfIncubator = styledField("e.g. StartupTN");

        // ── Error labels ──
        Label errName = FormValidator.errorLabel();
        Label errFunding = FormValidator.errorLabel();

        // ── Real-time validation ──
        FormValidator.clearOnType(tfName, errName);
        FormValidator.enforceLengthLimit(tfName, 100);
        tfFunding.setTextFormatter(ValidationUtil.unsignedDecimalFormatter());
        FormValidator.validateDoubleOnType(tfFunding, errFunding, false);
        FormValidator.enforceLengthLimit(tfIncubator, 120);

        // ── Image picker ──
        String[] imageHolder = { existing != null ? existing.getImageURL() : null };
        Label errImage = FormValidator.errorLabel();
        HBox imageRow = buildImagePicker(imageHolder, errImage, existing != null ? existing.getImageURL() : null);

        dpCreation.setValue(LocalDate.now());

        if (isEdit) {
            tfName.setText(existing.getName());
            tfSector.setText(existing.getSector());
            taDesc.setText(existing.getDescription());
            if (existing.getCreationDate() != null)
                dpCreation.setValue(existing.getCreationDate());
            tfStage.setText(existing.getStage());
            cbStatus.setValue(existing.getStatus() != null ? existing.getStatus() : "Active");
            if (existing.getFundingAmount() != null)
                tfFunding.setText(String.valueOf(existing.getFundingAmount()));
            tfIncubator.setText(existing.getIncubatorProgram());
        }

        // ── Layout ──
        Label secBasic = sectionHeader("Basic Info");
        Label secDetails = sectionHeader("Details");
        Label secFinance = sectionHeader("Finance & Programme");

        VBox form = new VBox(8);
        form.setPadding(new Insets(6, 4, 6, 4));
        form.getChildren().addAll(
                secBasic,
                FormValidator.fieldRow("Name *", tfName, errName),
                FormValidator.fieldRow("Sector", tfSector, null),
                FormValidator.fieldRow("Description", taDesc, null),
                new VBox(5, DialogStyler.fieldLabel("Image"), imageRow, errImage),
                secDetails,
                FormValidator.fieldRow("Creation Date", dpCreation, null),
                FormValidator.fieldRow("Stage", tfStage, null),
                FormValidator.fieldRow("Status", cbStatus, null),
                secFinance,
                FormValidator.fieldRow("Funding (TND)", tfFunding, errFunding),
                FormValidator.fieldRow("Incubator", tfIncubator, null));

        ScrollPane scroll = new ScrollPane(form);
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(460);
        scroll.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");

        // ── Dialog ──
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(isEdit ? "Edit Startup" : "New Startup");
        dialog.setHeaderText(isEdit ? "Edit \"" + existing.getName() + "\"" : "Create a new Startup");
        dialog.getDialogPane().setContent(scroll);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.getDialogPane().setPrefWidth(500);
        DialogStyler.style(dialog);

        // Block dialog close if validation fails — full ValidationUtil pipeline
        dialog.getDialogPane().lookupButton(ButtonType.OK)
                .addEventFilter(ActionEvent.ACTION, ev -> {
                    List<String> existingNames = allStartups.stream()
                            .map(Startup::getName).collect(Collectors.toList());
                    String excludeName = isEdit ? existing.getName() : null;
                    List<String> errors = ValidationUtil.gatherErrors(
                            ValidationUtil.checkName(tfName.getText(), "Name", existingNames, excludeName),
                            ValidationUtil.checkFunding(tfFunding.getText()));
                    // Image extension check
                    if (!FormValidator.requireImageExtension(imageHolder[0], errImage))
                        errors.add("Image must be PNG, JPG, or JPEG.");
                    // Visual highlights
                    ValidationUtil.checkName(tfName.getText(), "Name", existingNames, excludeName)
                            .ifPresentOrElse(
                                    msg -> ValidationUtil.markFieldError(tfName, errName, msg),
                                    () -> ValidationUtil.clearFieldError(tfName, errName));
                    ValidationUtil.checkFunding(tfFunding.getText())
                            .ifPresentOrElse(
                                    msg -> ValidationUtil.markFieldError(tfFunding, errFunding, msg),
                                    () -> ValidationUtil.clearFieldError(tfFunding, errFunding));
                    if (!AlertUtil.checkAndShowErrors(errors,
                            dialog.getDialogPane().getScene().getWindow()))
                        ev.consume();
                });

        Optional<ButtonType> res = dialog.showAndWait();
        if (res.isPresent() && res.get() == ButtonType.OK) {
            // Validation already guaranteed by event filter
            Double funding = null;
            String fText = tfFunding.getText().trim();
            if (!fText.isBlank())
                funding = Double.parseDouble(fText);

            if (isEdit) {
                existing.setName(tfName.getText().trim());
                existing.setSector(tfSector.getText().trim());
                existing.setDescription(taDesc.getText().trim());
                existing.setImageURL(imageHolder[0]);
                existing.setCreationDate(dpCreation.getValue());
                existing.setStage(tfStage.getText().trim());
                existing.setStatus(cbStatus.getValue());
                existing.setFundingAmount(funding);
                existing.setIncubatorProgram(tfIncubator.getText().trim());
                service.update(existing);
                AlertUtil.showSuccess("\u270F  Startup Updated",
                        "\"" + existing.getName() + "\" has been updated successfully.",
                        cardsContainer.getScene().getWindow());
            } else {
                Startup s = new Startup();
                s.setName(tfName.getText().trim());
                s.setSector(tfSector.getText().trim());
                s.setDescription(taDesc.getText().trim());
                s.setImageURL(imageHolder[0]);
                s.setCreationDate(dpCreation.getValue());
                s.setStage(tfStage.getText().trim());
                s.setStatus(cbStatus.getValue());
                s.setFundingAmount(funding);
                s.setIncubatorProgram(tfIncubator.getText().trim());
                if (SessionManager.getUser() != null) {
                    s.setUserId(SessionManager.getUser().getId());
                }
                service.add(s);
                AlertUtil.showSuccess("\u2705  Startup Created",
                        "\"" + s.getName() + "\" has been added successfully.",
                        cardsContainer.getScene().getWindow());
            }
            loadAll();
        }
    }

    // ── Form builder helpers ──────────────────────────────────

    /** Section divider — delegates to shared DialogStyler. */
    private Label sectionHeader(String text) {
        return DialogStyler.sectionLabel(text);
    }

    /**
     * Builds an image-picker row: thumbnail preview + Choose/Change button + path
     * label.
     * The selected path is written into {@code holder[0]} so the caller can read it
     * after OK.
     */
    private HBox buildImagePicker(String[] holder, Label errLbl, String existingPath) {
        // Thumbnail
        ImageView preview = new ImageView();
        preview.setFitWidth(72);
        preview.setFitHeight(72);
        preview.setPreserveRatio(true);

        Label placeholder = new Label("📷");
        placeholder.setStyle("-fx-font-size: 26px; -fx-text-fill: rgba(196,181,253,0.82);");

        StackPane thumb = new StackPane(preview, placeholder);
        thumb.setPrefSize(84, 84);
        thumb.setMinSize(84, 84);
        thumb.setMaxSize(84, 84);
        thumb.setStyle(
                ThemeManager.getInstance().isDark()
                        ? "-fx-background-color: rgba(255,255,255,0.05);" +
                                "-fx-border-color: rgba(139,92,246,0.40);" +
                                "-fx-border-radius: 14;" +
                                "-fx-background-radius: 14;" +
                                "-fx-border-width: 1.5;"
                        : "-fx-background-color: rgba(245,240,255,0.70);" +
                                "-fx-border-color: rgba(139,92,246,0.30);" +
                                "-fx-border-radius: 14;" +
                                "-fx-background-radius: 14;" +
                                "-fx-border-width: 1.5;");

        // Load existing image if available
        if (existingPath != null && !existingPath.isBlank()) {
            try {
                File f = new File(existingPath);
                if (f.exists()) {
                    preview.setImage(new Image(f.toURI().toString()));
                    placeholder.setVisible(false);
                }
            } catch (Exception ignored) {
            }
        }

        // Path label
        Label pathLabel = new Label(existingPath != null && !existingPath.isBlank()
                ? new File(existingPath).getName()
                : "No image selected");
        pathLabel.setStyle(
                ThemeManager.getInstance().isDark()
                        ? "-fx-font-size: 10.5px;-fx-text-fill: rgba(196,181,253,0.92);-fx-font-style: italic;"
                        : "-fx-font-size: 10.5px;-fx-text-fill: #4c1d95;-fx-font-style: italic;");
        pathLabel.setWrapText(true);

        // Choose button
        Button btnChoose = new Button("📁  Choose Image");
        btnChoose.setStyle(
                ThemeManager.getInstance().isDark()
                        ? "-fx-background-color: rgba(255,255,255,0.07);" +
                                "-fx-border-color: rgba(139,92,246,0.40);" +
                                "-fx-border-radius: 12;-fx-background-radius: 12;" +
                                "-fx-border-width: 1.5;-fx-text-fill: #a78bfa;" +
                                "-fx-font-size: 13px;-fx-font-weight: 800;" +
                                "-fx-padding: 9 18 9 18;-fx-cursor: hand;"
                        : "-fx-background-color: rgba(245,240,255,0.80);" +
                                "-fx-border-color: rgba(139,92,246,0.30);" +
                                "-fx-border-radius: 12;-fx-background-radius: 12;" +
                                "-fx-border-width: 1.5;-fx-text-fill: #6d28d9;" +
                                "-fx-font-size: 13px;-fx-font-weight: 800;" +
                                "-fx-padding: 9 18 9 18;-fx-cursor: hand;");
        btnChoose.setMaxWidth(Double.MAX_VALUE);
        btnChoose.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Choose Image");
            fc.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Images (PNG, JPG, JPEG)", "*.png", "*.jpg", "*.jpeg"));
            File chosen = fc.showOpenDialog(btnChoose.getScene().getWindow());
            if (chosen != null) {
                holder[0] = chosen.getAbsolutePath();
                pathLabel.setText(chosen.getName());
                preview.setImage(new Image(chosen.toURI().toString()));
                placeholder.setVisible(false);
                FormValidator.requireImageExtension(holder[0], errLbl);
            }
        });

        VBox right = new VBox(8, btnChoose, pathLabel);
        right.setFillWidth(true);
        HBox.setHgrow(right, Priority.ALWAYS);

        HBox row = new HBox(12, thumb, right);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private TextField styledField(String prompt) {
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        tf.setMaxWidth(Double.MAX_VALUE);
        tf.setStyle(DialogStyler.inputStyle());
        return tf;
    }

    private TextArea styledArea(String prompt) {
        TextArea ta = new TextArea();
        ta.setPromptText(prompt);
        ta.setPrefRowCount(3);
        ta.setWrapText(true);
        ta.setMaxWidth(Double.MAX_VALUE);
        ta.setStyle(DialogStyler.inputStyle());
        return ta;
    }

    private DatePicker styledDatePicker() {
        DatePicker dp = new DatePicker();
        dp.setMaxWidth(Double.MAX_VALUE);
        dp.setStyle(DesignTokens.comboNormal());
        return dp;
    }

    private ComboBox<String> styledCombo(String... items) {
        ComboBox<String> cb = new ComboBox<>();
        cb.getItems().addAll(items);
        cb.setValue(items[0]);
        cb.setMaxWidth(Double.MAX_VALUE);
        cb.setStyle(DesignTokens.comboNormal());
        return cb;
    }

    // ── Admin Dashboard entry point ───────────────────────────

    @FXML
    private void openAdminDashboard() {
        try {
            Stage stage = (Stage) cardsContainer.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/admindashboard.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root, stage.getScene().getWidth(),
                    stage.getScene().getHeight());
            ThemeManager.getInstance().applyTo(scene);
            stage.setScene(scene);
            stage.setTitle("StartupFlow — Admin Dashboard");
        } catch (IOException e) {
            showError("Cannot open Admin Dashboard: " + e.getMessage());
        }
    }

    /** Navigate back to the role-appropriate dashboard. */
    @FXML
    private void goBackToDashboard() {
        NavigationManager.goToDashboard(cardsContainer);
    }

    private void showError(String msg) {
        PopupManager.showError(modalLayer, mainContent, msg);
    }
}
