package org.perudo;

import java.io.IOException;
import java.net.Socket;

public class ClientInterface implements Runnable {
    private Socket clientSocket;

    public ClientInterface(String address, Integer port) {
        try {
            this.clientSocket = new Socket(address, port);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void run() {

    }
}
