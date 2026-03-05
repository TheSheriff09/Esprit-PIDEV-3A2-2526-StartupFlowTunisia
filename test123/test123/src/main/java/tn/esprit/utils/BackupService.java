package tn.esprit.utils;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class BackupService {

    private static final String BACKUP_DIR = "backups";
    private static ScheduledExecutorService scheduler;

    public static void startAutoBackup() {
        if (scheduler != null && !scheduler.isShutdown()) {
            return;
        }

        File dir = new File(BACKUP_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            return t;
        });

        // Backup every 12 hours, starting in 12 hours (delay=12) to avoid startup
        // errors
        scheduler.scheduleAtFixedRate(BackupService::performBackup, 12, 12, TimeUnit.HOURS);
    }

    public static void performBackup() {
        try {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
            String backupFileName = "startupflow_backup_" + timestamp + ".sql";
            File backupFile = new File(BACKUP_DIR, backupFileName);

            // Using mysqldump for MySQL database
            String command = String.format("mysqldump -u %s startupflow_sarah -r %s", "root",
                    backupFile.getAbsolutePath());
            Process process = Runtime.getRuntime().exec(command);
            int exitCode = process.waitFor();

            if (exitCode == 0) {
                AuditLogger.log("Database backup completed successfully: " + backupFileName);
            } else {
                AuditLogger.logWarning("Database backup mysqldump failed with exit code: " + exitCode);
            }

            cleanupOldBackups();
        } catch (java.io.IOException e) {
            AuditLogger.logWarning("Database backup skipped. 'mysqldump' not found in system PATH.");
        } catch (Exception e) {
            AuditLogger.logWarning("Error during database backup: " + e.getMessage());
        }
    }

    private static void cleanupOldBackups() {
        File dir = new File(BACKUP_DIR);
        File[] files = dir.listFiles((d, name) -> name.startsWith("startupflow_backup_") && name.endsWith(".sql"));
        if (files != null && files.length > 5) { // Keep last 5 backups
            java.util.Arrays.sort(files, java.util.Comparator.comparingLong(File::lastModified));
            for (int i = 0; i < files.length - 5; i++) {
                if (files[i].delete()) {
                    AuditLogger.log("Deleted old backup: " + files[i].getName());
                }
            }
        }
    }

    public static void stopAutoBackup() {
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
    }
}
