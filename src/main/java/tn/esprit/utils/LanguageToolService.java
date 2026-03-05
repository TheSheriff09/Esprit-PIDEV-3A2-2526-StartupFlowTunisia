package tn.esprit.utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * LanguageTool public API (no key) for grammar/spelling correction.
 * Endpoint: https://api.languagetool.org/v2/check
 *
 * Notes:
 * - Free public endpoint is rate-limited.
 * - Best for short texts (like "applicationReason").
 */
public class LanguageToolService {

    private static final String ENDPOINT = "https://api.languagetool.org/v2/check";

    public String correctText(String text) throws Exception {
        if (text == null) return "";
        String original = text.trim();
        if (original.isEmpty()) return "";

        String body = "text=" + url(original)
                + "&language=auto"
                + "&enabledOnly=false";

        HttpURLConnection con = (HttpURLConnection) new URL(ENDPOINT).openConnection();
        con.setRequestMethod("POST");
        con.setConnectTimeout(8000);
        con.setReadTimeout(15000);
        con.setDoOutput(true);

        con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        con.setRequestProperty("Accept", "application/json");

        try (OutputStream os = con.getOutputStream()) {
            os.write(body.getBytes(StandardCharsets.UTF_8));
        }

        int code = con.getResponseCode();
        String json = readAll(code >= 200 && code < 300 ? con.getInputStream() : con.getErrorStream());

        if (code != 200) {
            throw new RuntimeException("LanguageTool HTTP " + code + ": " + json);
        }

        // Parse matches (offset, length, first replacement value)
        List<Fix> fixes = parseFixes(json);

        if (fixes.isEmpty()) {
            return original; // no corrections found
        }

        // Apply from end -> start so offsets remain valid
        fixes.sort(Comparator.comparingInt((Fix f) -> f.offset).reversed());

        StringBuilder sb = new StringBuilder(original);
        for (Fix f : fixes) {
            if (f.offset < 0 || f.offset + f.length > sb.length()) continue;
            sb.replace(f.offset, f.offset + f.length, f.replacement);
        }

        return sb.toString().trim();
    }

    private List<Fix> parseFixes(String json) {
        List<Fix> fixes = new ArrayList<>();

        // Extract blocks that contain offset/length and replacements
        // This is a lightweight parsing approach (no external JSON library).
        Pattern p = Pattern.compile("\"offset\"\\s*:\\s*(\\d+).*?\"length\"\\s*:\\s*(\\d+).*?\"replacements\"\\s*:\\s*\\[(.*?)\\]",
                Pattern.DOTALL);
        Matcher m = p.matcher(json);

        while (m.find()) {
            int offset = Integer.parseInt(m.group(1));
            int length = Integer.parseInt(m.group(2));
            String replacementsBlock = m.group(3);

            String replacement = firstReplacementValue(replacementsBlock);
            if (replacement != null && !replacement.isBlank()) {
                fixes.add(new Fix(offset, length, unescapeJson(replacement)));
            }
        }

        return fixes;
    }

    private String firstReplacementValue(String replacementsBlock) {
        if (replacementsBlock == null) return null;

        // Find first: {"value":"..."}
        Pattern p = Pattern.compile("\"value\"\\s*:\\s*\"(.*?)\"", Pattern.DOTALL);
        Matcher m = p.matcher(replacementsBlock);
        if (m.find()) return m.group(1);

        return null;
    }

    private String readAll(java.io.InputStream in) throws Exception {
        if (in == null) return "";
        try (BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            return sb.toString();
        }
    }

    private static String url(String s) throws Exception {
        return URLEncoder.encode(s, "UTF-8");
    }

    private static String unescapeJson(String s) {
        return s.replace("\\n", "\n")
                .replace("\\t", "\t")
                .replace("\\r", "")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
    }

    private static class Fix {
        final int offset;
        final int length;
        final String replacement;

        Fix(int offset, int length, String replacement) {
            this.offset = offset;
            this.length = length;
            this.replacement = replacement;
        }
    }
}