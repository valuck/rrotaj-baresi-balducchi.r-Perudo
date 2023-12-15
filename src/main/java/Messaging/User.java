package Messaging;

import org.perudo.ClientHandler;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.LinkedList;

public class User {
    private static final LinkedList<User> users = new LinkedList<>();
    private PublicKey encodingKey = null;
    private ClientHandler handler;
    private String currentToken;
    private String username;

    public User() {
        users.add(this);
    }

    public void setEncodingKey(String encodingKey) {
        try {
            byte[] decodedPublicKey = Base64.getDecoder().decode(encodingKey);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(decodedPublicKey);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");

            this.encodingKey = keyFactory.generatePublic(keySpec);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static LinkedList<User> getUsers() {
        return users;
    }

    public static User getUserByName(String username) {
        for (int i=0; i<users.size(); i++) {
            User user = users.get(i);
            if (user != null) {
                String userName = user.getUsername();
                if (userName != null && userName.equals(username))
                    return user;
            }
        }

        return null;
    }

    public static void removeByHandler(ClientHandler handler) {
        for (int i=0; i<users.size(); i++) {
            User user = users.get(i);
            if (user != null && user.handler == handler)
                users.remove(user);
        }
    }

    public void setEncodingKey(PublicKey encodingKey) {
        this.encodingKey = encodingKey;
    }

    public PublicKey getEncodingKey() {
        return this.encodingKey;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getUsername() {
        return this.username;
    }

    public void setCurrentToken(String currentToken) {
        this.currentToken = currentToken;
    }

    public String getCurrentToken() {
        return this.currentToken;
    }

    public void setHandler(ClientHandler handler) {
        this.handler = handler;
    }

    public ClientHandler getHandler() {
        return this.handler;
    }
}
