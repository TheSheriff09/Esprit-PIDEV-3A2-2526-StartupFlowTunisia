package tn.esprit.utils;

import javafx.scene.Parent;
import javafx.scene.Scene;

import java.io.*;
import java.util.Properties;

// NEW FILE
public class ThemeManager {
    private static ThemeManager instance;
    private boolean isDarkMode;
    private final String CONFIG_FILE = "config.properties";

    private ThemeManager() {
        loadConfig();
    }

    public static ThemeManager getInstance() {
        if (instance == null) {
            instance = new ThemeManager();
        }
        return instance;
    }

    private void loadConfig() {
        Properties props = new Properties();
        try (InputStream in = new FileInputStream(CONFIG_FILE)) {
            props.load(in);
            isDarkMode = Boolean.parseBoolean(props.getProperty("dark.mode", "false"));
        } catch (IOException e) {
            isDarkMode = false;
        }
    }

    private void saveConfig() {
        Properties props = new Properties();
        props.setProperty("dark.mode", String.valueOf(isDarkMode));
        try (OutputStream out = new FileOutputStream(CONFIG_FILE)) {
            props.store(out, null);
        } catch (IOException e) {
            System.err.println("Could not save theme config.");
        }
    }

    public boolean isDarkMode() {
        return isDarkMode;
    }

    public void setDarkMode(boolean darkMode) {
        this.isDarkMode = darkMode;
        saveConfig();
    }

    public void toggleTheme() {
        setDarkMode(!isDarkMode);
    }

    public void applyTheme(Scene scene) {
        if (scene == null)
            return;
        String css = null;
        try {
            if (getClass().getResource("/css/dark-theme.css") != null) {
                css = getClass().getResource("/css/dark-theme.css").toExternalForm();
            }
        } catch (Exception e) {
        }

        if (css != null) {
            if (isDarkMode) {
                if (!scene.getStylesheets().contains(css)) {
                    scene.getStylesheets().add(css);
                }
            } else {
                scene.getStylesheets().remove(css);
            }
        }
    }

    public void applyTheme(Parent parent) {
        if (parent == null)
            return;
        String css = null;
        try {
            if (getClass().getResource("/css/dark-theme.css") != null) {
                css = getClass().getResource("/css/dark-theme.css").toExternalForm();
            }
        } catch (Exception e) {
        }

        if (css != null) {
            if (isDarkMode) {
                if (!parent.getStylesheets().contains(css)) {
                    parent.getStylesheets().add(css);
                }
            } else {
                parent.getStylesheets().remove(css);
            }
        }
    }
}
