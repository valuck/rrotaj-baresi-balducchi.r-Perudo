package org.perudo;

import Messaging.Message;
import Messaging.SignatureException;
import Messaging.User;
import com.google.gson.internal.LinkedTreeMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.util.LinkedHashMap;
import java.util.LinkedList;

import static java.lang.StringTemplate.STR;

public class ClientHandler implements Runnable {
    private final Logger logger = LogManager.getLogger(ClientHandler.class);
    private final Socket clientSocket;
    private final User user = new User();
    private BufferedReader in;
    private PrintWriter out;
    private boolean closed;

    public ClientHandler(Socket clientSocket) {
        // Handle a new client connection in a new thread
        this.clientSocket = clientSocket;

        try {
            // Initialize input and output
            this.in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            this.out = new PrintWriter(clientSocket.getOutputStream(), true);

            // Link the handler to the user
            this.user.setHandler(this);
        } catch (Exception e) {
            logger.error("Error while initializing the client:", e);
            this.close();
        }
    }

    @Override
    public void run() {
        try {
            String inputLine;

            // Checks for a message input
            while ((inputLine = in.readLine()) != null) {
                String finalInputLine = inputLine;
                new Thread(() -> {
                    // Setup response message
                    LinkedHashMap<String, Object> newData = new LinkedHashMap<>();
                    Message response = null;

                    try {
                        // Get the received message
                        System.out.println(STR."Received from client: \{finalInputLine}");

                        boolean toEncode = false;
                        Message message = new Message(user, finalInputLine);

                        // Set encoding key for RSA encryption if present in the message
                        String encoder = message.getEncodingKey();
                        if (encoder != null) {
                            user.setEncodingKey(encoder);
                            toEncode = true; // If the message is encoded, reply with an encoded message
                        }

                        // Get required data
                        String scope = message.getScope();
                        Object data = message.getData();

                        if (scope != null) {
                            switch (scope) {
                                case "Connection": {
                                    toEncode = true; // Allows the client to get the encoding key
                                    newData.put("Success", true); // Build response
                                    break;
                                }

                                case "Ping": { // Allows the client to check for the server presence
                                    newData.put("Success", true);
                                    break;
                                }

                                case "Login": {
                                    if (data == null) { // Data is required
                                        newData.put("Error", "Missing data");
                                        break;
                                    }

                                    // Get the data
                                    LinkedTreeMap<String, String> info = (LinkedTreeMap<String, String>) data;
                                    String missing = "Missing:";

                                    if (info.containsKey("Username")) // Set the username or set error
                                        user.setUsername(info.get("Username").trim());
                                    else
                                        missing = STR."\{missing} Username: String";

                                    if (info.containsKey("LastToken")) { // Allow to log back in a lobby if disconnected
                                        user.setCurrentToken(info.get("LastToken"));
                                        // Put lobby check here!
                                    }

                                    // Build response
                                    newData.put("Success", missing.equals("Missing:"));
                                    newData.put("Error", missing.equals("Missing:") ? null : missing);
                                    break;
                                }

                                case "Lobbies": {
                                    user.disconnectFromLobby();

                                    // Separate each type of lobby
                                    LinkedTreeMap<String, String> publicLobbies = new LinkedTreeMap<>();
                                    LinkedTreeMap<String, String> privateLobbies = new LinkedTreeMap<>();

                                    // Fetch all the lobbies
                                    LinkedTreeMap<Integer, Game> lobbies = Game.getLobbies();
                                    lobbies.forEach((key, value) -> {
                                        String name = STR."\{value.getName()} (\{value.getPlayers().size()}/\{value.getSize()})";

                                        if (value.hasPassword()) // Identify the lobby type
                                            privateLobbies.put(key.toString(), name);
                                        else
                                            publicLobbies.put(key.toString(), name);
                                    });

                                    // Build a response
                                    newData.put("Success", true);
                                    newData.put("Public", publicLobbies);
                                    newData.put("Private", privateLobbies);
                                    break;
                                }

                                case "Create": {
                                    // Check if the user is logged in correctly
                                    if (user.getUsername() == null) {
                                        newData.put("Error", "Not logged in");
                                        break;
                                    }

                                    // Cast and check for data
                                    LinkedTreeMap<String, Object> casted = (LinkedTreeMap) data;
                                    if (data == null || !casted.containsKey("Size")) {
                                        newData.put("Error", "Missing data");
                                        break;
                                    }

                                    String password = null; // Check for a custom password
                                    if (casted.containsKey("Password"))
                                        password = (String) casted.get("Password");

                                    // Create the actual lobby
                                    new Game(((Number) casted.get("Size")).intValue(), user, password);
                                    newData.put("Success", true);
                                    break;
                                }

                                case "Join": {
                                    // Check if the user is logged in correctly
                                    if (user.getUsername() == null) {
                                        newData.put("Error", "Not logged in");
                                        break;
                                    }

                                    // Cast and check for data
                                    LinkedTreeMap<String, Object> casted = (LinkedTreeMap) data;
                                    if (data == null || !casted.containsKey("Lobby")) { // Data is required
                                        newData.put("Error", "Missing data");
                                        break;
                                    }

                                    // Check for the requested lobby
                                    Game lobby = Game.getByLobbyId(Integer.parseInt((String) casted.get("Lobby")));
                                    if (lobby != null) {
                                        boolean flagged = false;
                                        for (User player : lobby.getPlayers()) { // Check if another player is using the same name
                                            if (player.getUsername().equalsIgnoreCase(user.getUsername())) {
                                                newData.put("Error", "Username already in use in this lobby");
                                                flagged = true;
                                                break;
                                            }
                                        }

                                        if (flagged) // Exit
                                            break;

                                        // Join the lobby
                                        String token = lobby.join(user, (String) casted.get("Password"));

                                        if (token == null) // If the user cannot join
                                            newData.put("Error", "Not authorized to join");
                                        else {
                                            newData.put("Success", true);
                                            newData.put("Token", token);
                                            // Update member list on other clients
                                        }
                                    } else
                                        newData.put("Error", "Lobby not found");

                                    break;
                                }

                                case "Start": {
                                    // Check if the user is in a lobby
                                    Game lobby = user.getLobby();
                                    if (lobby == null) {
                                        newData.put("Error", "Not in a lobby");
                                        return;
                                    }

                                    // Check if the user is the host of the lobby
                                    if (lobby.getHost() != user) {
                                        newData.put("Error", "Not authorized");
                                        return;
                                    }

                                    // Start the game
                                    lobby.startGame();
                                    newData.put("Success", true);
                                    break;
                                }

                                case "Choice": {
                                    // Check if the user is in a lobby
                                    Game lobby = user.getLobby();
                                    if (lobby == null) {
                                        newData.put("Error", "Not in a lobby");
                                        return;
                                    }

                                    // Process the new values picked by the player
                                    LinkedTreeMap<String, Object> casted = (LinkedTreeMap) data;
                                    if (casted.containsKey("Amount") && casted.containsKey("Value") && casted.containsKey("Jolly")) // formulate a response by using the result of the processed data
                                        newData.put("Success", lobby.processPicks(user, ((Number) casted.get("Amount")).intValue(), ((Number) casted.get("Value")).intValue(), (Boolean) casted.get("Jolly")));
                                    else
                                        newData.put("Error", "Missing data");

                                    break;
                                }

                                case "Dudo": {
                                    // Check if the user is in a lobby
                                    Game lobby = user.getLobby();
                                    if (lobby == null) {
                                        newData.put("Error", "Not in a lobby");
                                        return;
                                    }

                                    // Process dudo action
                                    newData.put("Success", lobby.processPicks(user, 0, 7, false));
                                    break;
                                }

                                case "Sock": {
                                    // Check if the user is in a lobby
                                    Game lobby = user.getLobby();
                                    if (lobby == null) {
                                        newData.put("Error", "Not in a lobby");
                                        return;
                                    }

                                    // Process calza action
                                    newData.put("Success", lobby.processPicks(user, 0, 8, false));
                                    break;
                                }

                                default: {
                                    newData.put("Error", "Invalid scope");
                                }
                            }
                        } else {
                            // Build error response
                            newData.put("Error", "Missing scope");
                        }

                        response = new Message(user, scope, newData, toEncode);
                    } catch (SignatureException e) {
                        // Build signature exception response
                        newData.put("Error", e.getMessage());
                    } catch (Exception e) {
                        logger.error("Error while processing the client message", e);

                        // Build general exception response
                        newData.put("Error", "Server exception!");
                    }

                    if (!newData.containsKey("Success")) // Set success as false by default
                        newData.put("Success", false);

                    if (response == null) // build an exception message
                        response = new Message(user, "Exception", newData, false);

                    // send response message
                    out.println(response.toJson());
                }).start();
            }
        } catch (SocketException e) {
            // Socket is closing, ignore.

        } catch (Exception e) {
            logger.error("Error while handling the client", e);
        }

        // Closing the handler due to client disconnection
        logger.info("Client disconnected");

        // Put disconnection check here!
        if (!this.closed) {
            Game lobby = this.user.getLobby();
            if (lobby != null) // Remove the player from the lobby if he was playing
                lobby.disconnect(this.user);
        }

        this.close();
        logger.info("Client handler closed");
    }

    public User getUser() {
        return this.user;
    }

    public void sendMessage(String scope, Object data, boolean encode) {
        // Send a new message to the handled client
        Message message = new Message(this.user, scope, data, encode);
        out.println(message.toJson());
    }

    public void close() {
        if (this.closed)
            return;

        try {
            // Remove handled client from the user list
            User.removeByHandler(this);
            logger.warn("Client handler closing");
            closed = true;

            // Close the socket, input & output
            if (this.clientSocket != null && !this.clientSocket.isClosed())
                this.clientSocket.close();

            if (this.out != null)
                this.out.close();

            if (this.in != null)
                this.in.close();

        } catch (Exception e) {
            logger.error("Error while closing clientSocket", e);
        }
    }

    public static void replicateMessage(String scope, Object data, boolean encode) { // Sends the message to all the users
        LinkedList<User> users = User.getUsers();

        // Send a new message to all the connected users
        for (int i = 0; i < users.size(); i++) { // foreach loops can give an error
            User user = users.get(i);
            ClientHandler handler = user.getHandler();

            if (handler != null) // if the user has a valid handler
                handler.sendMessage(scope, data, encode);
        }
    }
}
