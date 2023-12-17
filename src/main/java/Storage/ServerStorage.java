package Storage;

import com.mysql.cj.jdbc.exceptions.CommunicationsException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class ServerStorage {
    private static Connection conn;
    private static Statement stmt;

    public static boolean setup() {
        try {
            conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/", "root", "");
            stmt = conn.createStatement();

            // Creates the database
            stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS perudo");

            // Selects the database
            stmt.execute("USE perudo");

            // Creates the 'lobby' table
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS lobby " +
                    "(lobby_id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");

            // Creates the 'token' table
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS token " +
                    "(token_id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "lobby_id INT NOT NULL," +
                    "token VARCHAR(255) NOT NULL, " +
                    "dice INT DEFAULT 5 CHECK (dice >= 0), " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "FOREIGN KEY (lobby_id) REFERENCES lobby(lobby_id) " +
                    "ON UPDATE CASCADE ON DELETE CASCADE)");

            System.out.println("[SERVER DATABASE]: Online");
            return true;
        } catch (CommunicationsException e) {
            System.err.println("Unable to connect the server to the database.");
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    public static int newLobby() {
        if (conn == null || stmt == null)
            throw new RuntimeException("Server database not initialized.");

        return 0;
    }
}
