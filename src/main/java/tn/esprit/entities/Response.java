package tn.esprit.entities;

import java.sql.Timestamp;

public class Response {

    private int id;
    private String content;
    private Timestamp createdAt;
    private int reclamationId;
    private int responderUserId;

    public Response(String content, int reclamationId, int responderUserId) {
        this.content = content;
        this.reclamationId = reclamationId;
        this.responderUserId = responderUserId;
    }

    public Response(int id, String content, Timestamp createdAt, int reclamationId, int responderUserId) {
        this.id = id;
        this.content = content;
        this.createdAt = createdAt;
        this.reclamationId = reclamationId;
        this.responderUserId = responderUserId;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public int getReclamationId() { return reclamationId; }
    public void setReclamationId(int reclamationId) { this.reclamationId = reclamationId; }

    public int getResponderUserId() { return responderUserId; }
    public void setResponderUserId(int responderUserId) { this.responderUserId = responderUserId; }

    @Override
    public String toString() {
        return "Response{" +
                "id=" + id +
                ", content='" + content + '\'' +
                ", createdAt=" + createdAt +
                ", reclamationId=" + reclamationId +
                ", responderUserId=" + responderUserId +
                '}';
    }
}
