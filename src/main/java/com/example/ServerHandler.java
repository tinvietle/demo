package com.example;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

import org.bson.Document;

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
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String helloClient() throws IOException {
        output.writeUTF("Enter username:");
        String user = input.readUTF();
        output.writeUTF("Enter password:");
        String pass = input.readUTF();

        if (!authenticate(user, pass)) {
            output.writeUTF("Invalid credentials. Closing connection.");
            socket.close();
            return null;
        }

        output.writeUTF("Authentication successful");
        output.writeUTF("Hello Client");
        output.writeUTF("Welcome to the FTP server");
        return user;
    }
    // 6. New helper method querying MongoDB
    private boolean authenticate(String user, String pass) {
        Document userDoc = usersColl.find(Filters.eq("username", user)).first();  // :contentReference[oaicite:11]{index=11}
        if (userDoc == null) return false;
        String storedPass = userDoc.getString("password");
        return storedPass.equals(pass);  // replace with secure hash comparison later :contentReference[oaicite:12]{index=12}
    }

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
                message = parts[0];
                switch (message) {
                    case "put":
                        ftp.receiveFile();
                        break;
                    case "get":
                        // Get the file name from message
                        String filename = parts[1];
                        String filePath = serverDirectory + "/" + filename;
                        ftp.sendFile(filePath);
                        break;
                    case "ls":
                        ftp.listFiles();
                        break;
                    case "delete":
                        // Get the file name from message
                        String fileToDelete = parts[1];
                        String filePathToDelete = serverDirectory + "/" + fileToDelete;
                        ftp.deleteFile(filePathToDelete);
                        break;
                    case "rename":
                        // Get old and new file names from message
                        String oldName = parts[1];
                        String newName = parts[2];
                        ftp.renameFile(serverDirectory, oldName, newName);
                        break;
                    case "mkdir":
                        if(parts.length >= 2){
                            ftp.createDirectory(parts[1]);
                        } else {
                            output.writeUTF("Usage: mkdir <directoryName>");
                        }
                        break;
                    case "rmdir":
                        if(parts.length >= 2){
                            ftp.deleteDirectory(parts[1]);
                        } else {
                            output.writeUTF("Usage: rmdir <directoryName>");
                        }
                        break;
                    case "move":
                        if(parts.length >= 3){
                            ftp.moveFile(parts[1], parts[2]);
                        } else {
                            output.writeUTF("Usage: move <source> <destination>");
                        }
                        break;
                    case "pwd":
                        ftp.printWorkingDirectory();
                        break;
                    case "help":
                        output.writeUTF("Available commands: put, get, ls, delete, rename <oldName> <newName>, mkdir <dirName>, rmdir <dirName>, move <source> <destination>, pwd, help, quit");
                        break;
                    case "quit":
                        output.writeUTF("221 Service closing control connection"); 
                        System.out.println("Client disconnected");
                        socket.close();
                        return;
                    case "cd":
                        if (parts.length >= 2){
                            String directory = parts[1];
                            ftp.changeDirectory(directory);
                        } else {
                            output.writeUTF("Usage: cd <directory>");
                        }
                        break;
                    default:
                        System.out.println("Unknown command");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
