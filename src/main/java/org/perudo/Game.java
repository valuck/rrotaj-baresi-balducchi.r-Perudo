package org.perudo;

import Storage.ServerStorage;
import Messaging.User;
import com.google.gson.internal.LinkedTreeMap;

import java.util.LinkedList;

public class Game {
    private static final LinkedTreeMap<Integer, Game> games = new LinkedTreeMap();
    private final LinkedList<User> players;
    private final String password;
    private final int lobbyId;

    public Game(int size, User host, String password) {
        this.lobbyId = ServerStorage.newLobby(size);
        this.players = new LinkedList<>();
        this.password = password;
        this.players.add(host);

        games.put(this.lobbyId, this);
    }

    public void join(User user) {
        user.setLobbyId(this.lobbyId);
        this.players.add(user);
    }

    public static Game getByLobbyId(int lobbyId) {
        if (games.containsKey(lobbyId))
            return games.get(lobbyId);

        return null;
    }

    public static LinkedTreeMap<Integer, Game> getLobbies() {
        return games;
    }

    public User getHost() {
        return this.players.getFirst();
    }

    public String getPassword() {
        return password;
    }
}
