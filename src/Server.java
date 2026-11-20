import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;

public class Server {
    public static void main(String[] args) throws Exception{
        int port = 5000;

        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.bind(new InetSocketAddress(port));
        System.out.println("Server starting on port " + port);

        while(true){

        }
    }
}
