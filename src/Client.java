import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Scanner;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;
public class Client {
    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.err.println("Usage: java Client <hostname> <port>");
            System.exit(1);
        }

        String ip = args[0];
        int port = Integer.parseInt(args[1]);

        Scanner scn = new Scanner(System.in);

        while (true) {
            System.out.println("Commands:");
            System.out.println("List (L)");
            System.out.println("Delete (D)");
            System.out.println("Rename (R)");
            System.out.println("Save/Download (S)");
            System.out.println("Upload (U)");
            System.out.println("Quit (Q)");
            System.out.print("Enter command: ");
            String command = scn.nextLine().trim().toUpperCase();
            if (command.equals("L")) {
                list(ip, port);

            } else if (command.equals("D")) {
                System.out.print("File name:");
                String fileName = scn.nextLine().trim();
                delete(ip, port, fileName);
            } else if (command.equals("R")) {
                System.out.print("Current file name: ");
                String oldName = scn.nextLine().trim();
                System.out.print("New file name: ");
                String newName = scn.nextLine().trim();
                rename(ip, port, oldName, newName);
            } else if (command.equals("S")) {
                System.out.print("File name to download: ");
                String fileName = scn.nextLine().trim();
                download(ip, port, fileName);
            } else if (command.equals("U")) {
                System.out.print("File name to upload: ");
                String fileName = scn.nextLine().trim();
                upload(ip, port, fileName);
            } else if (command.equals("Q")) {
                System.out.println("Quitting...");
                System.exit(0);
                break;
            } else {
                System.out.println("Unknown command: " + command);
            }
        }
        scn.close();
    }

    static void list(String ip, int port) throws Exception {
        SocketChannel socket = SocketChannel.open(
                new InetSocketAddress(ip, port)
        );

        ByteBuffer request = ByteBuffer.allocate(1);
        request.put((byte) 'L');
        request.flip();

        socket.write(request);
        socket.shutdownOutput();

        ByteBuffer response = ByteBuffer.allocate(1024);

        System.out.println("Files on server:");

        while (socket.read(response) != -1) {
            response.flip();

            while (response.hasRemaining()) {
                System.out.print((char) response.get());
            }

            response.clear();
        }

        System.out.println();
        socket.close();
    }

    static void rename(String ip, int port, String oldName, String newName) throws Exception {
        SocketChannel socket = SocketChannel.open(
                new InetSocketAddress(ip, port)
        );

        ByteBuffer buffer = ByteBuffer.allocate(1024);
        buffer.put((byte) 'R');
        buffer.put(oldName.getBytes());
        buffer.put((byte) 0);
        buffer.put(newName.getBytes());
        buffer.flip();

        socket.write(buffer);
        socket.shutdownOutput();

        ByteBuffer response = ByteBuffer.allocate(1);
        socket.read(response);
        response.flip();

        byte status = response.get();

        if (status == 'S') {
            System.out.println("Rename successful");
        } else {
            System.out.println("Rename failed");
        }

        socket.close();
    }

    static void delete(String ip, int port, String fileName) throws Exception {
        SocketChannel socket = SocketChannel.open(new InetSocketAddress(ip, port));
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        buffer.put((byte) 'D');
        buffer.put(fileName.getBytes());
        buffer.flip();
        socket.write(buffer);
        socket.shutdownOutput();

        ByteBuffer response = ByteBuffer.allocate(1);
        socket.read(response);
        response.flip();
        byte status = response.get();

        if (status == 'S') {
            System.out.println("Delete successful");
        } else {
            System.out.println("Delete failed");
        }

        socket.close();
    }

    static void download(String ip, int port, String fileName) throws Exception {
        SocketChannel socket = SocketChannel.open(
                new InetSocketAddress(ip, port)
        );

        ByteBuffer request = ByteBuffer.allocate(1024);
        request.put((byte) 'S');
        request.put(fileName.getBytes());
        request.flip();

        socket.write(request);
        socket.shutdownOutput();

        ByteBuffer response = ByteBuffer.allocate(1);
        socket.read(response);
        response.flip();

        byte status = response.get();

        if (status == 'S') {
            FileOutputStream output = new FileOutputStream("ClientFiles/" + fileName);

            ByteBuffer fileData = ByteBuffer.allocate(1024);

            while (socket.read(fileData) != -1) {
                fileData.flip();

                byte[] data = new byte[fileData.remaining()];
                fileData.get(data);
                output.write(data);

                fileData.clear();
            }

            output.close();
            System.out.println("Download successful");
        } else {
            System.out.println("Download failed. File not found.");
        }

        socket.close();
    }

    static void upload(String ip, int port, String fileName) throws Exception {
        File file = new File("ClientFiles", fileName);
        System.out.println(file.getAbsolutePath());

        if (!file.exists() || !file.isFile()) {
            System.out.println("File not found.");
            return;
        }

        SocketChannel socket = SocketChannel.open(
                new InetSocketAddress(ip, port)
        );

        byte[] nameBytes = fileName.getBytes();

        ByteBuffer command = ByteBuffer.allocate(1);
        command.put((byte) 'U');
        command.flip();
        socket.write(command);
        ByteBuffer ready = ByteBuffer.allocate(1);

        while (ready.hasRemaining()) {
            socket.read(ready);
        }

        ByteBuffer nameLength = ByteBuffer.allocate(4);
        nameLength.putInt(nameBytes.length);
        nameLength.flip();

        while (nameLength.hasRemaining()) {
            socket.write(nameLength);
        }

        ByteBuffer nameBuffer = ByteBuffer.wrap(nameBytes);

        while (nameBuffer.hasRemaining()) {
            socket.write(nameBuffer);
        }

        FileInputStream input = new FileInputStream(file);
        byte[] data = new byte[1024];
        int bytesRead;

        while ((bytesRead = input.read(data)) != -1) {
            ByteBuffer fileData = ByteBuffer.wrap(data, 0, bytesRead);

            while (fileData.hasRemaining()) {
                socket.write(fileData);
            }
        }

        input.close();
        socket.shutdownOutput();

        ByteBuffer response = ByteBuffer.allocate(1);

        while (response.hasRemaining()) {
            if (socket.read(response) == -1) {
                break;
            }
        }

        response.flip();

        if (response.hasRemaining() && response.get() == 'S') {
            System.out.println("Upload successful");
        } else {
            System.out.println("Upload failed");
        }

        socket.close();
    }
}