package tn.esprit.Services;

import tn.esprit.entities.User;
import tn.esprit.entities.SessionFeedback;
import java.util.*;
import java.util.stream.Collectors;

// NEW FILE
public class RecommendationService {
    private final UserService userService;
    private final SessionFeedbackService feedbackService;

    public RecommendationService() {
        this.userService = new UserService();
        this.feedbackService = new SessionFeedbackService();
    }

    public List<User> getTopMentors(int limit) {
        List<User> mentors = userService.listMentors();
        Map<User, Double> scores = new HashMap<>();

        for (User mentor : mentors) {
            List<SessionFeedback> feedbacks = feedbackService.listByMentor(mentor.getId());
            double score = 0.0;
            if (feedbacks != null && !feedbacks.isEmpty()) {
                double avgRating = feedbacks.stream().mapToInt(SessionFeedback::getProgressScore).average().orElse(0.0);
                long positiveCount = feedbacks.stream().filter(f -> "Positive".equalsIgnoreCase(f.getSentiment()))
                        .count();
                score = (avgRating * 0.7) + ((positiveCount * 100.0 / feedbacks.size()) * 0.3);
            }
            scores.put(mentor, score);
        }

        return mentors.stream()
                .sorted((m1, m2) -> Double.compare(scores.getOrDefault(m2, 0.0), scores.getOrDefault(m1, 0.0)))
                .limit(limit)
                .collect(Collectors.toList());
    }
}
