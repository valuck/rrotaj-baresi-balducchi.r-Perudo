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
    private final LinkedTreeMap<String, Boolean> finished = new LinkedTreeMap<>();
    private final LinkedList<User> disconnected = new LinkedList<>();
    private final LinkedList<User> players = new LinkedList<>();
    private boolean lastPickState;
    private final String password;
    private final int lobbyId;
    private final String name;
    private User lastPlayer;
    private boolean started;
    private final int size;
    private boolean paused;
    private int lastAmount;
    private int lastValue;
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
        tokenUpdate(host, this.join(host));
    }

    private Game(User host, int size, String password) {
        this.size = size;
        this.password = password;
        this.name = STR."\{host.getUsername()}'s lobby";

        this.lobbyId = ServerStorage.newLobby(this.name, this.size, this.password);
        games.put(this.lobbyId, this);
        tokenUpdate(host, this.join(host));
    }

    public void remove() {
        games.remove(this.lobbyId);
        ServerStorage.deleteLobby(this.lobbyId);
    }

    private String join(User user) {
        String token;
        if (this.started && this.players.size() + this.disconnected.size() >= this.size) { // If the lobby is full
            String tempToken = user.getCurrentToken();
            boolean found = false;

            if (tempToken != null) {
                for (User value : this.disconnected) { // If a disconnected player access with the right Token
                    if (value.getCurrentToken().equals(tempToken)) {
                        user.setDice(value.getDice());
                        user.setShuffle(value.getLastShuffle());

                        this.disconnected.remove(value);
                        found = true;
                        break;
                    }
                }
            }

            if (found) {
                token = tempToken;
                // Send all the data required
                ClientHandler handler = user.getHandler();

                if (handler != null) {
                    LinkedTreeMap<String, Object> replicatedData = new LinkedTreeMap<>();
                    replicatedData.put("Success", true);
                    diceUpdate(user, true);

                    replicatedData.put("Picks", STR."Amount: \{this.lastAmount}, Value: \{this.lastValue == 1 ? "J" : this.lastValue}");
                    handler.sendMessage("Picked", replicatedData, true);
                    replicatedData.remove("Picked");

                    if (this.lastPlayer != null) {
                        replicatedData.put("User", this.lastPlayer.getUsername());
                        handler.sendMessage("Turn", replicatedData, true);
                    }
                }
            } else
                return null;
        }
        else {
            token = ServerStorage.newToken(this.lobbyId);
            user.setCurrentToken(token);
        }

        if (user.getLobby() != this)
            user.disconnectFromLobby();
        user.setLobby(this);

        boolean found = false;
        for (User player : this.players) {
            if (user.getCurrentToken().equals(player.getCurrentToken())) {
                found = true;
                break;
            }
        }

        if (!found)
            this.players.add(user);
        this.used = true;

        if (this.players.size() >= this.size)
            this.paused = false;

        this.membersUpdated(this.paused);
        User next = this.getUserByShift();

        if (this.started && !this.paused && next != null)
            pickUpdate(next, this.lastPickState);

        return token;
    }

    public String join(User user, String password) {
        if (this.password != null && (password == null || !Argon2Factory.create().verify(this.password, password.toCharArray())))
            return null;

        return join(user);
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

        this.setupTurn();
        this.startShift(false);
    }

    private void setupTurn() {
        this.lastPlayer = null;
        lastAmount = 0;
        lastValue = 0;

        for (User player : this.players) {
            diceUpdate(player, false);
        }
    }

    public boolean processPicks(User player, int amount, int value) {
        User current = getUserByShift();
        boolean tookAction = false;

        boolean itsSock = value >= 8;
        if (!itsSock && (player == null || player != current))
            return false;

        boolean itsDudo = value == 7 && this.lastPlayer != null;
        String picked = "";

        if (itsSock || itsDudo) { // its dudo!
            sockUpdate(false);
            tookAction = true;
            int correct = 0;

            for (User plr : this.players)
                for (int dice : plr.getLastShuffle())
                    if (dice == this.lastValue || dice == 1)
                        correct++;

            User victim = player;

            if (itsSock)
                if (correct == this.lastAmount) {
                    ServerStorage.incrementDice(player.getCurrentToken(), 1);
                    this.dudoUpdate(true, player, true);
                }
                else {
                    ServerStorage.incrementDice(player.getCurrentToken(), -1);
                    this.dudoUpdate(false, player, true);
                }
            else
                if (correct <= this.lastAmount) {
                    ServerStorage.incrementDice(player.getCurrentToken(), -1);
                    this.dudoUpdate(false, player, false);
                } else {
                    ServerStorage.incrementDice(this.lastPlayer.getCurrentToken(), -1);
                    this.dudoUpdate(true, this.lastPlayer, false);
                    victim = this.lastPlayer;
                }

            if (ServerStorage.getDice(victim.getCurrentToken()) <= 0) {
                this.finished.putIfAbsent(player.getCurrentToken(), true);
                if (checkWin(victim))
                    return true;
            }

            try {
                Thread.sleep(5000);
                setupTurn();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        else {
            boolean am = amount > 0;
            amount = amount < this.lastAmount ? this.lastAmount + 1 : amount;

            if (this.lastValue >= 6)
                value = 1;
            else if (value < 6)
                value = value < this.lastValue ? this.lastValue + 1 : value;

            if (am)
                this.lastAmount = amount;

            if (!am || this.lastPlayer == null)
                this.lastValue = value;

            picked = picked + STR."Amount: \{this.lastAmount}, Value: \{this.lastValue == 1 ? "J" : this.lastValue}";
            this.lastPlayer = current;
        }

        ServerStorage.incrementLobbyShift(this.lobbyId);
        startShift(!tookAction, picked);
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

        if (player == null || ServerStorage.getDice(player.getCurrentToken()) <= 0) {
            if (player != null) {
                this.finished.putIfAbsent(player.getCurrentToken(), true);
                checkWin(player);
            }

            ServerStorage.incrementLobbyShift(this.lobbyId);
            this.startShift(canDudo);
            return;
        } else {
            this.sockUpdate(true);
            this.pickUpdate(player, canDudo);
        }

        System.err.println(player.getUsername());
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
        if (pause) {
            if (!this.started)
                return;

            this.paused = true;
        }
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
            replicatedData.put("Started", this.started);
            replicatedData.put("Host", currentHost.getUsername());

            replicateMessage("Members", replicatedData, true);
        }).start();
    }

    private void dudoUpdate(boolean dudo, User plr, boolean itsSock) {
        LinkedTreeMap<String, Object> replicatedData = new LinkedTreeMap<>();
        LinkedList<String> plrs = new LinkedList<>();

        for (User player : this.players) {
            StringBuilder result = new StringBuilder();
            result.append(player.getUsername()).append(": ");

            for (int dice : player.getLastShuffle())
                result.append(dice == 1 ? "J" : dice).append(" ");

            plrs.add(result.toString());
        }

        replicatedData.put("Success", true);
        replicatedData.put("Results", plrs);
        replicatedData.put("Victim", STR."\{plr.getUsername()} \{itsSock ? (dudo ? "earned" : "lost") : "lost"} a dice!");
        replicatedData.put("Verdict", STR."\{itsSock ? "Sock" : "Dudo"} was \{(dudo ? "right" : "wrong")}!");
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

    private static void diceUpdate(User player, boolean oldDices) {
        LinkedTreeMap<String, Object> replicatedData = new LinkedTreeMap<>();
        StringBuilder results = new StringBuilder();

        for (Integer dice : (oldDices ? player.getLastShuffle() : player.shuffle())) {
            results.append(STR."\{dice == 1 ? "J" : dice.toString()} ");
        }

        replicatedData.put("Success", true);
        replicatedData.put("Dice", results.toString());
        player.getHandler().sendMessage("Dice", replicatedData, true);
    }

    private void sockUpdate(boolean canSock) {
        if (canSock && this.lastPlayer == null)
            canSock = false;

        LinkedTreeMap<String, Object> replicatedData = new LinkedTreeMap<>();
        replicatedData.put("Success", true);
        replicatedData.put("Sock", canSock);

        LinkedTreeMap<String, Boolean> blacklist = new LinkedTreeMap<>();
        if (canSock) {
            User current = this.getUserByShift();
            if (current != null)
                blacklist.put(current.getCurrentToken(), true);

            if (this.lastPlayer != null)
                blacklist.put(this.lastPlayer.getCurrentToken(), true);

            this.finished.forEach((player, value) -> {
                blacklist.put(player, true);
            });
        }

        this.replicateMessage("Sock", replicatedData, true, blacklist);
    }

    public void tokenUpdate(User player, String token) {
        LinkedTreeMap<String, Object> data = new LinkedTreeMap<>();
        data.put("Success", true);
        data.put("Token", this.join(player));

        ClientHandler handler = player.getHandler();
        handler.sendMessage("Join", data, true);
    }

    private boolean checkWin(User player) {
        if (player != null && this.finished.size() >= this.size -1) {
            LinkedTreeMap<String, Object> data = new LinkedTreeMap<>();
            this.started = false;

            data.put("Success", true);
            data.put("User", player.getUsername());
            this.replicateMessage("Winner", data, true);

            new Thread(() -> {
                try {
                    Thread.sleep(5000);
                    User host = getHost();
                    Game newLobby = new Game(host, this.size, this.password);

                    this.players.forEach((plr) -> {
                        if (plr != host)
                            tokenUpdate(plr, newLobby.join(plr));
                    });

                    this.remove();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }).start();
            return true;
        }

        return false;
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
