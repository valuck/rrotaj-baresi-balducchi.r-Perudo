package org.perudo;

import java.sql.Statement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.DriverManager;

public class Main {
    public static void main(String[] args) throws ClassNotFoundException {
        //Class.forName("com.mysql.cj.jdbc.Driver");

        try(Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/", "root", "");
            Statement stmt = conn.createStatement();
        ) {
            // Create the database
            stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS perudo");

            // Select the database
            stmt.execute("USE perudo");

            // Create the 'lobby' table
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS lobby " +
                    "(lobby_id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");

            // Create the 'player' table
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS player " +
                    "(player_id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "player_name VARCHAR(20), " +
                    "joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "lobby_id INT, " +
                    "FOREIGN KEY (lobby_id) REFERENCES lobby(lobby_id))");

            System.out.println("Database created successfully...");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}