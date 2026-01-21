# File Exchange System (Java)

## Overview

A terminal-based **File Exchange System** built using a **Client–Server architecture** in Java.  
The system enables multiple users to connect to a central server to share and manage files through a custom command protocol. It utilizes **TCP sockets** for reliable data transfer and **multi-threading** to efficiently handle concurrent client requests.

## Features

- **Connection Management:** Connect to a specific server IP and port using `/join` and exit gracefully with `/leave`.
- **User Registration:** Users must register a unique handle via `/register` before performing file operations.
- **File Storage:** Upload local files to the server’s central repository using the `/store` command.
- **Remote Directory:** View a real-time list of all files currently hosted on the server with `/dir`.
- **File Retrieval:** Download specific files from the server repository using `/get`. Downloaded files are automatically organized into a local folder named after the user's handle.
- **Help System:** An integrated `/?` command provides a quick reference for all supported syntax and parameters.

## Technical Specifications

- **Language:** Java
- **Networking:** Java Sockets (TCP/IP)
- **Architecture:** Multi-threaded Client–Server
- **Platform:** Terminal / Command Line

## Project Context

This project was developed as part of **CSNETWK – Introduction to Computer Networks** at **De La Salle University**.  
It demonstrates the use of:

- **Socket Programming:** Establishing stable communication channels between distributed applications.
- **Multi-threading:** Using a `ClientHandler` thread for every connected user to prevent blocking the main server loop.
- **I/O Streaming:** Handling file data as byte streams to ensure integrity during transfer.
- **Command Parsing:** Implementing a custom string-based protocol to trigger server-side logic.

## Design Highlights

- **Threaded Communication:** The server uses `ClientHandler` (extending `Thread`) to manage multiple sessions simultaneously.
- **Centralized Repository:** All uploaded files are stored in a dedicated `server_files` directory on the server.
- **User-Specific Downloads:** Downloaded files are isolated into directories based on the user's registered handle to prevent file name collisions.
- **Robust Error Handling:** The system includes checks for unregistered handles, missing files, and incorrect command parameters.
