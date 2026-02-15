package util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBconnection {

    private final String url = "jdbc:mysql://localhost:3306/pi_db_doctor";
    private final String user = "root";
    private final String password = "";
    private static DBconnection instance;

    public static DBconnection getInstance() {
        if (instance == null) {
            instance = new DBconnection();
        }
        return instance;
    }

    // Returns a fresh connection every time — caller closes it via try-with-resources
    public Connection getConn() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    private DBconnection() {
        // Test the connection once at startup just to confirm DB is reachable
        try (Connection test = DriverManager.getConnection(url, user, password)) {
            System.out.println("Connection established");
        } catch (SQLException e) {
            System.out.println("DB connection failed: " + e.getMessage());
        }
    }
}