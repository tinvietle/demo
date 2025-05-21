package com.example;

import java.io.IOException;
import java.net.Socket;

public class AnonymousFTPFunctions extends AbstractFTPFunctions {

    public AnonymousFTPFunctions(Socket socket, String serverDirectory) throws IOException {
        super(socket, serverDirectory);
    }

    @Override
    public void sendFile(String filePath) {
        deny("Sending files");
    }

    @Override
    public void receiveFile() {
        deny("Receiving/uploading files");
    }

    @Override
    public void deleteFile(String filePath) {
        deny("Deleting files");
    }

    @Override
    public void createDirectory(String dirName) {
        deny("Creating directories");
    }

    @Override
    public void deleteDirectory(String dirName) {
        deny("Deleting directories");
    }

    @Override
    public void moveFile(String sourceName, String destinationName) {
        deny("Moving files");
    }

    @Override
    public void showHelp() {
        try {
            outputStream.writeUTF("Anonymous user has limited permissions. Available commands: get, ls, cd <dirname>, pwd, help, quit");
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
