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
    private boolean running = true;
    private User server;

    public ClientInterface(String address, Integer port) {
        try {
            this.clientSocket = new Socket(address, port);
            this.out = new PrintWriter(clientSocket.getOutputStream(), true);
            this.in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            this.running = true;
            this.initConnection();
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
            while (this.running) {
                System.out.println("sos?");
                try {
                    String serverResponse = in.readLine();
                    if (serverResponse != null) {
                        System.out.println("Server response: " + serverResponse);

                        Message response = new Message(this.server, serverResponse);
                        String encoder = response.getEncodingKey();
                        String scope = response.getScope();

                        if (encoder != null) // Updates server RSA key
                            this.server.setEncodingKey(encoder);

                        if (scope != null)
                            switch (scope) {
                                // If needed to handle any response
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
        if (!this.running)
            return;

        this.server = new User();
        this.server.setUsername("Server");

        // Validate connection and get server RSA key
        this.sendMessage("Connection", null, true);
    }

    public void sendMessage(String scope, Object data, boolean encode) {
        if (!this.running) {
            System.err.println("Unable to send message, client is closed!");
            return;
        }

        if (!Objects.equals(scope, "Connection"))
            while (this.server.getEncodingKey() == null) {
                try { // Yields until the server replies
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

        Message msg = new Message(this.server, scope, data, encode);
        out.println(msg.toJson());
    }

    public void close() {
        this.running = false;

        try {
            if (this.in != null) this.in.close();
            if (this.out != null) this.out.close();
            if (this.clientSocket != null && !this.clientSocket.isClosed()) {
                this.clientSocket.close();
            }
        } catch (Exception e) {
            System.err.println("Error while closing the client: " + e);
        }
    }
}
