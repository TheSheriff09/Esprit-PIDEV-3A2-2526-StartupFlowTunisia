package tn.esprit.entities;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalTime;

public class Booking {

    private int bookingID;
    private int entrepreneurID;
    private int mentorID;
    private int startupID;
    private LocalDate requestedDate;
    private LocalTime requestedTime;
    private String topic;
    private String status;
    private Timestamp creationDate;

    public Booking() {}

    public Booking(int entrepreneurID, int mentorID, int startupID,
                   LocalDate requestedDate, LocalTime requestedTime, String topic) {
        this.entrepreneurID = entrepreneurID;
        this.mentorID       = mentorID;
        this.startupID      = startupID;
        this.requestedDate  = requestedDate;
        this.requestedTime  = requestedTime;
        this.topic          = topic;
        this.status         = "pending";
    }

    public Booking(int bookingID, int entrepreneurID, int mentorID, int startupID,
                   LocalDate requestedDate, LocalTime requestedTime,
                   String topic, String status, Timestamp creationDate) {
        this.bookingID      = bookingID;
        this.entrepreneurID = entrepreneurID;
        this.mentorID       = mentorID;
        this.startupID      = startupID;
        this.requestedDate  = requestedDate;
        this.requestedTime  = requestedTime;
        this.topic          = topic;
        this.status         = status;
        this.creationDate   = creationDate;
    }

    public int getBookingID()                    { return bookingID; }
    public void setBookingID(int v)              { this.bookingID = v; }
    public int getEntrepreneurID()               { return entrepreneurID; }
    public void setEntrepreneurID(int v)         { this.entrepreneurID = v; }
    public int getMentorID()                     { return mentorID; }
    public void setMentorID(int v)               { this.mentorID = v; }
    public int getStartupID()                    { return startupID; }
    public void setStartupID(int v)              { this.startupID = v; }
    public LocalDate getRequestedDate()          { return requestedDate; }
    public void setRequestedDate(LocalDate v)    { this.requestedDate = v; }
    public LocalTime getRequestedTime()          { return requestedTime; }
    public void setRequestedTime(LocalTime v)    { this.requestedTime = v; }
    public String getTopic()                     { return topic; }
    public void setTopic(String v)               { this.topic = v; }
    public String getStatus()                    { return status; }
    public void setStatus(String v)              { this.status = v; }
    public Timestamp getCreationDate()           { return creationDate; }
    public void setCreationDate(Timestamp v)     { this.creationDate = v; }

    @Override
    public String toString() {
        return "Booking{id=" + bookingID + ", topic='" + topic + "', status=" + status + "}";
    }
}
