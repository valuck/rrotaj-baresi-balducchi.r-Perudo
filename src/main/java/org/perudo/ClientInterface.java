package org.perudo;

import Messaging.Message;
import Messaging.User;
import com.google.gson.internal.LinkedTreeMap;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.Socket;
import java.net.SocketException;
import java.sql.Timestamp;
import java.util.Objects;

public class ClientInterface implements Runnable {
    private Timestamp ping_start;
    private Timestamp ping_end;
    private Socket clientSocket;
    private BufferedReader in;
    private PrintWriter out;
    private boolean running;
    private User server;

    public ClientInterface(String address, Integer port) {
        try {
            // Open a new client socket to the selected port and address, open input & output
            this.clientSocket = new Socket(address, port);
            this.out = new PrintWriter(clientSocket.getOutputStream(), true);
            this.in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            this.running = true;
            this.initConnection(); // Initialize connection with the server to share RSA keys.

            new Thread(new Runnable() { // Connection checking thread
                @Override
                public void run() {
                    while (true) { // ping the server every 3 sec
                        Timestamp newTime = new Timestamp(System.currentTimeMillis());

                        if (server.getEncodingKey() != null) { // if connection is initialized
                            if (ping_end != null) {
                                long ping = Math.abs(ping_end.getTime() - ping_start.getTime()); // calculate ping
                                System.out.println(ping + "ms");
                                // get ping event from here!

                                if (ping > 10000) { // if over 10 seconds
                                    System.err.println("Connection lost or ping too high.");
                                    // get connection lost event from here!

                                    close();
                                    break; // exits the loop
                                }
                            }
                            else // Initialize ping
                                ping_end = newTime;

                            ping_start = newTime;
                            if (!sendMessage("Ping", null, true)) // send a ping message
                                break; // Client already closed, disconnect
                        }

                        try {
                            Thread.sleep(3000);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }).start();
        } catch (ConnectException e) {
            this.running = false;
            System.err.println("Server connection refused!");

        } catch (Exception e) {
            this.running = false;
            throw new RuntimeException(e);
        }
    }

    @Override
    public void run() {
        try {
            // Listen for server messages until closure
            while (this.running) {
                try {
                    String serverResponse = in.readLine();
                    if (serverResponse != null) { // On message received
                        System.out.println("Server response: " + serverResponse);

                        // Generate a new message from the received data
                        Message response = new Message(this.server, serverResponse);
                        String encoder = response.getEncodingKey();
                        String scope = response.getScope();
                        Object data = response.getData();

                        if (encoder != null) // Updates server's RSA key
                            this.server.setEncodingKey(encoder);

                        if (scope != null)
                            // Do requested action or response

                            switch (scope) { // If needed to handle any action response
                                case "Ping": // Check for action success
                                    if (data != null && ((LinkedTreeMap) data).containsKey("Success") && (boolean)((LinkedTreeMap) data).get("Success"))
                                        ping_end = new Timestamp(System.currentTimeMillis());

                                    break;

                                case "Shutdown":
                                    if (data != null) {
                                        System.err.println((String) data);
                                        // get shutdown event from here!

                                        System.err.println("Disconnecting client");
                                        close();
                                    }

                                    break;
                            }

                        //System.err.println(this.server.getEncodingKey());
                    }
                } catch (SocketException e) {
                    // Socket is closing, ignore.

                } catch (Exception e) {
                    System.err.println("Error on processing server message: " );
                    e.printStackTrace();
                }
            }

            System.out.println("Client closed");
        } catch (Exception e) {
            throw new RuntimeException("Error while listening for the server" + e);
        }
    }

    public void initConnection() {
        // Initialize the connection between client and server
        if (!this.running)
            return;

        // Create a new user as the server
        this.server = new User();
        this.server.setUsername("Server");

        // Validate connection and share RSA keys with the server
        this.sendMessage("Connection", null, true);
    }

    public boolean sendMessage(String scope, Object data, boolean encode) {
        if (!this.running)
            return false;
            //System.err.println("Unable to send message, client is closed!");

        // Wait for the sever to share it's RSA key if not present
        if (!Objects.equals(scope, "Connection"))
            while (this.server.getEncodingKey() == null) {
                try { // Yields until the server replies
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

        // Send a new message to the server
        Message msg = new Message(this.server, scope, data, encode);
        out.println(msg.toJson());

        return true;
    }

    public void close() {
        // Stop listening for the server
        this.running = false;

        try {
            System.out.println("Client closing");
            // Close clientSocket, input & output
            if (this.clientSocket != null && !this.clientSocket.isClosed())
                this.clientSocket.close();

            if (this.out != null)
                this.out.close();

            if (this.in != null)
                this.in.close();

        } catch (Exception e) {
            System.err.println("Error while closing the client: " + e);
        }
    }
}
