package Messaging;

public class SignatureException extends Exception {
    public SignatureException(String msg) {
        // Used to handle "Invalid message signature" in Message.java
        super(msg);
    }
}
