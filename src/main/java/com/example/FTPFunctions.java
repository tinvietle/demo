package com.example;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.List;
import java.io.File;

public class FTPFunctions {
    Socket socket;
    DataOutputStream outputStream;
    

    public FTPFunctions (Socket socket) {
        this.socket = socket;
        try {
            this.outputStream = new DataOutputStream(socket.getOutputStream());
        } catch (Exception e) {
            System.out.println("Error initializing output stream: " + e.getMessage());
        }           
    }

    private String statusMessage(int statusCode){
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

    public void sendFile(){
        System.out.println("Sending file...");
        try {
            outputStream.writeUTF("File sent successfully");
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    public void receiveFile(){
        System.out.println("Receiving file...");
        try {
            outputStream.writeUTF("File received successfully");
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    public void listFiles(String directoryPath){
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
    public void deleteFile(){
        System.out.println("Deleting file...");
        try {
            outputStream.writeUTF("File deleted successfully");
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    public void renameFile(){
        System.out.println("Renaming file...");
        try {
            outputStream.writeUTF("File renamed successfully");
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    public void createDirectory(){
        System.out.println("Creating directory...");
        try {
            outputStream.writeUTF("Directory created successfully");
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    public void deleteDirectory(){
        System.out.println("Deleting directory...");
        try {
            outputStream.writeUTF("Directory deleted successfully");
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
