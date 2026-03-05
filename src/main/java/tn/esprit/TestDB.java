package tn.esprit;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class TestDB {
    public static void main(String[] args) {
        String url = "jdbc:mysql://localhost:3306/startupflow";
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.out.println("Driver not found");
        }
        try (Connection cnx = DriverManager.getConnection(url, "root", "")) {
            System.out.println("--- DB: " + url + " ---");
            try (Statement st = cnx.createStatement();
                    ResultSet rs = st.executeQuery("DESCRIBE fundingevaluation")) {
                while (rs.next()) {
                    System.out.printf("%s \t %s \t %s \t %s \t %s \t %s\n",
                            rs.getString("Field"),
                            rs.getString("Type"),
                            rs.getString("Null"),
                            rs.getString("Key"),
                            rs.getString("Default"),
                            rs.getString("Extra"));
                }
            } catch (Exception e) {
                System.out.println("Error reading schema: " + e.getMessage());
            }
        } catch (Exception e) {
            System.out.println("Could not connect: " + e.getMessage());
        }
    }
}
