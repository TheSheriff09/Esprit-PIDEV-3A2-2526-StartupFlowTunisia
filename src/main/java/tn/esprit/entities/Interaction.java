package tn.esprit.entities;

import java.time.LocalDateTime;

public class Interaction {
    private int id;
    private int postId;
    private int userId;
    private String type; // LIKE, LOVE, HAHA
    private LocalDateTime createdAt;

    public Interaction() {
    }

    public Interaction(int postId, int userId, String type) {
        this.postId = postId;
        this.userId = userId;
        this.type = type;
        this.createdAt = LocalDateTime.now();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getPostId() {
        return postId;
    }

    public void setPostId(int postId) {
        this.postId = postId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
