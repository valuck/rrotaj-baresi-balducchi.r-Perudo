package org.perudo;

import Messaging.DataUnion;
import Messaging.Message;
import Messaging.User;
import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.LinkedHashMap;
import java.util.LinkedList;

public class ClientHandler implements Runnable {
    private static final LinkedList<User> users = new LinkedList<>();
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

                    Boolean toEncode = false;
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
                    writer.println(response.toJson());
                } catch (Exception e) {
                    System.err.println("Error while processing the client message: ");
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            System.err.println("Error while handling the client: ");
            e.printStackTrace();
        } finally {
            try {
                clientSocket.close();
            } catch (Exception e) {
                System.err.println("Error while force-closing clientSocket: ");
                e.printStackTrace();
            }
        }
    }
}
