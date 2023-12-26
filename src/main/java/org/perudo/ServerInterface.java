package org.perudo;

import Storage.ServerStorage;

import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

public class ServerInterface implements Runnable {
    private final ServerSocket serverSocket;
    private boolean running = true;

    public ServerInterface(int port) {
        try {
            this.running = ServerStorage.setup(); // If fails it closes the server
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

                System.out.println("[SERVER]: New client connected: " + clientSocket);

                // Create a new thread for each incoming client
                new Thread(new ClientHandler(clientSocket)).start();
            }

            try {
                if (this.serverSocket != null) {
                    this.serverSocket.close(); // Close the ServerSocket to interrupt accept()
                    System.out.println("[SERVER]: Closed");
                }
            } catch (Exception e) {
                System.err.println("Error while shutting down the server: " + e);
            }
        } catch (SocketException e) {
            // Socket is closing, ignore.
        }
        catch (Exception e) {
            System.err.println("Error while listening for clients: ");
            e.printStackTrace();
        }

        System.out.println("Server closed");
    }

    public void shutdown() {
        this.running = false;

        try {
            if (this.serverSocket != null && !this.serverSocket.isClosed()) {
                this.serverSocket.close();
            }
        } catch (Exception e) {
            System.err.println("Error while closing client socket:");
            e.printStackTrace();
        }
    }
}
