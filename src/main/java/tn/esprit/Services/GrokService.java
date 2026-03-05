package tn.esprit.Services;

import tn.esprit.utils.AuditLogger;

import java.io.*;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;

/**
 * GrokService — Async Grok AI API integration.
 *
 * <p>
 * Design decisions:
 * <ul>
 * <li>Singleton pattern: one HttpClient is reused for all calls (efficient,
 * avoids resource leaks).</li>
 * <li>CompletableFuture: all calls are non-blocking — the JavaFX UI thread is
 * never frozen.</li>
 * <li>API key stored in config.properties (gitignored) — never hard-coded in
 * source.</li>
 * <li>Graceful degradation: on any error, a helpful fallback string is returned
 * so the UI still works.</li>
 * </ul>
 */
public class GrokService {

    // ── Singleton ──────────────────────────────────────────────────────────
    private static GrokService instance;

    public static synchronized GrokService getInstance() {
        if (instance == null)
            instance = new GrokService();
        return instance;
    }

    // ── Fields ─────────────────────────────────────────────────────────────
    private final HttpClient httpClient;
    private final String apiKey;
    private final String model;
    private final String apiUrl;

    private GrokService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();

        Properties props = loadConfig();
        this.apiKey = props.getProperty("grok.api.key", "");
        this.model = props.getProperty("grok.model", "grok-4-latest");
        this.apiUrl = props.getProperty("grok.api.url", "https://api.x.ai/v1/chat/completions");
    }

    // ── Public API ─────────────────────────────────────────────────────────

    /**
     * Summarize mentor feedback text asynchronously.
     * Business need: Mentors/Entrepreneurs want a concise AI summary of long
     * feedback reports.
     */
    public CompletableFuture<String> summarizeFeedbackAsync(String feedbackText) {
        String prompt = "You are an expert business analyst for a mentorship platform. "
                + "Please provide a concise, professional 3-sentence summary of the following "
                + "mentor feedback, highlighting key strengths, main weaknesses, and top recommendation:\n\n"
                + feedbackText;
        return callGrokAsync("You are a professional feedback summarizer for a startup mentorship platform.", prompt);
    }

    /**
     * Generate personalized mentorship advice asynchronously.
     * Business need: Entrepreneurs need actionable, AI-driven guidance based on
     * their session context.
     */
    public CompletableFuture<String> generateMentorshipAdviceAsync(String SessionManager) {
        String prompt = "You are a senior startup advisor. Based on the following mentorship session context, "
                + "provide 3 specific, actionable pieces of advice the entrepreneur should focus on next:\n\n"
                + SessionManager;
        return callGrokAsync("You are a startup mentorship advisor.", prompt);
    }

    /**
     * Explain why a mentor is recommended asynchronously.
     * Business need: Entrepreneurs want to understand WHY an AI recommends a
     * specific mentor.
     */
    public CompletableFuture<String> explainRecommendationAsync(String mentorName, String expertise,
            double performanceIndex) {
        String prompt = String.format(
                "You are an AI recommendation engine for a mentorship platform. "
                        + "Explain in 2–3 sentences why %s (expertise: %s, performance index: %.1f/100) "
                        + "is an excellent mentor recommendation. Be specific, professional, and encouraging.",
                mentorName, expertise, performanceIndex);
        return callGrokAsync("You are a professional mentorship recommendation system.", prompt);
    }

    /**
     * Analyze sentiment of feedback text and return a detailed label.
     * Business need: Supplement local sentiment analysis with AI-grade
     * understanding.
     */
    public CompletableFuture<String> analyzeSentimentAsync(String text) {
        String prompt = "Analyze the sentiment of the following text and respond with exactly one word: "
                + "Positive, Negative, or Neutral. Text:\n" + text;
        return callGrokAsync("You are a sentiment analysis expert.", prompt);
    }

    // ── Private HTTP Logic ─────────────────────────────────────────────────

    /**
     * Core async HTTP call to Grok API. Returns a CompletableFuture<String>.
     * Uses Java 11 HttpClient sendAsync for non-blocking I/O.
     */
    private CompletableFuture<String> callGrokAsync(String systemMessage, String userMessage) {
        if (apiKey == null || apiKey.isBlank()) {
            AuditLogger.logWarning("GrokService: API key not configured.");
            return CompletableFuture.completedFuture("[AI unavailable: API key not configured]");
        }

        String body = buildRequestBody(systemMessage, userMessage);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .timeout(Duration.ofSeconds(30))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() == 200) {
                        String content = extractContent(response.body());
                        AuditLogger.log("GrokService: successful response (" + content.length() + " chars)");
                        return content;
                    } else {
                        AuditLogger.logWarning("GrokService: HTTP " + response.statusCode());
                        return "[AI response error: HTTP " + response.statusCode() + "]";
                    }
                })
                .exceptionally(ex -> {
                    AuditLogger.logWarning("GrokService: call failed — " + ex.getMessage());
                    return "[AI temporarily unavailable. Please check your connection.]";
                });
    }

    /**
     * Build a minimal JSON request body compatible with the OpenAI-style Grok API.
     * Manual JSON construction avoids adding a heavy JSON library dependency.
     */
    private String buildRequestBody(String systemMessage, String userMessage) {
        return "{"
                + "\"model\":\"" + escapeJson(model) + "\","
                + "\"stream\":false,"
                + "\"temperature\":0.7,"
                + "\"messages\":["
                + "{\"role\":\"system\",\"content\":\"" + escapeJson(systemMessage) + "\"},"
                + "{\"role\":\"user\",\"content\":\"" + escapeJson(userMessage) + "\"}"
                + "]"
                + "}";
    }

    /**
     * Parse the "content" field from the Grok JSON response.
     * Lightweight manual parsing — no external JSON library needed.
     */
    private String extractContent(String json) {
        try {
            // Look for "content":"..." in the response
            int idx = json.indexOf("\"content\":");
            if (idx < 0)
                return "[No content in response]";
            int start = json.indexOf("\"", idx + 10) + 1;
            int end = start;
            // Walk forward handling escaped quotes
            while (end < json.length()) {
                char c = json.charAt(end);
                if (c == '\\') {
                    end += 2;
                    continue;
                }
                if (c == '"')
                    break;
                end++;
            }
            String raw = json.substring(start, end);
            return raw.replace("\\n", "\n").replace("\\\"", "\"").replace("\\'", "'");
        } catch (Exception e) {
            return "[Response parse error]";
        }
    }

    private String escapeJson(String s) {
        if (s == null)
            return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private Properties loadConfig() {
        Properties props = new Properties();
        // Try file system first (dev mode), then classpath
        File configFile = new File("config.properties");
        if (configFile.exists()) {
            try (InputStream in = new FileInputStream(configFile)) {
                props.load(in);
                return props;
            } catch (IOException ignored) {
            }
        }
        try (InputStream in = getClass().getResourceAsStream("/config.properties")) {
            if (in != null)
                props.load(in);
        } catch (IOException ignored) {
        }
        return props;
    }
}
