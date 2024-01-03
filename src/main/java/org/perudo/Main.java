package org.perudo;

import Storage.ClientStorage;
import UserInterface.CustomConsole;
import UserInterface.OptionsMenu;
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

    private static void printPort(String address) {
        console.clear();
        console.println("Please enter the port of the server:");
        Integer old = ((Number) ClientStorage.getSetting("port")).intValue();

        if (old != null && old > 0) {
            console.println(old.toString());
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

    private static void connectClient(String addres, int port) {
        currentClient = new ClientInterface(addres, port);
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

    public static void printRestart(String message) {
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
}