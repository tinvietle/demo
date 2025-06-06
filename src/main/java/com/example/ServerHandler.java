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
            sendMessage("Directory does not exist: /" + username + " - creating new directory");
            if (userDir.mkdir()) {
                sendMessage("Directory created successfully: /" + username);
            } else {
                sendMessage("Failed to create directory: /" + username);
                try {
                    socket.close();    // now caught locally
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return;
            }
        } else {
            sendMessage("Directory already exists: /" + username);
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
                String userInput = input.readUTF();
                if (userInput == null) break;
                System.out.println("Received: " + username + " type " + userInput);
                String[] parts = userInput.split(" ");
                message = parts[0];

                // Handle commands
                switch (message) {
                    case "put":
                        ftp.receiveFile(parts);
                        break;
                    case "get":
                        ftp.sendFile(parts);
                        break;
                    case "ls":
                        ftp.listFiles(parts);
                        break;
                    case "rm":
                        ftp.deleteFile(parts);
                        break;
                    case "mkdir":
                        ftp.createDirectory(parts);
                        break;
                    case "rmdir":
                        ftp.deleteDirectory(parts);
                        break;
                    case "mv":
                        ftp.moveFile(parts);
                        break;
                    case "pwd":
                        ftp.printWorkingDirectory(parts);
                        break;
                    case "help":
                        ftp.showHelp(parts);
                        break;
                    case "cd":
                        ftp.changeDirectory(parts);
                        break;
                    case "quit":
                        output.writeUTF("221 Service closing control connection"); 
                        System.out.println("Client disconnected");
                        socket.close();
                        return;
                    default:
                        output.writeUTF("Unknown command. Type 'help' for a list of commands.");
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
