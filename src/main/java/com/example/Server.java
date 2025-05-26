package com.example;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Main FTP Server class that handles client connections
 */
public final class Server {
    
    // ===============================
    // CONSTANTS
    // ===============================
    
    private static final int NTHREADS = 100; // Maximum threads for this program
    private static final int SERVER_PORT = 1234;
    
    // ===============================
    // STATIC FIELDS
    // ===============================
    
    private static final ExecutorService exec = Executors.newFixedThreadPool(NTHREADS);
    
    // ===============================
    // CONSTRUCTOR
    // ===============================
    
    private Server() {
        // Private constructor to prevent instantiation
    }

    // ===============================
    // MAIN METHOD
    // ===============================
    
    /**
     * Main method to start the FTP server
     * @param args The arguments of the program (unused)
     */
    public static void main(String[] args) {
        System.out.println("Starting FTP Server on port " + SERVER_PORT + "...");
        
        try (ServerSocket serverSocket = new ServerSocket(SERVER_PORT)) {
            System.out.println("FTP Server started successfully. Waiting for connections...");
            
            while (true) {
                final Socket connection = serverSocket.accept();
                System.out.println("New client connected: " + connection.getInetAddress());
                
                ServerHandler clientHandler = new ServerHandler(connection);
                exec.execute(clientHandler);
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            exec.shutdown();
        }
    }
}
