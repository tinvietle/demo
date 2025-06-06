package com.example;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.nio.file.Path;
import java.nio.file.Paths;

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
                case COMMAND_OK:
                    return "200 Command okay";
                case SERVICE_READY:
                    return "220 Service ready for new user";
                case CLOSING_DATA:
                    return "226 Closing data connection";
                case USER_LOGGED_IN:
                    return "230 User logged in, proceed";
                case FILE_ACTION_OK:
                    return "250 Requested file action okay, completed";
                case USERNAME_OK:
                    return "331 User name okay, need password";
                case CANT_OPEN_DATA:
                    return "425 Can't open data connection";
                case FILE_ACTION_NOT_TAKEN:
                    return "450 Requested file action not taken";
                case ACTION_ABORTED:
                    return "451 Requested action aborted";
                case INSUFFICIENT_STORAGE:
                    return "452 Insufficient storage";
                case NOT_LOGGED_IN:
                    return "530 Not logged in";
                case FILE_UNAVAILABLE:
                    return "550 Requested action not taken. File unavailable";
                case SYNTAX_ERROR:
                    return "500 Syntax error, command unrecognized";
                case ACTION_NOT_IMPLEMENTED:
                    return "502 Command not implemented";
                case BAD_SEQUENCE:
                    return "503 Bad sequence of commands";
                case PARAMETER_NOT_IMPLEMENTED:
                    return "504 Command not implemented for that parameter";
                default:
                    return code + " Unknown Status Code";
            }
        }
    }

    protected boolean validateCommand(String[] parts, int expectedLength, String usage) {
        if (parts.length != expectedLength) {
            try {
                outputStream.writeUTF(FTPStatus.message(FTPStatus.SYNTAX_ERROR) + ": " + usage);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return false;
        }
        return true;
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

    public static String getCommonBasePath(String path1, String path2) {
        Path p1 = Paths.get(path1).normalize();
        Path p2 = Paths.get(path2).normalize();

        int minLength = Math.min(p1.getNameCount(), p2.getNameCount());
        Path common = p1.getRoot(); 

        for (int i = 0; i < minLength; i++) {
            if (p1.getName(i).equals(p2.getName(i))) {
                common = common.resolve(p1.getName(i));
            } else {
                break;
            }
        }

        return common.toString();
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

            String commonBase = getCommonBasePath(baseDir.getCanonicalPath(), targetPath.getCanonicalPath());
            return (commonBase.equals(baseDir.getCanonicalPath()));
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void listFiles(String[] parts) {
        if (!validateCommand(parts, 1, "Usage: ls")) {
            return;
        }
        System.out.println("Listing files...");
        try {
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
    
    public String displayWorkingDirectory() {
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
            String display = "/" + baseFolder;
            if (!relative.isEmpty())
                display += "/" + relative;
            // Change backslashes to forward slashes before returning
            return display.replace("\\", "/");
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }

    public void printWorkingDirectory(String[] parts) {
        if (!validateCommand(parts, 1, "Usage: pwd")) {
            return;
        }
        String display = displayWorkingDirectory();
        try {
            outputStream.writeUTF(FTPStatus.message(FTPStatus.COMMAND_OK) + ": Current directory: " + display);
        } catch (IOException e) {
            e.printStackTrace();
            try {
                outputStream.writeUTF(
                        FTPStatus.message(FTPStatus.ACTION_ABORTED) + ": Error printing working directory: " + e.getMessage());
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    public synchronized void changeDirectory(String[] parts) {
        if (!validateCommand(parts, 2, "Usage: cd [DIRECTORY]")) {
            return;
        }

        String newDirectory = parts[1];

        try {
            File targetDir;

            if (new File(newDirectory).isAbsolute()) {
                targetDir = new File("src/main/java/com/example/storage/", newDirectory);
            } else {
                targetDir = new File(serverDirectory, newDirectory);
            }

            targetDir = targetDir.getCanonicalFile();
            if (!isPathWithinAllowedDirectory(newDirectory)) {
                outputStream.writeUTF(FTPStatus.message(FTPStatus.FILE_UNAVAILABLE)
                        + ": Access denied - unable to change directory.");
                return;
            }
            if (targetDir.exists() && targetDir.isDirectory()) {
                serverDirectory = targetDir.getPath();
                // Compute the relative path as in printWorkingDirectory()
                String display = displayWorkingDirectory();
                outputStream.writeUTF(FTPStatus.message(FTPStatus.COMMAND_OK) + ": Directory changed to: " + display);
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

    public synchronized void sendFile(String[] parts) {
        if (!validateCommand(parts, 2, "Usage: get [FILE]")) {
            return;
        }
        System.out.println("Sending file...");
        String filePath = parts[1];
        try {
            if (!isPathWithinAllowedDirectory(filePath)) {
                outputStream.writeUTF(FTPStatus.message(FTPStatus.FILE_UNAVAILABLE)
                        + ": Access denied - cannot access files outside allowed space");
                return;
            }

            // Load file for absolute and relative path
            File file;
            if (new File(filePath).isAbsolute()) {
                file = new File("src/main/java/com/example/storage/", filePath);
            } else {
                file = new File(serverDirectory, filePath);
            }

            if (file.exists()) {
                // Trigger Client to receive File instead of normal text
                outputStream.writeUTF("put");
                // Send file name and size
                outputStream.writeUTF(file.getName());
                outputStream.writeLong(file.length());
                // Send file data
                byte[] buffer = new byte[4096];
                int bytesRead;
                try (java.io.FileInputStream fis = new java.io.FileInputStream(file)) {
                    while ((bytesRead = fis.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                }
                // Success status after sending file
                outputStream.writeUTF(FTPStatus.message(FTPStatus.FILE_ACTION_OK));

                // Listen for confirmation from Client
                String confirmation = inputStream.readUTF();
                if (confirmation.equals("File received successfully : " + file.getName())) {
                    System.out.println("File sent successfully: " + file.getName());
                } else {
                    System.out.println("Error sending file: " + confirmation);
                }
            } else {
                outputStream.writeUTF(FTPStatus.message(FTPStatus.FILE_UNAVAILABLE));
            }
        } catch (IOException e) {
            e.printStackTrace();
            try {
                outputStream.writeUTF(FTPStatus.message(FTPStatus.ACTION_ABORTED)
                        + ": Error sending file - " + e.getMessage());
            } catch (IOException ignored) {
            }
        }
    }

    protected void handleException(IOException e) {
        e.printStackTrace();
        try {
            outputStream.writeUTF("Error: " + e.getMessage());
        } catch (IOException ignored) {
        }
    }

    // Abstract methods to be implemented by subclasses
    public abstract void deleteFile(String[] parts);

    public abstract void createDirectory(String[] parts);

    public abstract void deleteDirectory(String[] parts);

    public abstract void receiveFile(String[] parts);

    public abstract void moveFile(String[] parts);

    public abstract void showHelp(String[] parts);
}
