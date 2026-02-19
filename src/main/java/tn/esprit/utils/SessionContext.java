package tn.esprit.utils;

import tn.esprit.entities.User;

/**
 * Application-wide logged-in user context.
 * Set once at login; read by every controller to know role and ID.
 */
public class SessionContext {

    private static User currentUser;

    private SessionContext() {
    }

    public static User getUser() {
        return currentUser;
    }

    public static void setUser(User u) {
        currentUser = u;
    }

    public static void clear() {
        currentUser = null;
    }

    public static boolean isMentor() {
        return currentUser != null && "mentor".equalsIgnoreCase(currentUser.getRole());
    }

    public static boolean isEntrepreneur() {
        return currentUser != null
                && ("evaluator".equalsIgnoreCase(currentUser.getRole())
                        || "entrepreneur".equalsIgnoreCase(currentUser.getRole()));
    }

    public static int getUserId() {
        return currentUser == null ? 0 : currentUser.getId();
    }
}
