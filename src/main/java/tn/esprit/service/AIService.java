package tn.esprit.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.StringEntity;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class AIService {

    private final String apiKey;
    private final String apiUrl;
    private final ObjectMapper objectMapper;

    public AIService() {
        Properties config = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("config.properties")) {
            if (input == null) {
                throw new RuntimeException("config.properties not found in resources");
            }
            config.load(input);
        } catch (IOException e) {
            throw new RuntimeException("Error loading config.properties", e);
        }

        apiKey = config.getProperty("openai.api.key");
        apiUrl = config.getProperty("openai.api.url");
        objectMapper = new ObjectMapper();
    }

    public static class AIEvaluationResult {
        public int score;
        public String decision;
        public String explanation;

        public AIEvaluationResult(int score, String decision, String explanation) {
            this.score = score;
            this.decision = decision;
            this.explanation = explanation;
        }
    }

    public AIEvaluationResult evaluateApplication(String pdfText) {
        String prompt = buildEvaluationPrompt(pdfText);

        try {
            String response = callOpenAIAPI(prompt);
            return parseAIResponse(response);
        } catch (Exception e) {
            System.err.println("AI evaluation failed: " + e.getMessage());
            // Return default values on error
            return new AIEvaluationResult(50, "REVIEW", "AI evaluation failed: " + e.getMessage());
        }
    }

    private String buildEvaluationPrompt(String pdfText) {
        return "You are an AI assistant evaluating startup funding applications. " +
               "Analyze the following business proposal and provide:\n" +
               "1. A score from 0-100 based on business model clarity, financial feasibility, market potential, and risk level\n" +
               "2. A decision: APPROVE, REVIEW, or REJECT\n" +
               "3. A brief explanation (max 200 words)\n\n" +
               "Business Proposal Text:\n" + pdfText + "\n\n" +
               "Respond in JSON format: {\"score\": number, \"decision\": \"string\", \"explanation\": \"string\"}";
    }

    private String callOpenAIAPI(String prompt) throws IOException {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost(apiUrl);
            httpPost.setHeader("Authorization", "Bearer " + apiKey);
            httpPost.setHeader("Content-Type", "application/json");

            String jsonBody = String.format(
                "{\"model\": \"gpt-3.5-turbo\", \"messages\": [{\"role\": \"user\", \"content\": \"%s\"}], \"max_tokens\": 300}",
                prompt.replace("\"", "\\\"").replace("\n", "\\n")
            );

            httpPost.setEntity(new StringEntity(jsonBody, ContentType.APPLICATION_JSON));

            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                int statusCode = response.getCode();
                if (statusCode != 200) {
                    throw new IOException("OpenAI API returned status: " + statusCode);
                }

                String responseBody = new String(response.getEntity().getContent().readAllBytes());
                return responseBody;
            }
        }
    }

    private AIEvaluationResult parseAIResponse(String response) throws IOException {
        JsonNode root = objectMapper.readTree(response);
        String content = root.path("choices").get(0).path("message").path("content").asText();

        // Try to parse the JSON content
        try {
            JsonNode result = objectMapper.readTree(content);
            int score = result.path("score").asInt(50);
            String decision = result.path("decision").asText("REVIEW");
            String explanation = result.path("explanation").asText("AI evaluation completed");

            return new AIEvaluationResult(score, decision, explanation);
        } catch (Exception e) {
            // If JSON parsing fails, extract information from text
            return extractFromText(content);
        }
    }

    private AIEvaluationResult extractFromText(String text) {
        int score = 50;
        String decision = "REVIEW";
        String explanation = text;

        // Simple text parsing (could be improved)
        if (text.toLowerCase().contains("approve")) {
            decision = "APPROVE";
        } else if (text.toLowerCase().contains("reject")) {
            decision = "REJECT";
        }

        // Try to find a number for score
        String[] words = text.split("\\s+");
        for (String word : words) {
            try {
                int num = Integer.parseInt(word);
                if (num >= 0 && num <= 100) {
                    score = num;
                    break;
                }
            } catch (NumberFormatException ignored) {}
        }

        return new AIEvaluationResult(score, decision, explanation);
    }
}