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
                String[] tokens = message.split(" ");
                String command = tokens[0];
                switch (command) {
                    case "put":
                        ftp.receiveFile();
                        break;
                    case "get":
                        ftp.sendFile();
                        break;
                    case "ls":
                        String currentDir = System.getProperty("user.dir");
                        ftp.listFiles(currentDir);
                        break;
                    case "delete":
                        ftp.deleteFile();
                        break;
                    case "rename":
                        if(tokens.length >= 3){
                            ftp.renameFile();
                        } else {
                            output.writeUTF("Usage: rename <oldName> <newName>");
                        }
                        break;
                    case "mkdir":
                        if(tokens.length >= 2){
                            ftp.createDirectory(tokens[1]);
                        } else {
                            output.writeUTF("Usage: mkdir <directoryName>");
                        }
                        break;
                    case "rmdir":
                        if(tokens.length >= 2){
                            ftp.deleteDirectory(tokens[1]);
                        } else {
                            output.writeUTF("Usage: rmdir <directoryName>");
                        }
                        break;
                    case "move":
                        if(tokens.length >= 3){
                            ftp.moveFile(tokens[1], tokens[2]);
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
