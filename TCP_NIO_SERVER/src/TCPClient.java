import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.Charset;
import java.util.Iterator;

public class TCPClient implements Runnable{
    private int               serverPort;
    private int               selfPort;
    private SocketChannel     sc;
    private Selector          sel;
    private InputStreamReader fileInputStream;
    private BufferedReader    bufferedReader;
    private String            encoding    = System.getProperty("file.encoding");
    private Charset           cs          = Charset.forName(encoding);
    private ByteBuffer        buf         = ByteBuffer.allocate(16);
    public  volatile String   testRequest = "";
    public  volatile String   testAnswer  = "";
    public  volatile boolean  success     = true;

    TCPClient( ) {
        serverPort = 1080;
        selfPort = 1880;
    }

    TCPClient(int serverPort) {
        this.serverPort = serverPort;
        selfPort = 1880;
    }

    TCPClient(int serverPort, int selfPort) {
        this.serverPort = serverPort;
        this.selfPort = selfPort;
    }

    private void openIOChannels( ) throws IOException {
        fileInputStream                   = new InputStreamReader(System.in);
        bufferedReader                    = new BufferedReader(fileInputStream);
        sc                                = SocketChannel.open();
        sel                               = Selector.open();
        sc.configureBlocking(false);
        sc.socket().bind(new InetSocketAddress(selfPort));
        sc.register(sel, SelectionKey.OP_READ | SelectionKey.OP_WRITE | SelectionKey.OP_CONNECT);
    }

    public void run() {
        try {
            openIOChannels();
            runSession();
        } catch (ClosedChannelException e) {
            success = false;
            e.printStackTrace();
        } catch (UnknownHostException e) {
            success = false;
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            try {
                closeIOChannels();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void runSession( ) throws IOException, InterruptedException {
        boolean done        = false;
        boolean fileReading = false;
        while (!done) {
            sel.select();
            Iterator it = sel.selectedKeys().iterator();
            while (it.hasNext()) {
                SelectionKey key = (SelectionKey) it.next();
                it.remove();
                sc = (SocketChannel) key.channel();
                if (key.isConnectable() && !sc.isConnected()) {
                    System.out.println("connection");
                    InetAddress addr = InetAddress.getByName(null);
                    boolean success = sc.connect(new InetSocketAddress(addr, serverPort));
                    if (!success)
                        sc.finishConnect();
                }
                if (key.isReadable()) {
                    if (fileReading) {
                        readFile();
                        fileReading = false;
                    }
                    else if (sc.read((ByteBuffer) buf.clear()) > 0)
                        done = readAnswer();
                }
                else if (key.isWritable()) {
                    if (bufferedReader.ready())
                        fileReading = sendRequest();
                    else if (testRequest.length() == 4)
                        fileReading = sendTestRequest();
                }
            }
        }
    }

    /**
     * reads new user request and send it
     * @return true if request for file transferring from server
     * @throws IOException
     */
    private boolean sendRequest( ) throws IOException {
        String sWhatever = bufferedReader.readLine();
        sc.write(ByteBuffer.wrap(sWhatever.getBytes()));

        return sWhatever.contains("file");
    }

    public boolean sendTestRequest () throws IOException {
        sc.write(ByteBuffer.wrap(testRequest.getBytes()));
        testRequest = "";

        return testRequest.contains("file");
    }

    /**
     * reads answer from server
     * @return true if client's session must be finished
     */
    private boolean readAnswer( ) {
        String answer = cs.decode((ByteBuffer) buf.flip()).toString();
        System.out.print(answer);
        testAnswer = answer;
        return answer.contains("END");
    }

    private void readFile( ) throws IOException, InterruptedException {
        buf.clear();
        sc.read((ByteBuffer) buf.clear());
        String response = cs.decode((ByteBuffer) buf.flip()).toString();
        System.out.print(response + "\n");
        Integer size = 0;
        if (response.length() == 0)
            return;

        size = Integer.parseInt(response);
        if (size <= 0)
            return;

        getFile(size);
    }

    private void closeIOChannels( ) throws IOException {
        System.out.println("client closing");
        sc.close();
        sel.close();
        bufferedReader.close();
        fileInputStream.close();
    }

    private void getFile(Integer size) throws IOException, InterruptedException {
        long bytesCount = 0;
        long bytesGet;
        int BUFFER_SIZE = 1024;
        FileOutputStream toFileStream = new FileOutputStream("file");//http://www.programcreek.com/java-api-examples/index.php?source_dir=my-ministry-assistant-master/src/com/myMinistry/util/FileUtils.java
        FileChannel fChannel = toFileStream.getChannel();
        fChannel.position(0);

        do {
            bytesGet = fChannel.transferFrom(sc, fChannel.position(), BUFFER_SIZE);
            bytesCount += bytesGet;
            System.out.println("." + bytesCount + " bytes get of: " + size);
            fChannel.position(bytesCount);
//            Thread.sleep(1);
        }
        while (bytesCount < size);
        fChannel.close();
        toFileStream.close();
        System.out.println(".file received " + bytesCount + " from " + size);
    }
}
