package tn.esprit.utils;

import java.util.Arrays;
import java.util.List;

public class ProfanityFilter {

    private static final List<String> BANNED_WORDS = Arrays.asList(
            "bad", "kill", "hate", "scam", "spam", "idiot", "stupid", "dumb", "fuck"

    );

    public static boolean containsProfanity(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        String lower = text.toLowerCase();
        for (String word : BANNED_WORDS) {
            // Check for whole word match or part of word depending on strictness
            // Simplest: contains
            if (lower.contains(word.toLowerCase())) {
                return true;
            }
        }
        return false;
    }
}
