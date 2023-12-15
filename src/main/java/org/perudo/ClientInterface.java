package org.perudo;

import Messaging.Message;
import Messaging.User;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Objects;

public class ClientInterface implements Runnable {
    private final Socket clientSocket;
    private final BufferedReader in;
    private final PrintWriter out;
    private boolean running = true;
    private User server;

    public ClientInterface(String address, Integer port) {
        try {
            this.clientSocket = new Socket(address, port);
            this.out = new PrintWriter(clientSocket.getOutputStream(), true);
            this.in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void run() {
        try {
            while (this.running) {
                try {
                    String serverResponse = in.readLine();
                    if (serverResponse != null) {
                        System.out.println("Server response: " + serverResponse);

                        Message response = new Message(serverResponse);
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
                } catch (Exception e) {
                    System.err.println("Error on processing server message: " );
                    e.printStackTrace();
                }
            }

            try {
                if (in != null) in.close();
                if (out != null) out.close();
                if (clientSocket != null && !clientSocket.isClosed()) {
                    clientSocket.close();
                    System.out.println("Client closed");
                }
            } catch (Exception e) {
                System.err.println("Error while closing the client: " + e);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error while listening for the server" + e);
        }
    }

    public void initConnection() {
        this.server = new User();
        this.server.setUsername("Server");

        // Validate connection and get server RSA key
        this.sendMessage("Connection", null, true);
    }

    public void sendMessage(String scope, Object data, boolean encode) {
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
    }
}
