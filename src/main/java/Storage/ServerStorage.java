package Storage;

import com.mysql.cj.Query;
import com.mysql.cj.jdbc.exceptions.CommunicationsException;

import java.sql.*;
import java.util.LinkedList;

public class ServerStorage {
    private static Connection conn;

    private static void initCheck() {
        if (conn == null)
            throw new RuntimeException("Server database not initialized.");
    }

    public static boolean setup() {
        if (conn != null)
            throw new RuntimeException("Server database already initialized.");

        try {
            conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/", "root", "");
            Statement stmt = conn.createStatement();

            // Creates the database
            stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS perudo");

            // Selects the database
            stmt.execute("USE perudo");

            // Creates the 'lobby' table
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS lobbies " +
                    "(lobby_id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "lobby_size INT NOT NULL DEFAULT 2 CHECK (lobby_size >= 2 AND lobby_size <= 6), " +
                    "shift_index INT NOT NULL DEFAULT 1 CHECK (shift_index > 0 AND shift_index <= lobby_size), " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");

            // Creates the 'token' table
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS tokens " +
                    "(token_id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "lobby_id INT NOT NULL," +
                    "token VARCHAR(255) NOT NULL, " +
                    "dice INT DEFAULT 5 CHECK (dice >= 0), " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "FOREIGN KEY (lobby_id) REFERENCES lobbies(lobby_id) " +
                    "ON UPDATE CASCADE ON DELETE CASCADE)");

            System.out.println("[SERVER DATABASE]: Online");
            stmt.close();

            return true;
        } catch (CommunicationsException e) {
            System.err.println("Unable to connect the server to the database.");
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    public static boolean eraseDatabase(boolean recreate) {
        initCheck();

        try {
            String query = "DROP DATABASE perudo";
            PreparedStatement newState = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
            newState.executeUpdate();
            newState.close();
            conn.close();
            conn = null;

            if (recreate) {
                setup();
            }

            return true;
        } catch (Exception e) {
            System.err.println("Error while erasing the database:");
            e.printStackTrace();
        }

        return false;
    }

    public static LinkedList<Integer> getLobbies() {
        initCheck();
        LinkedList<Integer> ids = new LinkedList<Integer>();

        try {
            String query = "SELECT lobby_id AS id FROM lobbies";
            PreparedStatement newState = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
            ResultSet resultSet = newState.executeQuery();

            while (resultSet.next()) {
                ids.add(resultSet.getInt("id"));
            }

            newState.close();
        } catch (Exception e) {
            System.err.println("Error while getting the lobbies list from the database:");
            e.printStackTrace();
        }

        return ids;
    }

    public static int newLobby(int lobbySize) {
        initCheck();
        int result = -1;

        try {
            String query = "INSERT INTO lobbies (lobby_size) VALUES (?)";
            PreparedStatement newState = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
            newState.setInt(1, lobbySize);

            if (newState.executeUpdate() > 0) {
                // Retrieve the generated keys
                ResultSet generatedKeys = newState.getGeneratedKeys();

                if (generatedKeys.next()) {
                    result = generatedKeys.getInt(1);
                }
            }

            newState.close();
        } catch (Exception e) {
            System.err.println("Error while creating a new lobby in the database:");
            e.printStackTrace();
        }

        return result;
    }

    public static boolean incrementLobbyShift(int lobbyId) {
        initCheck();

        try {
            String query = "UPDATE lobbies SET shift_index = shift_index + 1 WHERE lobby_id = ?";
            PreparedStatement newState = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
            newState.setInt(1, lobbyId);
            int results = newState.executeUpdate();
            newState.close();

            return results > 0;
        } catch (SQLIntegrityConstraintViolationException e) {
            try {
                String query = "UPDATE lobbies SET shift_index = 1 WHERE lobby_id = ?";
                PreparedStatement newState = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
                newState.setInt(1, lobbyId);
                int results = newState.executeUpdate();
                newState.close();

                return results > 0;
            } catch (Exception e2) {
                System.err.println("Error resetting shift index in lobby " + lobbyId);
                e2.printStackTrace();
            }
        } catch (Exception e) {
            System.err.println("Error shifting index in lobby " + lobbyId);
            e.printStackTrace();
        }

        return false;
    }

    public static boolean deleteLobby(int lobbyId) {
        initCheck();

        try {
            String query = "DELETE FROM lobbies WHERE lobby_id = ?";
            PreparedStatement newState = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);

            newState.setInt(1, lobbyId);
            int results = newState.executeUpdate();
            newState.close();

            return results > 0;
        } catch (Exception e) {
            System.err.println("Error while deleting the lobby " + lobbyId + " from the database:");
            e.printStackTrace();
        }

        return false;
    }
}
