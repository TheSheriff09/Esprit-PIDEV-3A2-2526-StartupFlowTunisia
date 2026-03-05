package tn.esprit.entities;

import java.sql.Timestamp;

public class SessionTodo {
    private int id;
    private int sessionID;
    private String taskDescription;
    private boolean isDone;
    private Timestamp createdAt;

    public SessionTodo() {
    }

    public SessionTodo(int sessionID, String taskDescription, boolean isDone) {
        this.sessionID = sessionID;
        this.taskDescription = taskDescription;
        this.isDone = isDone;
    }

    public SessionTodo(int id, int sessionID, String taskDescription, boolean isDone, Timestamp createdAt) {
        this.id = id;
        this.sessionID = sessionID;
        this.taskDescription = taskDescription;
        this.isDone = isDone;
        this.createdAt = createdAt;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getSessionID() {
        return sessionID;
    }

    public void setSessionID(int sessionID) {
        this.sessionID = sessionID;
    }

    public String getTaskDescription() {
        return taskDescription;
    }

    public void setTaskDescription(String taskDescription) {
        this.taskDescription = taskDescription;
    }

    public boolean isDone() {
        return isDone;
    }

    public void setDone(boolean done) {
        isDone = done;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}
