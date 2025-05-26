package com.example;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class UserFTPFunctions extends AbstractFTPFunctions {

    public UserFTPFunctions(Socket socket, String serverDirectory) throws IOException {
        super(socket, serverDirectory);
    }

    @Override
    public synchronized void receiveFile(String[] parts) {
        if (parts.length < 2) {
            outputWriter.println("501 No filename given");
            return;
        }

        String fileName = parts[1];

        try {
            // Security check for path traversal
            if (fileName.contains("..") || !isPathWithinAllowedDirectory(fileName)) {
                outputWriter.println(FTPStatus.message(FTPStatus.FILE_UNAVAILABLE) + ": File upload failed - invalid file path");
                return;
            }

            File outputFile = new File(serverDirectory, fileName);

            // Verify the path after canonicalization
            File canonicalFile = outputFile.getCanonicalFile();
            File canonicalBase = new File(defaultDirectory).getCanonicalFile();
            if (!canonicalFile.getPath().startsWith(canonicalBase.getPath())) {
                outputWriter.println(FTPStatus.message(FTPStatus.FILE_UNAVAILABLE) + ": File upload failed - invalid file path");
                return;
            }

            if (outputFile.exists()) {
                outputWriter.println("550 File already exists");
                return;
            }

            // Binary mode transfer
            if (transferMode == TransferType.BINARY) {
                BufferedOutputStream fout = null;
                BufferedInputStream fin = null;

                outputWriter.println("150 Opening binary mode data connection for requested file " + fileName);

                try {
                    fout = new BufferedOutputStream(new FileOutputStream(outputFile));
                    fin = new BufferedInputStream(dataConnection.getInputStream());

                    System.out.println("Start receiving file " + fileName);

                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = fin.read(buffer, 0, 1024)) != -1) {
                        fout.write(buffer, 0, bytesRead);
                    }

                    System.out.println("Completed receiving file " + fileName);
                    outputWriter.println("226 File transfer successful. Closing data connection.");

                } catch (IOException e) {
                    System.out.println("Could not read from or write to file streams");
                    e.printStackTrace();
                    outputWriter.println("426 Transfer aborted");
                } finally {
                    try {
                        if (fin != null) fin.close();
                        if (fout != null) fout.close();
                    } catch (IOException e) {
                        System.out.println("Could not close file streams");
                        e.printStackTrace();
                    }
                }
            }
            // ASCII mode transfer
            else {
                outputWriter.println("150 Opening ASCII mode data connection for requested file " + fileName);

                BufferedReader rin = null;
                PrintWriter rout = null;

                try {
                    rin = new BufferedReader(new InputStreamReader(dataConnection.getInputStream()));
                    rout = new PrintWriter(new FileOutputStream(outputFile), true);

                    String line;
                    while ((line = rin.readLine()) != null) {
                        rout.println(line);
                    }

                    outputWriter.println("226 File transfer successful. Closing data connection.");
                } catch (IOException e) {
                    System.out.println("Could not read from or write to file streams");
                    e.printStackTrace();
                    outputWriter.println("426 Transfer aborted");
                } finally {
                    try {
                        if (rout != null) rout.close();
                        if (rin != null) rin.close();
                    } catch (IOException e) {
                        System.out.println("Could not close file streams");
                        e.printStackTrace();
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            outputWriter.println(FTPStatus.message(FTPStatus.ACTION_ABORTED) + ": File upload failed - " + e.getMessage());
        } finally {
            closeDataConnection();
        }
    }

    @Override
    public synchronized void deleteFile(String[] parts) {
        if (!validateCommand(parts, 2, "Usage: DELE [FILE]")) {
            return;
        }
        String filename = parts[1];
        if (!isPathWithinAllowedDirectory(filename)) {
            outputWriter.println(FTPStatus.message(FTPStatus.FILE_ACTION_NOT_TAKEN) + ": Access denied - cannot delete directories outside allowed space");
            return;
        }

        File file;
        // Check if filename is an absolute or relative path
        if (new File(filename).isAbsolute()) {
            file = new File("src/main/java/com/example/storage/", filename);
        } else {
            file = new File(serverDirectory, filename);
        }

        if (file.delete()) {
            outputWriter.println(FTPStatus.message(FTPStatus.FILE_ACTION_OK));
        } else {
            if (!file.exists()) {
                outputWriter.println(FTPStatus.message(FTPStatus.FILE_UNAVAILABLE) + ": File does not exist");
            } else {
                outputWriter.println(FTPStatus.message(FTPStatus.ACTION_ABORTED) + ": Failed to delete file");
            }
        }
    }

    @Override
    // Modified createDirectory to accept the directory name as a parameter
    public synchronized void createDirectory(String[] parts) {
        if (!validateCommand(parts, 2, "Usage: MKD [DIRECTORY]")) {
            return;
        }
        String dirName = parts[1];
        if (!isPathWithinAllowedDirectory(dirName)) {
            outputWriter.println(FTPStatus.message(FTPStatus.FILE_ACTION_NOT_TAKEN)
                    + ": Access denied - cannot create directories outside allowed space");
            return;
        }

        File newDir = new File(serverDirectory, dirName);
        if (!newDir.exists()) {
            if (newDir.mkdir()) {
                outputWriter.println(FTPStatus.message(FTPStatus.FILE_ACTION_OK) + ": Directory created successfully: " + dirName);
            } else {
                outputWriter.println(FTPStatus.message(FTPStatus.ACTION_ABORTED) + ": Failed to create directory: " + dirName);
            }
        } else {
            outputWriter.println(FTPStatus.message(FTPStatus.FILE_ACTION_NOT_TAKEN) + ": Directory already exists: " + dirName);
        }
    }

    @Override
    // Modified deleteDirectory to accept the directory name as a parameter
    public synchronized void deleteDirectory(String[] parts) {
        if (!validateCommand(parts, 2, "Usage: RMD [DIRECTORY]")) {
            return;
        }
        String dirName = parts[1];
        if (!isPathWithinAllowedDirectory(dirName)) {
            outputWriter.println(FTPStatus.message(FTPStatus.FILE_ACTION_NOT_TAKEN)
                    + ": Access denied - cannot delete directories outside allowed space");
            return;
        }

        File dir = new File(serverDirectory, dirName);
        if (dir.exists() && dir.isDirectory()) {
            deleteDirectoryRecursively(dir);
            outputWriter.println(
                    FTPStatus.message(FTPStatus.FILE_ACTION_OK) + ": Directory deleted successfully: " + dirName);
        } else {
            outputWriter.println(FTPStatus.message(FTPStatus.FILE_UNAVAILABLE)
                    + ": Directory does not exist or is not a directory: " + dirName);
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
    public synchronized void handleRenameFrom(String[] parts) {
        if (!validateCommand(parts, 2, "Usage: RNFR [SOURCE_FILE]")) {
            return;
        }
        
        String sourceName = parts[1];
        if (!isPathWithinAllowedDirectory(sourceName)) {
            outputWriter.println(FTPStatus.message(FTPStatus.FILE_ACTION_NOT_TAKEN)
                    + ": Access denied - cannot rename files outside allowed space");
            return;
        }

        File sourceFile = new File(serverDirectory, sourceName);
        if (!sourceFile.exists()) {
            outputWriter.println(FTPStatus.message(FTPStatus.FILE_UNAVAILABLE) + ": Source file does not exist: " + sourceName);
            return;
        }

        pendingRenameSource = sourceName;
        outputWriter.println("350 Requested file action pending further information");
    }

    @Override
    public synchronized void handleRenameTo(String[] parts) {
        if (pendingRenameSource == null) {
            outputWriter.println(FTPStatus.message(FTPStatus.BAD_SEQUENCE) + ": RNTO must be preceded by RNFR");
            return;
        }
        
        if (!validateCommand(parts, 2, "Usage: RNTO [DESTINATION_FILE]")) {
            pendingRenameSource = null;
            return;
        }
        
        String destinationName = parts[1];
        if (!isPathWithinAllowedDirectory(destinationName)) {
            outputWriter.println(FTPStatus.message(FTPStatus.FILE_ACTION_NOT_TAKEN)
                    + ": Access denied - cannot rename files outside allowed space");
            pendingRenameSource = null;
            return;
        }

        File sourceFile = new File(serverDirectory, pendingRenameSource);
        File destFile;
        if (new File(destinationName).isAbsolute()) {
            destFile = new File("src/main/java/com/example/storage/", destinationName);
        } else {
            destFile = new File(serverDirectory, destinationName);
        }

        if (sourceFile.renameTo(destFile)) {
            outputWriter.println(FTPStatus.message(FTPStatus.FILE_ACTION_OK) + ": Rename successful");
        } else {
            outputWriter.println(FTPStatus.message(FTPStatus.ACTION_ABORTED) + ": Rename operation failed");
        }
        
        pendingRenameSource = null;
    }

    @Override
    public void showHelp(String[] parts) {
        outputWriter.println("Available commands: STOR, RETR [FILE], LIST, CWD [DIRECTORY], DELE [FILE], MKD [DIRECTORY], RMD [DIRECTORY], RNFR [SOURCE], RNTO [DESTINATION], PWD, HELP, QUIT");
    }
}
