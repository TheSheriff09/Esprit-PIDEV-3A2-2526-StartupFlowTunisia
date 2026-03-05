package tn.esprit.entities;

import java.sql.Timestamp;

public class Favorite {
    private int id;
    private int entrepreneurID;
    private int mentorID;
    private Timestamp createdAt;

    public Favorite() {
    }

    public Favorite(int entrepreneurID, int mentorID) {
        this.entrepreneurID = entrepreneurID;
        this.mentorID = mentorID;
    }

    public Favorite(int id, int entrepreneurID, int mentorID, Timestamp createdAt) {
        this.id = id;
        this.entrepreneurID = entrepreneurID;
        this.mentorID = mentorID;
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

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}
