package tn.esprit.entities;

import java.time.LocalDate;

public class Session {

    private int sessionID;
    private int mentorID;
    private int entrepreneurID;
    private int startupID;
    private int scheduleID;
    private LocalDate sessionDate;
    private String sessionType;
    private String status;
    private String notes;
    private double successProbability;

    public Session() {
    }

    public Session(int mentorID, int entrepreneurID, int startupID,
            LocalDate sessionDate, String sessionType, String notes) {
        this.mentorID = mentorID;
        this.entrepreneurID = entrepreneurID;
        this.startupID = startupID;
        this.sessionDate = sessionDate;
        this.sessionType = sessionType;
        this.status = "planned";
        this.notes = notes;
    }

    public Session(int sessionID, int mentorID, int entrepreneurID, int startupID,
            int scheduleID, LocalDate sessionDate, String sessionType,
            String status, String notes) {
        this.sessionID = sessionID;
        this.mentorID = mentorID;
        this.entrepreneurID = entrepreneurID;
        this.startupID = startupID;
        this.scheduleID = scheduleID;
        this.sessionDate = sessionDate;
        this.sessionType = sessionType;
        this.status = status;
        this.notes = notes;
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

    public int getEntrepreneurID() {
        return entrepreneurID;
    }

    public void setEntrepreneurID(int v) {
        this.entrepreneurID = v;
    }

    public int getStartupID() {
        return startupID;
    }

    public void setStartupID(int v) {
        this.startupID = v;
    }

    public int getScheduleID() {
        return scheduleID;
    }

    public void setScheduleID(int v) {
        this.scheduleID = v;
    }

    public LocalDate getSessionDate() {
        return sessionDate;
    }

    public void setSessionDate(LocalDate v) {
        this.sessionDate = v;
    }

    public String getSessionType() {
        return sessionType;
    }

    public void setSessionType(String v) {
        this.sessionType = v;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String v) {
        this.status = v;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String v) {
        this.notes = v;
    }

    public double getSuccessProbability() {
        return successProbability;
    }

    public void setSuccessProbability(double v) {
        this.successProbability = v;
    }

    @Override
    public String toString() {
        return "Session{id=" + sessionID + ", date=" + sessionDate + ", status=" + status + "}";
    }
}
