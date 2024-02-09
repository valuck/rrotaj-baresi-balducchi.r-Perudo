package org.perudo;

import Messaging.User;
import Storage.ServerStorage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.LinkedList;

public class ServerInterface implements Runnable {
    private static final Logger logger = LogManager.getLogger(ServerInterface.class);

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
        logger.info("[SERVER]: Online");
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

                logger.info(STR."[SERVER]: New client connected: \{clientSocket}");

                // Create a new thread for each incoming client
                new Thread(new ClientHandler(clientSocket)).start();
            }

            try {
                if (serverSocket != null) {
                    serverSocket.close(); // close the ServerSocket to interrupt accept()
                }

            } catch (Exception e) {
                logger.error("Error while shutting down the server", e);
            }
        } catch (SocketException e) {
            // Socket is closing, ignore.
        }
        catch (Exception e) {
            logger.error("Error while listening for clients", e);
        }

        // Save games
        Game.getLobbies().forEach((key, _) -> {
            ServerStorage.updateTable("lobbies", "lobby_id", key); // Update last edit time
        });

        ServerStorage.close();
        serverSocket = null;

        logger.info("[SERVER]: Offline");
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
            for (int i=0; i<users.size(); i++) { // Close each client handler
                ClientHandler handler = users.get(i).getHandler();
                if (handler != null)
                    handler.close();
            }

            if (serverSocket != null && !serverSocket.isClosed())
                serverSocket.close();

        } catch (Exception e) {
            logger.error("Error while closing client socket", e);
        }
    }

    public static void softShutdown() {
        if (!running) // If it isn't already close
            return;

        String message = STR."Shutting down in \{softShutdownTime/1000} sec";

        Main.printMessage(message); // Tell all the clients of the upcoming shutdown and disconnect them
        ClientHandler.replicateMessage("Shutdown", message, true);

        try { // Wait for the shutdown delay
            Thread.sleep(softShutdownTime);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        shutdown(); // Actual shutdown
    }
}
