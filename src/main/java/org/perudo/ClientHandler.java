package org.perudo;

import Messaging.DataUnion;
import Messaging.Message;
import Messaging.User;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.LinkedHashMap;
import java.util.LinkedList;

public class ClientHandler implements Runnable {
    private static LinkedList<User> users = new LinkedList<>();
    private final Socket clientSocket;
    private User user = new User();

    public ClientHandler(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    @Override
    public void run() {
        try (
                BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true);
        ) {
            String inputLine;
            while ((inputLine = reader.readLine()) != null) {
                try {
                    System.out.println("Received from client: " + inputLine);
                    // Process inputLine if needed and send a response back
                    // writer.println("Server received: " + inputLine);


                    Message message = new Message(inputLine);
                    LinkedHashMap<String, DataUnion> newData = new LinkedHashMap<>();

                    // Set encoding key for RSA encryption if sent
                    String encoder = message.getEncodingKey();
                    if (encoder != null)
                        this.user.setEncodingKey(encoder);

                    String scope = message.getScope();
                    Object data = message.getData();

                    if (scope != null && data != null)
                        switch (scope) {
                            /*
                                INFO:
                                - Username: String
                                - LastToken: String
                             */
                            case "info":
                                LinkedHashMap<String, String> info = (LinkedHashMap<String, String>) data;
                                String missing = "Missing:";

                                if (info.containsKey("Username"))
                                    this.user.setUsername(info.get("Username"));
                                else
                                    missing = " Username: String";

                                if (info.containsKey("LastToken"))
                                    this.user.setCurrentToken(info.get("LastToken"));


                                newData.put("Success", new DataUnion(missing.equals("Missing:")));
                                newData.put("Error", missing.equals("Missing:") ? null : new DataUnion(missing));

                                Message response = new Message(this.user, "info", newData, true);
                                writer.println(response.toJson());
                                break;

                            default:
                                newData.put("Success", new DataUnion(false));
                                newData.put("Error", new DataUnion("Scope not found"));
                        }
                    else {
                        newData.put("Success", new DataUnion(false));
                        newData.put("Error", new DataUnion("Scope or Data not found"));
                    }

                    Message response = new Message(this.user, scope, newData, true);
                    writer.println(response.toJson());
                } catch (Exception e) {
                    System.err.println("Error while processing the message: " + e);
                }
            }
        } catch (Exception e) {
            System.err.println("Error while handling the client: " + e);
        } finally {
            try {
                clientSocket.close();
            } catch (Exception e) {
                System.err.println("Error while force-closing clientSocket: " + e);
            }
        }
    }
}
