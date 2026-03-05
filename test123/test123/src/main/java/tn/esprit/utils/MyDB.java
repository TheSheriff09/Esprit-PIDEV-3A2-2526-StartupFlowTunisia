package tn.esprit.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyDB {

    private static MyDB instance;
    private Connection cnx;

    private static final String URL = "jdbc:mysql://localhost:3306/startupflow_sarah";
    private static final String USER = "root";
    private static final String PASS = "";

    private MyDB() {
        try {
            cnx = DriverManager.getConnection(URL, USER, PASS);
            System.out.println("✅ Connected to database!");
            DatabaseUpgradeService.upgrade(cnx);
            BackupService.startAutoBackup();
        } catch (SQLException e) {
            System.out.println("❌ DB connection error: " + e.getMessage());
        }
    }

    public static MyDB getInstance() {
        if (instance == null) {
            instance = new MyDB();
        }
        return instance;
    }

    public Connection getCnx() {
        return cnx;
    }
}