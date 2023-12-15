package Storage;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class ServerStorage {
    public static void setup() {
        try {
            Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/", "root", "");
            Statement stmt = conn.createStatement();

            // Create the database
            stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS perudo");

            // Select the database
            stmt.execute("USE perudo");

            // Create the 'lobby' table
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS lobby " +
                    "(lobby_id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");

            // Create the 'player' table
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS token " +
                    "(token_id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "lobby_id INT," +
                    "token VARCHAR(255), " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "FOREIGN KEY (lobby_id) REFERENCES lobby(lobby_id))");

            System.out.println("[SERVER DATABASE]: Online");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
