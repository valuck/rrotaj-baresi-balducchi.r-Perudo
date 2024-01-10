package Messaging;

import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.Base64;

public class Message {
    private static final Logger logger = LogManager.getLogger(Message.class);

    private LinkedTreeMap<String, Object> data;
    private static final PrivateKey privateKey;
    private static final PublicKey publicKey;

    static {
        try {
            // Generate RSA keys
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KeyPair pair = generator.generateKeyPair();

            publicKey = pair.getPublic();
            privateKey = pair.getPrivate();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean structureCheck() {
        return data != null && data.containsKey("Content");
    }

    public<T1> Message(User user, String scope, T1 data, boolean encoded) {
        // Build a new message for the specified user
        LinkedTreeMap<String, Object> body = new LinkedTreeMap<>();
        boolean isEncoded = encoded && user.getEncodingKey() != null;
        Base64.Encoder encoder = Base64.getEncoder();
        Gson gson = new Gson();
        String message = "";

        if (isEncoded)
            try {
                message = gson.toJson(data);

                // Encode the message's json using the receiver's public RSA key
                Cipher encryptCipher = Cipher.getInstance("RSA");
                encryptCipher.init(Cipher.ENCRYPT_MODE, user.getEncodingKey());

                byte[] secretMessageBytes = message.getBytes(StandardCharsets.UTF_8);
                byte[] encryptedMessageBytes = encryptCipher.doFinal(secretMessageBytes);

                // Encode bytes to Base64
                message = encoder.encodeToString(encryptedMessageBytes);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

        // Setup inner message structure
        LinkedTreeMap<String, Object> allData = new LinkedTreeMap<>();

        allData.put("Scope", scope); // Determinate the action
        allData.put("Data", isEncoded ? message : data); // Encoded or Raw data
        allData.put("Encoder", encoded ? encoder.encodeToString(publicKey.getEncoded()) : null); // Public RSA key for the response

        // Prevent json parsing problems (Ex: 0 -> 0.0)
        String predictedResult = gson.toJson(gson.fromJson(gson.toJson(allData), LinkedTreeMap.class));
        String encodedContent = encoder.encodeToString(predictedResult.getBytes()); // Used to generate the signature

        try {
            // Generate signature with RSA keys
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(privateKey);
            signature.update(encodedContent.getBytes());

            // Setup final message structure
            body.put("Content", allData); // Inner message structure
            body.put("Signature", encoder.encodeToString(signature.sign())); // Message signature in Base64

            this.data = body;
        } catch (Exception e) {
            logger.error("Unable to generate message signature: ", e);
        }
    }

    public Message(User user, String receivedData) throws SignatureException {
        // Build a message from the received data, knowing the sender
        Gson gson = new Gson();
        Base64.Decoder decoder = Base64.getDecoder();
        LinkedTreeMap body = gson.fromJson(receivedData, LinkedTreeMap.class); // Retrieve data from json string

        if (body.containsKey("Content")) { // Checks for the message structure
            LinkedTreeMap content = (LinkedTreeMap) body.get("Content"); // Get inner structure

            // Connection scope can have no signature since it's needed to initialize the communication and share RSA keys.
            if (content.containsKey("Scope") && !content.get("Scope").equals("Connection"))
                if (body.containsKey("Signature")) // Checks for the message signature.
                    if (user.getEncodingKey() != null)
                        try {
                            // Generate the signature from the received data
                            String ogSignature = Base64.getEncoder().encodeToString(gson.toJson(content).getBytes());
                            byte[] signatureReceived = decoder.decode((String) body.get("Signature"));

                            // Compare the generated signature with the received one using the sender public RSA key.
                            Signature signature = Signature.getInstance("SHA256withRSA");
                            signature.initVerify(user.getEncodingKey());
                            signature.update(ogSignature.getBytes());

                            if (!signature.verify(signatureReceived)) { // If the signature isn't valid
                                throw new SignatureException("Invalid message signature!");
                            }

                        } catch (SignatureException e) { // Invalid signature re-thrower.
                            throw new SignatureException(e.getMessage());

                        } catch (Exception e) {
                            logger.error("Unable to verify the message signature.", e);
                            return;
                        }
                    else
                        logger.warn("Missing RSA key.");
                else {
                    logger.warn("Missing message signature.");
                    return;
                }

                // Checks if the message was encoded
            if (content.containsKey("Encoder") && content.containsKey("Data"))
                try {
                    // Decode original data's Json from Base64
                    String encodedData = (String) content.get("Data");
                    byte[] decoded = Base64.getDecoder().decode(encodedData.getBytes());

                    // Decode with the receiver's private RSA key
                    Cipher decryptCipher = Cipher.getInstance("RSA");
                    decryptCipher.init(Cipher.DECRYPT_MODE, privateKey);
                    byte[] decryptedFileBytes = decryptCipher.doFinal(decoded);

                    // Convert the decoded Json back to original data (as Object)
                    String message = new String(decryptedFileBytes, StandardCharsets.UTF_8);
                    Object data = gson.fromJson(message, Object.class);

                    // Replace into the inner message structure
                    content.replace("Data", data);
                } catch (BadPaddingException e) {
                    logger.error("Message can not be decoded", e);
                    // Handle the exception accordingly
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

            // Replace changes into the final message structure
            body.replace("Content", content);
            this.data = body; // Set the new message's raw data
        }
        else {
            logger.warn("Missing content");
            return;
        }
    }

    public String getScope() {
        // returns the message's scope if present
        if (structureCheck() && ((LinkedTreeMap) data.get("Content")).containsKey("Scope"))
            return (String) ((LinkedTreeMap) data.get("Content")).get("Scope");

        return null;
    }

    public Object getData() {
        // returns the message's data if present
        if (structureCheck() && ((LinkedTreeMap) data.get("Content")).containsKey("Data"))
            return ((LinkedTreeMap) data.get("Content")).get("Data");

        return null;
    }

    public String getEncodingKey() {
        // returns the sender's public RSA key if present
        if (structureCheck() && ((LinkedTreeMap) data.get("Content")).containsKey("Encoder"))
            return (String) ((LinkedTreeMap) data.get("Content")).get("Encoder");

        return null;
    }

    public String toJson() {
        // Returns the message's data in json
        return new Gson().toJson(this.data);
    }
}
