package tn.esprit.Services;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Free grammar and spell-check via LanguageTool public API.
 * No API key required. Rate limit: 20 requests/minute.
 */
public class LanguageToolService {

    private static final String API_URL = "https://api.languagetool.org/v2/check";

    public record Suggestion(String message, String shortMessage, String context, String replacement) {
    }

    /**
     * Checks the text for grammar/spelling issues.
     * 
     * @param text The text to check
     * @param lang Language code, e.g. "en-US" or "fr"
     * @return list of suggestions (empty if no issues or request fails gracefully)
     */
    public List<Suggestion> check(String text, String lang) {
        List<Suggestion> result = new ArrayList<>();
        if (text == null || text.trim().length() < 5)
            return result;

        try {
            String body = "text=" + URLEncoder.encode(text, StandardCharsets.UTF_8)
                    + "&language=" + URLEncoder.encode(lang, StandardCharsets.UTF_8);

            URL url = new URL(API_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(8000);
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }

            if (conn.getResponseCode() != 200)
                return result;

            StringBuilder sb = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                String line;
                while ((line = br.readLine()) != null)
                    sb.append(line);
            }

            result = parseMatches(sb.toString());
        } catch (Exception e) {
            System.err.println("LanguageTool API error: " + e.getMessage());
        }
        return result;
    }

    /** Minimal JSON parsing without external libraries. */
    private List<Suggestion> parseMatches(String json) {
        List<Suggestion> list = new ArrayList<>();
        // Split by "matches" -> each item is an issue
        int matchesIdx = json.indexOf("\"matches\"");
        if (matchesIdx < 0)
            return list;

        String matchSection = json.substring(matchesIdx);
        String[] items = matchSection.split("\\{\"message\"");
        for (int i = 1; i < items.length; i++) {
            String item = items[i];
            String message = extractValue(item, "message");
            String shortMsg = extractValue(item, "shortMessage");
            String ctx = extractValue(item, "text"); // context.text

            // Extract first replacement
            String replacement = "";
            int repIdx = item.indexOf("\"replacements\"");
            if (repIdx >= 0) {
                int valIdx = item.indexOf("\"value\"", repIdx);
                if (valIdx >= 0)
                    replacement = extractValue(item.substring(valIdx), "value");
            }

            if (!message.isEmpty()) {
                list.add(new Suggestion(message, shortMsg, ctx, replacement));
            }
        }
        return list;
    }

    private String extractValue(String json, String key) {
        String search = "\"" + key + "\":\"";
        int s = json.indexOf(search);
        if (s < 0)
            return "";
        s += search.length();
        int e = json.indexOf("\"", s);
        return e < 0 ? "" : json.substring(s, e).replace("\\n", " ").replace("\\\"", "\"");
    }
}
