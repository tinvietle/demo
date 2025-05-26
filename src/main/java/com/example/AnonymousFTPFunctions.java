package com.example;

import java.io.IOException;
import java.net.Socket;

public class AnonymousFTPFunctions extends AbstractFTPFunctions {

    // ===============================
    // CONSTRUCTOR
    // ===============================
    
    public AnonymousFTPFunctions(Socket socket, String serverDirectory) throws IOException {
        super(socket, serverDirectory);
    }

    // ===============================
    // DENIED OPERATIONS
    // ===============================
    
    @Override
    public void receiveFile(String[] parts) {
        deny("Uploading files");
    }

    @Override
    public void deleteFile(String[] parts) {
        deny("Deleting files");
    }

    @Override
    public void createDirectory(String[] parts) {
        deny("Creating directories");
    }

    @Override
    public void deleteDirectory(String[] parts) {
        deny("Deleting directories");
    }

    @Override
    public void handleRenameFrom(String[] parts) {
        deny("Renaming files");
    }

    @Override
    public void handleRenameTo(String[] parts) {
        deny("Renaming files");
    }

    // ===============================
    // UTILITY METHODS
    // ===============================
    
    private void deny(String action) {
        outputWriter.println("Permission denied: Anonymous user cannot perform: " + action);
    }

    @Override
    public void showHelp(String[] parts) {
        outputWriter.println("214-Anonymous users may use the following commands:");
        outputWriter.println(" get ls cd pwd help quit");
        outputWriter.println("214 End of HELP info.");
    }
}
