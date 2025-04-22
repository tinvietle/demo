package com.example;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class ServerHandler implements Runnable {
    private Socket socket;
    private DataInputStream input;
    private DataOutputStream output;
    private String serverDirectory = "src/main/java/com/example/storage";
    

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
            FTPFunctions ftp = new FTPFunctions(socket, serverDirectory);
            while ((message = input.readUTF()) != null) {
                System.out.println("Received: " + message);
                // Split the message into command and arguments
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
                        // Get current directory path
                        String currentDir = System.getProperty("user.dir");
                        ftp.listFiles(currentDir);
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
