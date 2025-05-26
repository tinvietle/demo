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

    // ===============================
    // CONSTRUCTOR
    // ===============================
    
    public UserFTPFunctions(Socket socket, String serverDirectory) throws IOException {
        super(socket, serverDirectory);
    }

    // ===============================
    // FILE UPLOAD
    // ===============================
    
    @Override
    public synchronized void receiveFile(String[] parts) {
        if (parts.length < 2) {
            outputWriter.println("501 No filename given");
            return;
        }

        String fileName = parts[1];

        try {
            if (!validateFileUpload(fileName)) {
                return;
            }

            File outputFile = new File(serverDirectory, fileName);

            if (outputFile.exists()) {
                outputWriter.println("550 File already exists");
                return;
            }

            if (transferMode == TransferType.BINARY) {
                receiveFileBinary(outputFile, fileName);
            } else {
                receiveFileAscii(outputFile, fileName);
            }

        } catch (Exception e) {
            e.printStackTrace();
            outputWriter.println("451 Requested action aborted: File upload failed - " + 
                               e.getMessage());
        } finally {
            closeDataConnection();
        }
    }

    private boolean validateFileUpload(String fileName) {
        // Security check for path traversal
        if (fileName.contains("..") || !isPathWithinAllowedDirectory(fileName)) {
            outputWriter.println("550 Requested action not taken. File unavailable: " +
                               "File upload failed - invalid file path");
            return false;
        }

        try {
            File outputFile = new File(serverDirectory, fileName);
            File canonicalFile = outputFile.getCanonicalFile();
            File canonicalBase = new File(defaultDirectory).getCanonicalFile();
            
            if (!canonicalFile.getPath().startsWith(canonicalBase.getPath())) {
                outputWriter.println("550 Requested action not taken. File unavailable: " +
                                   "File upload failed - invalid file path");
                return false;
            }
        } catch (IOException e) {
            outputWriter.println("550 Requested action not taken. File unavailable: " +
                               "File upload failed - " + e.getMessage());
            return false;
        }

        return true;
    }

    private void receiveFileBinary(File outputFile, String fileName) {
        BufferedOutputStream fout = null;
        BufferedInputStream fin = null;

        outputWriter.println("150 Opening binary mode data connection for requested file " + 
                           fileName);

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
            closeFileStreams(fin, fout);
        }
    }

    private void receiveFileAscii(File outputFile, String fileName) {
        outputWriter.println("150 Opening ASCII mode data connection for requested file " + 
                           fileName);

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
            closeFileStreams(rin, rout);
        }
    }

    private void closeFileStreams(Object... streams) {
        for (Object stream : streams) {
            try {
                if (stream != null) {
                    if (stream instanceof BufferedInputStream) {
                        ((BufferedInputStream) stream).close();
                    } else if (stream instanceof BufferedOutputStream) {
                        ((BufferedOutputStream) stream).close();
                    } else if (stream instanceof BufferedReader) {
                        ((BufferedReader) stream).close();
                    } else if (stream instanceof PrintWriter) {
                        ((PrintWriter) stream).close();
                    }
                }
            } catch (IOException e) {
                System.out.println("Could not close file streams");
                e.printStackTrace();
            }
        }
    }

    // ===============================
    // FILE OPERATIONS
    // ===============================
    
    @Override
    public synchronized void deleteFile(String[] parts) {
        if (!validateCommand(parts, 2, "Usage: DELE [FILE]")) {
            return;
        }
        
        String filename = parts[1];
        if (!isPathWithinAllowedDirectory(filename)) {
            outputWriter.println("450 Requested file action not taken: " +
                               "Access denied - cannot delete directories outside allowed space");
            return;
        }

        File file = resolveFilePath(filename);

        if (file.delete()) {
            outputWriter.println("250 Requested file action okay, completed");
        } else {
            if (!file.exists()) {
                outputWriter.println("550 Requested action not taken. File unavailable: " +
                                   "File does not exist");
            } else {
                outputWriter.println("451 Requested action aborted: Failed to delete file");
            }
        }
    }

    // ===============================
    // DIRECTORY OPERATIONS
    // ===============================
    
    @Override
    public synchronized void createDirectory(String[] parts) {
        if (!validateCommand(parts, 2, "Usage: MKD [DIRECTORY]")) {
            return;
        }
        
        String dirName = parts[1];
        if (!isPathWithinAllowedDirectory(dirName)) {
            outputWriter.println("450 Requested file action not taken: " +
                               "Access denied - cannot create directories outside allowed space");
            return;
        }

        File newDir = new File(serverDirectory, dirName);
        if (!newDir.exists()) {
            if (newDir.mkdir()) {
                outputWriter.println("250 Requested file action okay, completed: " +
                                   "Directory created successfully: " + dirName);
            } else {
                outputWriter.println("451 Requested action aborted: " +
                                   "Failed to create directory: " + dirName);
            }
        } else {
            outputWriter.println("450 Requested file action not taken: " +
                               "Directory already exists: " + dirName);
        }
    }

    @Override
    public synchronized void deleteDirectory(String[] parts) {
        if (!validateCommand(parts, 2, "Usage: RMD [DIRECTORY]")) {
            return;
        }
        
        String dirName = parts[1];
        if (!isPathWithinAllowedDirectory(dirName)) {
            outputWriter.println("450 Requested file action not taken: " +
                               "Access denied - cannot delete directories outside allowed space");
            return;
        }

        File dir = new File(serverDirectory, dirName);
        if (dir.exists() && dir.isDirectory()) {
            deleteDirectoryRecursively(dir);
            outputWriter.println("250 Requested file action okay, completed: " +
                               "Directory deleted successfully: " + dirName);
        } else {
            outputWriter.println("550 Requested action not taken. File unavailable: " +
                               "Directory does not exist or is not a directory: " + dirName);
        }
    }

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

    // ===============================
    // RENAME OPERATIONS
    // ===============================
    
    @Override
    public synchronized void handleRenameFrom(String[] parts) {
        if (!validateCommand(parts, 2, "Usage: RNFR [SOURCE_FILE]")) {
            return;
        }
        
        String sourceName = parts[1];
        if (!isPathWithinAllowedDirectory(sourceName)) {
            outputWriter.println("450 Requested file action not taken: " +
                               "Access denied - cannot rename files outside allowed space");
            return;
        }

        File sourceFile = new File(serverDirectory, sourceName);
        if (!sourceFile.exists()) {
            outputWriter.println("550 Requested action not taken. File unavailable: " +
                               "Source file does not exist: " + sourceName);
            return;
        }

        pendingRenameSource = sourceName;
        outputWriter.println("350 Requested file action pending further information");
    }

    @Override
    public synchronized void handleRenameTo(String[] parts) {
        if (pendingRenameSource == null) {
            outputWriter.println("503 Bad sequence of commands: RNTO must be preceded by RNFR");
            return;
        }
        
        if (!validateCommand(parts, 2, "Usage: RNTO [DESTINATION_FILE]")) {
            pendingRenameSource = null;
            return;
        }
        
        String destinationName = parts[1];
        if (!isPathWithinAllowedDirectory(destinationName)) {
            outputWriter.println("450 Requested file action not taken: " +
                               "Access denied - cannot rename files outside allowed space");
            pendingRenameSource = null;
            return;
        }

        File sourceFile = new File(serverDirectory, pendingRenameSource);
        File destFile = resolveFilePath(destinationName);

        if (sourceFile.renameTo(destFile)) {
            outputWriter.println("250 Requested file action okay, completed: Rename successful");
        } else {
            outputWriter.println("451 Requested action aborted: Rename operation failed");
        }
        
        pendingRenameSource = null;
    }

    // ===============================
    // UTILITY METHODS
    // ===============================
    
    private File resolveFilePath(String filename) {
        if (new File(filename).isAbsolute()) {
            return new File("src/main/java/com/example/storage/", filename);
        } else {
            return new File(serverDirectory, filename);
        }
    }

    @Override
    public void showHelp(String[] parts) {
        outputWriter.println("Available commands: STOR, RETR [FILE], LIST, CWD [DIRECTORY], " +
                           "DELE [FILE], MKD [DIRECTORY], RMD [DIRECTORY], RNFR [SOURCE], " +
                           "RNTO [DESTINATION], PWD, HELP, QUIT");
    }
}
