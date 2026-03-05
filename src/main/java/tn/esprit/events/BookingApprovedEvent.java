package tn.esprit.events;

import tn.esprit.entities.Booking;

// NEW FILE
public class BookingApprovedEvent {
    private final Booking booking;

    public BookingApprovedEvent(Booking booking) {
        this.booking = booking;
    }

    public Booking getBooking() {
        return booking;
    }
}
