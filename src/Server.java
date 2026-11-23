import java.io.File;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.Scanner;

public class Server {
    public static void main(String[] args) throws Exception {
        int port = 5001;

        ExecutorService pool = Executors.newFixedThreadPool(10);

        ServerSocketChannel listener = ServerSocketChannel.open();
        listener.bind(new InetSocketAddress(port));
        System.out.println("Server starting on port " + port);
        System.out.println("Type Q to shut down the server.");

        new Thread(() -> {
            Scanner scn = new Scanner(System.in);
            while (!scn.nextLine().equalsIgnoreCase("Q")) {
            }
            try {
                listener.close();
            } catch (Exception e) {
            }
        }).start();

        try {
            while (true) {
                SocketChannel clientChannel = listener.accept();
                pool.submit(() -> handleClient(clientChannel));
            }
        } catch (Exception e) {
            pool.shutdown();
            System.out.println("Server stopped.");
        }
    }

    static void handleClient(SocketChannel clientChannel) {
        try {
            ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
            clientChannel.read(byteBuffer);
            byteBuffer.flip();

            byte comm = byteBuffer.get();

            if (comm == 'D') {
                String fileName = new String(byteBuffer.array(),
                        byteBuffer.position(),
                        byteBuffer.remaining());
                System.out.println("Delete: " + fileName);
                File file = new File("ServerFiles", fileName);
                boolean delete = file.delete();
                System.out.println("Delete file: " + delete);

                ByteBuffer response = ByteBuffer.allocate(1);
                if (delete) {
                    response.put((byte) 'S');
                } else {
                    response.put((byte) 'F');
                }
                response.flip();
                clientChannel.write(response);
            } else if (comm == 'L') {
                File folder = new File("ServerFiles");
                File[] files = folder.listFiles();

                StringBuilder fileList = new StringBuilder();

                if (files != null) {
                    for (File file : files) {
                        if (file.isFile()) {
                            fileList.append(file.getName()).append("\n");
                        }
                    }
                }
                if (fileList.length() == 0) {
                    fileList.append("No files found.");
                }
                ByteBuffer response = ByteBuffer.wrap(
                        fileList.toString().getBytes()
                );
                while (response.hasRemaining()) {
                    clientChannel.write(response);
                }
            } else if (comm == 'R') {
                byte[] names = new byte[byteBuffer.remaining()];
                byteBuffer.get(names);

                int separator = 0;
                while (separator < names.length && names[separator] != 0) {
                    separator++;
                }
                if (separator < names.length) {
                    String oldName = new String(names, 0, separator);
                    String newName = new String(
                            names,
                            separator + 1,
                            names.length - separator - 1
                    );
                    File oldFile = new File("ServerFiles", oldName);
                    File newFile = new File("ServerFiles", newName);
                    boolean rename = false;
                    if (oldFile.exists() && !newFile.exists()) {
                        rename = oldFile.renameTo(newFile);
                    }
                    ByteBuffer response = ByteBuffer.allocate(1);
                    if (rename) {
                        response.put((byte) 'S');
                    } else {
                        response.put((byte) 'F');
                    }

                    response.flip();
                    clientChannel.write(response);

                    System.out.println("Rename: " + oldName + " to " + newName);
                    System.out.println("Rename successful: " + rename);

                } else {
                    ByteBuffer response = ByteBuffer.allocate(1);
                    response.put((byte) 'F');
                    response.flip();
                    clientChannel.write(response);
                }

            } else if (comm == 'S') {
                String fileName = new String(
                        byteBuffer.array(),
                        byteBuffer.position(),
                        byteBuffer.remaining()
                );

                File file = new File("ServerFiles", fileName);

                ByteBuffer response = ByteBuffer.allocate(1);

                if (file.exists() && file.isFile()) {
                    response.put((byte) 'S');
                } else {
                    response.put((byte) 'F');
                }

                response.flip();
                clientChannel.write(response);

                if (file.exists() && file.isFile()) {
                    FileInputStream input = new FileInputStream(file);
                    byte[] data = new byte[1024];
                    int bytesRead;

                    while ((bytesRead = input.read(data)) != -1) {
                        ByteBuffer fileData = ByteBuffer.wrap(data, 0, bytesRead);

                        while (fileData.hasRemaining()) {
                            clientChannel.write(fileData);
                        }
                    }

                    input.close();
                    System.out.println("Download: " + fileName);
                }
            } else if (comm == 'U') {
                ByteBuffer ready = ByteBuffer.allocate(1);
                ready.put((byte) 'A');
                ready.flip();
                clientChannel.write(ready);

                ByteBuffer nameLength = ByteBuffer.allocate(4);
                while (nameLength.hasRemaining())   {
                    clientChannel.read(nameLength);
                }
                nameLength.flip();

                int length = nameLength.getInt();

                ByteBuffer nameBuffer = ByteBuffer.allocate(length);
                while (nameBuffer.hasRemaining()) {
                    clientChannel.read(nameBuffer);
                }
                nameBuffer.flip();

                byte[] nameBytes = new byte[length];
                nameBuffer.get(nameBytes);
                String fileName = new String(nameBytes);

                File file = new File("ServerFiles", fileName);
                FileOutputStream output = new FileOutputStream(file);

                ByteBuffer fileData = ByteBuffer.allocate(1024);

                while (clientChannel.read(fileData) != -1) {
                    fileData.flip();

                    byte[] data = new byte[fileData.remaining()];
                    fileData.get(data);
                    output.write(data);

                    fileData.clear();
                }

                output.close();

                ByteBuffer response = ByteBuffer.allocate(1);
                response.put((byte) 'S');
                response.flip();
                clientChannel.write(response);

                System.out.println("Upload: " + fileName);

            } else {
                System.out.println("Unknown " + (char) comm);
            }
            clientChannel.close();
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
}