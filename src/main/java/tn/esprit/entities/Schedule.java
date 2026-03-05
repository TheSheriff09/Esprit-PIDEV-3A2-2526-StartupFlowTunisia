package tn.esprit.entities;

import java.time.LocalDate;
import java.time.LocalTime;

public class Schedule {

    private int scheduleID;
    private int mentorID;
    private LocalDate availableDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private boolean isBooked;

    public Schedule() {}

    public Schedule(int mentorID, LocalDate availableDate,
                    LocalTime startTime, LocalTime endTime) {
        this.mentorID      = mentorID;
        this.availableDate = availableDate;
        this.startTime     = startTime;
        this.endTime       = endTime;
        this.isBooked      = false;
    }

    public Schedule(int scheduleID, int mentorID, LocalDate availableDate,
                    LocalTime startTime, LocalTime endTime, boolean isBooked) {
        this.scheduleID    = scheduleID;
        this.mentorID      = mentorID;
        this.availableDate = availableDate;
        this.startTime     = startTime;
        this.endTime       = endTime;
        this.isBooked      = isBooked;
    }

    public int getScheduleID()                { return scheduleID; }
    public void setScheduleID(int v)          { this.scheduleID = v; }
    public int getMentorID()                  { return mentorID; }
    public void setMentorID(int v)            { this.mentorID = v; }
    public LocalDate getAvailableDate()       { return availableDate; }
    public void setAvailableDate(LocalDate v) { this.availableDate = v; }
    public LocalTime getStartTime()           { return startTime; }
    public void setStartTime(LocalTime v)     { this.startTime = v; }
    public LocalTime getEndTime()             { return endTime; }
    public void setEndTime(LocalTime v)       { this.endTime = v; }
    public boolean isBooked()                 { return isBooked; }
    public void setBooked(boolean v)          { this.isBooked = v; }

    @Override
    public String toString() {
        return availableDate + " " + startTime + "-" + endTime + (isBooked ? " [BOOKED]" : " [FREE]");
    }
}
