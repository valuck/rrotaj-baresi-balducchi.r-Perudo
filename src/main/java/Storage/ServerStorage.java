package Storage;

import com.mysql.cj.jdbc.exceptions.CommunicationsException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.perudo.Main;

import java.sql.*;
import java.util.LinkedList;
import java.util.UUID;

public class ServerStorage {
    private static final Logger logger = LogManager.getLogger(ServerStorage.class);
    private static Connection conn;

    private static void initCheck() { // throw an exception if the database connection is not initialized
        if (conn == null)
            throw new RuntimeException("Server database not initialized.");
    }

    public static boolean setup() {
        if (conn != null) // throw an exception if the database connection is already initialized
            throw new RuntimeException("Server database already initialized.");

        try {
            // Connect to the local MySql database
            conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/", "root", "");
            Statement stmt = conn.createStatement();

            // Creates the database if it doesn't already exist
            stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS perudo");

            // Selects the database
            stmt.execute("USE perudo");

            // Creates the 'lobby' table if it doesn't already exist
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS lobbies " +
                    "(lobby_id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "lobby_name VARCHAR(255) NOT NULL, " +
                    "lobby_password VARCHAR(255) NOT NULL, " +
                    "lobby_size INT NOT NULL DEFAULT 2 CHECK (lobby_size >= 2 AND lobby_size <= 6), " +
                    "shift_index INT NOT NULL DEFAULT 1 CHECK (shift_index > 0 AND shift_index <= lobby_size), " +
                    "updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                    "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP)");

            // Creates the 'token' table if it doesn't already exist
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

            logger.info("[SERVER DATABASE]: Online");
            stmt.close();

            return true;
        } catch (CommunicationsException e) {
            String message = "Unable to connect the server to the database.";
            logger.error(message, e);
            Main.printMessage(message);
        } catch (Exception e) {
            logger.error("Exception on database initialization", e);
        }

