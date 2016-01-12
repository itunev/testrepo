import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        TCPServer serv = new TCPServer();
        serv.run();
    }
}
