package tn.esprit.events;

import tn.esprit.entities.SessionFeedback;

// NEW FILE
public class FeedbackSubmittedEvent {
    private final SessionFeedback feedback;

    public FeedbackSubmittedEvent(SessionFeedback feedback) {
        this.feedback = feedback;
    }

    public SessionFeedback getFeedback() {
        return feedback;
    }
}
