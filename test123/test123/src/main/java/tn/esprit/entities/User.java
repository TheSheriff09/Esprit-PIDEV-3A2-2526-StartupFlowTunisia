package tn.esprit.entities;

import java.sql.Timestamp;

public class User {

    private int id;
    private String fullName;
    private String email;
    private String passwordHash;
    private String role;
    private String status;
    private String mentorExpertise;
    private Timestamp createdAt;

    public User() {}

    public User(String fullName, String email, String passwordHash,
                String role, String status, String mentorExpertise) {
        this.fullName        = fullName;
        this.email           = email;
        this.passwordHash    = passwordHash;
        this.role            = role;
        this.status          = status;
        this.mentorExpertise = mentorExpertise;
    }

    public User(int id, String fullName, String email, String passwordHash,
                String role, String status, String mentorExpertise, Timestamp createdAt) {
        this.id              = id;
        this.fullName        = fullName;
        this.email           = email;
        this.passwordHash    = passwordHash;
        this.role            = role;
        this.status          = status;
        this.mentorExpertise = mentorExpertise;
        this.createdAt       = createdAt;
    }

    public int getId()                        { return id; }
    public void setId(int id)                 { this.id = id; }
    public String getFullName()               { return fullName; }
    public void setFullName(String v)         { this.fullName = v; }
    public String getEmail()                  { return email; }
    public void setEmail(String v)            { this.email = v; }
    public String getPasswordHash()           { return passwordHash; }
    public void setPasswordHash(String v)     { this.passwordHash = v; }
    public String getRole()                   { return role; }
    public void setRole(String v)             { this.role = v; }
    public String getStatus()                 { return status; }
    public void setStatus(String v)           { this.status = v; }
    public String getMentorExpertise()        { return mentorExpertise; }
    public void setMentorExpertise(String v)  { this.mentorExpertise = v; }
    public Timestamp getCreatedAt()           { return createdAt; }
    public void setCreatedAt(Timestamp v)     { this.createdAt = v; }

    @Override
    public String toString() { return fullName + " (ID: " + id + ")"; }
}
