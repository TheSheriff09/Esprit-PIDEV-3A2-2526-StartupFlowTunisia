package tn.esprit.Services;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class GeminiService {

    private static final String API_KEY = "AIzaSyDQqkyJfJ8zO9iRJyZz1xi0oEAexciARnM";
    private static final String ENDPOINT = "https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent?key="
            + API_KEY;

    public String getMentorAdvice(String mentorName, int sessionCount, double avgRating) {
        String prompt = "You are an expert mentorship coach. The mentor '" + mentorName + "' has completed "
                + sessionCount + " sessions and has an average rating of " + String.format("%.1f", avgRating)
                + " out of 5 stars. Provide a very short (2-3 sentences max) personalized advice on how they can improve or maintain their performance.";
        return callGemini(prompt);
    }

    public String rankMentorsForEntrepreneur(String entrepreneurName, String startupsContext,
            String allMentorsContext) {
        String prompt = "You are an AI matching agent. The entrepreneur '" + entrepreneurName
                + "' has the following startups: " + startupsContext
                + ". \n\nHere is a list of all available mentors: \n" + allMentorsContext
                + "\n\nAnalyze the entrepreneur's needs based on their startup sectors, and recommend the top 2 best mentors for them. Only respond with a short bulleted list of 2 mentor names and a one-sentence reason why. Do not use markdown like *, just plain text bullets (-).";
        return callGemini(prompt);
    }

    private String callGemini(String promptText) {
        try {
            URL url = new URL(ENDPOINT);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            JSONObject content = new JSONObject();
            JSONObject parts = new JSONObject();
            parts.put("text", promptText);
            JSONArray partsArray = new JSONArray();
            partsArray.put(parts);

            JSONObject contentsObj = new JSONObject();
            contentsObj.put("parts", partsArray);
            JSONArray contentsArray = new JSONArray();
            contentsArray.put(contentsObj);

            content.put("contents", contentsArray);

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = content.toString().getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            if (conn.getResponseCode() != 200) {
                BufferedReader br = new BufferedReader(
                        new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8));
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
                System.err.println("Gemini Error: " + response.toString());
                return "AI Error: Could not connect to Gemini.";
            }

            BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
            StringBuilder response = new StringBuilder();
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }

            // Parse response
            JSONObject jsonResponse = new JSONObject(response.toString());
            JSONArray candidates = jsonResponse.getJSONArray("candidates");
            if (candidates.length() > 0) {
                JSONObject firstCandidate = candidates.getJSONObject(0);
                JSONObject contentObj = firstCandidate.getJSONObject("content");
                JSONArray partsRes = contentObj.getJSONArray("parts");
                if (partsRes.length() > 0) {
                    return partsRes.getJSONObject(0).getString("text").trim();
                }
            }
            return "AI returned an empty response.";

        } catch (Exception e) {
            e.printStackTrace();
            return "Failed to fetch AI Insights.";
        }
    }
}
