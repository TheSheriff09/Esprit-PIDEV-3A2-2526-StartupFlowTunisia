package tn.esprit.utils;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class OcrService {

    // Free OCR API endpoint
    private static final String OCR_URL = "https://api.ocr.space/parse/image"; // :contentReference[oaicite:1]{index=1}

    // For quick testing OCR.space suggests "helloworld" (rate-limited) :contentReference[oaicite:2]{index=2}
    // Replace with your own FREE key from: https://ocr.space/ocrapi/freekey :contentReference[oaicite:3]{index=3}
    private static final String API_KEY = "K82153247488957";

    /**
     * Extract text from a local PDF/image file using OCR.space.
     * Works with PDF, JPG, PNG, etc.
     */
    public String extractText(File file) throws IOException {
        if (file == null || !file.exists()) {
            throw new IOException("File not found.");
        }

        String boundary = "----JavaBoundary" + System.currentTimeMillis();
        HttpURLConnection conn = (HttpURLConnection) new URL(OCR_URL).openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(60000);
        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

        try (OutputStream out = conn.getOutputStream()) {

            // apikey field
            writeFormField(out, boundary, "apikey", API_KEY);

            // language (optional) - "eng" works well; OCR.space supports many languages
            writeFormField(out, boundary, "language", "eng");

            // isOverlayRequired (optional)
            writeFormField(out, boundary, "isOverlayRequired", "false");

            // file field
            writeFileField(out, boundary, "file", file);

            // end boundary
            out.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        }

        int code = conn.getResponseCode();
        String response = readAll(code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream());

        if (code != 200) {
            throw new IOException("OCR API error HTTP " + code + ": " + response);
        }

        // OCR.space response: ParsedResults[0].ParsedText
        String parsedText = extractJsonValue(response, "ParsedText");
        if (parsedText == null) {
            // Also check for error message
            String err = extractJsonValue(response, "ErrorMessage");
            if (err != null && !err.isBlank()) {
                throw new IOException("OCR error: " + err);
            }
            return "";
        }

        return unescapeJson(parsedText).trim();
    }

    private static void writeFormField(OutputStream out, String boundary, String name, String value) throws IOException {
        out.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
        out.write(("Content-Disposition: form-data; name=\"" + name + "\"\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        out.write((value + "\r\n").getBytes(StandardCharsets.UTF_8));
    }

    private static void writeFileField(OutputStream out, String boundary, String fieldName, File file) throws IOException {
        String fileName = file.getName();
        out.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
        out.write(("Content-Disposition: form-data; name=\"" + fieldName + "\"; filename=\"" + fileName + "\"\r\n").getBytes(StandardCharsets.UTF_8));
        out.write(("Content-Type: application/octet-stream\r\n\r\n").getBytes(StandardCharsets.UTF_8));

        try (InputStream in = new FileInputStream(file)) {
            byte[] buf = new byte[8192];
            int r;
            while ((r = in.read(buf)) != -1) out.write(buf, 0, r);
        }
        out.write("\r\n".getBytes(StandardCharsets.UTF_8));
    }

    private static String readAll(InputStream in) throws IOException {
        if (in == null) return "";
        try (BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            return sb.toString();
        }
    }

    // minimal JSON value extraction (string fields)
    private static String extractJsonValue(String json, String key) {
        if (json == null) return null;
        String marker = "\"" + key + "\"";
        int k = json.indexOf(marker);
        if (k < 0) return null;

        int colon = json.indexOf(':', k);
        if (colon < 0) return null;

        int firstQuote = json.indexOf('"', colon + 1);
        if (firstQuote < 0) return null;

        int endQuote = findStringEnd(json, firstQuote + 1);
        if (endQuote < 0) return null;

        return json.substring(firstQuote + 1, endQuote);
    }

    private static int findStringEnd(String s, int start) {
        boolean esc = false;
        for (int i = start; i < s.length(); i++) {
            char c = s.charAt(i);
            if (esc) esc = false;
            else if (c == '\\') esc = true;
            else if (c == '"') return i;
        }
        return -1;
    }

    private static String unescapeJson(String s) {
        return s.replace("\\n", "\n")
                .replace("\\t", "\t")
                .replace("\\r", "")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
    }
}