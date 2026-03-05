package tn.esprit.Services;

import tn.esprit.entities.SessionFeedback;
import tn.esprit.entities.User;

import java.util.*;
import java.util.stream.Collectors;

/**
 * AnalyticsService — Mentor Performance Index (MPI) computation.
 *
 * <p>
 * Business justification: In a real mentorship platform, decision-makers need
 * objective, data-driven metrics to evaluate mentor effectiveness. The MPI
 * synthesises multiple signals into one actionable score.
 *
 * <p>
 * MPI Formula:
 * 
 * <pre>
 *   MPI = (avgProgressScore  × 0.40)   // weighted: primary quality signal
 *       + (positivityRate%   × 0.30)   // sentiment health
 *       + (completionRate%   × 0.20)   // reliability
 *       + (sessionBonus      × 0.10)   // experience (capped at 20 pts)
 * </pre>
 *
 * <p>
 * Design: Stateless service (no fields) — follows the "service as a function"
 * pattern.
 * All heavy lifting is done in pure Java streams over existing service data.
 */
public class AnalyticsService {

    private final SessionFeedbackService feedbackService;
    private final SessionService sessionService;
    private final UserService userService;

    public AnalyticsService() {
        this.feedbackService = new SessionFeedbackService();
        this.sessionService = new SessionService();
        this.userService = new UserService();
    }

    // ── MPI Computation ────────────────────────────────────────────────────

    /**
     * Compute the Mentor Performance Index (0–100) for one mentor.
     */
    public double getMentorPerformanceIndex(int mentorID) {
        List<SessionFeedback> feedbacks = feedbackService.listByMentor(mentorID);
        List<tn.esprit.entities.Session> sessions = sessionService.listByMentor(mentorID);

        // Average progress score (0–100)
        double avgScore = feedbacks.stream()
                .mapToInt(SessionFeedback::getProgressScore)
                .average().orElse(0.0);

        // Positivity rate (% of feedbacks with Positive sentiment)
        double positivityRate = feedbacks.isEmpty() ? 0.0
                : feedbacks.stream()
                        .filter(f -> "Positive".equalsIgnoreCase(f.getSentiment()))
                        .count() * 100.0 / feedbacks.size();

        // Completion rate (% of sessions that are "completed")
        double completionRate = sessions.isEmpty() ? 0.0
                : sessions.stream()
                        .filter(s -> "completed".equalsIgnoreCase(s.getStatus()))
                        .count() * 100.0 / sessions.size();

        // Session volume bonus (experience factor, capped at 20)
        double sessionBonus = Math.min(sessions.size() * 2.0, 20.0);

        double mpi = (avgScore * 0.40)
                + (positivityRate * 0.30)
                + (completionRate * 0.20)
                + (sessionBonus * 0.10);

        return Math.max(0.0, Math.min(100.0, mpi));
    }

    /**
     * Rank all mentors by MPI descending.
     *
     * @return sorted list of {@link MentorStats}
     */
    public List<MentorStats> getRankedMentors() {
        List<User> mentors = userService.listMentors();
        return mentors.stream()
                .map(m -> {
                    List<SessionFeedback> fb = feedbackService.listByMentor(m.getId());
                    List<tn.esprit.entities.Session> ss = sessionService.listByMentor(m.getId());
                    double mpi = getMentorPerformanceIndex(m.getId());
                    double avgR = fb.stream().mapToInt(SessionFeedback::getProgressScore).average().orElse(0.0);
                    return new MentorStats(m, mpi, avgR, ss.size(), fb.size());
                })
                .sorted(Comparator.comparingDouble(MentorStats::mpi).reversed())
                .collect(Collectors.toList());
    }

    /**
     * Entrepreneur engagement score — measures how actively an entrepreneur
     * participates in the platform (sessions attended, feedback received).
     */
    public double getEntrepreneurEngagementScore(int entrepreneurID) {
        List<tn.esprit.entities.Session> sessions = sessionService.listByEvaluator(entrepreneurID);
        long completed = sessions.stream()
                .filter(s -> "completed".equalsIgnoreCase(s.getStatus())).count();
        double completionBonus = sessions.isEmpty() ? 0.0 : completed * 100.0 / sessions.size();
        double volumeBonus = Math.min(sessions.size() * 5.0, 40.0);
        return Math.min(100.0, completionBonus * 0.60 + volumeBonus * 0.40);
    }

    /**
     * Platform-wide statistics snapshot — used for admin dashboard.
     */
    public Map<String, Object> getPlatformSnapshot() {
        Map<String, Object> snap = new LinkedHashMap<>();
        List<User> mentors = userService.listMentors();
        snap.put("totalMentors", mentors.size());

        // Average MPI across all mentors
        double avgMPI = mentors.stream()
                .mapToDouble(m -> getMentorPerformanceIndex(m.getId()))
                .average().orElse(0.0);
        snap.put("averageMPI", String.format("%.1f", avgMPI));

        // Best mentor
        mentors.stream()
                .max(Comparator.comparingDouble(m -> getMentorPerformanceIndex(m.getId())))
                .ifPresent(m -> snap.put("topMentor", m.getFullName()));

        return snap;
    }

    // ── Inner Record ───────────────────────────────────────────────────────

    /**
     * Immutable value object holding analytics results for one mentor.
     * Using a record-style class for clean data transport (Java 17).
     */
    public static final class MentorStats {
        private final User mentor;
        private final double mpi;
        private final double avgRating;
        private final int sessionCount;
        private final int feedbackCount;

        public MentorStats(User mentor, double mpi, double avgRating,
                int sessionCount, int feedbackCount) {
            this.mentor = mentor;
            this.mpi = mpi;
            this.avgRating = avgRating;
            this.sessionCount = sessionCount;
            this.feedbackCount = feedbackCount;
        }

        public User getMentor() {
            return mentor;
        }

        public double mpi() {
            return mpi;
        }

        public double getAvgRating() {
            return avgRating;
        }

        public int getSessionCount() {
            return sessionCount;
        }

        public int getFeedbackCount() {
            return feedbackCount;
        }

        /** Formatted MPI string for UI display. */
        public String getMpiDisplay() {
            return String.format("%.1f", mpi);
        }

        @Override
        public String toString() {
            return mentor.getFullName() + " [MPI: " + getMpiDisplay() + "]";
        }
    }
}
