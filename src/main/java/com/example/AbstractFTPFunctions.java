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

    public AbstractFTPFunctions(Socket socket, String serverDirectory) {
        this.socket = socket;
        this.serverDirectory = serverDirectory;
        this.defaultDirectory = serverDirectory;

        try {
            this.outputStream = new DataOutputStream(socket.getOutputStream());
            this.inputStream = new DataInputStream(socket.getInputStream());
        } catch (Exception e) {
            System.out.println("Error initializing output stream: " + e.getMessage());
        }
    }

    protected boolean isPathWithinAllowedDirectory(String path) {
        try {
            File baseDir = new File(defaultDirectory).getCanonicalFile();
            // Check if the path is absolute or relative
            File targetPath;
            if (new File(path).isAbsolute()) {
                targetPath = new File("src/main/java/com/example/storage/", path);
            } else {
                targetPath = new File(serverDirectory, path);
            }
            targetPath = targetPath.getCanonicalFile();

            return targetPath.getPath().startsWith(baseDir.getPath());
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void listFiles() {
        System.out.println("Listing files...");
        try {
            // Compute the relative directory using canonical paths similar to
            // printWorkingDirectory()
            String baseFolder = new File(defaultDirectory).getName();
            String basePath = new File(defaultDirectory).getCanonicalPath();
            String currentPath = new File(serverDirectory).getCanonicalPath();
            String relative = "";
            if (currentPath.startsWith(basePath)) {
                relative = currentPath.substring(basePath.length());
                if (relative.startsWith(File.separator))
                    relative = relative.substring(1);
            }
            String display = baseFolder;
            if (!relative.isEmpty())
                display += "/" + relative;
            if (!display.endsWith("/"))
                display += "/";
            outputStream.writeUTF(FTPStatus.message(FTPStatus.COMMAND_OK) + ": Directory listing for: " + display);

            File directory = new File(serverDirectory);

            // Verify the directory is within allowed space
            File canonicalDir = directory.getCanonicalFile();
            File canonicalBase = new File(defaultDirectory).getCanonicalFile();
            if (!canonicalDir.getPath().startsWith(canonicalBase.getPath())) {
                outputStream.writeUTF(FTPStatus.message(FTPStatus.FILE_ACTION_NOT_TAKEN) + ": Access denied");
                return;
            }

            String[] files = directory.list();
            if (files != null) {
                for (String file : files) {
                    outputStream.writeUTF(file);
                }
                if (files.length == 0) {
                    outputStream.writeUTF(FTPStatus.message(FTPStatus.COMMAND_OK) + ": Directory is empty");
                }
            } else {
                outputStream.writeUTF(FTPStatus.message(FTPStatus.FILE_UNAVAILABLE) + ": Directory cannot be read");
            }
        } catch (IOException e) {
            e.printStackTrace();
            try {
                outputStream.writeUTF(
                        FTPStatus.message(FTPStatus.ACTION_ABORTED) + ": Error listing files: " + e.getMessage());
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    public void printWorkingDirectory() {
        try {
            String baseFolder = new File(defaultDirectory).getName();
            String basePath = new File(defaultDirectory).getCanonicalPath();
            String currentPath = new File(serverDirectory).getCanonicalPath();
            String relative = "";
            if (currentPath.startsWith(basePath)) {
                relative = currentPath.substring(basePath.length());
                if (relative.startsWith(File.separator))
                    relative = relative.substring(1);
            }
            String display = baseFolder;
            if (!relative.isEmpty())
                display += "/" + relative;
            if (!display.endsWith("/"))
                display += "/";
            outputStream.writeUTF(FTPStatus.message(FTPStatus.COMMAND_OK) + ": Server working directory: " + display);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void changeDirectory(String newDirectory) {
        try {
            if (newDirectory == null || newDirectory.trim().isEmpty()) {
                outputStream.writeUTF(FTPStatus.message(FTPStatus.SYNTAX_ERROR) + ": Usage: cd <directory>");
                return;
            }
            File baseDir = new File(defaultDirectory).getCanonicalFile();
            File targetDir;

            if (new File(newDirectory).isAbsolute()) {
                targetDir = new File("src/main/java/com/example/storage/", newDirectory);
            } else {
                targetDir = new File(serverDirectory, newDirectory);
            }

            targetDir = targetDir.getCanonicalFile();
            if (!targetDir.getPath().startsWith(baseDir.getPath())) {
                outputStream.writeUTF(FTPStatus.message(FTPStatus.FILE_UNAVAILABLE)
                        + ": Access denied - unable to change directory.");
                return;
            }
            if (targetDir.exists() && targetDir.isDirectory()) {
                serverDirectory = targetDir.getPath();
                // Compute the relative path as in printWorkingDirectory()
                String baseFolder = new File(defaultDirectory).getName();
                String basePath = new File(defaultDirectory).getCanonicalPath();
                String currentPath = new File(serverDirectory).getCanonicalPath();
                String relative = "";
                if (currentPath.startsWith(basePath)) {
                    relative = currentPath.substring(basePath.length());
                    if (relative.startsWith(File.separator))
                        relative = relative.substring(1);
                }
                String display = baseFolder;
                if (!relative.isEmpty())
                    display += "/" + relative;
                if (!display.endsWith("/"))
                    display += "/";
                outputStream
                        .writeUTF(FTPStatus.message(FTPStatus.FILE_ACTION_OK) + ": Changed directory to: " + display);
            } else {
                outputStream.writeUTF(FTPStatus.message(FTPStatus.FILE_UNAVAILABLE) + ": Invalid directory");
            }
        } catch (IOException e) {
            e.printStackTrace();
            try {
                outputStream.writeUTF(
                        FTPStatus.message(FTPStatus.ACTION_ABORTED) + ": Error changing directory - " + e.getMessage());
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    public void receiveFile(boolean isGuest) {
        try {
            // Read file name and size
            String fileName = inputStream.readUTF();
            long fileSize = inputStream.readLong();
            final long MAX_FILE_SIZE = 1024 * 1024 * 10; // 10 MB limit

            // Check if user is guest and limit file size
            if (isGuest && fileSize > MAX_FILE_SIZE) { // 1 MB limit for guest users
                outputStream.writeUTF(FTPStatus.message(FTPStatus.INSUFFICIENT_STORAGE)
                        + ": File upload failed - size exceeds limit for guest users");
                return;
            }

            // Security check for path traversal
            if (fileName.contains("..") || !isPathWithinAllowedDirectory(fileName)) {
                outputStream.writeUTF(FTPStatus.message(FTPStatus.FILE_UNAVAILABLE)
                        + ": File upload failed - invalid file path");
                return;
            }

            // Create output file in the server directory
            File outputFile = new File(serverDirectory, fileName);

            // Verify the path after canonicalization
            File canonicalFile = outputFile.getCanonicalFile();
            File canonicalBase = new File(defaultDirectory).getCanonicalFile();
            if (!canonicalFile.getPath().startsWith(canonicalBase.getPath())) {
                outputStream.writeUTF(FTPStatus.message(FTPStatus.FILE_UNAVAILABLE)
                        + ": File upload failed - invalid file path");
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

            // Compute the relative path for the uploaded file
            String basePath = new File(defaultDirectory).getCanonicalPath();
            String filePath = canonicalFile.getCanonicalPath();
            String relativeFile = "";
            if (filePath.startsWith(basePath)) {
                relativeFile = filePath.substring(basePath.length());
                if (relativeFile.startsWith(File.separator))
                    relativeFile = relativeFile.substring(1);
            }
            String uploadDisplay = new File(defaultDirectory).getName() + "/" + relativeFile;
            outputStream.writeUTF(
                    FTPStatus.message(FTPStatus.FILE_ACTION_OK) + ": File upload successful: " + uploadDisplay);
            System.out.println("Received file: " + uploadDisplay);

        } catch (IOException e) {
            e.printStackTrace();
            try {
                outputStream.writeUTF(FTPStatus.message(FTPStatus.ACTION_ABORTED)
                        + ": File upload failed - " + e.getMessage());
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
