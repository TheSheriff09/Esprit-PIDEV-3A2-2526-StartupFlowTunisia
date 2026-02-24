package tn.esprit.entities;

import java.time.LocalDateTime;

public class Comment {
    private int id;
    private String content;
    private LocalDateTime createdAt;
    private int postId; // foreign key to ForumPost

    public Comment() {}

    public Comment(int id, String content, LocalDateTime createdAt, int postId) {
        this.id = id;
        this.content = content;
        this.createdAt = createdAt;
        this.postId = postId;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public int getPostId() { return postId; }
    public void setPostId(int postId) { this.postId = postId; }

    @Override
    public String toString() {
        return "Comment{" +
                "id=" + id +
                ", content='" + content + '\'' +
                ", createdAt=" + createdAt +
                ", postId=" + postId +
                '}';
    }
}
