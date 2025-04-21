package com.example;

import java.io.File;
import java.net.Socket;
import java.io.DataOutputStream;
import java.io.DataInputStream;
import java.io.IOException;

import javax.swing.JFileChooser;

public class ClientHandler {
    private Socket socket;

    public ClientHandler(Socket socket) {
        // Constructor implementation
        this.socket = socket;
    }

    public void openFileChoser() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select a file to upload");
        int userSelection = fileChooser.showOpenDialog(null);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToUpload = fileChooser.getSelectedFile();
            System.out.println("Selected file: " + fileToUpload.getAbsolutePath());
            uploadFile(fileToUpload);
        }
    }

    public void uploadFile(File file) {
        try (DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                DataInputStream dis = new DataInputStream(socket.getInputStream())) {
            // Send file name and size
            dos.writeUTF(file.getName());
            dos.writeLong(file.length());

            // Send file data
            byte[] buffer = new byte[4096];
            int bytesRead;
            try (java.io.FileInputStream fis = new java.io.FileInputStream(file)) {
                while ((bytesRead = fis.read(buffer)) != -1) {
                    dos.write(buffer, 0, bytesRead);
                }
            }

            // Receive confirmation from server
            String response = dis.readUTF();
            System.out.println("Server response: " + response);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
