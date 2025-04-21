package com.example;

import java.io.DataOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.List;
import java.io.File;
import java.io.FileOutputStream;

import javax.swing.JFileChooser;

public class FTPFunctions {
    Socket socket;
    DataOutputStream outputStream;
    DataInputStream inputStream;
    String serverDirectory;

    public FTPFunctions(Socket socket, String serverDirectory) {
        this.socket = socket;
        this.serverDirectory = serverDirectory;
        try {
            this.outputStream = new DataOutputStream(socket.getOutputStream());
            this.inputStream = new DataInputStream(socket.getInputStream());
        } catch (Exception e) {
            System.out.println("Error initializing output stream: " + e.getMessage());
        }
    }

    private String statusMessage(int statusCode) {
        String message = "";
        switch (statusCode) {
            case 200:
                message = "OK";
                break;
            case 400:
                message = "Bad Request";
                break;
            case 404:
                message = "Not Found";
                break;
            case 500:
                message = "Internal Server Error";
                break;
            default:
                message = "Unknown Status Code";
        }
        return message;
    }

    public void sendFile() {
        System.out.println("Sending file...");
    }

    public void listFiles(String directoryPath) {
        // List files and folders in current directory
        System.out.println("Listing files...");
        try {
            File directory = new File(directoryPath);
            String[] files = directory.list();
            if (files != null) {
                String statusMessage = statusMessage(200);
                outputStream.writeUTF(statusMessage);
                for (String file : files) {
                    System.out.println(file);
                    outputStream.writeUTF(file);
                }
            } else {
                String statusMessage = statusMessage(404);
                outputStream.writeUTF(statusMessage);
                System.out.println("No files found in the directory.");
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void deleteFile() {
        System.out.println("Deleting file...");
        try {
            outputStream.writeUTF("File deleted successfully");
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void renameFile() {
        System.out.println("Renaming file...");
        try {
            outputStream.writeUTF("File renamed successfully");
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void createDirectory() {
        System.out.println("Creating directory...");
        try {
            outputStream.writeUTF("Directory created successfully");
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void deleteDirectory() {
        System.out.println("Deleting directory...");
        try {
            outputStream.writeUTF("Directory deleted successfully");
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void receiveFile() {
        try {
            // Read file name and size
            String fileName = inputStream.readUTF();
            long fileSize = inputStream.readLong();

            // Create output file in the server directory
            File outputFile = new File(serverDirectory, fileName);
            try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                long remaining = fileSize;

                while (remaining > 0 &&
                        (bytesRead = inputStream.read(buffer, 0, (int) Math.min(buffer.length, remaining))) != -1) {
                    fos.write(buffer, 0, bytesRead);
                    remaining -= bytesRead;
                }
            }

            // Send confirmation to client
            outputStream.writeUTF("File upload successful: " + fileName);
            System.out.println("Received file: " + fileName);

        } catch (IOException e) {
            e.printStackTrace();
            try {
                try (DataOutputStream dos = new DataOutputStream(socket.getOutputStream())) {
                    dos.writeUTF("File upload failed.");
                }
            } catch (IOException ignored) {
            }
        }
    }

}
