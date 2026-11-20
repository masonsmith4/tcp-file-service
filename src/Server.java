import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

public class Server {
    public static void main(String[] args) throws Exception{
        int port = 5000;

        ServerSocketChannel listener = ServerSocketChannel.open();
        listener.bind(new InetSocketAddress(port));
        System.out.println("Server starting on port " + port);

        while(true){
            SocketChannel clientChannel = listener.accept();
            ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
            clientChannel.read(byteBuffer);
            byteBuffer.flip();

            byte comm = byteBuffer.get();

            if (comm == 'D') {
                String fileName = new String(byteBuffer.array(), byteBuffer.position(), byteBuffer.remaining());
                System.out.println("Delete: " + fileName);
            } else {
                System.out.println("Unknown " + (char) comm);
            }

            clientChannel.close();
        }
    }
}
