package tn.esprit.plugins;

public interface IMentorshipPlugin {
    String getName();

    String getVersion();

    void start();

    void stop();
}
