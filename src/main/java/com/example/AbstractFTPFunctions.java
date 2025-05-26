package com.example;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.InputStreamReader;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.net.ServerSocket;
import java.nio.file.Path;
import java.nio.file.Paths;

public abstract class AbstractFTPFunctions {
    protected Socket socket;
    protected PrintWriter outputWriter;
    protected BufferedReader inputReader;
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
    
    // Transfer mode enum
    protected enum TransferType {
        ASCII, BINARY
    }
    protected TransferType transferMode = TransferType.BINARY;
    
    // Rename operation state
    protected String pendingRenameSource = null;

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
            outputWriter.println(FTPStatus.message(FTPStatus.SYNTAX_ERROR) + ": " + usage);
            return false;
        }
        return true;
    }

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

    private void debugOutput(String msg) {
        if (debugMode) {
        System.out.println("Thread " + ": " + msg);
        }
    }
    /**
     * Open a new data connection socket and wait for new incoming connection from client.
     */
    private void sendMsgToClient(String msg) {
        outputWriter.println(msg);
    }

    /**
     * Send a message to the connected client over the data connection.
     * 
     * @param msg Message to be sent
     */
    private void sendDataMsgToClient(String msg) {
        if (dataConnection == null || dataConnection.isClosed()) {
        sendMsgToClient("425 No data connection was established");
        debugOutput("Cannot send message, because no data connection is established");
        } else {
        dataOutWriter.print(msg + '\r' + '\n');
        }

    }
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

    /**
     * Connect to client socket for data connection. Used for active mode.
     */
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
    /**
     * Set client data connection parameters for active mode
     */
    protected void setClientDataConnection(String ipAddress, int port) {
        this.clientDataIP = ipAddress;
        this.clientDataPort = port;
        this.isPassiveMode = false;
    }

    /**
     * Close previously established data connection sockets and streams
     */
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

    public void handlePort(String[] parts) {

    String args = parts[1];
    // Extract IP address and port number from arguments
    String[] stringSplit = args.split(",");
    String hostName = stringSplit[0] + "." + stringSplit[1] + "." + stringSplit[2] + "." + stringSplit[3];

    int p = Integer.parseInt(stringSplit[4]) * 256 + Integer.parseInt(stringSplit[5]);

    // Initiate data connection to client
    openDataConnectionActive(hostName, p);
    sendMsgToClient("200 Command OK");
  }

  /**
   * Handler for the EPORT command. The client issues an EPORT command to the
   * server in active mode, so the server can open a data connection to the client
   * through the given address and port number.
   * 
   * @param args This string is separated by vertical bars and encodes the IP
   *             version, the IP address and the port number
   */
  public void handleEPort(String[] parts) {
    String args = parts[1];
    final String IPV4 = "1";
    final String IPV6 = "2";

    // Example arg: |2|::1|58770| or |1|132.235.1.2|6275|
    String[] splitArgs = args.split("\\|");
    String ipVersion = splitArgs[1];
    String ipAddress = splitArgs[2];

    System.out.println("IP Version: " + ipVersion);
    System.out.println("IP Address: " + ipAddress);

    System.out.println(IPV4.equals(ipVersion) + " " + IPV6.equals(ipVersion));

    if (!IPV4.equals(ipVersion) && !IPV6.equals(ipVersion)) {
      throw new IllegalArgumentException("Unsupported IP version");
    }

    int port = Integer.parseInt(splitArgs[3]);

    // Initiate data connection to client
    openDataConnectionActive(ipAddress, port);
    sendMsgToClient("200 Command OK");

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
        if (!validateCommand(parts, 1, "Usage: LIST")) {
            return;
        }
        System.out.println("Listing files...");
        try {
            File directory = new File(serverDirectory);

            File canonicalDir = directory.getCanonicalFile();
            File canonicalBase = new File(defaultDirectory).getCanonicalFile();
            if (!canonicalDir.getPath().startsWith(canonicalBase.getPath())) {
                outputWriter.println(FTPStatus.message(FTPStatus.FILE_ACTION_NOT_TAKEN) + ": Access denied");
                return;
            }

            String[] files = directory.list();
            if (files != null) {
                sendMsgToClient("125 Opening ASCII mode data connection for file list.");
                for (String file : files) {
                    sendDataMsgToClient(file);
                }
                if (files.length == 0) {
                    outputWriter.println(FTPStatus.message(FTPStatus.COMMAND_OK) + ": Directory is empty");
                }
            } else {
                outputWriter.println(FTPStatus.message(FTPStatus.FILE_UNAVAILABLE) + ": Directory cannot be read");
            }
            sendMsgToClient("226 Transfer complete.");
            closeDataConnection();
        } catch (IOException e) {
            e.printStackTrace();
            outputWriter.println(FTPStatus.message(FTPStatus.ACTION_ABORTED) + ": Error listing files: " + e.getMessage());
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
        if (!validateCommand(parts, 1, "Usage: PWD")) {
            return;
        }
        String display = displayWorkingDirectory();
        outputWriter.println(FTPStatus.message(FTPStatus.COMMAND_OK) + ": Current directory: " + display);
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
                outputWriter.println(FTPStatus.message(FTPStatus.FILE_UNAVAILABLE) + ": Access denied - unable to change directory.");
                return;
            }
            if (targetDir.exists() && targetDir.isDirectory()) {
                serverDirectory = targetDir.getPath();
                String display = displayWorkingDirectory();
                outputWriter.println(FTPStatus.message(FTPStatus.COMMAND_OK) + ": Directory changed to: " + display);
            } else {
                outputWriter.println(FTPStatus.message(FTPStatus.FILE_UNAVAILABLE) + ": Invalid directory");
            }
        } catch (IOException e) {
            e.printStackTrace();
            outputWriter.println(FTPStatus.message(FTPStatus.ACTION_ABORTED) + ": Error changing directory - " + e.getMessage());
        }
    }

    public void handleType(String[] parts) {
        String mode = parts[1];
        if (mode.toUpperCase().equals("A")) {
        transferMode = TransferType.ASCII;
        sendMsgToClient("200 OK");
        } else if (mode.toUpperCase().equals("I")) {
        transferMode = TransferType.BINARY;
        sendMsgToClient("200 OK");
        } else
        sendMsgToClient("504 Not OK");
        ;

    }

    public synchronized void sendFile(String[] parts) {
        if (!validateCommand(parts, 2, "Usage: RETR [FILE]")) {
            return;
        }
        
        String filePath = parts[1];
        try {
            if (!isPathWithinAllowedDirectory(filePath)) {
                outputWriter.println(FTPStatus.message(FTPStatus.FILE_UNAVAILABLE) + ": Access denied - cannot access files outside allowed space");
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

            // Binary mode transfer
            if (transferMode == TransferType.BINARY) {
                BufferedOutputStream fout = null;
                BufferedInputStream fin = null;

                outputWriter.println("150 Opening binary mode data connection for requested file " + file.getName());

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
            // ASCII mode transfer
            else {
                outputWriter.println("150 Opening ASCII mode data connection for requested file " + file.getName());

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

        } catch (Exception e) {
            e.printStackTrace();
            outputWriter.println(FTPStatus.message(FTPStatus.ACTION_ABORTED) + ": Error sending file - " + e.getMessage());
        } finally {
            closeDataConnection();
        }
    }

    protected void handleException(IOException e) {
        e.printStackTrace();
        outputWriter.println("Error: " + e.getMessage());
    }

    // Abstract methods to be implemented by subclasses
    public abstract void deleteFile(String[] parts);

    public abstract void createDirectory(String[] parts);

    public abstract void deleteDirectory(String[] parts);

    public abstract void receiveFile(String[] parts);

    public abstract void handleRenameFrom(String[] parts);
    
    public abstract void handleRenameTo(String[] parts);

    public abstract void showHelp(String[] parts);
}
