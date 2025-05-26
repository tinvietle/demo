package com.example;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.nio.file.Paths;

public abstract class AbstractFTPFunctions {
    
    // ===============================
    // FIELDS
    // ===============================
    
    // Connection fields
    protected Socket socket;
    protected PrintWriter outputWriter;
    protected BufferedReader inputReader;
    
    // Directory fields
    protected String defaultDirectory;
    protected String serverDirectory;
    
    // Data connection fields
    protected ServerSocket dataSocket;
    protected Socket dataConnection;
    protected PrintWriter dataOutWriter;
    protected int dataPort = 20;
    
    // Active mode data connection fields
    protected String clientDataIP;
    protected int clientDataPort;
    protected boolean isPassiveMode = true;
    private boolean debugMode = true;
    
    // Transfer mode enum and state
    protected enum TransferType {
        ASCII, BINARY
    }
    protected TransferType transferMode = TransferType.BINARY;
    
    // Rename operation state
    protected String pendingRenameSource = null;

    // ===============================
    // CONSTRUCTOR
    // ===============================
    
    public AbstractFTPFunctions(Socket socket, String serverDirectory) {
        this.socket = socket;
        this.serverDirectory = serverDirectory;
        this.defaultDirectory = serverDirectory;

        try {
            this.outputWriter = new PrintWriter(socket.getOutputStream(), true);
            this.inputReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (Exception e) {
            System.out.println("Error initializing streams: " + e.getMessage());
        }
    }

    // ===============================
    // UTILITY METHODS
    // ===============================
    
    protected boolean validateCommand(String[] parts, int expectedLength, String usage) {
        if (parts.length != expectedLength) {
            outputWriter.println("500 Syntax error, command unrecognized: " + usage);
            return false;
        }
        return true;
    }

    private void debugOutput(String msg) {
        if (debugMode) {
            System.out.println("Thread " + ": " + msg);
        }
    }

    private void sendMsgToClient(String msg) {
        outputWriter.println(msg);
    }

    private void sendDataMsgToClient(String msg) {
        if (dataConnection == null || dataConnection.isClosed()) {
            sendMsgToClient("425 No data connection was established");
            debugOutput("Cannot send message, because no data connection is established");
        } else {
            dataOutWriter.print(msg + '\r' + '\n');
        }
    }

    protected void handleException(IOException e) {
        e.printStackTrace();
        outputWriter.println("Error: " + e.getMessage());
    }

    // ===============================
    // DATA CONNECTION METHODS
    // ===============================
    
    protected void openDataConnectionPassive(int port) {
        try {
            dataSocket = new ServerSocket(port);
            dataConnection = dataSocket.accept();
            dataOutWriter = new PrintWriter(dataConnection.getOutputStream(), true);
            System.out.println("Data connection - Passive Mode - established");
        } catch (IOException e) {
            System.out.println("Could not create data connection.");
            e.printStackTrace();
        }
    }

    protected void openDataConnectionActive(String ipAddress, int port) {
        try {
            dataConnection = new Socket(ipAddress, port);
            dataOutWriter = new PrintWriter(dataConnection.getOutputStream(), true);
            System.out.println("Data connection - Active Mode - established");
        } catch (IOException e) {
            System.out.println("Could not connect to client data socket");
            e.printStackTrace();
        }
    }

    protected void setClientDataConnection(String ipAddress, int port) {
        this.clientDataIP = ipAddress;
        this.clientDataPort = port;
        this.isPassiveMode = false;
    }

    protected void closeDataConnection() {
        try {
            if (dataOutWriter != null) {
                dataOutWriter.close();
            }
            if (dataConnection != null) {
                dataConnection.close();
            }
            if (dataSocket != null) {
                dataSocket.close();
            }
            System.out.println("Data connection was closed");
        } catch (IOException e) {
            System.out.println("Could not close data connection");
            e.printStackTrace();
        }
        dataOutWriter = null;
        dataConnection = null;
        dataSocket = null;
    }

    // ===============================
    // PROTOCOL COMMAND HANDLERS
    // ===============================
    
    public void handlePort(String[] parts) {
        String args = parts[1];
        String[] stringSplit = args.split(",");
        String hostName = stringSplit[0] + "." + stringSplit[1] + "." + 
                         stringSplit[2] + "." + stringSplit[3];

        int p = Integer.parseInt(stringSplit[4]) * 256 + Integer.parseInt(stringSplit[5]);

        openDataConnectionActive(hostName, p);
        sendMsgToClient("200 Command OK");
    }

    public void handleEPort(String[] parts) {
        String args = parts[1];
        final String IPV4 = "1";
        final String IPV6 = "2";

        String[] splitArgs = args.split("\\|");
        String ipVersion = splitArgs[1];
        String ipAddress = splitArgs[2];

        System.out.println("IP Version: " + ipVersion);
        System.out.println("IP Address: " + ipAddress);

        if (!IPV4.equals(ipVersion) && !IPV6.equals(ipVersion)) {
            throw new IllegalArgumentException("Unsupported IP version");
        }

        int port = Integer.parseInt(splitArgs[3]);
        openDataConnectionActive(ipAddress, port);
        sendMsgToClient("200 Command OK");
    }

    public void handleType(String[] parts) {
        String mode = parts[1];
        if (mode.toUpperCase().equals("A")) {
            transferMode = TransferType.ASCII;
            sendMsgToClient("200 OK");
        } else if (mode.toUpperCase().equals("I")) {
            transferMode = TransferType.BINARY;
            sendMsgToClient("200 OK");
        } else {
            sendMsgToClient("504 Not OK");
        }
    }

    // ===============================
    // PATH VALIDATION METHODS
    // ===============================
    
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
            File targetPath;
            
            if (new File(path).isAbsolute()) {
                targetPath = new File("src/main/java/com/example/storage/", path);
            } else {
                targetPath = new File(serverDirectory, path);
            }
            targetPath = targetPath.getCanonicalFile();

            String commonBase = getCommonBasePath(baseDir.getCanonicalPath(), 
                                                targetPath.getCanonicalPath());
            return (commonBase.equals(baseDir.getCanonicalPath()));
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    // ===============================
    // DIRECTORY OPERATIONS
    // ===============================
    
    public String displayWorkingDirectory() {
        try {
            String baseFolder = new File(defaultDirectory).getName();
            String basePath = new File(defaultDirectory).getCanonicalPath();
            String currentPath = new File(serverDirectory).getCanonicalPath();
            String relative = "";
            
            if (currentPath.startsWith(basePath)) {
                relative = currentPath.substring(basePath.length());
                if (relative.startsWith(File.separator)) {
                    relative = relative.substring(1);
                }
            }
            
            String display = "/" + baseFolder;
            if (!relative.isEmpty()) {
                display += "/" + relative;
            }
            
            return display.replace("\\", "/");
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }

    public void printWorkingDirectory(String[] parts) {
        if (!validateCommand(parts, 1, "Usage: PWD")) {
            return;
        }
        String display = displayWorkingDirectory();
        outputWriter.println("200 Command okay: Current directory: " + display);
    }

    public synchronized void changeDirectory(String[] parts) {
        if (!validateCommand(parts, 2, "Usage: CWD [DIRECTORY]")) {
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
                outputWriter.println("550 Requested action not taken. File unavailable: " +
                                   "Access denied - unable to change directory.");
                return;
            }
            
            if (targetDir.exists() && targetDir.isDirectory()) {
                serverDirectory = targetDir.getPath();
                String display = displayWorkingDirectory();
                outputWriter.println("200 Command okay: Directory changed to: " + display);
            } else {
                outputWriter.println("550 Requested action not taken. File unavailable: " +
                                   "Invalid directory");
            }
        } catch (IOException e) {
            e.printStackTrace();
            outputWriter.println("451 Requested action aborted: Error changing directory - " + 
                               e.getMessage());
        }
    }

    // ===============================
    // FILE LISTING
    // ===============================
    
    public void listFiles(String[] parts) {
        if (!validateCommand(parts, 1, "Usage: LIST")) {
            return;
        }
        
        System.out.println("Listing files...");
        
        try {
            File directory = new File(serverDirectory);
            File canonicalDir = directory.getCanonicalFile();
            File canonicalBase = new File(defaultDirectory).getCanonicalFile();
            
            if (!canonicalDir.getPath().startsWith(canonicalBase.getPath())) {
                outputWriter.println("450 Requested file action not taken: Access denied");
                return;
            }

            String[] files = directory.list();
            if (files != null) {
                sendMsgToClient("125 Opening ASCII mode data connection for file list.");
                for (String file : files) {
                    sendDataMsgToClient(file);
                }
                if (files.length == 0) {
                    outputWriter.println("200 Command okay: Directory is empty");
                }
            } else {
                outputWriter.println("550 Requested action not taken. File unavailable: " +
                                   "Directory cannot be read");
            }
            
            sendMsgToClient("226 Transfer complete.");
            closeDataConnection();
        } catch (IOException e) {
            e.printStackTrace();
            outputWriter.println("451 Requested action aborted: Error listing files: " + 
                               e.getMessage());
        }
    }

    // ===============================
    // FILE TRANSFER - DOWNLOAD
    // ===============================
    
    public synchronized void sendFile(String[] parts) {
        if (!validateCommand(parts, 2, "Usage: RETR [FILE]")) {
            return;
        }
        
        String filePath = parts[1];
        
        try {
            if (!isPathWithinAllowedDirectory(filePath)) {
                outputWriter.println("550 Requested action not taken. File unavailable: " +
                                   "Access denied - cannot access files outside allowed space");
                return;
            }

            File file;
            if (new File(filePath).isAbsolute()) {
                file = new File("src/main/java/com/example/storage/", filePath);
            } else {
                file = new File(serverDirectory, filePath);
            }

            if (!file.exists()) {
                outputWriter.println("550 File does not exist");
                return;
            }

            if (transferMode == TransferType.BINARY) {
                sendFileBinary(file);
            } else {
                sendFileAscii(file);
            }

        } catch (Exception e) {
            e.printStackTrace();
            outputWriter.println("451 Requested action aborted: Error sending file - " + 
                               e.getMessage());
        } finally {
            closeDataConnection();
        }
    }

    private void sendFileBinary(File file) throws IOException {
        BufferedOutputStream fout = null;
        BufferedInputStream fin = null;

        outputWriter.println("150 Opening binary mode data connection for requested file " + 
                           file.getName());

        try {
            fout = new BufferedOutputStream(dataConnection.getOutputStream());
            fin = new BufferedInputStream(new FileInputStream(file));

            System.out.println("Starting file transmission of " + file.getName());

            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = fin.read(buffer, 0, 1024)) != -1) {
                fout.write(buffer, 0, bytesRead);
            }
            fout.flush();

            System.out.println("Completed file transmission of " + file.getName());
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

    private void sendFileAscii(File file) throws IOException {
        outputWriter.println("150 Opening ASCII mode data connection for requested file " + 
                           file.getName());

        BufferedReader rin = null;
        PrintWriter rout = null;

        try {
            rin = new BufferedReader(new FileReader(file));
            rout = new PrintWriter(dataConnection.getOutputStream(), true);

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

    // ===============================
    // ABSTRACT METHODS
    // ===============================
    
    public abstract void deleteFile(String[] parts);
    public abstract void createDirectory(String[] parts);
    public abstract void deleteDirectory(String[] parts);
    public abstract void receiveFile(String[] parts);
    public abstract void handleRenameFrom(String[] parts);
    public abstract void handleRenameTo(String[] parts);
    public abstract void showHelp(String[] parts);
}
