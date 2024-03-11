package org.perudo;

import Storage.ClientStorage;
import Storage.ServerStorage;
import UserInterface.CustomConsole;
import UserInterface.OptionsMenu;
import com.google.gson.internal.LinkedTreeMap;

import java.util.ArrayList;

public class Main {
    public static OptionsMenu lastMenu = new OptionsMenu();
    private static ClientInterface currentClient;
    private static ArrayList<String> players;
    private static String currentUsername;
    private static CustomConsole console;
    private static String picked;
    private static boolean sock;
    private static long ping = 0;
    private static String lobby;
    private static String host;
    private static String dice;
    private static String turn;
    private static int size;

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

    public static String getUsername() {
        return currentUsername;
    }

    private static void startServer() {
        printPort(null, true);
    }

    public static void setPing(long ping) {
        Main.ping = ping;
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
        try {
            currentClient = new ClientInterface(address, port);
            new Thread(currentClient).start();
        } catch (Exception _) {
            printRestart("Unable to connect the client to the address");
        }
    }

    private static void clientLogin(String username) {
        // Send login message to the server
        LinkedTreeMap<String, String> data = new LinkedTreeMap<>();
        currentUsername = username;
        data.put("Username", username);
        data.put("LastToken", (String) ClientStorage.getSetting(STR."\{username.toLowerCase()}-token"));

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

            if (isServerPort)
                connectServer(port);
            else
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
        if (console == null)
            return;

        console.println(message);
    }

    public static void printSoloMessage(String message) {
        if (console == null)
            return;

        console.clear();
        console.println(message);
    }

    public static void printRestart(String message) {
        if (console == null)
            return;

        console.setTitle("Perudo");
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
        if (console == null)
            return;

        console.clear();
        console.println("Please enter the username you would like to use:");
        String old = (String) ClientStorage.getSetting("username");

        if (old != null && !old.isEmpty()) {
            console.setTitle(STR."Perudo - \{old}");
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
            console.setTitle(STR."Perudo - \{username}");
            currentUsername = username;
            clientLogin(username);
        }
    }

    public static void printLobbies(LinkedTreeMap<String, String> publicLobbies, LinkedTreeMap<String, String> privateLobbies) {
        if (console == null)
            return;

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

    private static void printPlayers(ArrayList<String> players) {
        if (console == null || players == null || players.isEmpty())
            return;

        players.forEach((value) -> console.println(value));
        console.println("------------------");
    }

    public static void printLobbyRoom(String name, ArrayList<String> players, String host, Number size, ArrayList<Object> list) {
        if (console == null)
            return;

        Main.size = size.intValue();
        Main.players = players;
        Main.lobby = name;
        Main.host = host;

        if ((boolean) list.get(1)) {
            printGame("Members", list);
            return;
        }

        console.clear();
        console.println(STR."\{ping}ms");
        console.println(STR."\{name} (\{players.size()}/\{size.intValue()})");

        printPlayers(players);
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

    private static void sendChoice(int amount, int value, boolean jolly) {
        if (console == null)
            return;

        console.clear();
        console.println("Pick sent, waiting..");

        LinkedTreeMap<String, Object> values = new LinkedTreeMap<>();
        values.put("Amount", amount);
        values.put("Value", value);
        values.put("Jolly", jolly);

        currentClient.sendMessage("Choice", values, true);
    }

    private static void printStats() {
        if (console == null)
            return;

        console.println("Your dice:");
        console.println(Main.dice);
        console.println("Last choice:");
        console.println(Main.picked);
        console.println("------------------");
    }

    public static void printWinner(String username) {
        if (console == null)
            return;

        console.println(STR."\{username} won the game!");
    }

    private static int incrementAmount(int startValue, int endValue) {
        console.clear();
        printStats();
        console.println(STR."Set amount to: (must be \{endValue > 0 ? STR."between \{startValue +1} and \{endValue}" : STR."over \{startValue}"})");

        int val = -1;
        while (val <= startValue) {
            try {
                val = Integer.parseInt(console.readln());
                if (endValue > 0 && val > endValue)
                    val = -1;

            } catch (Exception e) {
                // Ignore
            }
        }

        return val;
    }

    private static void incrementValue(int startValue, int amount, int lastAmount) {
        OptionsMenu menu = new OptionsMenu();
        console.clear();
        printStats();

        boolean one = false;
        if (startValue >= 6) {
            startValue = 0;
            one = true;
        }

        console.println("Set value to:");
        for (int i=startValue+1; i<=(one ? 1 : 6); i++) {
            int finalI = i;
            boolean isJolly = finalI==1;

            menu.addOption(String.valueOf(STR."\{finalI}\{isJolly ? " (jolly)" : ""}"), (_) -> {
                if (isJolly) {
                    menu.clearOptions();
                    console.clear();
                    printStats();

                    console.println("Do you want to choose the amount?");
                    menu.addOption("Yes", (_) -> {
                        sendChoice(incrementAmount(0, (int) Math.ceil((double) lastAmount/2)), finalI, true);
                        return null;
                    });

                    menu.addOption("No", (_) -> {
                        sendChoice(amount, finalI, false);
                        return null;
                    });

                    console.drawOptionsMenu(menu);
                } else
                    sendChoice(amount, finalI, false);

                return null;
            });
        }

        console.drawOptionsMenu(menu);
    }

    public static void printGame(String scope, ArrayList<Object> data) {
        if (console == null)
            return;

        console.clear();
        console.println(STR."\{ping}ms");
        printPlayers(Main.players);

        switch (scope) {
            case "Dice": {
                Main.dice = (String) data.get(0);
                break;
            }

            case "Dudo": {
                console.println("Results:");
                lastMenu.clearOptions();

                ((ArrayList) data.get(0)).forEach((value) -> console.println((String) value));

                console.println(" ");
                console.println((String) data.get(2));
                console.println((String) data.get(1));
                return;
            }

            case "Turn": {
                String name = (String) data.get(0);
                Main.turn = name.equals(currentUsername) ? "your" : name;
                break;
            }

            case "Pick": {
                Main.turn = "your";
                lastMenu.clearOptions();

                int startAmount = ((Number) data.get(1)).intValue();
                int startValue = ((Number) data.get(2)).intValue();

                if ((boolean) data.get(0)) {
                    lastMenu.addOption("Increment amount", (_) -> {
                        sendChoice(incrementAmount(startAmount, 0), 0, false);
                        return null;
                    });

                    lastMenu.addOption("Increment value", (_) -> {
                        incrementValue(startValue, 0, startAmount);
                        return null;
                    });

                    lastMenu.addOption("Say dudo!", (_) -> {
                        console.clear();
                        console.println("Sending dudo..");
                        currentClient.sendMessage("Dudo", null, true);
                        return null;
                    });
                }
                else {
                    lastMenu.addOption("Insert values", (_) -> {
                        int amount = incrementAmount(startAmount, 0);
                        incrementValue(startValue, amount, 0);
                        return null;
                    });
                }

                break;
            }

            case "Picked": {
                Main.picked = (String) data.get(0);
                lastMenu.clearOptions();

                break;
            }

            case "Members": {
                if ((boolean) data.get(0)) {
                    console.println("Waiting for players to reconnect..");
                    return;
                }

                break;
            }

            case "Sock": {
                sock = (Boolean) data.get(0);
                break;
            }
        }

        printStats();
        console.println(STR."It's \{Main.turn} turn!");

        if (sock)
            lastMenu.addOption("Say sock!", (_) -> {
                currentClient.sendMessage("Sock", null, true);
                return null;
            });

        console.drawOptionsMenu(lastMenu);
    }
}