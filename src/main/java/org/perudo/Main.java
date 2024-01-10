package org.perudo;

import Storage.ClientStorage;
import Storage.ServerStorage;
import UserInterface.CustomConsole;
import UserInterface.OptionsMenu;
import com.google.gson.internal.LinkedTreeMap;

import java.util.ArrayList;

public class Main {
    private static CustomConsole console;
    private static String currentUsername;
    private static ClientInterface currentClient;

    public static void main(String[] args) {
        // System.setProperty("log4j.configurationFile", String.valueOf(new File("resources", "log4j.xml").toURI()));
        console = new CustomConsole("Perudo");
        printInitialize();

        /* Server testing
        ServerInterface server = new ServerInterface(3000);
        new Thread(server).start();

        ClientInterface client = new ClientInterface("localhost", 3000);
        new Thread(client).start();

        client.sendMessage("Create", 4, true);*/
    }

    private static void startServer() {
        printPort(null, true);
    }

    private static void starClient() {
        ClientStorage.loadSettings();

        console.clear();
        console.println("Please enter the address of the server:");
        String old = (String) ClientStorage.getSetting("address");

        if (old != null && !old.isEmpty()) {
            console.println(old);
            console.println("------------------");

            OptionsMenu menu = new OptionsMenu();
            menu.addOption("Continue", _ -> {
                printPort(old, false);
                return null;
            });

            menu.addOption("Change", _ -> {
                ClientStorage.updateSetting("address", "", true);
                starClient(); // Restart
                return null;
            });

            console.drawOptionsMenu(menu);
        } else {
            String address = console.readln();
            ClientStorage.updateSetting("address", address, true);
            printPort(address, false);
        }
    }

    private static void connectServer(int port) {
        console.clear();
        OptionsMenu menu = new OptionsMenu();
        menu.addOption("Soft Shutdown", _ -> {
            ServerInterface.softShutdown();
            printRestart("Server has been closed");

            return null;
        });

        menu.addOption("Erase Database and Soft Shutdown", _ -> {
            ServerStorage.eraseDatabase(true);
            ServerInterface.softShutdown();
            return null;
        });

        console.drawOptionsMenu(menu);
        console.println("------------------");

        new Thread(new ServerInterface(port)).start();
    }

    private static void connectClient(String address, int port) {
        currentClient = new ClientInterface(address, port);
        new Thread(currentClient).start();
    }

    private static void clientLogin(String username) {
        // Send login message to the server
        LinkedTreeMap<String, String> data = new LinkedTreeMap<>();
        data.put("Username", username);
        data.put("LastToken", (String) ClientStorage.getSetting("token"));

        currentClient.sendMessage("Login", data, true);
    }

    private static void createLobby(int size, String password) {
        console.clear();
        console.println("Creating lobby..");

        LinkedTreeMap<String, Object> data = new LinkedTreeMap<>();
        data.put("Password", password);
        data.put("Size", size);

        currentClient.sendMessage("Create", data, true);
    }

    private static void lobbyLogin(String lobby, String password) {
        console.clear();
        console.println("Joining lobby..");

        LinkedTreeMap<String, String> data = new LinkedTreeMap<>();
        data.put("Lobby", lobby);
        data.put("Password", password);

        currentClient.sendMessage("Join", data, true);
    }

    private static void printPort(String address, boolean isServerPort) {
        console.clear();
        console.println("Please enter the port of the server:");

        Number saved = (Number) ClientStorage.getSetting("port");
        int old = 0;

        if (saved != null)
            old = saved.intValue();

        if (old > 0) {
            console.println(String.valueOf(old));
            console.println("------------------");

            OptionsMenu menu = new OptionsMenu();
            int finalOld = old;

            menu.addOption("Continue", _ -> {
                if (isServerPort)
                    connectServer(finalOld);
                else
                    connectClient(address, finalOld);

                return null;
            });

            menu.addOption("Change", _ -> {
                ClientStorage.updateSetting("port", 0, true);
                printPort(address, isServerPort); // Restart
                return null;
            });

            console.drawOptionsMenu(menu);
        } else {
            int port = Integer.parseInt(console.readln());
            ClientStorage.updateSetting("port", port, true);
            connectClient(address, port);
        }
    }

    private static void hostLobby(boolean isPrivate) {
        console.clear();
        String pass = null;

        if (isPrivate) {
            console.println("Create a password for the lobby");
            pass = console.readln();
            console.clear();
        }

        console.println("Choose the lobby size");
        OptionsMenu menu = new OptionsMenu();

        String finalPass = pass;
        menu.addOption("2", _ -> {
            createLobby(2, finalPass);
            return null;
        });

        menu.addOption("3", _ -> {
            createLobby(3, finalPass);
            return null;
        });

        menu.addOption("4", _ -> {
            createLobby(4, finalPass);
            return null;
        });

        menu.addOption("5", _ -> {
            createLobby(5, finalPass);
            return null;
        });

        menu.addOption("6", _ -> {
            createLobby(6, finalPass);
            return null;
        });

        console.drawOptionsMenu(menu);
    }

