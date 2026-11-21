import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class Client {
    public static void main(String[] args) throws Exception{
        if (args.length != 2) {
            System.err.println("Usage: java Client <hostname> <port>");
            System.exit(1);
        }

        String ip = args[0];
        int port = Integer.parseInt(args[1]);

        String filename = "HelloWorld.txt";

        SocketChannel socket = SocketChannel.open(new InetSocketAddress(ip, port));
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        buffer.put((byte) 'D');
        buffer.put(filename.getBytes());
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
