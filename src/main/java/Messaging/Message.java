package Messaging;

import com.google.gson.Gson;

import javax.crypto.Cipher;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.Base64;
import java.util.LinkedHashMap;

public class Message<T1> {
    private LinkedHashMap<String, Object> data = null;
    private static PrivateKey privateKey = null;
    private static PublicKey publicKey = null;

    static {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KeyPair pair = generator.generateKeyPair();

            publicKey = pair.getPublic();
            privateKey = pair.getPrivate();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public Message(User user, String scope, T1 data, boolean encoded) {
        LinkedHashMap<String, Object> body = new LinkedHashMap<String, Object>();
        encoded = encoded && user.getEncodingKey() != null;
        String message = "";

        if (encoded)
            try {
                message = new Gson().toJson(data);

                Cipher encryptCipher = Cipher.getInstance("RSA");
                encryptCipher.init(Cipher.ENCRYPT_MODE, user.getEncodingKey());

                byte[] secretMessageBytes = message.getBytes(StandardCharsets.UTF_8);
                byte[] encryptedMessageBytes = encryptCipher.doFinal(secretMessageBytes);

                message = Base64.getEncoder().encodeToString(encryptedMessageBytes);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

        body.put("Scope", scope);
        body.put("Data", encoded ? message : data);
        body.put("Encoder", encoded ? Base64.getEncoder().encodeToString(publicKey.getEncoded()) : null);

        this.data = body;
    }

    public String toJson() {
        return new Gson().toJson(this.data);
    }

    public static String getSecureString(int lenght) {
        byte bytes[] = new byte[lenght];

        SecureRandom random = new SecureRandom();
        random.nextBytes(bytes);

        Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
        return encoder.encodeToString(bytes);
    }
}
