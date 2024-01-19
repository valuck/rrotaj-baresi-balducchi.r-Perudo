package Messaging;

import Storage.ServerStorage;
import org.perudo.ClientHandler;
import org.perudo.Game;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.LinkedList;
import java.util.Random;

public class User {
    private static final LinkedList<User> users = new LinkedList<>();
    private PublicKey encodingKey = null;
    LinkedList<Integer> lastShuffle;
    private ClientHandler handler;
    private String currentToken;
    private String username;
    private Game lobby;
    private int dice;

    public User() {
        users.add(this);
    }

    public void setEncodingKey(String encodingKey) {
        try {
            // Generate the encoding key for a user given its Public RSA key encoded in Base64
            byte[] decodedPublicKey = Base64.getDecoder().decode(encodingKey);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(decodedPublicKey);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");

            this.encodingKey = keyFactory.generatePublic(keySpec);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static LinkedList<User> getUsers() {
        // returns the whole list of users registered
        return users;
    }

    public static User getUserByName(String username) {
        // returns the user with the specified name, if registered
        for (int i=0; i<users.size(); i++) { // foreach loops can give an error
            User user = users.get(i);

            if (user != null) {
                String userName = user.getUsername(); // Checks for the name
                if (userName != null && userName.equals(username))
                    return user;
            }
        }

        return null;
    }

    public static void removeByHandler(ClientHandler handler) {
        // removes the users handled by the specified ClientHandler, used while disconnecting
        for (int i=0; i<users.size(); i++) {
            User user = users.get(i);

            // Checks for the handler
            if (user != null && user.handler == handler)
                users.remove(user);
        }
    }

    public void setEncodingKey(PublicKey encodingKey) {
        // set the raw encoding key
        this.encodingKey = encodingKey;
    }

    public PublicKey getEncodingKey() {
        // get the raw encoding key previously given
        return this.encodingKey;
    }

    public void setUsername(String username) {
        // set the user username
        this.username = username;
    }

    public String getUsername() {
        // get the user username previously given
        return this.username;
    }

    public void setCurrentToken(String currentToken) {
        // set the current token for lobby access authorization
        this.currentToken = currentToken;
    }

    public String getCurrentToken() {
        // get the current token previously given
        return this.currentToken;
    }

    public void setHandler(ClientHandler handler) {
        // set the user's handler for clients management
        this.handler = handler;
    }

    public ClientHandler getHandler() {
        // get the user's handler previously given
        return this.handler;
    }

    public void setLobby(Game lobby) {
        this.lobby = lobby;
    }

    public Game getLobby() {
        return this.lobby;
    }

    public void setDice(int dice) {
        this.dice = dice;
    }

    public int getDice() {
        return dice;
    }

    public void disconnectFromLobby() {
        if (this.lobby != null)
            this.lobby.disconnect(this);
    }

    public LinkedList<Integer> shuffle() {
        if (this.lobby.getId() < 0 || this.currentToken == null)
            return null;

        int dice = ServerStorage.getDice(this.currentToken);
        LinkedList<Integer> results = new LinkedList<>();

        for(int i=0; i<dice; i++)
            results.add(new Random().nextInt(1, 6));

        this.lastShuffle = results;
        return results;
    }

    public LinkedList<Integer> getLastShuffle() {
        return this.lastShuffle;
    }
}
