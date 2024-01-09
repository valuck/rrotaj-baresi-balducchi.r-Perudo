package org.perudo;

import Messaging.User;
import Storage.ServerStorage;

import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.LinkedList;

public class ServerInterface implements Runnable {
    private static final int softShutdownTime = 5000;
    private static ServerSocket serverSocket;
    private static boolean running;

    public ServerInterface(int port) {
        if (serverSocket != null) { // Checks if the server has already been initialized
            Main.printRestart("Server is already initialized");
            throw new RuntimeException("Server is already initialized"); // Block thread
        }

        try {
            // Open a new serverSocket to the selected port after setting up the database
            running = ServerStorage.setup(); // If fails it closes the server
            serverSocket = new ServerSocket(port);
        } catch (BindException e) {
            Main.printRestart("Address already in use");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void run() {
        System.out.println("[SERVER]: Online");
        Main.printMessage("Server online");

        if (serverSocket == null)
            running = false;

        if (running) // can start on false
            Game.reloadGames();

        try {
            while (running) { // Listen for clients to connect
                Socket clientSocket = serverSocket.accept(); // it yields until a new client is connected

                if (!running)
                    break; // Exits the loop if shutting down

                System.out.println(STR."[SERVER]: New client connected: \{clientSocket}");

                // Create a new thread for each incoming client
                new Thread(new ClientHandler(clientSocket)).start();
            }

            try {
                if (serverSocket != null) {
                    serverSocket.close(); // close the ServerSocket to interrupt accept()
                }

            } catch (Exception e) {
                System.err.println(STR."Error while shutting down the server: \{e}");
            }
        } catch (SocketException e) {
            // Socket is closing, ignore.
        }
        catch (Exception e) {
            System.err.println("Error while listening for clients: ");
            e.printStackTrace();
        }

        // Save games
        Game.getLobbies().forEach((key, value) -> {
            ServerStorage.updateTable("lobbies", "lobby_id", key); // Update last edit time
        });

        ServerStorage.close();
        serverSocket = null;

        System.out.println("[SERVER]: Offline");
        Main.printRestart("Server closed");
    }

    public static void shutdown() {
        if (!running)
            return;

        // stop listening for clients to connect
        running = false;

        try {
            // close the server socket
            LinkedList<User> users = User.getUsers();
            for (int i=0; i<users.size(); i++) {
                ClientHandler handler = users.get(i).getHandler();
                if (handler != null)
                    handler.close();
            }

            if (serverSocket != null && !serverSocket.isClosed())
                serverSocket.close();

        } catch (Exception e) {
            System.err.println("Error while closing client socket:");
            e.printStackTrace();
        }
    }

    public static void softShutdown() {
        if (!running)
            return;

        String message = STR."Shutting down in \{softShutdownTime/1000} sec";

        Main.printMessage(message);
        ClientHandler.replicateMessage("Shutdown", message, true);

        try {
            Thread.sleep(softShutdownTime);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        shutdown();
    }
}
