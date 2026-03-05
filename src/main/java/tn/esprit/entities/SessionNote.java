package tn.esprit.entities;

import java.time.LocalDate;

public class SessionNote {

    private int noteID;
    private int sessionID;
    private int entrepreneurID;
    private int satisfactionScore;
    private String notes;
    private LocalDate noteDate;

    public SessionNote() {
    }

    /** Used when creating a new note. */
    public SessionNote(int sessionID, int entrepreneurID, int satisfactionScore, String notes, LocalDate noteDate) {
        this.sessionID = sessionID;
        this.entrepreneurID = entrepreneurID;
        this.satisfactionScore = satisfactionScore;
        this.notes = notes;
        this.noteDate = noteDate;
    }

    /** Full constructor including DB-generated ID. */
    public SessionNote(int noteID, int sessionID, int entrepreneurID, int satisfactionScore, String notes,
            LocalDate noteDate) {
        this.noteID = noteID;
        this.sessionID = sessionID;
        this.entrepreneurID = entrepreneurID;
        this.satisfactionScore = satisfactionScore;
        this.notes = notes;
        this.noteDate = noteDate;
    }

    public int getNoteID() {
        return noteID;
    }

    public void setNoteID(int noteID) {
        this.noteID = noteID;
    }

    public int getSessionID() {
        return sessionID;
    }

    public void setSessionID(int sessionID) {
        this.sessionID = sessionID;
    }

    public int getEntrepreneurID() {
        return entrepreneurID;
    }

    public void setEntrepreneurID(int entrepreneurID) {
        this.entrepreneurID = entrepreneurID;
    }

    public int getSatisfactionScore() {
        return satisfactionScore;
    }

    public void setSatisfactionScore(int satisfactionScore) {
        this.satisfactionScore = satisfactionScore;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public LocalDate getNoteDate() {
        return noteDate;
    }

    public void setNoteDate(LocalDate noteDate) {
        this.noteDate = noteDate;
    }

    @Override
    public String toString() {
        return "Session #" + sessionID + " — Score " + satisfactionScore + "/10";
    }
}
