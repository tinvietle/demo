package com.example;

import java.io.File;
import java.io.IOException;
import java.net.Socket;

public class UserFTPFunctions extends AbstractFTPFunctions {

    public UserFTPFunctions(Socket socket, String serverDirectory) throws IOException {
        super(socket, serverDirectory);
    }

    @Override
    public void sendFile(String filePath) {
        System.out.println("Sending file...");
        try {
            if (!isPathWithinAllowedDirectory(filePath)) {
                outputStream.writeUTF("Access denied - cannot access files outside allowed space");
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
            } else {
                outputStream.writeUTF(FTPStatus.message(FTPStatus.FILE_UNAVAILABLE));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void deleteFile(String filePath) {
        System.out.println("Deleting file...");
        try {
            if (!isPathWithinAllowedDirectory(filePath)) {
                outputStream.writeUTF(FTPStatus.message(FTPStatus.FILE_UNAVAILABLE) + ": Access denied - cannot delete files outside allowed space");
                return;
            }
            
            File file = new File(serverDirectory, filePath);
            if (file.delete()) {
                outputStream.writeUTF(FTPStatus.message(FTPStatus.FILE_ACTION_OK));
            } else {
                outputStream.writeUTF(FTPStatus.message(FTPStatus.FILE_UNAVAILABLE));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    // Modified createDirectory to accept the directory name as a parameter
    public void createDirectory(String dirName) {
        try {
            if (!isPathWithinAllowedDirectory(dirName)) {
                outputStream.writeUTF(FTPStatus.message(FTPStatus.FILE_UNAVAILABLE) + ": Access denied - cannot create directories outside allowed space");
                return;
            }
            
            File newDir = new File(serverDirectory, dirName);
            if (!newDir.exists()) {
                if (newDir.mkdir()) {
                    outputStream.writeUTF(FTPStatus.message(FTPStatus.FILE_ACTION_OK) + ": Directory created successfully: " + dirName);
                } else {
                    outputStream.writeUTF(FTPStatus.message(FTPStatus.FILE_UNAVAILABLE) + ": Failed to create directory: " + dirName);
                }
            } else {
                outputStream.writeUTF(FTPStatus.message(FTPStatus.FILE_UNAVAILABLE) + ": Directory already exists: " + dirName);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    // Modified deleteDirectory to accept the directory name as a parameter
    public void deleteDirectory(String dirName) {
        try {
            if (!isPathWithinAllowedDirectory(dirName)) {
                outputStream.writeUTF(FTPStatus.message(FTPStatus.FILE_UNAVAILABLE) + ": Access denied - cannot delete directories outside allowed space");
                return;
            }
            
            File dir = new File(serverDirectory, dirName);
            if (dir.exists() && dir.isDirectory()) {
                deleteDirectoryRecursively(dir);
                outputStream.writeUTF(FTPStatus.message(FTPStatus.FILE_ACTION_OK) + ": Directory deleted successfully: " + dirName);
            } else {
                outputStream.writeUTF(FTPStatus.message(FTPStatus.FILE_UNAVAILABLE) + ": Directory does not exist or is not a directory: " + dirName);
            }
        } catch(IOException e) {
            e.printStackTrace();
            try { outputStream.writeUTF("Error deleting directory: " + e.getMessage()); }
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
    
    @Override
    // Modified moveFile to accept source and destination names as parameters
    public void moveFile(String sourceName, String destinationName) {
        try {
            if (!isPathWithinAllowedDirectory(sourceName) || !isPathWithinAllowedDirectory(destinationName)) {
                outputStream.writeUTF(FTPStatus.message(FTPStatus.FILE_UNAVAILABLE) + ": Access denied - cannot move files outside allowed space");
                return;
            }
            
            File sourceFile = new File(serverDirectory, sourceName);
            File destFile = new File(serverDirectory, destinationName);
            if(sourceFile.exists()){
                if(sourceFile.renameTo(destFile)){
                    outputStream.writeUTF(FTPStatus.message(FTPStatus.FILE_ACTION_OK));
                } else {
                    outputStream.writeUTF(FTPStatus.message(FTPStatus.FILE_UNAVAILABLE));
                }
            } else {
                outputStream.writeUTF(FTPStatus.message(FTPStatus.FILE_UNAVAILABLE) + ": Source file does not exist: " + sourceName);
            }
        } catch(IOException e) {
            e.printStackTrace();
            try { outputStream.writeUTF("Error moving file: " + e.getMessage()); }
            catch(IOException ex) { ex.printStackTrace(); }
        }
    }

    @Override
    public void showHelp() {
        try {
            outputStream.writeUTF("Available commands: put, get, ls, cd <dirname>, delete <filename>, mkdir <dirName>, rmdir <dirName>, move <source> <destination>, pwd, help, quit");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
