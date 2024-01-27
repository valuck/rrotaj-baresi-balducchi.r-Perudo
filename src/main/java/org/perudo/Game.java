package org.perudo;

import Messaging.User;
import Storage.ServerStorage;
import com.google.gson.internal.LinkedTreeMap;
import de.mkammerer.argon2.Argon2;
import de.mkammerer.argon2.Argon2Factory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.LinkedList;

public class Game {
    private static final Logger logger = LogManager.getLogger(Game.class);

    private static final LinkedTreeMap<Integer, Game> games = new LinkedTreeMap<>();
    private final LinkedList<User> disconnected = new LinkedList<>();
    private final LinkedList<User> players = new LinkedList<>();
    private boolean lastPickState;
    private final String password;
    private final String name;
    private final int lobbyId;
    private User lastPlayer;
    private boolean started;
    private final int size;
    private int lastAmount;
    private int lastValue;
    private boolean paused;
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
            ServerStorage.removeToken(value);
        });

        games.put(this.lobbyId, this);
        logger.info(STR."Reloaded \{this.name}");

        new Thread(() -> {
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
        }).start();
    }

    public Game(int size, User host, String password) {
        this.password = hashString(password);
        this.size = size;
        this.name = STR."\{host.getUsername()}'s lobby";

        this.lobbyId = ServerStorage.newLobby(this.name, this.size, this.password);
        games.put(this.lobbyId, this);
        this.join(host, password);
    }

    public void remove() {
        games.remove(this.lobbyId);
        ServerStorage.deleteLobby(this.lobbyId);
    }

    public String join(User user, String password) {
        if (this.password != null && (password == null || !Argon2Factory.create().verify(this.password, password.toCharArray())))
            return null;

        String token;
        if (this.started && this.players.size() + this.disconnected.size() >= this.size) { // If the lobby is full
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

        if (this.players.size() >= this.size)
            this.paused = false;

        this.membersUpdated(this.paused);
        if (this.started && !this.paused && this.getUserByShift() == user)
            pickUpdate(user, this.lastPickState);

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

        this.membersUpdated(this.started);
    }

    public void startGame() {
        if (this.started)
            return;

        this.started = true;
        this.disconnected.clear();

        this.startTurn();
    }

    private void startTurn() {
        this.lastPlayer = null;
        lastAmount = 1;
        lastValue = 1;

        for (User player : this.players) {
            diceUpdate(player);
        }

        startShift(false);
    }

    public boolean processPicks(User player, int amount, int value) {
        User current = getUserByShift();

        if (player == null || player != current)
            return false;

        boolean itsDudo = value >= 7 && lastPlayer != null;
        String picked = "";

        if (itsDudo) { // its dudo!
            int correct = 0;

            for (User plr : players)
                for (int dice : plr.getLastShuffle())
                    if (dice == value || dice == 1)
                        correct++;

            if (correct >= amount) {
                ServerStorage.incrementDice(player.getCurrentToken(), -1);
                this.dudoUpdate(false, player);
            } else {
                ServerStorage.incrementDice(lastPlayer.getCurrentToken(), -1);
                this.dudoUpdate(true, lastPlayer);
            }

            lastPlayer = null;
            picked = "Dudo!";

            new Thread(() -> {
                try {
                    Thread.sleep(5000);
                    startTurn();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }).start();
        }
        else {
            boolean am = amount > 0;

            amount = amount < lastAmount ? lastAmount + 1 : amount;

            if (lastValue >= 6)
                value = 1;
            else if (value < 6)
                value = value < lastValue ? lastValue + 1 : value;

            if (am)
                lastAmount = amount;

            if (!am || lastPlayer != null)
                lastValue = value;

            picked = picked + STR."Amount: \{amount}, Value: \{value}";
        }

        ServerStorage.incrementLobbyShift(this.lobbyId);
        this.lastPlayer = current;

        startShift(!itsDudo, picked);
        return true;
    }

    private User getUserByShift() {
        int shift = ServerStorage.getLobbyShift(this.lobbyId) -1;
        LinkedList<String> tokens = ServerStorage.getTokensInLobby(this.lobbyId);
        if (shift >= 0 && tokens.size() > shift) {
            String selected = tokens.get(shift);

            for (User player : this.players) {
                if (player.getCurrentToken().equals(selected)) {
                    return player;
                }
            }

            // User not in list anymore
        }

        return null;
    }

    private void startShift(boolean canDudo) {
        User player = getUserByShift();
        this.lastPickState = canDudo;

        if (player != null) {
            LinkedTreeMap<String, Object> data = new LinkedTreeMap<>();
            LinkedTreeMap<String, Boolean> blacklist = new LinkedTreeMap<>();

            data.put("Success", true);
            data.put("Sock", true);

            blacklist.put(player.getCurrentToken(), true);
            this.replicateMessage("Sock", data, true, blacklist);
            data.replace("Sock", false);

            if (this.lastPlayer != null) {
                blacklist.put(this.lastPlayer.getCurrentToken(), true);
                this.lastPlayer.getHandler().sendMessage("Sock", data, true);
            }

            player.getHandler().sendMessage("Sock", data, true);
            pickUpdate(player, canDudo);
        }
    }

    private void startShift(boolean canDudo, String picked) {
        LinkedTreeMap<String, Object> replicatedData = new LinkedTreeMap<>();
        replicatedData.put("Success", true);
        replicatedData.put("Picks", picked);
        
        this.replicateMessage("Picked", replicatedData, true);
        this.startShift(canDudo);
    }

    public static Game getByLobbyId(int lobbyId) {
        if (games.containsKey(lobbyId))
            return games.get(lobbyId);

        return null;
    }

    public void replicateMessage(String scope, Object data, boolean encode, LinkedTreeMap<String, Boolean> blacklist) { // Sends the message to all lobby members
        LinkedList<User> users = this.players;

        // Send a new message to all the connected users
        for (int i = 0; i < users.size(); i++) { // foreach loops can give an error
            User user = users.get(i);
            if (blacklist.containsKey(user.getCurrentToken()))
                continue;
            
            ClientHandler handler = user.getHandler();

            if (handler != null) // if the user has a valid handler
                handler.sendMessage(scope, data, encode);
        }
    }

    public void replicateMessage(String scope, Object data, boolean encode) {
        replicateMessage(scope, data, encode, new LinkedTreeMap<>());
    }

    public void membersUpdated(boolean pause) {
        if (pause)
            this.paused = true;
        // Replicate to all lobby members
        new Thread(() -> { // Update player list
            LinkedTreeMap<String, Object> replicatedData = new LinkedTreeMap<>();
            LinkedList<String> plrs = new LinkedList<>();
            User currentHost = getHost();

            players.forEach((value) -> plrs.add(value.getUsername() + (value == currentHost ? " (host)" : "")));

            replicatedData.put("Size", size);
            replicatedData.put("Pause", pause);
            replicatedData.put("Success", true);
            replicatedData.put("Players", plrs);
            replicatedData.put("Name", getName());
            replicatedData.put("Host", currentHost.getUsername());

            replicateMessage("Members", replicatedData, true);
        }).start();
    }

    private void dudoUpdate(boolean dudo, User plr) {
        LinkedTreeMap<String, Object> replicatedData = new LinkedTreeMap<>();
        LinkedList<String> plrs = new LinkedList<>();

        for (User player : this.players) {
            StringBuilder result = new StringBuilder();
            result.append(player.getUsername()).append(": ");

            for (int dice : player.getLastShuffle())
                result.append(dice).append(" ");

            plrs.add(result.toString());
        }

        replicatedData.put("Success", true);
        replicatedData.put("Results", plrs);
        replicatedData.put("Victim", STR."\{plr.getUsername()} lost a dice!");
        replicatedData.put("Verdict", STR."Pick was \{(dudo ? "right" : "wrong")}!");
        replicateMessage("Dudo", replicatedData, true);
    }

    private void pickUpdate(User player, boolean canDudo) {
        LinkedTreeMap<String, Object> replicatedData = new LinkedTreeMap<>();
        replicatedData.put("Success", true);
        replicatedData.put("Dudo", canDudo);
        replicatedData.put("Amount", lastAmount);
        replicatedData.put("Value", lastValue);

        player.getHandler().sendMessage("Pick", replicatedData, true);
        LinkedTreeMap<String, Boolean> blacklist = new LinkedTreeMap<>();
        blacklist.put(player.getCurrentToken(), true);

        replicatedData.clear();
        replicatedData.put("Success", true);
        replicatedData.put("User", player.getUsername());

        replicateMessage("Turn", replicatedData, true, blacklist);
    }

    private static void diceUpdate(User player) {
        LinkedTreeMap<String, Object> replicatedData = new LinkedTreeMap<>();
        StringBuilder results = new StringBuilder();

        for (Integer dice : player.shuffle()) {
            results.append(STR."\{dice.toString()} ");
        }

        replicatedData.put("Success", true);
        replicatedData.put("Dice", results.toString());
        player.getHandler().sendMessage("Dice", replicatedData, true);
    }

    public static void reloadGames() {
        LinkedList<Integer> lobbies = ServerStorage.getLobbies();

        // Just initialize them
        lobbies.forEach(Game::new);
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
