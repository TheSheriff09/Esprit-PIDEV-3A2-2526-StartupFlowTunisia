package tn.esprit.utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Free currency conversion using ExchangeRate-API Open Access (no key).
 * Docs: https://www.exchangerate-api.com/docs/free  (Open Access endpoint)
 *
 * Endpoint used:
 *   https://open.er-api.com/v6/latest/{BASE}
 *
 * Note: Updates once per day and rate-limited.
 */
public class CurrencyConverterService {

    public double convert(double amount, String from, String to) throws Exception {
        if (from == null || to == null || from.isBlank() || to.isBlank()) {
            throw new IllegalArgumentException("Currency codes are required.");
        }
        from = from.trim().toUpperCase();
        to = to.trim().toUpperCase();

        if (from.equals(to)) return amount;

        String urlStr = "https://open.er-api.com/v6/latest/" + from;
        HttpURLConnection con = (HttpURLConnection) new URL(urlStr).openConnection();
        con.setRequestMethod("GET");
        con.setConnectTimeout(8000);
        con.setReadTimeout(12000);

        int code = con.getResponseCode();
        if (code != 200) {
            throw new RuntimeException("Currency API HTTP error: " + code);
        }

        String json = readAll(con);

        // basic success check
        if (!json.contains("\"result\":\"success\"")) {
            throw new RuntimeException("Currency API returned error: " + json);
        }

        // extract "rates": { ... "TO": 1.234 ... }
        double rate = extractRate(json, to);
        if (rate <= 0) {
            throw new RuntimeException("Rate not found for currency: " + to);
        }

        return amount * rate;
    }

    private String readAll(HttpURLConnection con) throws Exception {
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            return sb.toString();
        }
    }

    // minimal JSON parsing: find "rates":{"XXX":...} and get value for "TO"
    private double extractRate(String json, String currencyCode) {
        String ratesKey = "\"rates\":";
        int rIdx = json.indexOf(ratesKey);
        if (rIdx < 0) return -1;

        int braceStart = json.indexOf('{', rIdx);
        if (braceStart < 0) return -1;

        // find the closing brace for rates object (simple scan)
        int depth = 0;
        int braceEnd = -1;
        for (int i = braceStart; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '{') depth++;
            else if (c == '}') {
                depth--;
                if (depth == 0) {
                    braceEnd = i;
                    break;
                }
            }
        }
        if (braceEnd < 0) return -1;

        String ratesObj = json.substring(braceStart, braceEnd + 1);

        // look for: "USD":1.234
        String needle = "\"" + currencyCode + "\":";
        int idx = ratesObj.indexOf(needle);
        if (idx < 0) return -1;

        int start = idx + needle.length();
        int end = start;
        while (end < ratesObj.length()) {
            char ch = ratesObj.charAt(end);
            if ((ch >= '0' && ch <= '9') || ch == '.' || ch == 'E' || ch == 'e' || ch == '-' || ch == '+') {
                end++;
            } else {
                break;
            }
        }

        try {
            return Double.parseDouble(ratesObj.substring(start, end));
        } catch (Exception e) {
            return -1;
        }
    }
}