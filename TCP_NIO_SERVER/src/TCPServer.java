import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.*;
import java.nio.channels.spi.SelectorProvider;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;


public class TCPServer implements Runnable {
    public  static int           PORT;
    public  static Charset       CS;
    private ServerSocketChannel  ssc;
    private Selector             sel;
    private ThreadPoolExecutor   executor;
    private volatile boolean     stop;

    public int testActiveHandlersNumber() {
        return executor.getActiveCount();
    }

    public TCPServer() {
        PORT = 1080;
        init();
    }

    public TCPServer(int port) {
        PORT = port;
        init();
    }

    private void init( ) {
        stop            = false;
        String encoding = System.getProperty("file.encoding");
        CS              = Charset.forName(encoding);
    }

    @Override
    public void run( ) {
        try {
            open();
            listen();
        }
        catch (IOException e) {
            e.printStackTrace();
        } finally {
            stop = true;
            try {
                close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void listen( ) throws IOException {
        while (!stop && sel.select() > -1) {
            Iterator it = sel.selectedKeys().iterator();
            while (it.hasNext()) {
                SelectionKey skey = (SelectionKey) it.next();
                it.remove();
                if (skey.isValid() && skey.isAcceptable()) {
                    if (stop) {
                        System.out.println("Refused connection");
                        break;
                    }

                    runNewHandler();
                }
            }
        }
    }

    private void runNewHandler( ) throws IOException {
        SocketChannel channel = ssc.accept();
        channel.configureBlocking(false);
        System.out.println("Accepted connection from:" + channel.socket());

        executor.execute(new Handler(channel, this));
    }

    public void stop( ) {
        stop = true;
    }

    public boolean isStopped( ) {
        return stop;
    }

    private void open( ) throws IOException {
        ssc      = ServerSocketChannel.open();
        sel      = SelectorProvider.provider().openSelector();
        executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();
        ssc.configureBlocking(false);
        ssc.socket().bind(new InetSocketAddress(PORT));
        ssc.register(sel, ssc.validOps());
        System.out.println("Server on port: " + PORT);
    }

    private void close( ) throws IOException, InterruptedException {
        System.out.println("---server closing");
        executor.shutdown();
        sel.close();
        ssc.close();
        System.out.println("server closed");
    }
} // /:~
