# FTP Simulator (Team TAM)

## 📑 Table of Contents
- [Summary](#summary)  
- [👥 Team Members](#-team-members)  
- [⚙️ Technologies](#️-technologies)  
- [🚀 Features](#-features)  
- [📥 Installation & Usage](#-installation--usage)  

## 🗒️ Summary

This repository, developed by **Team TAM**, provides a simulation of an FTP server and client in Java, complete with authentication powered by MongoDB. Our implementation takes reference from the guide from Colorado State University: [reference link](https://www.cs.colostate.edu/helpdocs/ftp.html). 

The project is developed as part of the Computer Network 2 course at Vietnamese-German University (Binh Duong, Vietnam) in the Summer Semester of 2025, which was taught by Dr. Truong Dinh Huy and Mr. Le Duy Hung.

## 👥 Team Members

| Name           | Student ID                     |
|----------------|--------------------------------|
| Yamashita Tri An       | 10422004           |
| Ton That Nhat Minh       | 10422050           |
| Le Viet Tin       | 10422078           |

## ⚙️ Technologies

- **Java 8+**  
- **Maven** for build and dependency management  
- **MongoDB** for storing user credentials, authentication, and records of anonymous access  
- **TCP Sockets** for server-client communication  

## 🚀 Features

- **User Authentication**
  - Login with username and password
  - Anonymous login with limited access
  - MongoDB-based user credential management
  
- **File Operations**
  - Upload files (put)
  - Download files (get [FILE])
  - List directories and files (ls)
  - Change directories (cd [DIRECTORY])
  - Remove files (rm [FILE])
  - Create directories (mkdir [DIRECTORY])
  - Remove directories (rmdir [DIRECTORY])
  - Move files (mv [SOURCE] [DESTINATION])
  - Print working directory (pwd)
  - Help command for available operations (help)
  - Quit command to exit the client (quit)
  
- **Connection Management**
  - Use TCP sockets for client-server communication
  - Handle multiple client connections using threads
  
- **Security Features**
  - User and guest features separation
  - Handle command parameters and arguments
  
- **Client Interface**
  - Command-line interface for client operations
  - Response code handling according to FTP protocol standards
  
- **Extensibility**
  - Modular design for adding new commands
  - Customizable server configurations

## 📥 Installation & Usage

### Prerequisites 🔧

Before you begin, make sure you have the following software installed and configured:

* **Java Runtime Environment (JRE)** (bundled in this repository under `JRE/`)
* **Java Development Kit (JDK) 23** (required only if you plan to build or modify the source code)
* **IDE (optional)**: IntelliJ IDEA, Eclipse, or NetBeans (latest versions recommended)

> **Tip:** If you’re using an OS without a bundled JDK 23, download it from the [official Oracle website](https://www.oracle.com/java/technologies/downloads/#jdk23).

---

### 📁 Project Structure

```bash
demo/
├── .vscode/                
├── JRE/                    
├── src/main/java/com/example/
│   ├── storage/                     # Holds server-side storage (directories and files)
│   ├── AbstractFTPFunctions.java    # Abstract class defining common FTP commands and utility methods for unauthenticated and authenticated users
│   ├── AnonymousFTPFunctions.java   # FTP command implementations available to unauthenticated (anonymous) users
│   ├── Client.java                  # FTP client implementations: Start new FTP connections with the server
│   ├── ClientHandler.java           # Handles reading server replies and dispatching client-side actions
│   ├── ClientReceiving.java         # Handles incoming messages from the server on the client side
│   ├── ClientSending.java           # Handles outcoming messages to the server on the client side
│   ├── Server.java                  # FTP server implementations: Starts the FTP server, listens for new client connections
│   ├── ServerHandler.java           # Handles an individual client session: authentication, command parsing, and response
│   └── UserFTPFunctions.java        # FTP command implementations for authenticated users
├── test/                  
├── target/                 
├── .editorconfig           
├── .gitignore              
├── client.jar              
├── client.xml              
├── FTPClient.exe           # Windows executable file for client
├── FTPServer.exe           # Windows executable file for server
├── pom.xml                 # Maven project file
├── README.md               # Project documentation
├── server.jar             
└── server.xml            
```

---

### ▶️ Running the Executable Binaries

1. **Open** the `DEMO/` folder in your file explorer.
2. **Verify** the `JRE/` subfolder contains both `bin/` and `lib/` directories.
3. **Launch**:

   * Double-click `FTPServer.exe` to start the server.
   * Double-click `FTPClient.exe` to start the client.
---

### 🧑‍💻 Running from Source

1. Open your preferred IDE (e.g., VS Code, IntelliJ IDEA).
2. **Import** the Maven project (`pom.xml`).
3. Navigate to `src/main/java/com/example/`.
4. Run `Server.java` first to start the server.
5. Run `Client.java` to start the client.

> **Note:** You can also build and run the JARs via Maven:
>
> ```bash
> mvn clean package
> java -jar target/server.jar --config server.xml
> java -jar target/client.jar --config client.xml
> ```

---
