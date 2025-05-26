package com.example;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Set;

import org.bson.Document;

import com.mongodb.MongoWriteException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;

public class ServerHandler implements Runnable {
    
    // ===============================
    // STATIC MONGODB FIELDS
    // ===============================
    
    private static final MongoClient mongoClient;
    private static final MongoDatabase database;
    private static final MongoCollection<Document> usersColl;
    private static final MongoCollection<Document> anonymColl;

    static {
        String uri = "mongodb+srv://10422050:10422050@tam.kp7bhlj.mongodb.net/";
        mongoClient = MongoClients.create(uri);
        database = mongoClient.getDatabase("TAM");
        usersColl = database.getCollection("users");
        anonymColl = database.getCollection("anonymous");
    }

    // ===============================
    // INSTANCE FIELDS
    // ===============================
    
    private Socket socket;
    private BufferedReader input;
    private PrintWriter output;
    private String username;
    private String serverDirectory;
    private AbstractFTPFunctions ftp;
    private boolean userReceived = false;
    private String pendingUsername = null;

    // ===============================
    // CONSTRUCTOR
    // ===============================
    
    public ServerHandler(Socket socket) {
        this.socket = socket;
        try {
            input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            output = new PrintWriter(socket.getOutputStream(), true);
            sendWelcomeMessage();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendWelcomeMessage() {
        output.println("220 Welcome to FTP server");
        output.println("If you do not have a username for this system, you can log in using anonymous FTP.");
        output.println("To do this, enter 'USER anonymous' as your username.");
        output.println("When prompted for a password, enter 'PASS your_email@example.com'.");
    }

    // ===============================
    // AUTHENTICATION HANDLERS
    // ===============================
    
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
            if (pendingUsername.equalsIgnoreCase("anonymous")) {
                handleAnonymousLogin(password);
            } else {
                handleUserLogin(password);
            }
        } catch (Exception e) {
            output.println("530 Login failed");
            resetLoginState();
        }
    }

    private void handleAnonymousLogin(String password) {
        if (recordAnonymousUser(password)) {
            output.println("230 Anonymous user logged in");
            username = "public";
            initializeFTP();
        } else {
            output.println("530 Login failed");
            resetLoginState();
        }
    }

    private void handleUserLogin(String password) {
        if (authenticate(pendingUsername, password)) {
            output.println("230 User logged in, proceed");
            username = pendingUsername;
            initializeFTP();
        } else {
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

    // ===============================
    // DATABASE OPERATIONS
    // ===============================
    
    private boolean authenticate(String user, String pass) {
        Document userDoc = usersColl.find(Filters.eq("username", user)).first();
        if (userDoc == null) return false;
        String storedPass = userDoc.getString("password");
        return storedPass.equals(pass);
    }

    private boolean recordAnonymousUser(String email) {
        Document newAnonymousLogIn = new Document("email", email)
                                    .append("timeOfLogIn", System.currentTimeMillis());
        try {
            anonymColl.insertOne(newAnonymousLogIn);
            sendMessage("Anonymous user recorded successfully.");
            return true;
        } catch (MongoWriteException e) {
            sendMessage("Error recording anonymous user: " + e.getMessage());
            return false;
        }
    }

    // ===============================
    // UTILITY METHODS
    // ===============================
    
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

    private void handlePasv() {
        String myIp = "127.0.0.1";
        String[] myIpSplit = myIp.split("\\.");
        int dataPort = 20;
        int p1 = dataPort / 256;
        int p2 = dataPort % 256;

        output.println("227 Entering Passive Mode (" + 
                      String.join(",", myIpSplit) + "," + p1 + "," + p2 + ")");
    }

    private void handleEpsv() {
        int dataPort = 20;
        output.println("229 Entering Extended Passive Mode (|||" + dataPort + "|)");
    }

    // ===============================
    // MAIN RUN METHOD
    // ===============================
    
    @Override
    public void run() {
        try {
            String userInput;
            while ((userInput = input.readLine()) != null) {
                if (socket.isClosed()) break;
                
                System.out.println("Received: " + userInput);
                String[] parts = userInput.split(" ");
                if (parts.length == 0) continue;
                
                String command = parts[0].toUpperCase();

                // Handle authentication commands
                if (handleAuthenticationCommands(command, parts)) {
                    continue;
                }

                // Check if user is logged in for other commands
                if (username == null || ftp == null) {
                    output.println("530 Please login with USER and PASS");
                    continue;
                }

                // Handle FTP commands
                handleFTPCommands(command, parts);
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

    private boolean handleAuthenticationCommands(String command, String[] parts) {
        switch (command) {
            case "USER":
                handleUser(parts);
                return true;
            case "PASS":
                handlePass(parts);
                return true;
            case "QUIT":
                output.println("221 Service closing control connection");
                System.out.println("Client disconnected");
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return true;
            default:
                return false;
        }
    }

    private void handleFTPCommands(String command, String[] parts) {
        // Handle FTP commands
        switch (command) {
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
                
            case "NOOP":
                output.println("200 NOOP command successful");
                break;
        
            case "HELP":
                ftp.showHelp(parts);
                break;
        
            default:
                output.println("502 Command not implemented: " + command);
                break;
        }
    }
}
