package com.example;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;

public class FTPFunctions {
    Socket socket;
    DataOutputStream outputStream;
    DataInputStream inputStream;
    String defaultDirectory;
    String serverDirectory;
    String baseDirectory; // new field to store the original user directory

    public FTPFunctions(Socket socket, String serverDirectory) {
        this.socket = socket;
        this.serverDirectory = serverDirectory;
        this.defaultDirectory = serverDirectory;
        this.baseDirectory = serverDirectory; // store the user's home
        try {
            this.outputStream = new DataOutputStream(socket.getOutputStream());
            this.inputStream = new DataInputStream(socket.getInputStream());
        } catch (Exception e) {
            System.out.println("Error initializing output stream: " + e.getMessage());
        }
    }

    /* ----------------- Status Codes ----------------- */
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

    /* ----------------- Utility Methods ----------------- */
    // Helper method to validate if a path is within the allowed directory space
    private boolean isPathWithinAllowedDirectory(String path) {
        try {
            File baseDir = new File(defaultDirectory).getCanonicalFile();
            File targetPath = new File(serverDirectory, path).getCanonicalFile();
            return targetPath.getPath().startsWith(baseDir.getPath());
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /* ----------------- File Operations ----------------- */
    public void sendFile(String filePath) {
        System.out.println("Sending file...");
        try {
            if (!isPathWithinAllowedDirectory(filePath)) {
                outputStream.writeUTF(FTPStatus.message(FTPStatus.FILE_UNAVAILABLE)
                    + ": Access denied - cannot access files outside allowed space");
                return;
            }
            
            File file = new File(serverDirectory, filePath);
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

    public void receiveFile() {
        try {
            // Read file name and size
            String fileName = inputStream.readUTF();
            long fileSize = inputStream.readLong();

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
            String basePath = new File(baseDirectory).getCanonicalPath();
            String filePath = canonicalFile.getCanonicalPath();
            String relativeFile = "";
            if (filePath.startsWith(basePath)) {
                relativeFile = filePath.substring(basePath.length());
                if(relativeFile.startsWith(File.separator))
                    relativeFile = relativeFile.substring(1);
            }
            String uploadDisplay = new File(baseDirectory).getName() + "/" + relativeFile;
            outputStream.writeUTF(FTPStatus.message(FTPStatus.FILE_ACTION_OK) + ": File upload successful: " + uploadDisplay);
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
    
    public void moveFile(String sourceName, String destinationName) {
        try {
            if (!isPathWithinAllowedDirectory(sourceName) || !isPathWithinAllowedDirectory(destinationName)) {
                outputStream.writeUTF(FTPStatus.message(FTPStatus.FILE_ACTION_NOT_TAKEN) + ": Access denied - cannot move files outside allowed space");
                return;
            }
            
            File sourceFile = new File(serverDirectory, sourceName);
            File destFile = new File(serverDirectory, destinationName);
            if(sourceFile.exists()){
                if(sourceFile.renameTo(destFile)){
                    outputStream.writeUTF(FTPStatus.message(FTPStatus.FILE_ACTION_OK));
                } else {
                    outputStream.writeUTF(FTPStatus.message(FTPStatus.ACTION_ABORTED) + ": File move operation failed");
                }
            } else {
                outputStream.writeUTF(FTPStatus.message(FTPStatus.FILE_UNAVAILABLE) + ": Source file does not exist: " + sourceName);
            }
        } catch(IOException e) {
            e.printStackTrace();
            try { outputStream.writeUTF(FTPStatus.message(FTPStatus.ACTION_ABORTED) + ": Error moving file: " + e.getMessage()); }
            catch(IOException ex) { ex.printStackTrace(); }
        }
    }
    
    public void deleteFile(String filePath) {
        System.out.println("Deleting file...");
        try {
            if (!isPathWithinAllowedDirectory(filePath)) {
                outputStream.writeUTF(FTPStatus.message(FTPStatus.FILE_ACTION_NOT_TAKEN) + ": Access denied - cannot delete files outside allowed space");
                return;
            }
            
            File file = new File(serverDirectory, filePath);
            if (file.delete()) {
                outputStream.writeUTF(FTPStatus.message(FTPStatus.FILE_ACTION_OK));
            } else {
                if (!file.exists()) {
                    outputStream.writeUTF(FTPStatus.message(FTPStatus.FILE_UNAVAILABLE) + ": File does not exist");
                } else {
                    outputStream.writeUTF(FTPStatus.message(FTPStatus.ACTION_ABORTED) + ": Failed to delete file");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            try { outputStream.writeUTF(FTPStatus.message(FTPStatus.ACTION_ABORTED) + ": Error deleting file: " + e.getMessage()); }
            catch (IOException ex) { ex.printStackTrace(); }
        }
    }
    
    public void renameFile(String dir, String oldName, String newName) {
        System.out.println("Renaming file...");
        try {
            // Check if paths are within allowed directory
            if (!isPathWithinAllowedDirectory(oldName) || !isPathWithinAllowedDirectory(newName)) {
                outputStream.writeUTF(FTPStatus.message(FTPStatus.FILE_ACTION_NOT_TAKEN) + ": Access denied - cannot rename files outside allowed space");
                return;
            }
            
            File oldFile = new File(serverDirectory, oldName);
            File newFile = new File(serverDirectory, newName);
            
            // Check if newFile's parent directory exists (handles '../' attacks)
            // /* Validate Parent Directory Existence */
            if (!newFile.getParentFile().exists() || !oldFile.getParentFile().exists()) {
                outputStream.writeUTF(FTPStatus.message(FTPStatus.FILE_UNAVAILABLE) + ": Invalid directory path");
                return;
            }
            
            if (oldFile.exists() && oldFile.renameTo(newFile)) {
                outputStream.writeUTF(FTPStatus.message(FTPStatus.FILE_ACTION_OK));
            } else {
                if (!oldFile.exists()) {
                    outputStream.writeUTF(FTPStatus.message(FTPStatus.FILE_UNAVAILABLE) + ": File not found");
                } else {
                    outputStream.writeUTF(FTPStatus.message(FTPStatus.ACTION_ABORTED) + ": Cannot rename file");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            try {
                outputStream.writeUTF(FTPStatus.message(FTPStatus.ACTION_ABORTED) + ": Error renaming file: " + e.getMessage());
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }
    
    public void printWorkingDirectory() {
        try {
            String baseFolder = new File(baseDirectory).getName();
            String basePath = new File(baseDirectory).getCanonicalPath();
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

    /* ----------------- Directory Operations ----------------- */
    public void listFiles() {
        System.out.println("Listing files...");
        try {
            // Compute the relative directory using canonical paths similar to printWorkingDirectory()
            String baseFolder = new File(baseDirectory).getName();
            String basePath = new File(baseDirectory).getCanonicalPath();
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
            try { outputStream.writeUTF(FTPStatus.message(FTPStatus.ACTION_ABORTED) + ": Error listing files: " + e.getMessage()); }
            catch (IOException ex) { ex.printStackTrace(); }
        }
    }
    
    public void changeDirectory(String newDirectory){
        try {
            if (newDirectory == null || newDirectory.trim().isEmpty()){
                outputStream.writeUTF(FTPStatus.message(FTPStatus.SYNTAX_ERROR) + ": Usage: cd <directory>");
                return;
            }
            File baseDir = new File(baseDirectory).getCanonicalFile();
            File targetDir;
            if ("..".equals(newDirectory)) {
                targetDir = new File(new File(serverDirectory).getParent());
                if (targetDir == null || targetDir.getCanonicalPath().equals(baseDir.getCanonicalPath())) {
                    String baseFolder = new File(baseDirectory).getName();
                    outputStream.writeUTF(FTPStatus.message(FTPStatus.FILE_ACTION_NOT_TAKEN) 
                         + ": Already in the root directory: " + baseFolder + "/");
                    return;
                }
            } else {
                targetDir = new File(serverDirectory, newDirectory);
            }
            targetDir = targetDir.getCanonicalFile();
            if (!targetDir.getPath().startsWith(baseDir.getPath())) {
                outputStream.writeUTF(FTPStatus.message(FTPStatus.FILE_UNAVAILABLE) + ": Access denied - unable to change directory.");
                return;
            }
            if (targetDir.exists() && targetDir.isDirectory()) {
                serverDirectory = targetDir.getPath();
                // Compute the relative path as in printWorkingDirectory()
                String baseFolder = new File(baseDirectory).getName();
                String basePath = new File(baseDirectory).getCanonicalPath();
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
                outputStream.writeUTF(FTPStatus.message(FTPStatus.FILE_ACTION_OK) + ": Changed directory to: " + display);
            } else {
                outputStream.writeUTF(FTPStatus.message(FTPStatus.FILE_UNAVAILABLE) + ": Invalid directory");
            }
        } catch (IOException e) {
            e.printStackTrace();
            try {
                outputStream.writeUTF(FTPStatus.message(FTPStatus.ACTION_ABORTED) + ": Error changing directory - " + e.getMessage());
            } catch(IOException ex) { ex.printStackTrace(); }
        }
    }
    
    public void createDirectory(String dirName) {
        try {
            if (!isPathWithinAllowedDirectory(dirName)) {
                outputStream.writeUTF(FTPStatus.message(FTPStatus.FILE_ACTION_NOT_TAKEN) 
                    + ": Access denied - cannot create directories outside allowed space");
                return;
            }
            
            File newDir = new File(serverDirectory, dirName);
            if (!newDir.exists()) {
                if (newDir.mkdir()) {
                    outputStream.writeUTF(FTPStatus.message(FTPStatus.FILE_ACTION_OK) + ": Directory created successfully: " + dirName);
                } else {
                    outputStream.writeUTF(FTPStatus.message(FTPStatus.ACTION_ABORTED) + ": Failed to create directory: " + dirName);
                }
            } else {
                outputStream.writeUTF(FTPStatus.message(FTPStatus.FILE_ACTION_NOT_TAKEN) + ": Directory already exists: " + dirName);
            }
        } catch (IOException e) {
            e.printStackTrace();
            try { outputStream.writeUTF(FTPStatus.message(FTPStatus.ACTION_ABORTED) + ": Error creating directory: " + e.getMessage()); }
            catch (IOException ex) { ex.printStackTrace(); }
        }
    }
    
    public void deleteDirectory(String dirName) {
        try {
            if (!isPathWithinAllowedDirectory(dirName)) {
                outputStream.writeUTF(FTPStatus.message(FTPStatus.FILE_ACTION_NOT_TAKEN) 
                    + ": Access denied - cannot delete directories outside allowed space");
                return;
            }
            
            File dir = new File(serverDirectory, dirName);
            if (dir.exists() && dir.isDirectory()) {
                deleteDirectoryRecursively(dir);
                outputStream.writeUTF(FTPStatus.message(FTPStatus.FILE_ACTION_OK) + ": Directory deleted successfully: " + dirName);
            } else {
                outputStream.writeUTF(FTPStatus.message(FTPStatus.FILE_UNAVAILABLE) 
                    + ": Directory does not exist or is not a directory: " + dirName);
            }
        } catch(IOException e) {
            e.printStackTrace();
            try { outputStream.writeUTF(FTPStatus.message(FTPStatus.ACTION_ABORTED) + ": Error deleting directory: " + e.getMessage()); }
            catch(IOException ex) { ex.printStackTrace(); }
        }
    }
    
    // Helper method to delete a directory recursively
    private void deleteDirectoryRecursively(File dir) {
        File[] contents = dir.listFiles();
        if (contents != null) {
            for (File file : contents) {
                if (file.isDirectory()) {
                    deleteDirectoryRecursively(file);
                } else {
                    file.delete();
                }
            }
        }
        dir.delete();
    }
}
