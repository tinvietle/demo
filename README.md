# Windows FTP Simulator (Team TAM)

## 📑 Table of Contents
- [Summary](#summary)  
- [👥 Team Members](#-team-members)  
- [⚙️ Technologies](#️-technologies)  
- [🚀 Features](#-features)  
- [📥 Installation & Usage](#-installation--usage)  

## 🗒️ Summary

This repository, developed by **Team TAM**, provides a simulation of an FTP server and client in Java, complete with authentication powered by MongoDB. Our implementation takes reference from the guide from this Github Project: [reference link](https://github.com/pReya/ftpServer.git). Using this project, we can connect our FTP Server with the Windows' built-in FTP client (`ftp.exe`), allowing users to perform file operations such as uploading, downloading, and managing directories without needing an extra FTP client application.

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

**Available commands for authenticated user in their own directory:** 

`put get ls cd delete mkdir rmdir rename pwd help quit`

**Available commands for unauthenticated user in the public directory:** 

`get ls cd pwd help quit`

### 🎯 Core (6 points)

- **Authentication**  
  - Login with username & password  
  - Anonymous (guest) login with username "anonymous" and their email address as the password
  - MongoDB-based user credentials and anonymous logins storage

- **Directory Listing (`ls`)**  
  - Lists files and subdirectories in the current working directory  

- **Download (`get [REMOTE FILE] [LOCAL NAME]`)**  
  - Retrieves the specified file from the server to the client  

- **Upload (`put [LOCAL FILE] [REMOTE FILE]`)**  
  - Sends the specified file from the client to the server  

- **Quit (`quit`)**  
  - Closes the client session and releases resources  

- **Concurrency**  
  - Supports multiple simultaneous client connections via threading  

---

### ➕ Additional Commands

- **Change Directory (`cd [DIRECTORY]`)**  
  - Switches the client’s working directory on the server  

- **Remove File (`delete [REMOTE FILE]`)**  
  - Deletes the specified file from the server  

- **Make Directory (`mkdir [DIRECTORY]`)**  
  - Creates a new directory on the server  

- **Remove Directory (`rmdir [DIRECTORY]`)**  
  - Deletes an existing empty directory on the server  

- **Move/Rename (`rename [SOURCE] [DESTINATION]`)**  
  - Moves or renames a file or directory on the server  

- **Print Working Directory (`pwd`)**  
  - Displays the full path of the current working directory  

- ** Port Forwarding (`PORT/EPRT [PORT]`)**  
  - Tells the server to use active mode, where the client specifies its own IP and port, and the server connects to the client for data transfer

- **Help (`help`)**  
  - Displays a list of all available commands with brief usage notes  

---

### 🗄️ Server Storage & Configuration

- **MongoDB**  
  - Stores user account collection (username, password)
  - Stores anonymous login records (email address and time of login)

---

### 🛠️ Extensibility

- **Modular Command Handlers**  
  - Each user roles' (authenticated and unauthenticated) FTP commands are implemented in its own class for easy addition or modification  

- **Synchronization**
  - Allows multiple clients to log in concurrently with the same user account
  - All incoming commands are placed into a per-account queue and executed one at a time in submission order
  - Ensuring a consistent view of the file system and preventing race conditions

--- 

## 📥 Installation & Usage

### Prerequisites 🔧

Before you begin, make sure you have the following software installed and configured:

* **Java Runtime Environment (JRE)** (bundled in this repository under `demo/JRE/`)
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
│   ├── Server.java                  # FTP server implementations: Starts the FTP server, listens for new client connections
│   ├── ServerHandler.java           # Handles an individual client session: authentication, command parsing, and response
│   └── UserFTPFunctions.java        # FTP command implementations for authenticated users
├── test/                  
├── target/                 
├── .editorconfig           
├── .gitignore              
├── FTPServer.exe           # Windows executable file for server
├── pom.xml                 # Maven project file
├── README.md               # Project documentation
├── server.jar             
└── server.xml            
```

---

### ▶️ Running the Executable Binaries

1. **Make sure** the executable files are in `demo/` folder.
2. **Verify** the `JRE/` subfolder contains both `bin/` and `lib/` directories.
3. **Launch**:

   * Double-click `FTPServer.exe` to start the server.
4. **Open** Windows' `ftp.exe` in your machine. A CLI will pop up in your screen.
5. **Connect** to the server by entering to the CLI `open {server ip address} {port number}`
---

### 🧑‍💻 Running from Command Line

1. Open your preferred IDE (e.g., VS Code, IntelliJ IDEA).
2. **Import** the Maven project (`pom.xml`).
3. Navigate to `src/main/java/com/example/`.
4. Run `Server.java` first to start the server.
5. Login Credentials:
   - Username: `tam`
   - Password: `12345678`
6. **Open** Windows' `ftp.exe` in your machine. A CLI will pop up in your screen.
7. **Connect** to the server by entering to the CLI `open {server ip address} {port number}` 
   - example: `open 192.168.1.13 1234`

> **Note:** You can also build and run the JARs via Maven:
>
> ```bash
> mvn clean package
> java -jar target/server.jar --config server.xml
> ```
---
