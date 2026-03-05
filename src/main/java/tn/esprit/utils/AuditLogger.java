package tn.esprit.utils;

import java.io.File;
import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class AuditLogger {
    private static final Logger LOGGER = Logger.getLogger("MentorshipAuditLogger");
    private static FileHandler fileHandler;

    static {
        try {
            File logsDir = new File("logs");
            if (!logsDir.exists()) {
                logsDir.mkdirs();
            }
            // Append mode, 10MB limit, 3 files
            fileHandler = new FileHandler("logs/audit.log", 1024 * 1024 * 10, 3, true);
            fileHandler.setFormatter(new SimpleFormatter());
            LOGGER.addHandler(fileHandler);
            LOGGER.setLevel(Level.ALL);
        } catch (IOException e) {
            System.err.println("Failed to initialize AuditLogger: " + e.getMessage());
        }
    }

    public static void log(String message) {
        String user = SessionManager.getUser() != null ? SessionManager.getUser().getFullName() : "System";
        LOGGER.info(String.format("User: %s | Action: %s", user, message));
    }

    public static void logWarning(String message) {
        String user = SessionManager.getUser() != null ? SessionManager.getUser().getFullName() : "System";
        LOGGER.warning(String.format("User: %s | Warning: %s", user, message));
    }
}
