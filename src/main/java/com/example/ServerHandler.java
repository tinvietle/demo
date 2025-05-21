package com.example;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.Socket;

import org.bson.Document;

import com.mongodb.MongoWriteException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;

public class ServerHandler implements Runnable {
    private static final MongoClient mongoClient;
    private static final MongoDatabase database;
    private static final MongoCollection<Document> usersColl;

    static {
        // 2. Initialize MongoDB client, database, and collection
        String uri = "mongodb+srv://10422050:10422050@tam.kp7bhlj.mongodb.net/";
        mongoClient = MongoClients.create(uri);                             // :contentReference[oaicite:9]{index=9}
        database    = mongoClient.getDatabase("TAM");                       // :contentReference[oaicite:10]{index=10}
        usersColl   = database.getCollection("users");
    }

    private Socket socket;
    private DataInputStream input;
    private DataOutputStream output;
    private String username;
    private String serverDirectory;

    public ServerHandler(Socket socket) {
        this.socket = socket;
        try {
            input = new DataInputStream(socket.getInputStream());
            output = new DataOutputStream(socket.getOutputStream());
            username = helloClient();
            if (username == null) return;
            serverDirectory = "src/main/java/com/example/storage/" + username;

            // 3. Create directory for the user if it doesn't exist
            ensureUserDirectory(serverDirectory);
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String helloClient() throws IOException {
        while (true) {
            output.writeUTF("Enter username:");
            String user = input.readUTF();
            output.writeUTF("Enter password:");
            String pass = input.readUTF();
    
            if (authenticate(user, pass)) {
                output.writeUTF("Authentication successful");
                return user;
            }
    
            // Invalid credentials: offer retry or signup
            output.writeUTF(
                "Invalid credentials. " +
                "Type 'retry' to try again, 'signup' to create a new account, or 'quit' to exit:"
            );
            String choice = input.readUTF();
            if ("quit".equalsIgnoreCase(choice)) {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            } else if ("signup".equalsIgnoreCase(choice)) {
                output.writeUTF("Enter username:");
                String newUser = input.readUTF();
                output.writeUTF("Enter password:");
                String newPass = input.readUTF();
                if (createAccount(newUser, newPass)) {
                    output.writeUTF("Account created. Please log in now.");
                } else {
                    output.writeUTF("Signup failed. Connection failed.");
                    try {
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            // otherwise loop to retry
        }
    }    

    // 6. New helper method querying MongoDB
    private boolean authenticate(String user, String pass) {
        Document userDoc = usersColl.find(Filters.eq("username", user)).first();  // :contentReference[oaicite:11]{index=11}
        if (userDoc == null) return false;
        String storedPass = userDoc.getString("password");
        return storedPass.equals(pass);  // replace with secure hash comparison later :contentReference[oaicite:12]{index=12}
    }

    private boolean createAccount(String user, String pass) {
        // 1. Check for existing user
        if (usersColl.find(Filters.eq("username", user)).first() != null) {
            sendMessage("Username already exists.");
            return false;
        }
        // 2. Build and insert document
        Document newUser = new Document("username", user)
                                .append("password", pass);
        try {
            usersColl.insertOne(newUser);                        // insertOne example :contentReference[oaicite:6]{index=6}
            sendMessage("Account created successfully.");
            return true;
        } catch (MongoWriteException e) {
            sendMessage("Error creating account: " + e.getMessage());
            return false;
        }
    }


    private void ensureUserDirectory(String userDirPath) {
        File userDir = new File(userDirPath);
        String relativePath = new File(userDirPath).getName(); // use only the last folder name
        if (!userDir.exists() || !userDir.isDirectory()) {
            sendMessage("Directory " + relativePath + " does not exist - creating new directory");
            if (userDir.mkdir()) {
                sendMessage("Directory " + relativePath + " created successfully");
            } else {
                sendMessage("Failed to create directory " + relativePath);
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return;
            }
        } else {
            sendMessage("Directory " + relativePath + " is ready");
        }
        sendMessage("Welcome to the FTP server, " + username);
        sendMessage("Type \"help\" for a list of available commands.");
    }

    private void sendMessage(String message) {
        try {
            output.writeUTF(message);
        } catch (IOException e) {
            e.printStackTrace();
        }        }

    @Override
    public void run() {
        try {
            String message;
            FTPFunctions ftp = new FTPFunctions(socket, serverDirectory);
            // Removed the initial prompt from helloClient; now sending prompt in each iteration
            while (true) {
                if (socket.isClosed()) break;
                output.writeUTF("ftp> "); // Send prompt before reading command so prompt and command appear on one line
                message = input.readUTF();
                if (message == null) break;
                System.out.println("Received: " + message);
                String[] parts = message.split(" ");
                String command = parts[0];
                try {
                    switch (command) {
                        case "put":
                            // Check if user is guest
                            boolean isGuest = false;
                            if (username.equals("guest")) {
                                isGuest = true;
                            }
                            ftp.receiveFile(isGuest);
                            break;
                        case "get":
                            if (parts.length != 2) {
                                output.writeUTF("Usage: get <filename>");
                            } else {
                                try {
                                    String filename = parts[1];
                                    // Directly pass the filename since FTPFunctions.sendFile already incorporates serverDirectory.
                                    ftp.sendFile(filename);
                                } catch(Exception e){
                                    output.writeUTF("Error executing get: " + e.getMessage());
                                }
                            }
                            break;
                        case "ls":
                            ftp.listFiles();
                            break;
                        case "delete":
                            if(parts.length != 2){
                                output.writeUTF("Usage: delete <filename>");
                            } else {
                                try {
                                    String fileToDelete = parts[1];
                                    ftp.deleteFile(fileToDelete);
                                } catch(Exception e){
                                    output.writeUTF("Error executing delete: " + e.getMessage());
                                }
                            }
                            break;
                        case "rename":
                            if(parts.length != 3){
                                output.writeUTF("Usage: rename <oldName> <newName>");
                            } else {
                                try {
                                    String oldName = parts[1];
                                    String newName = parts[2];
                                    ftp.renameFile(serverDirectory, oldName, newName);
                                } catch(Exception e){
                                    output.writeUTF("Error executing rename: " + e.getMessage());
                                }
                            }
                            break;
                        case "mkdir":
                            if(parts.length != 2){
                                output.writeUTF("Usage: mkdir <directoryName>");
                            } else {
                                try {
                                    ftp.createDirectory(parts[1]);
                                } catch(Exception e){
                                    output.writeUTF("Error executing mkdir: " + e.getMessage());
                                }
                            }
                            break;
                        case "rmdir":
                            if(parts.length != 2){
                                output.writeUTF("Usage: rmdir <directoryName>");
                            } else {
                                try {
                                    ftp.deleteDirectory(parts[1]);
                                } catch(Exception e){
                                    output.writeUTF("Error executing rmdir: " + e.getMessage());
                                }
                            }
                            break;
                        case "mv":
                            if(parts.length != 3){
                                output.writeUTF("Usage: move <source> <destination>");
                            } else {
                                try {
                                    ftp.moveFile(parts[1], parts[2]);
                                } catch(Exception e){
                                    output.writeUTF("Error executing move: " + e.getMessage());
                                }
                            }
                            break;
                        case "pwd":
                            ftp.printWorkingDirectory();
                            break;
                        case "help":
                            output.writeUTF("Available commands: put, get, ls, delete, rename <oldName> <newName>, mkdir <dirName>, rmdir <dirName>, move <source> <destination>, pwd, help, quit, cd <directory>");
                            break;
                        case "quit":
                            output.writeUTF("221 Service closing control connection");
                            System.out.println("Client disconnected");
                            socket.close();
                            return;
                        case "cd":
                            if(parts.length != 2){
                                output.writeUTF("Usage: cd <directory>");
                            } else {
                                try {
                                    String directory = parts[1];
                                    ftp.changeDirectory(directory);
                                } catch(Exception e){
                                    output.writeUTF("Error executing cd: " + e.getMessage());
                                }
                            }
                            break;
                        default:
                            output.writeUTF("Unknown command. Type 'help' for a list of commands.");
                    }
                } catch(Exception ex){
                    output.writeUTF("Error processing command: " + ex.getMessage());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try { socket.close(); } catch (IOException e) { e.printStackTrace(); }
        }
    }
}
