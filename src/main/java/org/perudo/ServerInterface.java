package org.perudo;

import Storage.ServerStorage;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerInterface implements Runnable {
    private final ServerSocket serverSocket;
    private boolean running = true;

    public ServerInterface(int port) {
        try {
            ServerStorage.setup();
            this.serverSocket = new ServerSocket(port);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void run() {
        System.out.println("[SERVER]: Online");

        try {
            while (this.running) {
                Socket clientSocket = serverSocket.accept();

                if (!this.running)
                    break; // Exits the loop if shutting down

                System.out.println("New client connected: " + clientSocket);

                // Create a new thread for each incoming client
                new Thread(new ClientHandler(clientSocket)).start();
            }

            try {
                if (this.serverSocket != null) {
                    this.serverSocket.close(); // Close the ServerSocket to interrupt accept()
                    System.out.println("Server closed");
                }
            } catch (Exception e) {
                System.err.println("Error while shutting down the server: " + e);
            }
        } catch (Exception e) {
            System.err.println("Error while listening for clients: ");
            e.printStackTrace();
        } finally {
            try {
                if (serverSocket != null)
                    serverSocket.close();

            } catch (IOException e) {
                System.err.println("Error while force-closing the server: ");
                e.printStackTrace();
            }
        }
    }

    public void shutdown() {
        this.running = false;
    }
}
