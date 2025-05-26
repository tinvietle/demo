package com.example;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.InputStreamReader;
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
    private BufferedReader input;
    private PrintWriter output;
    private String username;
    private String serverDirectory;
    private AbstractFTPFunctions ftp;
    private boolean userReceived = false;
    private String pendingUsername = null;

    public ServerHandler(Socket socket) {
        this.socket = socket;
        try {
            input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            output = new PrintWriter(socket.getOutputStream(), true);
            
            // Send welcome message
            output.println("220 Welcome to FTP server");
            output.println("If you do not have a username for this system, you can log in using anonymous FTP.");
            output.println("To do this, enter 'USER anonymous' as your username.");
            output.println("When prompted for a password, enter 'PASS your_email@example.com'.");
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleUser(String[] args) {
        if (args.length < 2) {
            output.println("501 Syntax error in parameters or arguments");
            return;
        }
        
        pendingUsername = args[1];
        userReceived = true;
        output.println("331 User name okay, need password");
    }

    private void handlePass(String[] args) {
        if (!userReceived || pendingUsername == null) {
            output.println("503 Bad sequence of commands");
            return;
        }
        
        if (args.length < 2) {
            output.println("501 Syntax error in parameters or arguments");
            return;
        }
        
        String password = args[1];
        
        try {
            // Check if user is anonymous
            if (pendingUsername.equalsIgnoreCase("anonymous")) {
                if (recordAnonymousUser(password)) {
                    output.println("230 Anonymous user logged in");
                    username = "public";
                    initializeFTP();
                } else {
                    output.println("530 Login failed");
                    resetLoginState();
                }
                return;
            }
            
            // Regular user authentication
            if (authenticate(pendingUsername, password)) {
                output.println("230 User logged in, proceed");
                username = pendingUsername;
                initializeFTP();
            } else {
                output.println("530 Login failed");
                resetLoginState();
            }
        } catch (Exception e) {
            output.println("530 Login failed");
            resetLoginState();
        }
    }

    private void resetLoginState() {
        userReceived = false;
        pendingUsername = null;
        username = null;
    }

    private void initializeFTP() {
        try {
            serverDirectory = "src/main/java/com/example/storage/" + username;
            ensureUserDirectory(serverDirectory);
            
            if (username.equalsIgnoreCase("public")) {
                ftp = new AnonymousFTPFunctions(socket, serverDirectory);
            } else {
                ftp = new UserFTPFunctions(socket, serverDirectory);
            }
        } catch (IOException e) {
            output.println("530 Failed to initialize user session");
            resetLoginState();
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
            if (userDir.mkdir()) {
                output.println("257 Directory created successfully: " + userDirPath);
            } else {
                output.println("550 Failed to create directory: " + userDirPath);
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return;
            }
        }

        output.println("220 Welcome to the FTP server, " + username);
        output.println("214 Type \"help\" for a list of available commands.");
    }

    private void sendMessage(String message) {
        output.println(message);
    }

    @Override
    public void run() {
        try {
            String message;
            while (true) {
                if (socket.isClosed()) break;
                
                String userInput = input.readLine();
                if (userInput == null) break;
                
                System.out.println("Received: " + userInput);
                String[] parts = userInput.split(" ");
                if (parts.length == 0) continue;
                
                message = parts[0].toUpperCase();

                // Handle authentication commands first
                switch (message) {
                    case "USER":
                        handleUser(parts);
                        continue;
                    case "PASS":
                        handlePass(parts);
                        continue;
                    case "QUIT":
                        output.println("221 Service closing control connection");
                        System.out.println("Client disconnected");
                        socket.close();
                        return;
                }

                // Check if user is logged in for other commands
                if (username == null || ftp == null) {
                    output.println("530 Please login with USER and PASS");
                    continue;
                }

                // Handle FTP commands with standard names
                switch (message) {
                    case "STOR":
                        ftp.receiveFile(parts);
                        break;
                    case "RETR":
                        ftp.sendFile(parts);
                        break;
                    case "LIST":
                    case "NLST":
                        ftp.listFiles(parts);
                        break;
                    case "DELE":
                        ftp.deleteFile(parts);
                        break;
                    case "MKD":
                    case "XMKD":
                        ftp.createDirectory(parts);
                        break;
                    case "RMD":
                    case "XRMD":
                        ftp.deleteDirectory(parts);
                        break;
                    case "RNFR":
                        ftp.handleRenameFrom(parts);
                        break;
                    case "RNTO":
                        ftp.handleRenameTo(parts);
                        break;
                    case "PWD":
                    case "XPWD":
                        ftp.printWorkingDirectory(parts);
                        break;
                    case "CWD":
                        ftp.changeDirectory(parts);
                        break;
                    case "PASV":
                        handlePasv();
                        break;
                    case "EPSV":
                        handleEpsv();
                        break;
                    case "PORT":
                        ftp.handlePort(parts);
                        break;
                    case "TYPE":
                        ftp.handleType(parts);
                        break;
                    case "EPRT":
                        ftp.handleEPort(parts);
                        break;
                    case "SYST":
                        output.println("215 UNIX Type: L8");
                        break;
                    case "FEAT":
                        output.println("211-Features:");
                        output.println(" MDTM");
                        output.println(" SIZE");
                        output.println("211 End");
                        break;
                    case "NOOP":
                        output.println("200 NOOP command successful");
                        break;
                    default:
                        output.println("502 Command not implemented: " + message);
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

    private void handlePasv() {
        String myIp = "127.0.0.1";
        String[] myIpSplit = myIp.split("\\.");
        int dataPort = 20;
        int p1 = dataPort / 256;
        int p2 = dataPort % 256;

        output.println("227 Entering Passive Mode (" + myIpSplit[0] + "," + myIpSplit[1] + "," + myIpSplit[2] + "," + myIpSplit[3] + "," + p1 + "," + p2 + ")");
    }

    private void handleEpsv() {
        int dataPort = 20;
        output.println("229 Entering Extended Passive Mode (|||" + dataPort + "|)");
    }
    // ...existing methods...
}
