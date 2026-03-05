package tn.esprit.entities;

import java.sql.Timestamp;

public class MentorEvaluation {
    private int id;
    private int entrepreneurID;
    private int mentorID;
    private int sessionID;
    private int rating;
    private String comment;
    private Timestamp createdAt;

    public MentorEvaluation() {
    }

    public MentorEvaluation(int entrepreneurID, int mentorID, int sessionID, int rating, String comment) {
        this.entrepreneurID = entrepreneurID;
        this.mentorID = mentorID;
        this.sessionID = sessionID;
        this.rating = rating;
        this.comment = comment;
    }

    public MentorEvaluation(int id, int entrepreneurID, int mentorID, int sessionID, int rating, String comment,
            Timestamp createdAt) {
        this.id = id;
        this.entrepreneurID = entrepreneurID;
        this.mentorID = mentorID;
        this.sessionID = sessionID;
        this.rating = rating;
        this.comment = comment;
        this.createdAt = createdAt;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getEntrepreneurID() {
        return entrepreneurID;
    }

    public void setEntrepreneurID(int entrepreneurID) {
        this.entrepreneurID = entrepreneurID;
    }

    public int getMentorID() {
        return mentorID;
    }

    public void setMentorID(int mentorID) {
        this.mentorID = mentorID;
    }

    public int getSessionID() {
        return sessionID;
    }

    public void setSessionID(int sessionID) {
        this.sessionID = sessionID;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}
