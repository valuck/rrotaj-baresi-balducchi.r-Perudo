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

    private static final LinkedTreeMap<Integer, Game> games = new LinkedTreeMap<>(); // List of the lobbies
    private final LinkedTreeMap<String, Boolean> finished = new LinkedTreeMap<>(); // List of the user who lost in the current lobby
    private final LinkedList<User> disconnected = new LinkedList<>(); // List of the user who disconnected from the current lobby
    private final LinkedList<User> players = new LinkedList<>(); // List of the players in the current lobby
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

    private String hashString(String string) { // Hashes the lobby's password
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

    private Game(int lobbyId) { // Used to restore a lobby from the database
        this.lobbyId = lobbyId; // Retrieve name and player size
        this.name = ServerStorage.getLobbyName(lobbyId);
        this.size = ServerStorage.getLobbySize(lobbyId);

        // Retrieve the password
        String pass = ServerStorage.getLobbyPassword(lobbyId);
        this.password = pass.isEmpty() ? null : pass;

        LinkedList<String> players = ServerStorage.getTokensInLobby(lobbyId);
        players.forEach((value) -> { // Put old users in the wait list
            User user = new User();
            user.setCurrentToken(value);
            user.setDice(ServerStorage.getDice(value));

            disconnected.add(user);
            ServerStorage.removeToken(value);
        });

        games.put(this.lobbyId, this); // Register in the list of the lobbies
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

    public Game(int size, User host, String password) { // Public constructor
        this.password = hashString(password); // Hash the password
        this.size = size;
        this.name = STR."\{host.getUsername()}'s lobby";

        // Create a new lobby in the database
        this.lobbyId = ServerStorage.newLobby(this.name, this.size, this.password);
        games.put(this.lobbyId, this); // Register in the list of the lobbies
        tokenUpdate(host, this.join(host)); // Make the host join and send his new token
    }

    private Game(User host, int size, String password) { // Private constructor
        this.size = size;
        this.password = password; // Password is already hashed
        this.name = STR."\{host.getUsername()}'s lobby";

        // Create a new lobby in the database
        this.lobbyId = ServerStorage.newLobby(this.name, this.size, this.password);
        games.put(this.lobbyId, this); // Register in the list of the lobbies
        tokenUpdate(host, this.join(host)); // Make the host join and send his new token
    }

    public void remove() {
        // Remove the lobby from the list of lobbies and from the database
        games.remove(this.lobbyId);
        ServerStorage.deleteLobby(this.lobbyId);
    }

    private String join(User user) {
        String token; // Check if the game is already started and its waiting for players to reconnect
        if (this.started && this.players.size() + this.disconnected.size() >= this.size) { // If the lobby is full
            String tempToken = user.getCurrentToken(); // Get the token used in the login
            boolean found = false;

            if (tempToken != null) {
                for (User value : this.disconnected) { // If a disconnected player access with the right Token
                    if (value.getCurrentToken().equals(tempToken)) {
                        // Set its old data to the new User object
                        user.setDice(value.getDice());
                        user.setShuffle(value.getLastShuffle());

                        // Remove from the disconnected players list
                        this.disconnected.remove(value);
                        found = true;
                        break;
                    }
                }
            }

            if (found) { // If the user has rejoined
                token = tempToken;
                // Send all the data required
                ClientHandler handler = user.getHandler();

                if (handler != null) { // Sync all his data
                    LinkedTreeMap<String, Object> replicatedData = new LinkedTreeMap<>();
                    replicatedData.put("Success", true);
                    diceUpdate(user, true);

                    replicatedData.put("Picks", STR."Amount: \{this.lastAmount}, Value: \{this.lastValue == 1 ? "J" : this.lastValue}");
                    handler.sendMessage("Picked", replicatedData, true);
                    replicatedData.remove("Picked");
                }
            } else
                return null;
        }
        else { // Assign a new token once created in the database
            token = ServerStorage.newToken(this.lobbyId);
            user.setCurrentToken(token);
        }

        // Disconnect if in another lobby
        if (user.getLobby() != this)
            user.disconnectFromLobby();
        user.setLobby(this); // Set this as his new lobby

        this.players.add(user); // Add player in the lobby players list
        this.used = true; // Tell the server to stop the unused lobby loop when reloaded

        if (this.players.size() >= this.size) // Check if the game has to resume
            this.paused = false;

        this.membersUpdated(this.paused); // Update the member list on the clients
        User next = this.getUserByShift();

        if (this.started && !this.paused && next != null) // Send the pick update to the player that has the turn
            pickUpdate(next, this.lastPickState);

        return token; // Return the token if successfully joined the lobby
    }

    public String join(User user, String password) { // Join the lobby with password
        if (this.password != null && (password == null || !Argon2Factory.create().verify(this.password, password.toCharArray())))
            return null; // Return if invalid

        return join(user); // Actual join function
    }

    public void disconnect(User user) {
        if (!this.players.contains(user))
            return; // If user is not in the lobby

        this.disconnected.add(user); // Add to the wait to reconnect list
        this.players.remove(user); // Remove from the players list
        user.setLobby(null);

        if (this.players.isEmpty()) { // Delete the lobby if everybody left
            this.remove();
            return;
        }

        this.membersUpdated(this.started); // Update the member list on the clients
    }

    public void startGame() {
        if (this.started) // Start the game if not already started
            return;

        this.started = true;
        this.disconnected.clear(); // Clear the disconnected users list

        this.setupTurn(); // Setup variables and players
        this.startShift(false); // Start the round
    }

    private void setupTurn() {
        this.lastPlayer = null; // Set to default
        this.lastAmount = 0;
        this.lastValue = 0;

        for (User player : this.players) { // Send the dice of each player to their client
            diceUpdate(player, false);
        }
    }

    public boolean processPicks(User player, int amount, int value) {
        User current = getUserByShift(); // get the user on turn
        boolean tookAction = false; // whenever calza or dodo is used

        boolean itsSock = value >= 8; // Check for calza
        if (!itsSock && (player == null || player != current)) // Check if the user can do this action
            return false;

        boolean itsDudo = value == 7 && this.lastPlayer != null; // Check for dudo
        String picked = "";

        if (itsSock || itsDudo) {
            sockUpdate(false); // Disable all calza from the clients
            tookAction = true;
            int correct = 0;

            for (User plr : this.players) // Check how many entries of the latest value are there
                for (int dice : plr.getLastShuffle())
                    if (dice == this.lastValue || dice == 1)
                        correct++;

            User victim = player; // The player who loses or wins the dice

            if (itsSock) // If its calza
                if (correct == this.lastAmount) { // If calza was right
                    ServerStorage.incrementDice(player.getCurrentToken(), 1); // Increment dice in the player's database
                    this.dudoUpdate(true, player, true); // Tell results to the clients
                }
                else { // If it wasn't
                    ServerStorage.incrementDice(player.getCurrentToken(), -1); // Decrease dice in the player's database
                    this.dudoUpdate(false, player, true); // Tell results to the clients
                }
            else // If it was dudo
                if (correct <= this.lastAmount) {
                    ServerStorage.incrementDice(player.getCurrentToken(), -1); // Decrease dice in the player's database
                    this.dudoUpdate(false, player, false); // Tell results to the clients
                } else {
                    ServerStorage.incrementDice(this.lastPlayer.getCurrentToken(), -1); // Decrease dice in the player's database
                    this.dudoUpdate(true, this.lastPlayer, false); // Tell results to the clients
                    victim = this.lastPlayer; // Update the victim, as it is the player from the previous round
                }

            if (ServerStorage.getDice(victim.getCurrentToken()) <= 0) { // If the victim has run out of dice
                this.finished.putIfAbsent(player.getCurrentToken(), true); // Set as loser, SKILL ISSUE!!!1!11!
                if (checkWin(victim)) // Check if someone won the game
                    return true;
            }

            try {
                Thread.sleep(5000);
                setupTurn(); // Wait five seconds and start a new round
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        else { // If it was a pick
            boolean am = amount > 0; // Check if it's the amount being edited
            amount = amount < this.lastAmount ? this.lastAmount + 1 : amount; // Adjust the amount to prevent exploiting

            if (am) // Update the amount
                this.lastAmount = amount;

            if (!am || this.lastPlayer == null) { // If the amount is not being edited, or it's the first turn after any action
                if (this.lastValue >= 6) // If the value reached its max
                    value = 1; // Make it restart from 1
                    //value = value < 6 && value > 0 ? value : 1;
                else if (value < 6)
                    value = value < this.lastValue ? this.lastValue + 1 : value; // Adjust the value to prevent exploiting

                this.lastValue = value; // Update the value
            }

            // Sent the text to sent to each client
            picked = picked + STR."Amount: \{this.lastAmount}, Value: \{this.lastValue == 1 ? "J" : this.lastValue}";
            this.lastPlayer = current; // Update the last player
        }

        ServerStorage.incrementLobbyShift(this.lobbyId); // Pass the turn to the next player, updated and automatically restart in the database
        startShift(!tookAction, picked); // Start a new round and tell the clients what to do next
        return true;
    }

    private User getUserByShift() {
        // Get the current turn by its shift from the database
        int shift = ServerStorage.getLobbyShift(this.lobbyId) -1;
        LinkedList<String> tokens = ServerStorage.getTokensInLobby(this.lobbyId);

        // Assure that the shift is valid
        if (shift >= 0 && tokens.size() > shift) {
            String selected = tokens.get(shift); // Get the token of the user that matches with the shift

            // Return the player that matches the current one
            for (User player : this.players) {
                if (player.getCurrentToken().equals(selected)) {
                    return player;
                }
            }
        }

        return null;
    }

    private void startShift(boolean canDudo) {
        User player = getUserByShift(); // Get the current playing user
        this.lastPickState = canDudo; // Update in case a player has to reconnect

        // Check if the player has run out of dice
        if (player == null || ServerStorage.getDice(player.getCurrentToken()) <= 0) {
            if (player != null) {
                this.finished.putIfAbsent(player.getCurrentToken(), true); // Put him in the list of the losers, SKILL ISSUE!!!1!!1
                checkWin(player); // Check if anybody won
            }

            // Pass to the next player updating the database
            ServerStorage.incrementLobbyShift(this.lobbyId);
            this.startShift(canDudo); // Recaller, retrying to start the new round
            return;
        } else {
            this.sockUpdate(true); // Tell the clients who can claim Calza
            this.pickUpdate(player, canDudo); // Tell the clients who's going to pick
        }
    }

    private void startShift(boolean canDudo, String picked) {
        LinkedTreeMap<String, Object> replicatedData = new LinkedTreeMap<>();
        replicatedData.put("Success", true);
        replicatedData.put("Picks", picked);

        // Update the last picked values to the clients
        this.replicateMessage("Picked", replicatedData, true);
        this.startShift(canDudo); // Call the actual function
    }

    public static Game getByLobbyId(int lobbyId) {
        if (games.containsKey(lobbyId)) // get the lobby from its id, if present
            return games.get(lobbyId);

        return null;
    }

    public void replicateMessage(String scope, Object data, boolean encode, LinkedTreeMap<String, Boolean> blacklist) { // Sends the message to all lobby members
        LinkedList<User> users = this.players;

        // Send a new message to all the connected users
        for (int i = 0; i < users.size(); i++) { // foreach loops can give an error
            User user = users.get(i);
            if (blacklist.containsKey(user.getCurrentToken())) // Skip the blacklisted players
                continue;

            // Get the player's handler
            ClientHandler handler = user.getHandler();

            if (handler != null) // if the user has a valid handler
                handler.sendMessage(scope, data, encode);
        }
    }

    public void replicateMessage(String scope, Object data, boolean encode) {
        replicateMessage(scope, data, encode, new LinkedTreeMap<>()); // Replicate to all the clients with an empty blacklist
    }

    public void membersUpdated(boolean pause) {
        if (pause) {
            if (!this.started) // Return if the game is not started or its finished
                return;

            this.paused = true;
        }

        new Thread(() -> { // Update all the clients without yielding the execution
            LinkedTreeMap<String, Object> replicatedData = new LinkedTreeMap<>();
            LinkedList<String> plrs = new LinkedList<>();
            User currentHost = getHost();

            // Inserting players
            players.forEach((value) -> plrs.add(value.getUsername() + (value == currentHost ? " (host)" : "")));

            replicatedData.put("Size", size);
            replicatedData.put("Pause", pause);
            replicatedData.put("Success", true);
            replicatedData.put("Players", plrs);
            replicatedData.put("Name", getName());
            replicatedData.put("Started", this.started);
            replicatedData.put("Host", currentHost.getUsername());

            // Send the message
            replicateMessage("Members", replicatedData, true);
        }).start();
    }

    private void dudoUpdate(boolean dudo, User plr, boolean itsSock) {
        LinkedTreeMap<String, Object> replicatedData = new LinkedTreeMap<>();
        LinkedList<String> plrs = new LinkedList<>();

        // Generate the result of the action (dudo/calza)
        for (User player : this.players) {
            StringBuilder result = new StringBuilder(); // Insert name of the player
            result.append(player.getUsername()).append(": ");

            // Insert dice of the player
            for (int dice : player.getLastShuffle())
                result.append(dice == 1 ? "J" : dice).append(" ");

            plrs.add(result.toString()); // Add to the results
        }

        replicatedData.put("Success", true);
        replicatedData.put("Results", plrs); // Changes whenever its calza or dudo
        replicatedData.put("Victim", STR."\{plr.getUsername()} \{itsSock ? (dudo ? "earned" : "lost") : "lost"} a dice!");
        replicatedData.put("Verdict", STR."\{itsSock ? "Sock" : "Dudo"} was \{(dudo ? "right" : "wrong")}!");
        replicateMessage("Dudo", replicatedData, true); // Replicate to each client
    }

    private void pickUpdate(User player, boolean canDudo) {
        LinkedTreeMap<String, Object> replicatedData = new LinkedTreeMap<>();
        replicatedData.put("Success", true);
        replicatedData.put("Dudo", canDudo);
        replicatedData.put("Amount", this.lastAmount);
        replicatedData.put("Value", this.lastValue);

        // Tell the current playing player to make a choice
        player.getHandler().sendMessage("Pick", replicatedData, true);
        LinkedTreeMap<String, Boolean> blacklist = new LinkedTreeMap<>();
        blacklist.put(player.getCurrentToken(), true);

        replicatedData.clear();
        replicatedData.put("Success", true);
        replicatedData.put("User", player.getUsername());
        // Tell all the others who's making the choice
        replicateMessage("Turn", replicatedData, true, blacklist);
    }

    private static void diceUpdate(User player, boolean oldDices) {
        LinkedTreeMap<String, Object> replicatedData = new LinkedTreeMap<>();
        StringBuilder results = new StringBuilder();

        // For each dice append it to the string
        for (Integer dice : (oldDices ? player.getLastShuffle() : player.shuffle())) {
            results.append(STR."\{dice == 1 ? "J" : dice.toString()} "); // Replace 1 with J (jolly)
        }

        replicatedData.put("Success", true);
        replicatedData.put("Dice", results.toString()); // Tell the player his set of dice
        player.getHandler().sendMessage("Dice", replicatedData, true);
    }

    private void sockUpdate(boolean canSock) {
        if (canSock && this.lastPlayer == null) // If it's the first round after any action
            canSock = false;

        LinkedTreeMap<String, Object> replicatedData = new LinkedTreeMap<>();
        replicatedData.put("Success", true);
        replicatedData.put("Sock", canSock);

        LinkedTreeMap<String, Boolean> blacklist = new LinkedTreeMap<>();
        if (canSock) {
            User current = this.getUserByShift(); // Get the current playing player
            if (current != null)
                blacklist.put(current.getCurrentToken(), true); // Put him in the blacklist

            if (this.lastPlayer != null) // Put the last player too, if there
                blacklist.put(this.lastPlayer.getCurrentToken(), true);

            this.finished.forEach((player, value) -> { // Also put all the losers in it, SKILL ISSUE!!!1!1
                blacklist.put(player, true);
            });
        }

        // Tell all the remaining clients that they can claim calza
        this.replicateMessage("Sock", replicatedData, true, blacklist);
        replicatedData.replace("Sock", false);

        blacklist.forEach((token, value) -> { // Tell all the blacklisted ones that they cant anymore
            User player = User.getUserByToken(token); // Get the player from the token
            if (player != null) {
                ClientHandler handler = player.getHandler(); // Get his handler

                if (handler != null) // Tell his client
                    handler.sendMessage("Sock", replicatedData, true);
            }
        });
    }

    public void tokenUpdate(User player, String token) {
        LinkedTreeMap<String, Object> data = new LinkedTreeMap<>();
        data.put("Success", true);
        data.put("Token", token);

        ClientHandler handler = player.getHandler();
        if (handler != null) // Tell his client the new token
            handler.sendMessage("Join", data, true);
    }

    private boolean checkWin(User player) {
        // If is there only one player left, he won
        if (player != null && this.finished.size() >= this.size -1) {
            LinkedTreeMap<String, Object> data = new LinkedTreeMap<>();
            this.started = false; // Set to finished

            data.put("Success", true);
            data.put("User", player.getUsername()); // Tell all the clients the winner
            this.replicateMessage("Winner", data, true);

            new Thread(() -> { // Wait 5 seconds without stopping the execution
                try {
                    Thread.sleep(5000);
                    User host = getHost(); // Create a new lobby with the current host
                    Game newLobby = new Game(host, this.size, this.password);

                    this.players.forEach((plr) -> { // Move all the players to the current lobby
                        if (plr != host)
                            tokenUpdate(plr, newLobby.join(plr));
                    });

                    this.remove(); // Destroy the current lobby
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

        // Initialize all the lobbies in the database
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
