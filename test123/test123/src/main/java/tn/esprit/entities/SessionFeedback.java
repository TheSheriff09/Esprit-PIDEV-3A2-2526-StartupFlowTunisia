package tn.esprit.entities;

import java.time.LocalDate;

public class SessionFeedback {

    private int feedbackID;
    private int sessionID;
    private int mentorID;
    private int progressScore;
    private String strengths;
    private String weaknesses;
    private String recommendations;
    private String nextActions;
    private LocalDate feedbackDate;
    private String sentiment = "Neutral";

    public SessionFeedback() {
    }

    public SessionFeedback(int sessionID, int mentorID, int progressScore,
            String strengths, String weaknesses,
            String recommendations, String nextActions,
            LocalDate feedbackDate) {
        this.sessionID = sessionID;
        this.mentorID = mentorID;
        this.progressScore = progressScore;
        this.strengths = strengths;
        this.weaknesses = weaknesses;
        this.recommendations = recommendations;
        this.nextActions = nextActions;
        this.feedbackDate = feedbackDate;
    }

    public SessionFeedback(int feedbackID, int sessionID, int mentorID, int progressScore,
            String strengths, String weaknesses,
            String recommendations, String nextActions,
            LocalDate feedbackDate) {
        this.feedbackID = feedbackID;
        this.sessionID = sessionID;
        this.mentorID = mentorID;
        this.progressScore = progressScore;
        this.strengths = strengths;
        this.weaknesses = weaknesses;
        this.recommendations = recommendations;
        this.nextActions = nextActions;
        this.feedbackDate = feedbackDate;
    }

    public int getFeedbackID() {
        return feedbackID;
    }

    public void setFeedbackID(int v) {
        this.feedbackID = v;
    }

    public int getSessionID() {
        return sessionID;
    }

    public void setSessionID(int v) {
        this.sessionID = v;
    }

    public int getMentorID() {
        return mentorID;
    }

    public void setMentorID(int v) {
        this.mentorID = v;
    }

    public int getProgressScore() {
        return progressScore;
    }

    public void setProgressScore(int v) {
        this.progressScore = v;
    }

    public String getStrengths() {
        return strengths;
    }

    public void setStrengths(String v) {
        this.strengths = v;
    }

    public String getWeaknesses() {
        return weaknesses;
    }

    public void setWeaknesses(String v) {
        this.weaknesses = v;
    }

    public String getRecommendations() {
        return recommendations;
    }

    public void setRecommendations(String v) {
        this.recommendations = v;
    }

    public String getNextActions() {
        return nextActions;
    }

    public void setNextActions(String v) {
        this.nextActions = v;
    }

    public LocalDate getFeedbackDate() {
        return feedbackDate;
    }

    public void setFeedbackDate(LocalDate v) {
        this.feedbackDate = v;
    }

    public String getSentiment() {
        return sentiment;
    }

    public void setSentiment(String v) {
        this.sentiment = v;
    }

    @Override
    public String toString() {
        return "Feedback{sessionID=" + sessionID + ", score=" + progressScore + "}";
    }
}
