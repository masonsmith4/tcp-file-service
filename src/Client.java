public class Client {
    public static void main(String[] args) throws Exception{
        if (args.length != 2) {
            System.err.println("Usage: java Client <hostname> <port>");
            System.exit(1);
        }
    }
}