        return false;
    }

    public static void close() {
        if (conn == null)
            return;

        try {
            conn.close();
            conn = null;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean eraseDatabase(boolean recreate) {
        initCheck();

        try {
            String query = "DROP DATABASE perudo"; // Delete the database
            PreparedStatement newState = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
            newState.executeUpdate();
            newState.close();

            if (conn != null)
                conn.close();
            conn = null;

            if (recreate) { // Create a new the database if required
                setup();
            }

            return true;
        } catch (Exception e) {
            logger.error("Error while erasing the database", e);
        }

        return false;
    }

    public static boolean updateTable(String tableName, String keyName, int recordId) {
        try {
            // Update the last time a record of a table has been edited
            String query = STR."UPDATE \{tableName} SET updated_at = ? WHERE \{keyName} = ?";
            PreparedStatement newState = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
            newState.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
            newState.setInt(2, recordId);
            int results = newState.executeUpdate();
            newState.close();

            return results > 0;
        } catch (Exception e) {
            logger.error("Error while updating table", e);
        }

        return false;
    }

    public static LinkedList<Integer> getLobbies() {
        initCheck();
        LinkedList<Integer> ids = new LinkedList<>();

        try {
            // Get all the active lobby ids
            String query = "SELECT lobby_id AS 'id', updated_at AS 'date' FROM lobbies";
            PreparedStatement newState = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
            ResultSet resultSet = newState.executeQuery();

            long currentTime = new Timestamp(System.currentTimeMillis()).getTime();
            long maxTime = 60*60*1000;

            while (resultSet.next()) {
                Timestamp created = resultSet.getTimestamp("date");
                int lobbyId = resultSet.getInt("id");

                // Check if the lobby is still valid, expires after 1 hour
                if (currentTime - created.getTime() > maxTime)
                    deleteLobby(lobbyId); // Delete if expired
                else
                    ids.add(lobbyId);
            }

            newState.close();
        } catch (Exception e) {
            logger.error("Error while getting the lobbies list from the database", e);
        }

        return ids;
    }

    public static int newLobby(String name, int lobbySize, String password) {
        initCheck();
        int result = -1;

        try {
            // Create a new lobby of specified size
            String query = "INSERT INTO lobbies (lobby_name, lobby_size, lobby_password) VALUES (?, ?, ?)";
            PreparedStatement newState = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
            newState.setString(1, name);
            newState.setInt(2, lobbySize);
            newState.setString(3, password == null ? "" : password);

            if (newState.executeUpdate() > 0) {
                ResultSet generatedKeys = newState.getGeneratedKeys();

                if (generatedKeys.next()) { // Get the lobby id
                    result = generatedKeys.getInt(1);
                }
            }

            newState.close();
        } catch (Exception e) {
            logger.error("Error while creating a new lobby in the database", e);
        }

        return result;
    }

    public static int getLobbySize(int lobbyId) {
        initCheck();
        int size = 0;

        try {
            // get the shift that determinate the player that has to play the round
            String query = "SELECT lobby_size AS 'size' FROM lobbies WHERE lobby_id = ?";
            PreparedStatement newState = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
            newState.setInt(1, lobbyId); // of the specified lobby
            ResultSet resultSet = newState.executeQuery();

            if (resultSet.next()) { // get the shift as index
                size = resultSet.getInt("size");
            }

            newState.close();
        } catch (Exception e) {
            logger.error("Error while getting the lobby size from the database", e);
        }

        return size;
    }

    public static String getLobbyName(int lobbyId) {
        initCheck();
        String name = null;

        try {
            // get the shift that determinate the player that has to play the round
            String query = "SELECT lobby_name AS 'name' FROM lobbies WHERE lobby_id = ?";
            PreparedStatement newState = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
            newState.setInt(1, lobbyId); // of the specified lobby
            ResultSet resultSet = newState.executeQuery();

            if (resultSet.next()) { // get the shift as index
                name = resultSet.getString("name");
            }

            newState.close();
        } catch (Exception e) {
            logger.error("Error while getting the lobby name from the database", e);
        }

        return name;
    }

    public static String getLobbyPassword(int lobbyId) {
        initCheck();
        String password = null;

        try {
            // get the shift that determinate the player that has to play the round
            String query = "SELECT lobby_password AS 'password' FROM lobbies WHERE lobby_id = ?";
            PreparedStatement newState = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
            newState.setInt(1, lobbyId); // of the specified lobby
            ResultSet resultSet = newState.executeQuery();

            if (resultSet.next()) { // get the shift as index
                password = resultSet.getString("password");
            }

            newState.close();
        } catch (Exception e) {
            logger.error("Error while getting the lobby password from the database", e);
        }

        return password;
    }

    public static boolean incrementLobbyShift(int lobbyId) {
        initCheck();

        try {
            // increment the shift that determinate the player that has to play the round
            String query = "UPDATE lobbies SET shift_index = shift_index + 1 WHERE lobby_id = ?";
            PreparedStatement newState = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
            newState.setInt(1, lobbyId); // in the specified lobby
            int results = newState.executeUpdate();
            newState.close();

            updateTable("lobbies", "lobby_id", lobbyId); // update last edit time
            return results > 0;

        } catch (SQLIntegrityConstraintViolationException e) {
            try {
                // reset the shift when it exceeds the max players limit of the lobby
                String query = "UPDATE lobbies SET shift_index = 1 WHERE lobby_id = ?";
                PreparedStatement newState = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
                newState.setInt(1, lobbyId); // in the specified lobby
                int results = newState.executeUpdate();
                newState.close();

                updateTable("lobbies", "lobby_id", lobbyId); // update last edit time
                return results > 0;

            } catch (Exception e2) {
                logger.error(STR."Error resetting shift index in lobby \{lobbyId}", e);
            }
        } catch (Exception e) {
            logger.error(STR."Error shifting index in lobby \{lobbyId}", e);
        }

        return false;
    }

    public static int getLobbyShift(int lobbyId) {
        initCheck();
        int index = -1;

        try {
            // get the shift that determinate the player that has to play the round
            String query = "SELECT shift_index AS 'index' FROM lobbies WHERE lobby_id = ?";
            PreparedStatement newState = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
            newState.setInt(1, lobbyId); // of the specified lobby
            ResultSet resultSet = newState.executeQuery();

            if (resultSet.next()) { // get the shift as index
                index = resultSet.getInt("index");
            }

            newState.close();
        } catch (Exception e) {
            logger.error("Error while getting the lobbies list from the database", e);
        }

        return index;
    }

    public static boolean deleteLobby(int lobbyId) {
        initCheck();

        try {
            // delete the lobby specified by its id
            String query = "DELETE FROM lobbies WHERE lobby_id = ?";
            PreparedStatement newState = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);

            newState.setInt(1, lobbyId);
            int results = newState.executeUpdate();
            newState.close();

            return results > 0;
        } catch (Exception e) {
            logger.error(STR."Error while deleting the lobby \{lobbyId} from the database", e);
        }

        return false;
    }

    public static String newToken(int lobbyId) {
        initCheck();
        String result = null;
        int index = getTokensInLobby(lobbyId).size() +1;

        try {
            // create a new unique token for lobby access authorization
            String query = "INSERT INTO tokens (lobby_id, join_order, token_value) VALUES (?, ?, ?)";
            PreparedStatement newState = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
            String uuid = UUID.randomUUID().toString();
            newState.setInt(1, lobbyId); // for the specified lobby
            newState.setInt(2, index); // determinate the user's join order
            newState.setString(3, uuid); // the new unique token

            if (newState.executeUpdate() > 0) {
                result = uuid; // returns the new unique token
            }

            newState.close();
        } catch (Exception e) {
            logger.error("Error while creating a new token in the database", e);
        }

        return result;
    }

    public static int getLobbyByToken(String token) {
        initCheck();
        int lobbyId = -1;

        try {
            // get the lobby that contains the token specified
            String query = "SELECT lobby_id AS 'id' FROM tokens WHERE token_value = ?";
            PreparedStatement newState = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
            newState.setString(1, token); // the specified unique token
            ResultSet resultSet = newState.executeQuery();

            if (resultSet.next()) { // get the lobby id
                lobbyId = resultSet.getInt("id");
            }

            newState.close();
        } catch (Exception e) {
            logger.error("Error while getting the tokens list from the database", e);
        }

        return lobbyId;
    }

    public static LinkedList<String> getTokensInLobby(int lobbyId) {
        initCheck();
        LinkedList<String> tokens = new LinkedList<>();

        try {
            // get the tokens in a specified lobby
            String query = "SELECT token_value AS 'token' FROM tokens WHERE lobby_id = ? ORDER BY join_order";
            PreparedStatement newState = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
            newState.setInt(1, lobbyId); // in the specified lobby
            ResultSet resultSet = newState.executeQuery();
            
            while (resultSet.next()) { // list all the tokens
                tokens.add(resultSet.getString("token"));
            }

            newState.close();
        } catch (Exception e) {
            logger.error("Error while getting the tokens list from the database", e);
        }

        return tokens;
    }

    public static boolean removeToken(String token) {
        initCheck();

        try {
            // remove a token
            String query = "DELETE FROM tokens WHERE token_value = ?";
            PreparedStatement newState = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);

            newState.setString(1, token); // specified unique token
            int results = newState.executeUpdate();
            newState.close();

            return results > 0;
        } catch (Exception e) {
            logger.error(STR."Error while deleting the token \{token} from the database", e);
        }

        return false;
    }

    public static int getDice(String token) {
        initCheck();
        int count = -1;

        try {
            // get the amount of dice the user corresponding to the given token has
            String query = "SELECT dice_value AS 'dice' FROM tokens WHERE token_value = ?";
            PreparedStatement newState = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
            newState.setString(1, token); // specified unique token
            ResultSet resultSet = newState.executeQuery();

            if (resultSet.next()) { // get the amount of dice
                count = resultSet.getInt("dice");
            }

            newState.close();
        } catch (Exception e) {
            logger.error(STR."Error while getting the dice count from token \{token}", e);
        }

        return count;
    }

    public static boolean incrementDice(String token, int increment) {
        initCheck();

        try {
            // increment dice of the user corresponding to the given token
            String query = "UPDATE tokens SET dice_value = dice_value + ? WHERE token_value = ?";
            PreparedStatement newState = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
            newState.setInt(1, increment); // specified increment, can be negative
            newState.setString(2, token); // specified unique token
            boolean result = newState.executeUpdate() > 0;

            if (result) {
                ResultSet generatedKeys = newState.getGeneratedKeys();
                if (generatedKeys.next()) { // update the last edit time
                    updateTable("tokens", "token_id", generatedKeys.getInt("token_id"));
                    return true;
                }
            }

            newState.close();
            return result;
        } catch (SQLIntegrityConstraintViolationException e) {
            try {
                // reset the amount of dice if it goes under the minimum value
                String query = "UPDATE tokens SET dice_value = 0 WHERE token_value = ?";
                PreparedStatement newState = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
                newState.setString(1, token); // specified token
                boolean result = newState.executeUpdate() > 0;

                if (result) {
                    ResultSet generatedKeys = newState.getGeneratedKeys();
                    if (generatedKeys.next()) { // update the last edit time
                        updateTable("tokens", "token_id", generatedKeys.getInt("token_id"));
                    }
                }

                newState.close();
                logger.warn(STR."The dice value of token: \{token} has been reset to its minimum value due to an excessive decrement of its value");
                return result;
            } catch (Exception e2) {
                logger.error(STR."Error resetting dice value in token \{token}", e);
            }
        } catch (Exception e) {
            logger.error(STR."Error updating dice value in token \{token}", e);
        }

        return false;
    }
}
