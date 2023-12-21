package org.perudo;

import Messaging.DataUnion;
import Messaging.Message;
import Messaging.User;
import com.google.gson.internal.LinkedTreeMap;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.LinkedHashMap;

public class ClientHandler implements Runnable {
    private final Socket clientSocket;
    private final User user = new User();
    private BufferedReader in;
    private PrintWriter out;

    public ClientHandler(Socket clientSocket) {
        this.clientSocket = clientSocket;

        try {
            this.in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            this.out = new PrintWriter(clientSocket.getOutputStream(), true);

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
            while ((inputLine = in.readLine()) != null) {
                try {
                    System.out.println("Received from client: " + inputLine);
                    // Process inputLine if needed and send a response back
                    // writer.println("Server received: " + inputLine);

                    boolean toEncode = false;
                    Message message = new Message(inputLine);
                    LinkedHashMap<String, DataUnion> newData = new LinkedHashMap<>();

                    // Set encoding key for RSA encryption if sent
                    String encoder = message.getEncodingKey();
                    if (encoder != null)
                        this.user.setEncodingKey(encoder);

                    String scope = message.getScope();
                    Object data = message.getData();

                    if (scope != null)
                        switch (scope) {
                            /*
                                Connection
                             */
                            case "Connection":
                                toEncode = true; // Allows the client to get the encoding key
                                newData.put("Success", new DataUnion(true));
                                break;

                            /*
                                Info:
                                - Username: String
                                - LastToken: String
                             */
                            case "Info":
                                if (data == null) {
                                    newData.put("Success", new DataUnion(false));
                                    newData.put("Error", new DataUnion("Missing data"));
                                    break;
                                }

                                LinkedTreeMap<String, String> info = (LinkedTreeMap<String, String>) data;
                                String missing = "Missing:";

                                if (info.containsKey("Username"))
                                    this.user.setUsername(info.get("Username"));
                                else
                                    missing = missing + " Username: String";

                                if (info.containsKey("LastToken"))
                                    this.user.setCurrentToken(info.get("LastToken"));

                                newData.put("Success", new DataUnion(missing.equals("Missing:")));
                                newData.put("Error", missing.equals("Missing:") ? null : new DataUnion(missing));
                                break;

                            default:
                                newData.put("Success", new DataUnion(false));
                                newData.put("Error", new DataUnion("Invalid scope"));
                        }
                    else {
                        newData.put("Success", new DataUnion(false));
                        newData.put("Error", new DataUnion("Missing scope"));
                    }

                    Message response = new Message(this.user, scope, newData, toEncode);
                    out.println(response.toJson());
                } catch (Exception e) {
                    System.err.println("Error while processing the client message: ");
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            System.err.println("Error while handling the client: ");
            e.printStackTrace();
        }

        this.close();
    }

    public User getUser() {
        return this.user;
    }

    public void sendMessage(String scope, Object data, boolean encode) {
        Message message = new Message(this.user, scope, data, encode);
        out.println(message.toJson());
    }

    public void close() {
        try {
            User.removeByHandler(this);
            clientSocket.close();
            System.out.println("Client socket closed");
        } catch (Exception e) {
            System.err.println("Error while closing clientSocket: ");
            e.printStackTrace();
        }
    }

    public static void replicateMessage(String scope, Object data, boolean encode) { // Sends the message to all the users
        for (User user : User.getUsers()) {
            ClientHandler handler = user.getHandler();
            if (handler != null)
                handler.sendMessage(scope, data, encode);
        }
    }
}
