package tn.esprit.utils;

import java.sql.Connection;
import java.sql.Statement;

// NEW FILE
public class DatabaseUpgradeService {

    public static void upgrade(Connection cnx) {
        if (cnx == null)
            return;
        try (Statement st = cnx.createStatement()) {

            try {
                st.execute("ALTER TABLE session ADD COLUMN successProbability DOUBLE DEFAULT 0.0");
                System.out.println("✅ Added successProbability to session table");
            } catch (Exception e) {
                // Column likely already exists
            }

            try {
                st.execute("ALTER TABLE session_feedback ADD COLUMN sentiment VARCHAR(20) DEFAULT 'Neutral'");
                System.out.println("✅ Added sentiment to session_feedback table");
            } catch (Exception e) {
                // Column likely already exists
            }

            // Entrepreneur notes table
            try {
                st.execute("""
                            CREATE TABLE IF NOT EXISTS session_notes (
                                noteID INT AUTO_INCREMENT PRIMARY KEY,
                                sessionID INT NOT NULL,
                                entrepreneurID INT NOT NULL,
                                satisfactionScore INT DEFAULT 5,
                                notes TEXT NOT NULL,
                                noteDate DATE NOT NULL
                            )
                        """);
                System.out.println("✅ session_notes table ready");
            } catch (Exception e) {
                System.out.println("❌ session_notes table error: " + e.getMessage());
            }

        } catch (Exception e) {
            System.out.println("❌ DB upgrade error: " + e.getMessage());
        }
    }
}
