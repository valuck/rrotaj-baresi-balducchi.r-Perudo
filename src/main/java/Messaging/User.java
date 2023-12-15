package Messaging;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class User {
    private PublicKey encodingKey = null;
    private String currentToken;
    private String username;

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
}
