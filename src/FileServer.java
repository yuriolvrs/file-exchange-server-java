import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.*;

public class FileServer {
    private static final int PORT = 12345;
    private static final String SERVER_IP = "127.0.0.1";
    private static Map<String, Socket> clients = new HashMap<>();
    private static final String FILE_DIR = "server_files";

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            // server start
            System.out.println("Server started on port " + PORT);
            new File(FILE_DIR).mkdir(); 

            while (true) {
                Socket clientSocket = serverSocket.accept();
                new ClientHandler(clientSocket).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class ClientHandler extends Thread {
        private Socket socket;
        private BufferedReader in;
        private PrintWriter out;
        private String handle;
        private boolean isJoined = false; 

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                String message;
                while ((message = in.readLine()) != null) {
                    System.out.println("Received: " + message);
                    processCommand(message);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (handle != null) {
                        clients.remove(handle);
                    }
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private void processCommand(String command) {
            if (!command.startsWith("/")) {
                out.println("Error: Commands should start with /");
                return;
            }

            String[] parts = command.split(" ");
            String cmd = parts[0];

            // Message upon unsuccessful disconnection to the server due to not currently being connected
            if (!isJoined && (cmd.equals("/leave")) && handle == null) {
                out.println("Error: Disconnection failed. Please connect to the server first.");
                return;
            }

            if (isJoined && (cmd.equals("/store") || cmd.equals("/dir") || cmd.equals("/get")) && handle == null) {
                out.println("Error: You must register before using this command.");
                return;
            }

            switch (cmd) {
                case "/join":
                    handleJoin(parts);
                    break;
                case "/leave":
                    handleLeave();
                    break;
                case "/register":
                    handleRegister(parts);
                    break;
                case "/store":
                    handleStore(parts);
                    break;
                case "/dir":
                    handleDir();
                    break;
                case "/get":
                    handleGet(parts);
                    break;
                case "/?":
                    handleHelp();
                    break;
                default:
                    out.println("Error: Command not found.");
            }
        }

        private void handleJoin(String[] parts) {
            if (parts.length == 3) {
                String ip = parts[1];
                int port;
                try {
                    port = Integer.parseInt(parts[2]);
                } catch (NumberFormatException e) {
                    out.println("Error: Invalid port number.");
                    return;
                }

                // Validate IP and port
                if (SERVER_IP.equals(ip) && PORT == port) {
                    isJoined = true; 
                    // Message upon successful connection to the server
                    out.println("Connection to the File Exchange Server is successful!");
                } else {
                    out.println("Error: Connection to the Server has failed! Please check IP Address and Port Number.");
                }
            } else {
                out.println("Error: Command parameters do not match or is not allowed.");
            }
        }

        private void handleLeave() {
            // Message upon successful disconnection to the server
            out.println("Connection closed. Thank you!");
            isJoined = false; 
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void handleRegister(String[] parts) {
            if (parts.length == 2) {
                handle = parts[1];
                if (clients.containsKey(handle)) {
                    // Message upon unsuccessful registration of a handle or alias due to registered "handle" or alias already exists
                    out.println("Error: Registration failed. Handle or alias already exists.");
                } else {
                    clients.put(handle, socket);
                    // Message upon successful registration of a handle or alias
                    out.println("Welcome " + handle + "!");
                }
            } else {
                out.println("Error: Command parameters do not match or is not allowed.");
            }
        }

        private void handleStore(String[] parts) {
            if (parts.length == 2) {
                String filename = parts[1];
                try {
                    File file = new File(FILE_DIR + "/" + filename);
                    FileOutputStream fos = new FileOutputStream(file);
                    byte[] buffer = new byte[4096];
                    int bytesRead;

                    InputStream inputStream = socket.getInputStream();
                    DataInputStream dataInputStream = new DataInputStream(inputStream);
                    long fileSize = dataInputStream.readLong(); 

                    long totalBytesRead = 0;

                    while (totalBytesRead < fileSize && (bytesRead = dataInputStream.read(buffer)) != -1) {
                        fos.write(buffer, 0, bytesRead);
                        totalBytesRead += bytesRead;
                    }

                    fos.close();

                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    String currentTime = sdf.format(new Date());

                    out.println(handle + "<" + currentTime + ">: Uploaded " + filename);
                } catch (IOException e) {
                    e.printStackTrace();
                    out.println("Error: Could not store the file.");
                }
            } else {
                out.println("Error: Command parameters do not match or is not allowed.");
            }
        }

        private void handleDir() {
            out.println("Server Directory");
            File folder = new File(FILE_DIR);
            String[] files = folder.list();
            if (files != null) {
                for (String file : files) {
                    out.println(file);
                }
            }
        }

        private void handleGet(String[] parts) {
            if (parts.length == 2) {
                String filename = parts[1];
                File file = new File(FILE_DIR + "/" + filename);
                if (file.exists()) {
                    try {
                        FileInputStream fis = new FileInputStream(file);
                        DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                        dos.writeLong(file.length()); 
        
                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = fis.read(buffer)) != -1) {
                            dos.write(buffer, 0, bytesRead);
                        }
                        fis.close();

                        // Copy file to user's folder
                        File userDir = new File(handle);

                        userDir.mkdir();

                        File userFile = new File(userDir, filename);
                        Files.copy(file.toPath(), userFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

                        out.println("File received from Server: " + filename);
                    } catch (IOException e) {
                        e.printStackTrace();
                        out.println("Error: Could not read the file.");
                    }
                } else {
                    out.println("Error: File not found in the server.");
                }
            } else {
                out.println("Error: Command parameters do not match or is not allowed.");
            }
        }          

        private void handleHelp() {
            out.println("/join <server_ip> <port>");
            out.println("/leave");
            out.println("/register <handle>");
            out.println("/store <filename>");
            out.println("/dir");
            out.println("/get <filename>");
            out.println("/?");
        }
    }
}