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
        Base64.Encoder encoder = Base64.getEncoder();
        Gson gson = new Gson();
        String message = "";

        if (isEncoded)
            try {
                message = gson.toJson(data);

                Cipher encryptCipher = Cipher.getInstance("RSA");
                encryptCipher.init(Cipher.ENCRYPT_MODE, user.getEncodingKey());

                byte[] secretMessageBytes = message.getBytes(StandardCharsets.UTF_8);
                byte[] encryptedMessageBytes = encryptCipher.doFinal(secretMessageBytes);

                message = encoder.encodeToString(encryptedMessageBytes);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

        LinkedTreeMap<String, Object> allData = new LinkedTreeMap<>();

        allData.put("Scope", scope);
        allData.put("Data", isEncoded ? message : data);
        allData.put("Encoder", encoded ? encoder.encodeToString(publicKey.getEncoded()) : null);

        // Prevent json parsing problems (Ex: 0 -> 0.0)
        String predictedResult = gson.toJson(gson.fromJson(gson.toJson(allData), LinkedTreeMap.class));
        String encodedContent = encoder.encodeToString(predictedResult.getBytes());

        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(privateKey);
            signature.update(encodedContent.getBytes());

            body.put("Content", allData);
            body.put("Signature", encoder.encodeToString(signature.sign()));

            this.data = body;
        } catch (Exception e) {
            System.err.println("Unable to generate message signature: ");
            e.printStackTrace();
        }
    }

    public Message(User user, String receivedData) throws SignatureException {
        Gson gson = new Gson();
        LinkedTreeMap body = gson.fromJson(receivedData, LinkedTreeMap.class);
        Base64.Decoder decoder = Base64.getDecoder();

        if (body.containsKey("Content")) {
            LinkedTreeMap content = (LinkedTreeMap) body.get("Content");

            System.err.println(content.get("Scope"));

            if (content.containsKey("Scope") && !content.get("Scope").equals("Connection"))
                if (body.containsKey("Signature"))
                    if (user.getEncodingKey() != null) // First message should load it.
                        try {
                            String ogSignature = Base64.getEncoder().encodeToString(gson.toJson(content).getBytes());
                            byte[] signatureReceived = decoder.decode((String) body.get("Signature"));

                            Signature signature = Signature.getInstance("SHA256withRSA");
                            signature.initVerify(user.getEncodingKey());
                            signature.update(ogSignature.getBytes());

                            if (!signature.verify(signatureReceived)) {
                                throw new SignatureException("Invalid message signature!");
                            }

                        } catch (SignatureException e) {
                            throw new SignatureException(e.getMessage());
                        } catch (Exception e) {
                            System.err.println("Unable to verify the message signature.");
                            e.printStackTrace();
                            return;
                        }
                    else
                        System.err.println("Missing RSA key.");
                else {
                    System.err.println("Missing message signature.");
                    return;
                }

            if (content.containsKey("Encoder") && content.containsKey("Data"))
                try {
                    String encodedData = (String) content.get("Data");
                    byte[] decoded = Base64.getDecoder().decode(encodedData.getBytes());

                    Cipher decryptCipher = Cipher.getInstance("RSA");
                    decryptCipher.init(Cipher.DECRYPT_MODE, privateKey);
                    byte[] decryptedFileBytes = decryptCipher.doFinal(decoded);

                    String message = new String(decryptedFileBytes, StandardCharsets.UTF_8);
                    Object data = gson.fromJson(message, Object.class);

                    content.replace("Data", data);
                } catch (BadPaddingException e) {
                    System.err.println("Message can not be decoded: " + e.getMessage());
                    // Handle the exception accordingly
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

            body.replace("Content", content);
            this.data = body;
        }
        else {
            System.err.println("Missing content");
            return;
        }
    }

    public String getScope() {
        if (data != null && data.containsKey("Content") && ((LinkedTreeMap) data.get("Content")).containsKey("Scope"))
            return (String) ((LinkedTreeMap) data.get("Content")).get("Scope");

        return null;
    }

    public Object getData() {
        if (data != null && data.containsKey("Content") && ((LinkedTreeMap) data.get("Content")).containsKey("Data"))
            return ((LinkedTreeMap) data.get("Content")).get("Data");

        return null;
    }

    public String getEncodingKey() {
        if (data != null && data.containsKey("Content") && ((LinkedTreeMap) data.get("Content")).containsKey("Encoder"))
            return (String) ((LinkedTreeMap) data.get("Content")).get("Encoder");

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
