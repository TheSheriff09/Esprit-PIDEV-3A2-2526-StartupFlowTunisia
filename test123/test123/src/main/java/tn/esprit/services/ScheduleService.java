package tn.esprit.services;

import tn.esprit.entities.Schedule;
import tn.esprit.utils.MyDB;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ScheduleService implements ICRUD<Schedule> {

    private final Connection cnx;

    public ScheduleService() {
        cnx = MyDB.getInstance().getCnx();
    }

    @Override
    public Schedule add(Schedule s) {
        if (s.getMentorID() <= 0)
            throw new IllegalArgumentException("Mentor ID is required.");
        if (s.getAvailableDate() == null)
            throw new IllegalArgumentException("Date is required.");
        if (s.getAvailableDate().isBefore(LocalDate.now()))
            throw new IllegalArgumentException("Date cannot be in the past.");
        if (s.getStartTime() == null)
            throw new IllegalArgumentException("Start time is required.");
        if (s.getEndTime() == null)
            throw new IllegalArgumentException("End time is required.");
        if (!s.getEndTime().isAfter(s.getStartTime()))
            throw new IllegalArgumentException("End time must be after start time.");

        String sql = "INSERT INTO schedule (mentorID, availableDate, startTime, endTime, isBooked) VALUES (?,?,?,?,?)";
        try (PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, s.getMentorID());
            ps.setDate(2, Date.valueOf(s.getAvailableDate()));
            ps.setTime(3, Time.valueOf(s.getStartTime()));
            ps.setTime(4, Time.valueOf(s.getEndTime()));
            ps.setBoolean(5, false);
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next())
                s.setScheduleID(rs.getInt(1));
            return s;
        } catch (SQLException e) {
            throw new RuntimeException("Error adding schedule: " + e.getMessage());
        }
    }

    @Override
    public List<Schedule> list() {
        List<Schedule> list = new ArrayList<>();
        String sql = "SELECT * FROM schedule ORDER BY availableDate, startTime";
        try (Statement st = cnx.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next())
                list.add(mapRow(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Error listing schedules: " + e.getMessage());
        }
        return list;
    }

    @Override
    public void update(Schedule s) {
        if (s.getStartTime() != null && s.getEndTime() != null
                && !s.getEndTime().isAfter(s.getStartTime()))
            throw new IllegalArgumentException("End time must be after start time.");

        String sql = "UPDATE schedule SET mentorID=?, availableDate=?, startTime=?, endTime=?, isBooked=? WHERE scheduleID=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, s.getMentorID());
            ps.setDate(2, Date.valueOf(s.getAvailableDate()));
            ps.setTime(3, Time.valueOf(s.getStartTime()));
            ps.setTime(4, Time.valueOf(s.getEndTime()));
            ps.setBoolean(5, s.isBooked());
            ps.setInt(6, s.getScheduleID());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error updating schedule: " + e.getMessage());
        }
    }

    @Override
    public void delete(Schedule s) {
        if (s.isBooked())
            throw new IllegalStateException("Cannot delete a booked slot.");
        String sql = "DELETE FROM schedule WHERE scheduleID=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, s.getScheduleID());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error deleting schedule: " + e.getMessage());
        }
    }

    /** All free (non-booked) future slots for one mentor, sorted by date+time. */
    public List<Schedule> listAvailableByMentor(int mentorID) {
        List<Schedule> list = new ArrayList<>();
        String sql = "SELECT * FROM schedule WHERE mentorID=? AND isBooked=false AND availableDate >= CURDATE() ORDER BY availableDate, startTime";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, mentorID);
            ResultSet rs = ps.executeQuery();
            while (rs.next())
                list.add(mapRow(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Error listing available slots: " + e.getMessage());
        }
        return list;
    }

    private Schedule mapRow(ResultSet rs) throws SQLException {
        return new Schedule(
                rs.getInt("scheduleID"),
                rs.getInt("mentorID"),
                rs.getDate("availableDate").toLocalDate(),
                rs.getTime("startTime").toLocalTime(),
                rs.getTime("endTime").toLocalTime(),
                rs.getBoolean("isBooked"));
    }
}
