package com.example;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;

public class UserFTPFunctions extends AbstractFTPFunctions {

    public UserFTPFunctions(Socket socket, String serverDirectory) throws IOException {
        super(socket, serverDirectory);
    }

    @Override
    public void receiveFile(String[] parts) {
        try {
            if (parts.length != 1) {
                outputStream.writeUTF(FTPStatus.message(FTPStatus.SYNTAX_ERROR) + "Usage: put");
            }
            outputStream.writeUTF("put allowed");
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

    @Override
    public void deleteFile(String[] parts) {
        if (parts.length != 2) {
            try {
                outputStream.writeUTF(FTPStatus.message(FTPStatus.SYNTAX_ERROR) + "Usage: delete <filename>");
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }
        String dirName = parts[1];
        try {
            if (!isPathWithinAllowedDirectory(dirName)) {
                outputStream.writeUTF(FTPStatus.message(FTPStatus.FILE_ACTION_NOT_TAKEN)
                        + ": Access denied - cannot delete directories outside allowed space");
                return;
            }

            File dir = new File(serverDirectory, dirName);
            if (dir.exists() && dir.isDirectory()) {
                deleteDirectoryRecursively(dir);
                outputStream.writeUTF(
                        FTPStatus.message(FTPStatus.FILE_ACTION_OK) + ": Directory deleted successfully: " + dirName);
            } else {
                outputStream.writeUTF(FTPStatus.message(FTPStatus.FILE_UNAVAILABLE)
                        + ": Directory does not exist or is not a directory: " + dirName);
            }
        } catch (IOException e) {
            e.printStackTrace();
            try {
                outputStream.writeUTF(
                        FTPStatus.message(FTPStatus.ACTION_ABORTED) + ": Error deleting directory: " + e.getMessage());
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    @Override
    // Modified createDirectory to accept the directory name as a parameter
    public void createDirectory(String[] parts) {
        if (parts.length != 2) {
            try {
                outputStream.writeUTF(FTPStatus.message(FTPStatus.SYNTAX_ERROR) + "Usage: mkdir <dirname>");
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }
        String dirName = parts[1];
        try {
            if (!isPathWithinAllowedDirectory(dirName)) {
                outputStream.writeUTF(FTPStatus.message(FTPStatus.FILE_ACTION_NOT_TAKEN)
                        + ": Access denied - cannot create directories outside allowed space");
                return;
            }

            File newDir = new File(serverDirectory, dirName);
            if (!newDir.exists()) {
                if (newDir.mkdir()) {
                    outputStream.writeUTF(FTPStatus.message(FTPStatus.FILE_ACTION_OK)
                            + ": Directory created successfully: " + dirName);
                } else {
                    outputStream.writeUTF(
                            FTPStatus.message(FTPStatus.ACTION_ABORTED) + ": Failed to create directory: " + dirName);
                }
            } else {
                outputStream.writeUTF(
                        FTPStatus.message(FTPStatus.FILE_ACTION_NOT_TAKEN) + ": Directory already exists: " + dirName);
            }
        } catch (IOException e) {
            e.printStackTrace();
            try {
                outputStream.writeUTF(
                        FTPStatus.message(FTPStatus.ACTION_ABORTED) + ": Error creating directory: " + e.getMessage());
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    @Override
    // Modified deleteDirectory to accept the directory name as a parameter
    public void deleteDirectory(String[] parts) {
        if (parts.length != 2) {
            try {
                outputStream.writeUTF(FTPStatus.message(FTPStatus.SYNTAX_ERROR) + "Usage: rmdir <dirname>");
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }
        String dirName = parts[1];
        try {
            if (!isPathWithinAllowedDirectory(dirName)) {
                outputStream.writeUTF(FTPStatus.message(FTPStatus.FILE_ACTION_NOT_TAKEN)
                        + ": Access denied - cannot delete directories outside allowed space");
                return;
            }

            File dir = new File(serverDirectory, dirName);
            if (dir.exists() && dir.isDirectory()) {
                deleteDirectoryRecursively(dir);
                outputStream.writeUTF(
                        FTPStatus.message(FTPStatus.FILE_ACTION_OK) + ": Directory deleted successfully: " + dirName);
            } else {
                outputStream.writeUTF(FTPStatus.message(FTPStatus.FILE_UNAVAILABLE)
                        + ": Directory does not exist or is not a directory: " + dirName);
            }
        } catch (IOException e) {
            e.printStackTrace();
            try {
                outputStream.writeUTF(
                        FTPStatus.message(FTPStatus.ACTION_ABORTED) + ": Error deleting directory: " + e.getMessage());
            } catch (IOException ex) {
                ex.printStackTrace();
            }
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
    public void moveFile(String[] parts) {
        if (parts.length != 3) {
            try {
                outputStream.writeUTF(FTPStatus.message(FTPStatus.SYNTAX_ERROR) + "Usage: move <source> <destination>");
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }
        String sourceName = parts[1];
        String destinationName = parts[2];
        try {
            if (!isPathWithinAllowedDirectory(sourceName) || !isPathWithinAllowedDirectory(destinationName)) {
                outputStream.writeUTF(FTPStatus.message(FTPStatus.FILE_ACTION_NOT_TAKEN)
                        + ": Access denied - cannot move files outside allowed space");
                return;
            }

            File sourceFile = new File(serverDirectory, sourceName);

            // Check if destinationName is a absolute or relative path
            File destFile;
            if (new File(destinationName).isAbsolute()) {
                destFile = new File("src/main/java/com/example/storage/", destinationName);
            } else {
                destFile = new File(serverDirectory, destinationName);
            }

            if (sourceFile.exists()) {
                if (sourceFile.renameTo(destFile)) {
                    outputStream.writeUTF(FTPStatus.message(FTPStatus.FILE_ACTION_OK));
                } else {
                    outputStream.writeUTF(FTPStatus.message(FTPStatus.ACTION_ABORTED) + ": File move operation failed");
                }
            } else {
                outputStream.writeUTF(
                        FTPStatus.message(FTPStatus.FILE_UNAVAILABLE) + ": Source file does not exist: " + sourceName);
            }
        } catch (IOException e) {
            e.printStackTrace();
            try {
                outputStream.writeUTF(
                        FTPStatus.message(FTPStatus.ACTION_ABORTED) + ": Error moving file: " + e.getMessage());
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    @Override
    public void showHelp() {
        try {
            outputStream.writeUTF(
                    "Available commands: put, get <filepath>, ls, cd <dirname>, delete <filename>, mkdir <dirName>, rmdir <dirName>, move <source> <destination>, pwd, help, quit");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
