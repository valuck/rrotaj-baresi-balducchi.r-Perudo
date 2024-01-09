package org.perudo;

import Messaging.User;
import Storage.ServerStorage;
import com.google.gson.internal.LinkedTreeMap;
import de.mkammerer.argon2.Argon2;
import de.mkammerer.argon2.Argon2Factory;

import java.util.LinkedList;

public class Game {
    private static final LinkedTreeMap<Integer, Game> games = new LinkedTreeMap();
    private final LinkedList<User> disconnected = new LinkedList<>();
    private final LinkedList<User> players = new LinkedList<>();
    private final String password;
    private final String name;
    private final int lobbyId;
    private boolean started;
    private final int size;
    private boolean used;

    private String hashString(String string) {
        if (string == null || string.isEmpty())
            return null;

        Argon2 argon2 = Argon2Factory.create();
        char[] chars = string.toCharArray();
        try {
            // Hash the password. Parameters can be adjusted based on security vs performance requirements.
            return argon2.hash(2, 65536, 1, chars);
        } finally {
            // Wipe the password array for security reasons
            argon2.wipeArray(chars);
        }
    }

    private Game(int lobbyId) {
        this.lobbyId = lobbyId;
        this.name = ServerStorage.getLobbyName(lobbyId);
        this.size = ServerStorage.getLobbySize(lobbyId);

        String pass = ServerStorage.getLobbyPassword(lobbyId);
        this.password = pass.isEmpty() ? null : pass;

        LinkedList<String> players = ServerStorage.getTokensInLobby(lobbyId);
        players.forEach((value) -> { // Put old users in wait list
            User user = new User();
            user.setCurrentToken(value);
            user.setDice(ServerStorage.getDice(value));

            disconnected.add(user);
        });

        games.put(this.lobbyId, this);
        System.err.println(STR."Reloaded \{this.name}");

        new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i=0; i<40; i++) { // Loops for max 10 minutes
                    try { // Checks every 15 sec if the lobby has been used
                        Thread.sleep(15000);

                        if (used)
                            return;

                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }

                remove(); // Deletes the lobby if isn't used after 10m
            }
        }).start();
    }

    public Game(int size, User host, String password) {
        this.password = hashString(password);
        this.players.add(host);
        this.size = size;
        this.name = STR."\{host.getUsername()}'s lobby";

        this.lobbyId = ServerStorage.newLobby(this.name, this.size, this.password);
        games.put(this.lobbyId, this);
    }

    public void remove() {
        games.remove(this.lobbyId);
        ServerStorage.deleteLobby(this.lobbyId);
    }

    public String join(User user, String password) {
        if (this.password != null && (password == null || !Argon2Factory.create().verify(this.password, password.toCharArray())))
            return null;

        String token;
        if (!started && this.players.size() + this.disconnected.size() >= size) { // If the lobby is full
            String tempToken = user.getCurrentToken();
            boolean found = false;

            if (tempToken != null) {
                for (User value : this.disconnected) { // If a disconnected player access with the right Token
                    if (value.getCurrentToken().equals(tempToken)) {
                        user.setDice(value.getDice());

                        this.disconnected.remove(value);
                        found = true;
                        break;
                    }
                }
            }

            if (found) {
                token = tempToken;
            } else
                return null;
        }
        else {
            token = ServerStorage.newToken(this.lobbyId);
            user.setCurrentToken(token);
        }

        user.disconnectFromLobby();
        user.setLobby(this);

        this.players.add(user);
        this.used = true;

        this.membersUpdated();
        return token;
    }

    public void disconnect(User user) {
        if (!this.players.contains(user))
            return; // If user is not in the lobby

        this.disconnected.add(user); // add to the wait to reconnect list
        this.players.remove(user);
        user.setLobby(null);

        if (this.players.isEmpty()) {
            this.remove();
            return;
        }

        this.membersUpdated();
    }

    public void startGame() {
        this.started = true;
        this.disconnected.clear();
    }

    public static Game getByLobbyId(int lobbyId) {
        if (games.containsKey(lobbyId))
            return games.get(lobbyId);

        return null;
    }

    public void replicateMessage(String scope, Object data, boolean encode) { // Sends the message to all lobby members
        LinkedList<User> users = this.players;

        // Send a new message to all the connected users
        for (int i = 0; i < users.size(); i++) { // foreach loops can give an error
            User user = users.get(i);
            ClientHandler handler = user.getHandler();

            if (handler != null) // if the user has a valid handler
                handler.sendMessage(scope, data, encode);
        }
    }

    public void membersUpdated() {
        // Replicate to all lobby members
        new Thread(new Runnable() {
            @Override
            public void run() { // Update player list
                LinkedTreeMap<String, Object> replicatedData = new LinkedTreeMap<>();
                LinkedList<String> plrs = new LinkedList<>();
                players.forEach((value) -> {
                    plrs.add(value.getUsername());
                });

                replicatedData.put("Success", true);
                replicatedData.put("Players", plrs);
                replicatedData.put("Name", getName());
                replicatedData.put("Host", getHost().getUsername());

                replicateMessage("Members", replicatedData, true);
            }
        }).start();
    }

    public static void reloadGames() {
        LinkedList<Integer> lobbies = ServerStorage.getLobbies();

        lobbies.forEach((value) -> {
            new Game(value); // Just initialize them
        });
    }

    public static LinkedTreeMap<Integer, Game> getLobbies() {
        return games;
    }

    public User getHost() {
        return this.players.getFirst();
    }

    public LinkedList<User> getPlayers() {
        return this.players;
    }

    public boolean hasPassword() {
        return this.password != null;
    }

    public int getId() {
        return this.lobbyId;
    }

    public int getSize() {
        return this.size;
    }

    public String getName() {
        return STR."[\{this.lobbyId}] \{this.name}";
    }
}
