package tn.esprit.services;

import opennlp.tools.doccat.DoccatModel;
import opennlp.tools.doccat.DocumentCategorizerME;
import java.io.FileInputStream;
import java.io.InputStream;

// NEW FILE
public class SentimentAnalyzerService {
    private static SentimentAnalyzerService instance;
    private DoccatModel model;

    private SentimentAnalyzerService() {
        try (InputStream modelIn = new FileInputStream("models/en-doccat.bin")) {
            model = new DoccatModel(modelIn);
        } catch (Exception e) {
            System.err.println("OpenNLP Model not found. Sentiment will fallback to rule-based.");
        }
    }

    public static SentimentAnalyzerService getInstance() {
        if (instance == null) {
            instance = new SentimentAnalyzerService();
        }
        return instance;
    }

    public String analyzeSentiment(String text) {
        if (text == null || text.trim().isEmpty())
            return "Neutral";

        if (model != null) {
            try {
                DocumentCategorizerME myCategorizer = new DocumentCategorizerME(model);
                double[] outcomes = myCategorizer.categorize(text.split("\\s+"));
                return myCategorizer.getBestCategory(outcomes);
            } catch (Exception e) {
                return ruleBasedFallback(text);
            }
        } else {
            return ruleBasedFallback(text);
        }
    }

    private String ruleBasedFallback(String text) {
        String lower = text.toLowerCase();
        int score = 0;
        String[] pos = { "great", "excellent", "good", "amazing", "wonderful", "improving", "progress", "strength",
                "best", "helpful" };
        String[] neg = { "bad", "poor", "terrible", "worst", "awful", "lacking", "weak", "fail", "useless",
                "disappointed" };
        for (String p : pos) {
            if (lower.contains(p))
                score++;
        }
        for (String n : neg) {
            if (lower.contains(n))
                score--;
        }

        if (score > 0)
            return "Positive";
        else if (score < 0)
            return "Negative";
        else
            return "Neutral";
    }
}
