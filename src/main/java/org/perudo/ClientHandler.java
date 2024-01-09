package org.perudo;

import Messaging.Message;
import Messaging.SignatureException;
import Messaging.User;
import com.google.gson.internal.LinkedTreeMap;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.util.LinkedHashMap;
import java.util.LinkedList;

import static java.lang.StringTemplate.STR;

public class ClientHandler implements Runnable {
    private final Socket clientSocket;
    private final User user = new User();
    private BufferedReader in;
    private PrintWriter out;

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
            System.err.println("Error while initializing the client");
            e.printStackTrace();

            this.close();
        }
    }

    @Override
    public void run() {
        try {
            String inputLine;

            // Setup response message
            LinkedHashMap<String, Object> newData = new LinkedHashMap<>();
            Message response = null;

            // Checks for a message input
            while ((inputLine = in.readLine()) != null) {
                try {
                    // Get the received message
                    System.out.println(STR."Received from client: \{inputLine}");

                    boolean toEncode = false;
                    Message message = new Message(this.user, inputLine);

                    // Set encoding key for RSA encryption if present in the message
                    String encoder = message.getEncodingKey();
                    if (encoder != null) {
                        this.user.setEncodingKey(encoder);
                        toEncode = true; // If the message is encoded, reply with an encoded message
                    }

                    // Get required data
                    String scope = message.getScope();
                    Object data = message.getData();

                    if (scope != null)

                        // Process the requested action
                    {
                        switch (scope) {
                            /*
                                Connection
                             */
                            case "Connection": {
                                toEncode = true; // Allows the client to get the encoding key
                                newData.put("Success", true); // Build response
                                break;
                            }

                            /*
                                Ping
                             */
                            case "Ping": {
                                newData.put("Success", true);
                                break;
                            }

                            /*
                                Info:
                                - Username: String
                                - LastToken: String
                             */
                            case "Login": {
                                if (data == null) { // Data is required
                                    newData.put("Success", false);
                                    newData.put("Error", "Missing data");
                                    break;
                                }

                                // Get the data
                                LinkedTreeMap<String, String> info = (LinkedTreeMap<String, String>) data;
                                String missing = "Missing:";

                                if (info.containsKey("Username")) // Set the username or set error
                                    this.user.setUsername(info.get("Username"));
                                else
                                    missing = STR."\{missing} Username: String";

                                if (info.containsKey("LastToken")) { // Allow to log back in a lobby if disconnected
                                    this.user.setCurrentToken(info.get("LastToken"));
                                    // Put lobby check here!
                                }

                                // Build response
                                newData.put("Success", missing.equals("Missing:"));
                                newData.put("Error", missing.equals("Missing:") ? null : missing);
                                break;
                            }

                            /*
                                Lobbies:
                             */
                            case "Lobbies": {
                                this.user.disconnectFromLobby();

                                LinkedTreeMap<String, String> publicLobbies = new LinkedTreeMap<>();
                                LinkedTreeMap<String, String> privateLobbies = new LinkedTreeMap<>();

                                LinkedTreeMap<Integer, Game> lobbies = Game.getLobbies();
                                lobbies.forEach((key, value) -> {
                                    String name = STR."\{value.getName()} (\{value.getPlayers().size()}/\{value.getSize()})";

                                    if (value.hasPassword())
                                        privateLobbies.put(key.toString(), name);
                                    else
                                        publicLobbies.put(key.toString(), name);
                                });

                                newData.put("Success", true);
                                newData.put("Public", publicLobbies);
                                newData.put("Private", privateLobbies);
                                break;
                            }

                            /*
                                Create:
                                - Size: Int
                             */
                            case "Create": {
                                if (this.user.getUsername() == null) {
                                    newData.put("Success", false);
                                    newData.put("Error", "Not logged in");
                                    break;
                                }

                                if (data == null || !((LinkedTreeMap) data).containsKey("Size")) { // Data is required
                                    newData.put("Success", false);
                                    newData.put("Error", "Missing data");
                                    break;
                                }

                                String password = null;
                                if (((LinkedTreeMap) data).containsKey("Password"))
                                    password = (String) ((LinkedTreeMap) data).get("Password");

                                Game newLobby = new Game(((Number) ((LinkedTreeMap) data).get("Size")).intValue(), this.user, password);
                                newData.put("Success", true);
                                break;
                            }

                            case "Join": {
                                if (this.user.getUsername() == null) {
                                    newData.put("Success", false);
                                    newData.put("Error", "Not logged in");
                                    break;
                                }

                                if (data == null || !((LinkedTreeMap) data).containsKey("Lobby")) { // Data is required
                                    newData.put("Success", false);
                                    newData.put("Error", "Missing data");
                                    break;
                                }

                                Game lobby = Game.getByLobbyId(Integer.parseInt((String) ((LinkedTreeMap) data).get("Lobby")));
                                if (lobby != null) {
                                    for (User player : lobby.getPlayers()) {// Check if another player is using the same name
                                        if (player.getUsername().equals(this.user.getUsername())) {
                                            newData.put("Success", false);
                                            newData.put("Error", "Username already in use in this lobby");
                                            break;
                                        }
                                    }

                                    String token = lobby.join(this.user, (String) ((LinkedTreeMap) data).get("Password"));

                                    if (token == null) {
                                        newData.put("Success", false);
                                        newData.put("Error", "Not authorized to join");
                                    } else {
                                        newData.put("Success", true);
                                        newData.put("Token", token);
                                        // Update member list on other clients
                                    }
                                } else {
                                    newData.put("Success", false);
                                    newData.put("Error", "Lobby not found");
                                }

                                break;
                            }

                            default: {
                                newData.put("Success", false);
                                newData.put("Error", "Invalid scope");
                            }
                        }
                    } else {
                        // Build error response
                        newData.put("Success", false);
                        newData.put("Error", "Missing scope");
                    }

                    response = new Message(this.user, scope, newData, toEncode);
                } catch (SignatureException e) {
                    // Build signature exception response
                    newData.put("Success", false);
                    newData.put("Error", e.getMessage());
                } catch (Exception e) {
                    System.err.println("Error while processing the client message: ");
                    e.printStackTrace();

                    // Build general exception response
                    newData.put("Success", false);
                    newData.put("Error", "Server exception!");
                }

                if (response == null) // build an exception message
                    response = new Message(this.user, "Exception", newData, false);

                // send response message
                out.println(response.toJson());
            }
        } catch (SocketException e) {
            // Socket is closing, ignore.

        } catch (Exception e) {
            System.err.println("Error while handling the client: ");
            e.printStackTrace();
        }

        // Closing the handler due to client disconnection
        System.out.println("Client disconnected");

        // Put disconnection check here!
        Game lobby = this.user.getLobby();
        if (lobby != null) // Remove the player from the lobby if he was playing
            lobby.disconnect(this.user);

        this.close();
        System.out.println("Client handler closed");
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
        try {
            // Remove handled client from the user list
            User.removeByHandler(this);
            System.out.println("Client handler closing");

            // Close the socket, input & output
            if (this.clientSocket != null && !this.clientSocket.isClosed())
                this.clientSocket.close();

            if (this.out != null)
                this.out.close();

            if (this.in != null)
                this.in.close();

        } catch (Exception e) {
            System.err.println("Error while closing clientSocket: ");
            e.printStackTrace();
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
