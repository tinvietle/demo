package com.example;

import java.io.IOException;
import java.net.Socket;

public class AnonymousFTPFunctions extends AbstractFTPFunctions {

    public AnonymousFTPFunctions(Socket socket, String serverDirectory) throws IOException {
        super(socket, serverDirectory);
    }

    // @Override
    // public void sendFile(String filePath) {
    //     deny("Sending files");
    // }

    @Override
    public void receiveFile(String[] parts) {
        deny("Receiving files");
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
    public void moveFile(String[] parts) {
        deny("Moving files");
    }

    @Override
    public void showHelp(String[] parts) {
        try {
            outputStream.writeUTF("Anonymous user has limited permissions. Available commands: get [FILE], ls, cd [DIRECTORY], pwd, help, quit");
        } catch (IOException e) {
            handleException(e);
        }
    }

    private void deny(String action) {
        try {
            outputStream.writeUTF("Permission denied: Anonymous user cannot perform: " + action);
        } catch (IOException e) {
            handleException(e);
        }
    }
}
