package org.perudo;
import Messaging.User;
import Storage.ServerStorage;
import UserInterface.CustomConsole;
import UserInterface.OptionsMenu;
import org.perudo.ClientHandler;
import org.perudo.ClientInterface;
import org.perudo.ServerInterface;

import javax.swing.*;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.function.Function;

public class Main {
    public static void main(String[] args) {

        // Server testing
        ServerInterface server = new ServerInterface(3000);
        new Thread(server).start();

        ClientInterface client = new ClientInterface("localhost", 3000);
        new Thread(client).start();

        client.sendMessage("NewLobby", 4, true);
    }
}