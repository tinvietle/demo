package com.example;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class ServerHandler implements Runnable {
    private Socket socket;
    private DataInputStream input;
    private DataOutputStream output;
    

    public ServerHandler(Socket socket) {
        this.socket = socket;
        try {
            input = new DataInputStream(socket.getInputStream());
            output = new DataOutputStream(socket.getOutputStream());
            helloClient();
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void helloClient() {
        try {
            output.writeUTF("Hello Client");
            output.writeUTF("Welcome to the FTP server");
            output.writeUTF("Please enter a command (put/get/ls/delete/rename/create/deleteDir/quit):");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            String message;
            FTPFunctions ftp = new FTPFunctions(socket);
            while ((message = input.readUTF()) != null) {
                System.out.println("Received: " + message);
                switch (message) {
                    case "put":
                        ftp.sendFile();
                        break;
                    case "get":
                        ftp.receiveFile();
                        break;
                    case "ls":
                        // Get current directory path
                        String currentDir = System.getProperty("user.dir");
                        ftp.listFiles(currentDir);
                        break;
                    case "delete":
                        ftp.deleteFile();
                        break;
                    case "rename":
                        ftp.renameFile();
                        break;
                    case "create":
                        ftp.createDirectory();
                        break;
                    case "deleteDir":
                        ftp.deleteDirectory();
                        break;
                    case "quit":
                        System.out.println("Client disconnected");
                        socket.close();
                        return;
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
