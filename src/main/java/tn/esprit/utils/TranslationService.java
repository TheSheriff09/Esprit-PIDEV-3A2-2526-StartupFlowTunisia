package tn.esprit.utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class TranslationService {

    public String translate(String text, String targetLang) throws Exception {

        if (text == null || text.isBlank()) return text;

        String encodedText = URLEncoder.encode(text, "UTF-8");

        // Source language auto-detect (set to en if your app is English)
        String urlStr = "https://api.mymemory.translated.net/get?q="
                + encodedText
                + "&langpair=en|" + targetLang;

        URL url = new URL(urlStr);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");
        con.setConnectTimeout(5000);
        con.setReadTimeout(5000);

        int responseCode = con.getResponseCode();

        if (responseCode != 200) {
            throw new RuntimeException("HTTP Error: " + responseCode);
        }

        BufferedReader in = new BufferedReader(
                new InputStreamReader(con.getInputStream())
        );

        String inputLine;
        StringBuilder response = new StringBuilder();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        String json = response.toString();

        // Extract translatedText from JSON manually
        int start = json.indexOf("\"translatedText\":\"") + 18;
        int end = json.indexOf("\"", start);

        if (start < 18 || end < 0) {
            return text; // fallback
        }

        String translated = json.substring(start, end);

        // Decode escaped characters
        translated = translated.replace("\\u003d", "=");
        translated = translated.replace("\\\"", "\"");

        return translated;
    }
}