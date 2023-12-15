package Messaging;

import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.Base64;

public class Message {
    private LinkedTreeMap<String, Object> data;
    private static PrivateKey privateKey;
    private static PublicKey publicKey;

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

    public<T1> Message(User user, String scope, T1 data, boolean encoded) {
        LinkedTreeMap<String, Object> body = new LinkedTreeMap<>();
        boolean isEncoded = encoded && user.getEncodingKey() != null;
        String message = "";

        if (isEncoded)
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
        body.put("Data", isEncoded ? message : data);
        body.put("Encoder", encoded ? Base64.getEncoder().encodeToString(publicKey.getEncoded()) : null);

        this.data = body;
    }

    public Message(String receivedData) {
        Gson gson = new Gson();
        LinkedTreeMap body = gson.fromJson(receivedData, LinkedTreeMap.class);

        if (body.containsKey("Encoder") && body.containsKey("Data"))
            try {
                String encodedData = (String) body.get("Data");
                byte[] decoded = Base64.getDecoder().decode(encodedData.getBytes());

                Cipher decryptCipher = Cipher.getInstance("RSA");
                decryptCipher.init(Cipher.DECRYPT_MODE, privateKey);
                byte[] decryptedFileBytes = decryptCipher.doFinal(decoded);

                String message = new String(decryptedFileBytes, StandardCharsets.UTF_8);
                Object data = gson.fromJson(message, Object.class);

                body.replace("Data", data);
            } catch (BadPaddingException e) {
                System.err.println("Message can not be decoded: " + e.getMessage());
                // Handle the exception accordingly
            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }

        this.data = body;
    }

    public String getScope() {
        if (data != null && data.containsKey("Scope"))
            return (String) data.get("Scope");

        return null;
    }

    public Object getData() {
        if (data != null && data.containsKey("Data"))
            return data.get("Data");

        return null;
    }

    public String getEncodingKey() {
        if (data != null && data.containsKey("Encoder"))
            return (String) data.get("Encoder");

        return null;
    }

    public String toJson() {
        return new Gson().toJson(this.data);
    }

    public static String getSecureString(int lenght) {
        byte[] bytes = new byte[lenght];

        SecureRandom random = new SecureRandom();
        random.nextBytes(bytes);

        Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
        return encoder.encodeToString(bytes);
    }
}
