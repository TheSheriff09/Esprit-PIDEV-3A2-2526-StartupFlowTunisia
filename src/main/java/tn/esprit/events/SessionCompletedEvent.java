package tn.esprit.events;

import tn.esprit.entities.Session;

// NEW FILE
public class SessionCompletedEvent {
    private final Session session;

    public SessionCompletedEvent(Session session) {
        this.session = session;
    }

    public Session getSession() {
        return session;
    }
}
