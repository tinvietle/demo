# FTP Simulator (Team TAM)

## 📑 Table of Contents
- [Summary](#summary)  
- [👥 Team Members](#-team-members)  
- [⚙️ Technologies](#️-technologies)  
- [🚀 Features](#-features)  
- [📥 Installation & Usage](#-installation--usage)  

## 🗒️ Summary

This repository, developed by **Team TAM**, provides a simulation of an FTP server and client in Java, complete with authentication powered by MongoDB. Our implementation takes reference from the guide from Colorado State University: [https://www.cs.colostate.edu/helpdocs/ftp.html](reference link). 

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


