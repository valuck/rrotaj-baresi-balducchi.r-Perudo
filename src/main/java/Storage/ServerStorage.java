package Storage;

import com.mysql.cj.Query;
import com.mysql.cj.jdbc.exceptions.CommunicationsException;

import javax.swing.text.TabableView;
import java.sql.*;
import java.util.LinkedList;
import java.util.UUID;

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
                    "updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                    "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP)");

            // Creates the 'token' table
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS tokens " +
                    "(token_id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "lobby_id INT NOT NULL, " +
                    "join_order INT NOT NULL, " +
                    "token_value VARCHAR(255) NOT NULL UNIQUE, " +
                    "dice_value INT DEFAULT 5 CHECK (dice_value >= 0), " +
                    "updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                    "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                    "FOREIGN KEY (lobby_id) REFERENCES lobbies(lobby_id) " +
                    "   ON UPDATE CASCADE " +
                    "   ON DELETE CASCADE)");

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

    public static boolean updateTable(String tableName, int recordId) {
        try {
            String query = "UPDATE lobbies SET updated_at = ? WHERE lobby_id = ?";
            PreparedStatement newState = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
            newState.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
            newState.setInt(2, recordId);
            int results = newState.executeUpdate();
            newState.close();

            return results > 0;
        } catch (Exception e) {
            System.err.println("Error while updating table:");
            e.printStackTrace();
        }

        return false;
    }

    public static LinkedList<Integer> getLobbies() {
        initCheck();
        LinkedList<Integer> ids = new LinkedList<Integer>();

        try {
            String query = "SELECT lobby_id, updated_at AS 'id', 'date' FROM lobbies";
            PreparedStatement newState = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
            ResultSet resultSet = newState.executeQuery();

            long currentTime = new Timestamp(System.currentTimeMillis()).getTime();
            long maxTime = 60*60*1000;

            while (resultSet.next()) {
                Timestamp created = resultSet.getTimestamp("date");
                int lobbyId = resultSet.getInt("id");

                if (currentTime - created.getTime() > maxTime)
                    deleteLobby(lobbyId);
                else
                    ids.add(lobbyId);
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

            updateTable("lobbies", lobbyId);
            return results > 0;
        } catch (SQLIntegrityConstraintViolationException e) {
            try {
                String query = "UPDATE lobbies SET shift_index = 1 WHERE lobby_id = ?";
                PreparedStatement newState = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
                newState.setInt(1, lobbyId);
                int results = newState.executeUpdate();
                newState.close();

                updateTable("lobbies", lobbyId);
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

    public static int getLobbyShift(int lobbyId) {
        initCheck();
        int index = -1;

        try {
            String query = "SELECT shift_index AS 'index' FROM lobbies WHERE lobby_id = ?";
            PreparedStatement newState = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
            newState.setInt(1, lobbyId);
            ResultSet resultSet = newState.executeQuery();

            if (resultSet.next()) {
                index = resultSet.getInt("index");
            }

            newState.close();
        } catch (Exception e) {
            System.err.println("Error while getting the lobbies list from the database:");
            e.printStackTrace();
        }

        return index;
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

    public static String newToken(int lobbyId) {
        initCheck();
        String result = null;
        int index = getTokensInLobby(lobbyId).size() +1;

        try {
            String query = "INSERT INTO tokens (lobby_id, join_order, token_value) VALUES (?, ?, ?)";
            PreparedStatement newState = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
            String uuid = UUID.randomUUID().toString();
            newState.setInt(1, lobbyId);
            newState.setInt(2, index);
            newState.setString(3, uuid);

            if (newState.executeUpdate() > 0) {
                result = uuid;
            }

            newState.close();
        } catch (Exception e) {
            System.err.println("Error while creating a new token in the database:");
            e.printStackTrace();
        }

        return result;
    }

    public static LinkedList<String> getTokensInLobby(int lobbyId) {
        initCheck();
        LinkedList<String> tokens = new LinkedList<>();

        try {
            String query = "SELECT token_value AS 'token' FROM tokens WHERE lobby_id = ?";
            PreparedStatement newState = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
            newState.setInt(1, lobbyId);
            ResultSet resultSet = newState.executeQuery();
            
            while (resultSet.next()) {
                tokens.add(resultSet.getString("token"));
            }

            newState.close();
        } catch (Exception e) {
            System.err.println("Error while getting the tokens list from the database:");
            e.printStackTrace();
        }

        return tokens;
    }

    public static boolean removeToken(String token) {
        initCheck();

        try {
            String query = "DELETE FROM tokens WHERE token_value = ?";
            PreparedStatement newState = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);

            newState.setString(1, token);
            int results = newState.executeUpdate();
            newState.close();

            return results > 0;
        } catch (Exception e) {
            System.err.println("Error while deleting the token " + token + " from the database:");
            e.printStackTrace();
        }

        return false;
    }

    public static int getDice(String token) {
        initCheck();
        int count = -1;

        try {
            String query = "SELECT dice_value AS 'dice' FROM tokens WHERE token_value = ?";
            PreparedStatement newState = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
            newState.setString(1, token);
            ResultSet resultSet = newState.executeQuery();

            if (resultSet.next()) {
                count = resultSet.getInt("dice");
            }

            newState.close();
        } catch (Exception e) {
            System.err.println("Error while getting the dice count from token " + token);
            e.printStackTrace();
        }

        return count;
    }

    public static boolean incraseDice(String token, int increment) {
        initCheck();

        try {
            String query = "UPDATE tokens SET dice_value = dice_value + ? WHERE token_value = ?";
            PreparedStatement newState = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
            newState.setInt(1, increment);
            newState.setString(2, token);
            boolean result = newState.executeUpdate() > 0;

            if (result) {
                ResultSet generatedKeys = newState.getGeneratedKeys();
                if (generatedKeys.next()) {
                    updateTable("tokens", generatedKeys.getInt("token_id"));
                    return true;
                }
            }

            newState.close();
            return result;
        } catch (SQLIntegrityConstraintViolationException e) {
            try {
                String query = "UPDATE tokens SET dice_value = 0 WHERE token_value = ?";
                PreparedStatement newState = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
                newState.setString(1, token);
                boolean result = newState.executeUpdate() > 0;

                if (result) {
                    ResultSet generatedKeys = newState.getGeneratedKeys();
                    if (generatedKeys.next()) {
                        updateTable("tokens", generatedKeys.getInt("token_id"));
                    }
                }

                newState.close();
                return result;
            } catch (Exception e2) {
                System.err.println("Error resetting dice value in token " + token);
                e2.printStackTrace();
            }
        } catch (Exception e) {
            System.err.println("Error updating dice value in token " + token);
            e.printStackTrace();
        }

        return false;
    }
}
