package tn.esprit.utils;

public class NavContext {
    public static String backFxml = "/EntrepreneurDashboard.fxml";

    public static void setBack(String fxml) {
        if (fxml != null && !fxml.trim().isEmpty()) backFxml = fxml;
    }
}