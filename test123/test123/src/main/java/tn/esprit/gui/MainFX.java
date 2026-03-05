package tn.esprit.gui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainFX extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        tn.esprit.utils.PluginManager.getInstance().loadPlugins();
        Parent root = FXMLLoader.load(getClass().getResource("/fxml/LoginView.fxml"));
        primaryStage.setTitle("StartupFlow Tunisia — Login");
        primaryStage.setScene(new Scene(root, 1280, 720));
        primaryStage.setResizable(true);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void stop() throws Exception {
        tn.esprit.utils.PluginManager.getInstance().unloadPlugins();
        tn.esprit.utils.BackupService.stopAutoBackup();
        super.stop();
    }
}
