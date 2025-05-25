package com.example;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

import javax.swing.JFileChooser;
import javax.swing.JFrame;

public class ClientHandler {
    private Socket socket;
    private BufferedReader inputReader;
    private DataInputStream dataInputStream;
    private DataOutputStream dataOutputStream;

    public ClientHandler(Socket socket) {
        this.socket = socket;
        try {
            this.inputReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.dataInputStream = new DataInputStream(socket.getInputStream());
            this.dataOutputStream = new DataOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            System.out.println("Error initializing streams: " + e.getMessage());
        }
    }

    public void openFileChoser() {
        // Create a parent frame with proper setup
        JFrame parentFrame = new JFrame();
        parentFrame.setLocationRelativeTo(null);  // Center on screen
        parentFrame.setVisible(true);  // Make it visible - this is critical
        
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select a file to upload");
        
        // Use the parent frame for the dialog
        int userSelection = fileChooser.showOpenDialog(parentFrame);
        
        File fileToUpload = null;
        while (fileToUpload == null) {
            if (userSelection == JFileChooser.APPROVE_OPTION) {
                fileToUpload = fileChooser.getSelectedFile();
                System.out.println("Selected file: " + fileToUpload.getAbsolutePath());
                uploadFile(fileToUpload);
            } else {
                System.out.println("No file selected. Please select a file.");
                userSelection = fileChooser.showOpenDialog(parentFrame);
            }
        }
        
        // Dispose of the parent frame when done
        parentFrame.dispose();
    }


    public void uploadFile(File file) {
        // Send file name and size
        try {
            dataOutputStream.writeUTF(file.getName());
            dataOutputStream.writeLong(file.length());

            // Send file data
            byte[] buffer = new byte[4096];
            int bytesRead;
            try (FileInputStream fis = new java.io.FileInputStream(file)) {
                while ((bytesRead = fis.read(buffer)) != -1) {
                    dataOutputStream.write(buffer, 0, bytesRead);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void receiveFile() {
        try {
            String fileName = dataInputStream.readUTF();
            long fileSize = dataInputStream.readLong();

            // Get current directory path
            String currentDir = System.getProperty("user.dir");
            File file = new File(currentDir + "/" + fileName);
            try (FileOutputStream fos = new FileOutputStream(file)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                long totalBytesRead = 0;

                while (totalBytesRead < fileSize && (bytesRead = dataInputStream.read(buffer)) != -1) {
                    fos.write(buffer, 0, bytesRead);
                    totalBytesRead += bytesRead;
                }
            }

            // Send confirmation to server
            dataOutputStream.writeUTF("File received successfully: " + fileName);
            System.out.println("File received successfully: " + fileName);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
