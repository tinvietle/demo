package com.example;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;

public abstract class AbstractFTPFunctions {
    protected Socket socket;
    protected DataOutputStream outputStream;
    protected DataInputStream inputStream;
    protected String defaultDirectory;
    protected String serverDirectory;

    public static class FTPStatus {
        public static final int COMMAND_OK = 200;
        public static final int SERVICE_READY = 220;
        public static final int CLOSING_DATA = 226;
        public static final int USER_LOGGED_IN = 230;
        public static final int FILE_ACTION_OK = 250;
        public static final int USERNAME_OK = 331;
        public static final int CANT_OPEN_DATA = 425;
        public static final int FILE_ACTION_NOT_TAKEN = 450;
        public static final int ACTION_ABORTED = 451;
        public static final int INSUFFICIENT_STORAGE = 452;
        public static final int NOT_LOGGED_IN = 530;
        public static final int FILE_UNAVAILABLE = 550;
        public static final int SYNTAX_ERROR = 500;
        public static final int ACTION_NOT_IMPLEMENTED = 502;
        public static final int BAD_SEQUENCE = 503;
        public static final int PARAMETER_NOT_IMPLEMENTED = 504;

        public static String message(int code) {
            switch (code) {
                case COMMAND_OK: return "200 Command okay";
                case SERVICE_READY: return "220 Service ready for new user";
                case CLOSING_DATA: return "226 Closing data connection";
                case USER_LOGGED_IN: return "230 User logged in, proceed";
                case FILE_ACTION_OK: return "250 Requested file action okay, completed";
                case USERNAME_OK: return "331 User name okay, need password";
                case CANT_OPEN_DATA: return "425 Can't open data connection";
                case FILE_ACTION_NOT_TAKEN: return "450 Requested file action not taken";
                case ACTION_ABORTED: return "451 Requested action aborted";
                case INSUFFICIENT_STORAGE: return "452 Insufficient storage";
                case NOT_LOGGED_IN: return "530 Not logged in";
                case FILE_UNAVAILABLE: return "550 Requested action not taken. File unavailable";
                case SYNTAX_ERROR: return "500 Syntax error, command unrecognized";
                case ACTION_NOT_IMPLEMENTED: return "502 Command not implemented";
                case BAD_SEQUENCE: return "503 Bad sequence of commands";
                case PARAMETER_NOT_IMPLEMENTED: return "504 Command not implemented for that parameter";
                default: return code + " Unknown Status Code";
            }
        }
    }

    public AbstractFTPFunctions(Socket socket, String serverDirectory) throws IOException {
        this.socket = socket;
        this.serverDirectory = serverDirectory;
        this.defaultDirectory = serverDirectory;
        this.outputStream = new DataOutputStream(socket.getOutputStream());
        this.inputStream = new DataInputStream(socket.getInputStream());
    }

    protected boolean isPathWithinAllowedDirectory(String path) {
        try {
            File baseDir = new File(defaultDirectory).getCanonicalFile();
            File targetPath = new File(serverDirectory, path).getCanonicalFile();
            return targetPath.getPath().startsWith(baseDir.getPath());
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void listFiles() {
        try {
            File directory = new File(serverDirectory).getCanonicalFile();
            File base = new File(defaultDirectory).getCanonicalFile();
            if (!directory.getPath().startsWith(base.getPath())) {
                outputStream.writeUTF("Access denied");
                return;
            }

            String[] files = directory.list();
            outputStream.writeUTF(FTPStatus.message(FTPStatus.COMMAND_OK));
            if (files != null) {
                for (String file : files) outputStream.writeUTF(file);
            }
        } catch (IOException e) {
            handleException(e);
        }
    }

    public void printWorkingDirectory() {
        try {
            outputStream.writeUTF("Server working directory: " + serverDirectory);
        } catch (IOException e) {
            handleException(e);
        }
    }

    public void changeDirectory(String newDirectory) {
        try {
            File baseDir = new File(defaultDirectory).getCanonicalFile();
            File targetDir = "..".equals(newDirectory)
                    ? new File(serverDirectory).getParentFile()
                    : new File(serverDirectory, newDirectory).getCanonicalFile();

            if (targetDir == null || !targetDir.getPath().startsWith(baseDir.getPath()) || !targetDir.isDirectory()) {
                outputStream.writeUTF("Access denied or invalid directory.");
                return;
            }

            serverDirectory = targetDir.getPath();
            outputStream.writeUTF("Changed directory to: " + serverDirectory);
        } catch (IOException e) {
            handleException(e);
        }
    }

    public void receiveFile() {
        try {
            // Read file name and size
            String fileName = inputStream.readUTF();
            long fileSize = inputStream.readLong();

            // Security check for path traversal
            if (fileName.contains("..") || !isPathWithinAllowedDirectory(fileName)) {
                outputStream.writeUTF("File upload failed: Access denied - invalid file path");
                return;
            }

            // Create output file in the server directory
            File outputFile = new File(serverDirectory, fileName);
            
            // Verify the path after canonicalization
            File canonicalFile = outputFile.getCanonicalFile();
            File canonicalBase = new File(defaultDirectory).getCanonicalFile();
            if (!canonicalFile.getPath().startsWith(canonicalBase.getPath())) {
                outputStream.writeUTF("File upload failed: Access denied - invalid file path");
                return;
            }
            
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
                outputStream.writeUTF("File upload failed: " + e.getMessage());
            } catch (IOException ignored) {
            }
        }
    }

    protected void handleException(IOException e) {
        e.printStackTrace();
        try {
            outputStream.writeUTF("Error: " + e.getMessage());
        } catch (IOException ignored) {}
    }

    // Abstract methods to be implemented by subclasses
    public abstract void sendFile(String filePath);
    public abstract void deleteFile(String filePath);
    public abstract void createDirectory(String dirName);
    public abstract void deleteDirectory(String dirName);
    public abstract void moveFile(String sourceName, String destinationName);
    public abstract void showHelp();
}
