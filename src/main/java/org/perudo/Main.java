package org.perudo;

import Messaging.User;

import java.util.LinkedHashMap;

public class Main {
    public static void main(String[] args) {
        // Server testing
        ServerInterface server = new ServerInterface(3000);
        new Thread(server).start();

        ClientInterface client = new ClientInterface("localhost", 3000);
        new Thread(client).start();

        client.initConnection();

        LinkedHashMap<String, String> data = new LinkedHashMap<>();
        data.put("Username", "Rich");
        data.put("Token", null);

        client.sendMessage("Info", data, true);


        try { // wait client to load
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        User target = User.getUserByName("Rich");
        if (target != null) { // If user found
            ClientHandler targetClient = target.getHandler();
            if (targetClient != null) // If client handler is connected
                targetClient.sendMessage("Campo", "Alura!", false);
        }

        ClientHandler.replicateMessage("Campo2", "Mat!", false);

        /*
        LinkedHashMap<String, Integer> data = new LinkedHashMap<>();
        data.put("Mat", 10);
        data.put("Sos", 20);

        User user = new User();
        //user.setEncodingKey("MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAxGgZiGV/qwYYlpCOCLHsMULNdd4j96HKPZHg5gM7w9LcEcu0K+3g6eSMoLQ0YEFycc7n9zK9GE4TvT6v3VFvIwFKUfBBQ5OqmJjVD3mYx8M4JzGA/naUg7cIqEVJ33Wv/AkJNGDmnsyLfQbE3Xsr8mks/RT9Opqt0Re/zq5jmGJHvlao+5ownF/h3nVho6J0M5itvI32fg9Avajjs7xWR04k+YbZzSPb1GVQAgeEciJI1Sqky1d2a86byVFezUFI8rDuoyZ6SMXf/coGlX6z8jb5jct0avu7pgWrUHWgjT+Xv6KgEePHk+Z7Hi/0y6/4DdTlHanj/wO4tH1r07DImQIDAQAB");

        Message<LinkedHashMap<String, Integer>> msg = new Message<>(user, "Alura", data, true);
        System.out.println(msg.toJson());

        Message<LinkedHashMap<String, Integer>> received = new Message<>(msg.toJson());
        */
    }
}