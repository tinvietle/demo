package com.example;

import com.mongodb.MongoWriteException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;

import org.bson.Document;

public class CreateClient {
    private static final String uri = "mongodb+srv://10422050:10422050@tam.kp7bhlj.mongodb.net/";
    private static final MongoClient mongoClient = MongoClients.create(uri);
    private static final MongoDatabase database = mongoClient.getDatabase("TAM");
    private static final MongoCollection<Document> usersColl = database.getCollection("users");

    public static void main(String[] args) {
        // This is a placeholder for the client creation logic.
        // In a real application, you would create a client that connects to the FTP server.
        String username = "newUser"; // Replace with actual user input
        String password = "12345678"; // Replace with actual user input
        boolean accountCreated = createAccount(username, password);
        if (accountCreated) {
            System.out.println("Account created successfully.");
        } else {
            System.out.println("Failed to create account.");
        }
    }

    private static boolean createAccount(String user, String pass) {
        // 1. Check for existing user
        if (usersColl.find(Filters.eq("username", user)).first() != null) {
            System.out.println("Username already exists.");
            return false;
        }
        // 2. Build and insert document
        Document newUser = new Document("username", user)
                                .append("password", pass);
        try {
            usersColl.insertOne(newUser);
            System.out.println("Account created successfully for user: " + user);
            return true;
        } catch (MongoWriteException e) {
            System.err.println("Error creating account: " + e.getMessage());
            return false;
        }
    }
}