    private static void printHostLobby() {
        console.clear();
        console.println("Lobby creation");
        console.println("------------------");

        OptionsMenu menu = new OptionsMenu();
        menu.addOption("Make public", _ -> {
            hostLobby(false);

            return null;
        });

        menu.addOption("Make private", _ -> {
            hostLobby(true);

            return null;
        });

        console.drawOptionsMenu(menu);
    }

    private static void printInitialize() {
        console.clear();
        console.println("Welcome to Perudo.");
        console.println("------------------");

        OptionsMenu menu = new OptionsMenu();
        menu.addOption("Start as client", _ -> {
            starClient();
            return null;
        });

        menu.addOption("Start as server", _ -> {
            startServer();
            return null;
        });

        menu.addOption("Start both (Testing mode)", _ -> {
            connectServer(10);
            starClient();
            return null;
        });

        menu.addOption("Exit", _ -> {
            ServerInterface.softShutdown();

            if (currentClient != null)
                currentClient.close();

            console.close();
            return null;
        });

        console.setTextInput(false);
        console.drawOptionsMenu(menu);
    }

    public static void printMessage(String message) {
        console.println(message);
    }

    public static void printSoloMessage(String message) {
        console.clear();
        console.println(message);
    }

    public static void printRestart(String message) {
        if (currentClient != null)
            currentClient.close();

        console.clear();
        console.println(message);
        console.println("------------------");

        OptionsMenu menu = new OptionsMenu();
        menu.addOption("Continue", _ -> {
            printInitialize();
            return null;
        });

        console.drawOptionsMenu(menu);
    }

    public static void printLogin() {
        console.clear();
        console.println("Please enter the username you would like to use:");
        String old = (String) ClientStorage.getSetting("username");

        if (old != null && !old.isEmpty()) {
            currentUsername = old;
            console.println(old);
            console.println("------------------");

            OptionsMenu menu = new OptionsMenu();
            menu.addOption("Continue", _ -> {
                clientLogin(old);
                return null;
            });

            menu.addOption("Change", _ -> {
                ClientStorage.updateSetting("username", "", true);
                printLogin(); // Restart
                return null;
            });

            console.drawOptionsMenu(menu);
        } else {
            String username = console.readln();
            ClientStorage.updateSetting("username", username, true);
            currentUsername = username;
            clientLogin(username);
        }
    }

    public static void printLobbies(LinkedTreeMap<String, String> publicLobbies, LinkedTreeMap<String, String> privateLobbies) {
        console.clear();
        console.println("Public Lobbies");
        console.println("-----------------<");

        OptionsMenu menu = new OptionsMenu();
        publicLobbies.forEach((key, value) ->
                menu.addOption(value, _ -> {

            lobbyLogin(key, null);
            return null;
        }));

        menu.addOption(" ", null);
        menu.addOption("Private lobbies", null);
        menu.addOption("-----------------<", null);

        privateLobbies.forEach((key, value) ->
                menu.addOption(value, _ -> {

            console.clear();
            console.println("Please enter the lobby's password");
            lobbyLogin(key, console.readln());
            return null;
        }));

        if (privateLobbies.isEmpty())
            menu.addOption(" ", null);

        menu.addOption("----------------->", null);
        menu.addOption("Host", _ -> {
            printHostLobby();
            return null;
        });

        menu.addOption("Refresh", _ -> {
            console.clear();
            console.println("Refreshing..");
            currentClient.sendMessage("Lobbies", null, true);
            return null;
        });

        menu.addOption("Disconnect", _ -> {
            printRestart("Disconnected");
            return null;
        });

        console.drawOptionsMenu(menu);
    }

    public static void printLobbyRoom(String name, ArrayList<String> players, String host, Number size) {
        console.clear();
        console.println(STR."\{name} (\{players.size()}/\{size.intValue()})");

        players.forEach((value) -> console.println(value));

        console.println("------------------");
        OptionsMenu menu = new OptionsMenu();
        menu.addOption("Leave lobby", _ -> {
            currentClient.sendMessage("Lobbies", null, true);
            return null;
        });

        if (players.size() == size.intValue() && currentUsername != null && currentUsername.equals(host))
            menu.addOption("Start game", _ -> {
                currentClient.sendMessage("Start", null, true);
                return null;
            });

        console.drawOptionsMenu(menu);
    }
}