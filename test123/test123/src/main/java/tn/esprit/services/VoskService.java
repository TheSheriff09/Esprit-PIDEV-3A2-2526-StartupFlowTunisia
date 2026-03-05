package tn.esprit.services;

import javafx.application.Platform;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.function.Consumer;

/**
 * Windows-native Speech-to-Text using Windows SAPI via PowerShell.
 * No external libraries, no native DLLs, no Maven dependencies required.
 * Works on every Windows 10/11 machine out of the box.
 */
public class VoskService {

    private static final String SCRIPT_PATH = "speech_listen.ps1";

    private Process speechProcess;
    private Thread readerThread;
    private boolean isListening = false;

    public VoskService() {
        // Nothing to init — Windows SAPI is always available
    }

    public boolean isReady() {
        // Always ready on Windows — no model or JAR needed
        File script = new File(SCRIPT_PATH);
        return script.exists();
    }

    public void startListening(Consumer<String> onResult) {
        if (isListening)
            return;

        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "powershell",
                    "-ExecutionPolicy", "Bypass",
                    "-File", new File(SCRIPT_PATH).getAbsolutePath());
            pb.redirectErrorStream(false);
            speechProcess = pb.start();
            isListening = true;

            readerThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(speechProcess.getInputStream()))) {
                    String line;
                    while (isListening && (line = reader.readLine()) != null) {
                        String text = line.trim();
                        if (!text.isEmpty()) {
                            final String out = text;
                            Platform.runLater(() -> onResult.accept(out + " "));
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Speech reader: " + e.getMessage());
                }
            });
            readerThread.setDaemon(true);
            readerThread.start();
            System.out.println("✅ Windows Speech Recognition started.");
        } catch (Exception e) {
            System.err.println("Failed to start speech process: " + e.getMessage());
            isListening = false;
        }
    }

    public void stopListening() {
        isListening = false;
        if (speechProcess != null) {
            speechProcess.destroy();
            speechProcess = null;
        }
        if (readerThread != null) {
            readerThread.interrupt();
            readerThread = null;
        }
        System.out.println("🛑 Windows Speech Recognition stopped.");
    }
}
