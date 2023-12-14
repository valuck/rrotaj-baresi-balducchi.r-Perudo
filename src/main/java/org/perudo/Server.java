package org.perudo;

import Storage.ServerStorage;

import java.net.ServerSocket;

public class Server implements Runnable {
    private ServerSocket serverSocket;

    public Server(int port) {
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
    }
}
