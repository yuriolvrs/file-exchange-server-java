import java.io.*;
import java.net.*;
import java.util.Scanner;

public class FileClient {
    private static final String SERVER_IP = "127.0.0.1";
    private static final int SERVER_PORT = 12345;
    private static boolean running = true;
    private static boolean isJoined = false;

    public static void main(String[] args) {
        try (Socket socket = new Socket(SERVER_IP, SERVER_PORT)) {
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            Scanner scanner = new Scanner(System.in);

            // client start
            System.out.println("Connected to the server. Type '/?' for help.");

            new Thread(() -> {
                String response;
                try {
                    while (running && (response = in.readLine()) != null) {
                        System.out.println(response);
                        // read server response
                        // Message upon successful disconnection to the server
                        if (response.equals("Connection closed. Thank you!")) {
                            running = false;
                        // Message upon successful connection to the server
                        } else if (response.equals("Connection to the File Exchange Server is successful!")) {
                            isJoined = true;
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();

            while (running) {
                System.out.print("> ");
                String command = scanner.nextLine();
                String[] parts = command.split(" ");
                String cmd = parts[0];

                if (!command.startsWith("/")) {
                    out.println("Error: Commands should start with /");
                    continue;
                }

                if (!isJoined && !cmd.equals("/join") && !cmd.equals("/?")) {
                    System.out.println("Error: You must join the server before using other commands.");
                    continue;
                }

                if (cmd.equals("/store")) {
                    if (parts.length == 2) {
                        String filename = parts[1];
                        File file = new File(filename);
                        if (file.exists()) {
                            new Thread(() -> {
                                try (FileInputStream fis = new FileInputStream(file)) {
                                    out.println(command);
                                    DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
                                    dataOutputStream.writeLong(file.length());
                                    byte[] buffer = new byte[4096];
                                    int bytesRead;
                                    while ((bytesRead = fis.read(buffer)) != -1) {
                                        dataOutputStream.write(buffer, 0, bytesRead);
                                    }
                                    dataOutputStream.flush();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                    System.out.println("Error: Could not store the file.");
                                }
                            }).start();
                        // Message upon unsuccessful sending of a file that does not exist in the client directory.
                        } else {
                            System.out.println("Error: File not found.");
                        }
                    // Message due to incorrect or invalid parameters
                    } else {
                        System.out.println("Error: Command parameters do not match or is not allowed.");
                    }
                } else if (cmd.equals("/get")) {
                    if (parts.length == 2) {
                        String filename = parts[1];
                        out.println(command);

                        new Thread(() -> {
                            try {
                                DataInputStream dis = new DataInputStream(socket.getInputStream());
                                long fileSize = dis.readLong();

                                File file = new File(filename);

                                try (FileOutputStream fos = new FileOutputStream(file)) {
                                    byte[] buffer = new byte[4096];
                                    int bytesRead;
                                    long totalBytesRead = 0;

                                    while (totalBytesRead < fileSize && (bytesRead = dis.read(buffer)) != -1) {
                                        fos.write(buffer, 0, bytesRead);
                                        totalBytesRead += bytesRead;
                                    }

                                    fos.flush();
                                    // System.out.println("File " + filename + " downloaded successfully.");
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                                System.out.println("Error: Could not receive the file.");
                            }
                        }).start();
                    } else {
                        System.out.println("Error: Command parameters do not match or are not allowed.");
                    }
                } else if (cmd.equals("/leave")) {
                    out.println(command);
                    running = false;
                } else {
                    out.println(command);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
