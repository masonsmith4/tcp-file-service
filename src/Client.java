import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Scanner;

public class Client {
    public static void main(String[] args) throws Exception{
        if (args.length != 2) {
            System.err.println("Usage: java Client <hostname> <port>");
            System.exit(1);
        }

        String ip = args[0];
        int port = Integer.parseInt(args[1]);

        Scanner scn = new Scanner(System.in);

        while(true) {
            System.out.println("Commands: \nDelete\nQuit");
            System.out.print("Enter command: ");
            String command = scn.nextLine().trim().toUpperCase();

            if (command.equals("D")) {
                System.out.print("File name:");
                String fileName = scn.nextLine().trim();
                delete(ip, port, fileName);
            } else if (command.endsWith("Q")) {
                System.out.println("Quitting...");
                System.exit(0);
                break;
            } else {
                System.out.println("Unknown command: " + command);
            }
        }
    }

    static void delete(String ip, int port, String fileName) throws Exception{
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
}
