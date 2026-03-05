package tn.esprit.services;

import tn.esprit.entities.Session;
import tn.esprit.entities.SessionFeedback;

import java.util.*;

/**
 * SortingService — Multi-criteria Comparator chaining for Sessions, Feedback,
 * and Mentors.
 *
 * <p>
 * Business justification: In a growing mentorship platform, users need flexible
 * sorting
 * to surface the most relevant records. A mentor may want to see highest-rated
 * sessions;
 * an entrepreneur may want most-recent feedback.
 *
 * <p>
 * Design pattern: Strategy + Comparator chaining.
 * Each public method accepts a primary {@link SortCriteria} and returns a new
 * sorted List
 * (never mutates the input). Pure static utility — no state, easy to test.
 *
 * <p>
 * Comparator chains use {@link Comparator#thenComparing} to ensure
 * deterministic ordering
 * even when primary criteria tie (e.g. same date → fallback to ID).
 */
public class SortingService {

    // ── Sort Criteria Enums ───────────────────────────────────────────────

    public enum SessionSort {
        DATE_DESC("Date ↓"),
        DATE_ASC("Date ↑"),
        STATUS("Status A→Z"),
        SUCCESS_PROBABILITY_DESC("Success % ↓"),
        SUCCESS_PROBABILITY_ASC("Success % ↑"),
        TYPE("Session Type");

        private final String label;

        SessionSort(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    public enum FeedbackSort {
        DATE_DESC("Date ↓"),
        DATE_ASC("Date ↑"),
        SCORE_DESC("Score ↓"),
        SCORE_ASC("Score ↑"),
        SENTIMENT("Sentiment");

        private final String label;

        FeedbackSort(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    public enum MentorSort {
        NAME("Name A→Z"),
        MPI_DESC("Performance ↓"),
        MPI_ASC("Performance ↑"),
        SESSION_COUNT_DESC("Sessions ↓");

        private final String label;

        MentorSort(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    // ── Session Sorting ───────────────────────────────────────────────────

    /**
     * Sort sessions by the given criterion, with ID as tiebreaker for stability.
     */
    public static List<Session> sortSessions(List<Session> sessions, SessionSort criteria) {
        if (sessions == null || sessions.isEmpty())
            return sessions;

        Comparator<Session> comp = switch (criteria) {
            case DATE_DESC -> Comparator.comparing(Session::getSessionDate,
                    Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparingInt(Session::getSessionID);
            case DATE_ASC -> Comparator.comparing(Session::getSessionDate,
                    Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparingInt(Session::getSessionID);
            case STATUS -> Comparator.comparing(Session::getStatus,
                    Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(Session::getSessionDate, Comparator.nullsLast(Comparator.reverseOrder()));
            case SUCCESS_PROBABILITY_DESC -> Comparator.comparingDouble(Session::getSuccessProbability)
                    .reversed()
                    .thenComparingInt(Session::getSessionID);
            case SUCCESS_PROBABILITY_ASC -> Comparator.comparingDouble(Session::getSuccessProbability)
                    .thenComparingInt(Session::getSessionID);
            case TYPE -> Comparator.comparing(Session::getSessionType,
                    Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(Session::getSessionDate, Comparator.nullsLast(Comparator.reverseOrder()));
        };

        List<Session> sorted = new ArrayList<>(sessions);
        sorted.sort(comp);
        return sorted;
    }

    // ── Feedback Sorting ──────────────────────────────────────────────────

    /**
     * Sort feedback entries by the given criterion, with feedbackID as tiebreaker.
     */
    public static List<SessionFeedback> sortFeedback(List<SessionFeedback> list, FeedbackSort criteria) {
        if (list == null || list.isEmpty())
            return list;

        Comparator<SessionFeedback> comp = switch (criteria) {
            case DATE_DESC -> Comparator.comparing(SessionFeedback::getFeedbackDate,
                    Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparingInt(SessionFeedback::getFeedbackID);
            case DATE_ASC -> Comparator.comparing(SessionFeedback::getFeedbackDate,
                    Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparingInt(SessionFeedback::getFeedbackID);
            case SCORE_DESC -> Comparator.comparingInt(SessionFeedback::getProgressScore)
                    .reversed()
                    .thenComparingInt(SessionFeedback::getFeedbackID);
            case SCORE_ASC -> Comparator.comparingInt(SessionFeedback::getProgressScore)
                    .thenComparingInt(SessionFeedback::getFeedbackID);
            case SENTIMENT -> Comparator.comparing(SessionFeedback::getSentiment,
                    Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparingInt(SessionFeedback::getProgressScore);
        };

        List<SessionFeedback> sorted = new ArrayList<>(list);
        sorted.sort(comp);
        return sorted;
    }

    // ── Mentor Sorting ────────────────────────────────────────────────────

    /**
     * Sort mentor stats by the given criterion.
     */
    public static List<AnalyticsService.MentorStats> sortMentors(
            List<AnalyticsService.MentorStats> list, MentorSort criteria) {
        if (list == null || list.isEmpty())
            return list;

        Comparator<AnalyticsService.MentorStats> comp = switch (criteria) {
            case NAME -> Comparator.comparing(
                    ms -> ms.getMentor().getFullName(),
                    Comparator.nullsLast(Comparator.naturalOrder()));
            case MPI_DESC -> Comparator.comparingDouble(AnalyticsService.MentorStats::mpi).reversed();
            case MPI_ASC -> Comparator.comparingDouble(AnalyticsService.MentorStats::mpi);
            case SESSION_COUNT_DESC -> Comparator.comparingInt(
                    AnalyticsService.MentorStats::getSessionCount).reversed();
        };

        List<AnalyticsService.MentorStats> sorted = new ArrayList<>(list);
        sorted.sort(comp);
        return sorted;
    }

    // ── Convenience: apply default sort ──────────────────────────────────

    public static List<Session> defaultSortSessions(List<Session> s) {
        return sortSessions(s, SessionSort.DATE_DESC);
    }

    public static List<SessionFeedback> defaultSortFeedback(List<SessionFeedback> f) {
        return sortFeedback(f, FeedbackSort.DATE_DESC);
    }
}
