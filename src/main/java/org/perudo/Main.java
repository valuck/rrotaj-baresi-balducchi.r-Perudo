package org.perudo;

import Messaging.Message;
import Storage.ClientStorage;
import UserInterface.CustomConsole;
import UserInterface.OptionsMenu;
import com.google.gson.internal.LinkedTreeMap;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.function.Function;

public class Main {
    private static CustomConsole console;
    private static ClientInterface currentClient;

    public static void main(String[] args) {
        console = new CustomConsole("Perudo");
        printInitialize();

        /* Server testing
        ServerInterface server = new ServerInterface(3000);
        new Thread(server).start();

        ClientInterface client = new ClientInterface("localhost", 3000);
        new Thread(client).start();

        client.sendMessage("NewLobby", 4, true);*/
    }

    private static void startServer() {
        console.clear();
        OptionsMenu menu = new OptionsMenu();
        menu.addOption("Soft Shutdown", new Function<String, Void>() {
            @Override
            public Void apply(String string) {
                ServerInterface.softShutdown();
                printRestart("Server has been closed");

                return null;
            }
        });

        console.drawOptionsMenu(menu);
        console.println("------------------");

        new Thread(new ServerInterface(10)).start();
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
            menu.addOption("Continue", new Function<String, Void>() {
                @Override
                public Void apply(String string) {
                    printPort(old);
                    return null;
                }
            });

            menu.addOption("Change", new Function<String, Void>() {
                @Override
                public Void apply(String string) {
                    ClientStorage.updateSetting("address", "", true);
                    starClient(); // Restart

                    return null;
                }
            });

            console.drawOptionsMenu(menu);
        } else {
            String address = console.readln();
            ClientStorage.updateSetting("address", address, true);
            printPort(address);
        }
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

    private static void lobbyLogin(String lobby, String password) {
        console.clear();
        console.println("Joining lobby..");

        LinkedTreeMap<String, String> data = new LinkedTreeMap<>();
        data.put("Lobby", lobby);
        data.put("Password", password);

        currentClient.sendMessage("Join", data, true);
    }

    private static void printPort(String address) {
        console.clear();
        console.println("Please enter the port of the server:");
        int old = ((Number) ClientStorage.getSetting("port")).intValue();

        if (old > 0) {
            console.println(String.valueOf(old));
            console.println("------------------");

            OptionsMenu menu = new OptionsMenu();
            menu.addOption("Continue", new Function<String, Void>() {
                @Override
                public Void apply(String string) {
                    connectClient(address, old);
                    return null;
                }
            });

            menu.addOption("Change", new Function<String, Void>() {
                @Override
                public Void apply(String string) {
                    ClientStorage.updateSetting("port", 0, true);
                    printPort(address); // Restart

                    return null;
                }
            });

            console.drawOptionsMenu(menu);
        } else {
            int port = Integer.parseInt(console.readln());
            ClientStorage.updateSetting("port", port, true);
            connectClient(address, port);
        }
    }

    private static void printInitialize() {
        console.clear();
        console.println("Welcome to Perudo.");
        console.println("------------------");

        OptionsMenu menu = new OptionsMenu();
        menu.addOption("Start as client", new Function<String, Void>() {
            @Override
            public Void apply(String string) {
                starClient();
                return null;
            }
        });

        menu.addOption("Start as server", new Function<String, Void>() {
            @Override
            public Void apply(String string) {
                startServer();
                return null;
            }
        });

        menu.addOption("Start both", new Function<String, Void>() {
            @Override
            public Void apply(String string) {
                startServer();
                starClient();
                return null;
            }
        });

        menu.addOption("Exit", new Function<String, Void>() {
            @Override
            public Void apply(String string) {
                ServerInterface.softShutdown();

                if (currentClient != null)
                    currentClient.close();

                console.close();
                return null;
            }
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
        menu.addOption("Continue", new Function<String, Void>() {
            @Override
            public Void apply(String string) {
                printInitialize();
                return null;
            }
        });

        console.drawOptionsMenu(menu);
    }

    public static void printLogin() {
        console.clear();
        console.println("Please enter the username you would like to use:");
        String old = (String) ClientStorage.getSetting("username");

        if (old != null && !old.isEmpty()) {
            console.println(old);
            console.println("------------------");

            OptionsMenu menu = new OptionsMenu();
            menu.addOption("Continue", new Function<String, Void>() {
                @Override
                public Void apply(String string) {
                    clientLogin(old);
                    return null;
                }
            });

            menu.addOption("Change", new Function<String, Void>() {
                @Override
                public Void apply(String string) {
                    ClientStorage.updateSetting("username", "", true);
                    printLogin(); // Restart

                    return null;
                }
            });

            console.drawOptionsMenu(menu);
        } else {
            String username = console.readln();
            ClientStorage.updateSetting("username", username, true);
            clientLogin(username);
        }
    }

    public static void printLobbies(ArrayList<String> publicLobbies, ArrayList<String> privateLobbies) {
        console.clear();
        console.println("Public Lobbies");
        console.println("------------------");

        OptionsMenu menu = new OptionsMenu();
        publicLobbies.forEach((value) -> {
            menu.addOption(value, new Function<String, Void>() {
                @Override
                public Void apply(String string) {
                    lobbyLogin(string, null);
                    return null;
                }
            });
        });

        menu.addOption(" ", null);
        menu.addOption("Private lobbies", null);
        menu.addOption("------------------", null);

        privateLobbies.forEach((value) -> {
            menu.addOption(value, new Function<String, Void>() {
                @Override
                public Void apply(String string) {
                    console.clear();
                    console.println("Please enter the lobby's password");
                    lobbyLogin(string, console.readln());

                    return null;
                }
            });
        });

        console.drawOptionsMenu(menu);
    }
}