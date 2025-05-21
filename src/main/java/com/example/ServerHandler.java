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
    private static final MongoCollection<Document> anonymColl;

    static {
        // 2. Initialize MongoDB client, database, and collection
        String uri = "mongodb+srv://10422050:10422050@tam.kp7bhlj.mongodb.net/";
        mongoClient = MongoClients.create(uri);                             // :contentReference[oaicite:9]{index=9}
        database    = mongoClient.getDatabase("TAM");                       // :contentReference[oaicite:10]{index=10}
        usersColl   = database.getCollection("users");
        anonymColl   = database.getCollection("anonymous");
    }

    private Socket socket;
    private DataInputStream input;
    private DataOutputStream output;
    private String username;
    private String serverDirectory;
    private AbstractFTPFunctions ftp;

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

            // 5. Initialize FTP functions based on user type
            if (username.equalsIgnoreCase("public")) {
                ftp = new AnonymousFTPFunctions(socket, serverDirectory);
            } else {
                ftp = new UserFTPFunctions(socket, serverDirectory);
            }
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String helloClient() throws IOException {
        while (true) {
            output.writeUTF("If you do not have a username for this system, you can log in using anonymous FTP.");
            output.writeUTF("To do this, enter the word 'anonymous' as your username.");
            output.writeUTF("When prompted for a password, enter your email address. This helps the server log anonymous access.");
            output.writeUTF("Once logged in, you'll have access to the public anonymous directory, where you can download files.");
            output.writeUTF("Note: You will not be able to upload, delete, or modify files on the remote server as an anonymous user.");
            output.writeUTF("Enter username:");
            String user = input.readUTF();
            output.writeUTF("Enter password:");
            String pass = input.readUTF();

            // 4. Check if user is anonymous   
            if (user.equalsIgnoreCase("anonymous")) {
                if (recordAnonymousUser(pass)) {
                    output.writeUTF("Logged in as anonymous user.");
                    return "public";
                } else {
                    output.writeUTF("Error logging in as anonymous user. Please try again later.");
                    try {
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
    
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

    private boolean recordAnonymousUser(String pass) {
        Document newAnonymousLogIn = new Document("email", pass)
                                            .append("timeOfLogIn", System.currentTimeMillis());
        try {
            anonymColl.insertOne(newAnonymousLogIn);                        // insertOne example :contentReference[oaicite:6]{index=6}
            sendMessage("Anonymous user recorded successfully.");
            return true;
        } catch (MongoWriteException e) {
            sendMessage("Error recording anonymous user: " + e.getMessage());
            return false;
        }
    }

    private void ensureUserDirectory(String userDirPath) {
        File userDir = new File(userDirPath);

        if (!userDir.exists() || !userDir.isDirectory()) {
            sendMessage("Directory does not exist: " + userDirPath + " - creating new directory");
            if (userDir.mkdir()) {
                sendMessage("Directory created successfully: " + userDirPath);
            } else {
                sendMessage("Failed to create directory: " + userDirPath);
                try {
                    socket.close();    // now caught locally
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return;
            }
        } else {
            sendMessage("Directory already exists: " + userDirPath);
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
                        ftp.showHelp();
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
