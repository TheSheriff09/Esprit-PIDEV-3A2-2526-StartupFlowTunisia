package tn.esprit.entities;

import java.sql.Timestamp;

public class Reclamation {
    private int id;
    private String title;
    private String description;
    private String status;
    private Timestamp createdAt;
    private int requestedId;
    private Integer targetId;

    public Reclamation(String type, String desc, int requestedId, Integer targetId) {
        this.title = type;
        this.description = desc;
        this.requestedId = requestedId;
        this.targetId = targetId;
        this.status = "OPEN";
    }

    public Reclamation(int id, String title, String description, String status,
                       Timestamp createdAt, int requestedId, Integer targetId) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.status = status;
        this.createdAt = createdAt;
        this.requestedId = requestedId;
        this.targetId = targetId;
    }

    public Reclamation(String unfairEvaluation, String title, String description, int requestedId, Integer targetId) {
        this.title = title;
        this.description = description;
        this.requestedId = requestedId;
        this.targetId = targetId;
        this.status = "OPEN";
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public int getRequestedId() { return requestedId; }
    public void setRequestedId(int requestedId) { this.requestedId = requestedId; }

    public Integer getTargetId() { return targetId; }
    public void setTargetId(Integer targetId) { this.targetId = targetId; }


    @Override
    public String toString() {
        return "Reclamation{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", status='" + status + '\'' +
                ", requestedId=" + requestedId +
                ", targetId=" + targetId +
                ", createdAt=" + createdAt +
                '}';
    }
}