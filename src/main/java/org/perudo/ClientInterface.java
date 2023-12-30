package org.perudo;

import Messaging.Message;
import Messaging.User;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.Socket;
import java.net.SocketException;
import java.util.Objects;

public class ClientInterface implements Runnable {
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

                            switch (scope) {
                                // If needed to handle any action response
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

    public void sendMessage(String scope, Object data, boolean encode) {
        if (!this.running) {
            System.err.println("Unable to send message, client is closed!");
            return;
        }

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
