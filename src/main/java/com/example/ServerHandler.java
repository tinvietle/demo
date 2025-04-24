package com.example;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class ServerHandler implements Runnable {
    private Socket socket;
    private DataInputStream input;
    private DataOutputStream output;
    private String username;
    private String serverDirectory;
    

    public ServerHandler(Socket socket, String clientName) {
        this.socket = socket;
        try {
            input = new DataInputStream(socket.getInputStream());
            output = new DataOutputStream(socket.getOutputStream());
            username = clientName;
            serverDirectory = "src/main/java/com/example/storage" + "/" + username;
            helloClient();
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void helloClient() {
        try {
            output.writeUTF("Hello Client");
            output.writeUTF("Welcome to the FTP server");
            output.writeUTF("Please enter a command: ");
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
