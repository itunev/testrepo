import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

class TCPServerTest extends Assert {
    static final int             CLIENTS_COUNT = 5;
    static final ExecutorService threadPool    = Executors.newFixedThreadPool(CLIENTS_COUNT + 1);
    static TCPServer             serv          = new TCPServer(1080);
    static LinkedList<TCPClient> clients       = new LinkedList<TCPClient>();
    static int                   successClients;
    static int                   echoCount;
    static int                   filesCount;

    @Before
    public void setUpServerAndClients() {
        threadPool.submit(serv);
        Thread.sleep(10);

        assert serv.testActiveHandlersNumber() == 0;

        for (int i = 0; i < 5; i++) {
            clients.push(new TCPClient(1080, 1881 + i));
            clients[i].fileName = "file" + (1881 + i);
            threadPool.submit(clients[i]);
        }

        Thread.sleep(1000);

        successClients = 0;
        for (TCPClient client : clients)
            if (client.success)
                successClients ++;

        assert successClients == CLIENTS_COUNT;
    }

    @Test
    public void testForAsyncRequest() {
        setTestRequest(clients, "echo");
        Thread.sleep(1000);
        echoCount = getTestAnswerCount(clients, "echo");

        assert successClients >= serv.testActiveHandlersNumber();
        assert serv.testActiveHandlersNumber() >= echoCount;

        if (isFileExists()) {
            setTestRequest(clients, "file");
            Thread.sleep(10000);
            filesCount = testForAsyncFileTranser(clients);

            assert filesCount == CLIENTS_COUNT;
        }
    }

    @After
    public void closeServerAndClients() {
        System.out.print("\ncountSuccess: " + successClients + "\ncountEcho: " + echoCount);
        if (echoCount < CLIENTS_COUNT)
            System.out.print("\nTest result is not reliable");
        else
            System.out.print("\nTest success");

        assert echoCount == CLIENTS_COUNT;

        setTestRequest(clients, " END");
        Thread.sleep(1000);
        int countEND = getTestAnswerCount(clients, " END");

        Thread.sleep(1000);

        assert serv.testActiveHandlersNumber() == 0;
        assert echoCount == countEND;

        serv.stop();
        threadPool.awaitTermination(1000, TimeUnit.MILLISECONDS);
    }

    static int getTestAnswerCount(LinkedList<TCPClient> clients, String answer) {
        int count = 0;
        for (TCPClient client : clients)
            if (client.testAnswer == answer)
                count ++;

        return count;
    }

    static void setTestRequest(LinkedList<TCPClient> clients, String request) {
        for (TCPClient client : clients)
            client.testRequest = request;
    }

    static boolean  isFileExists( ) {
        File fh = new File(Handler.fileName);
        return (fh.exists() && !fh.isDirectory());
    }

    static int testForAsyncFileTranser(LinkedList<TCPClient> clients) {
        File fh       = new File(Handler.fileName);
        int countFile = 0;
        for (TCPClient client : clients) {
            File f = new File(client.fileName);
            if (f.exists() && !f.isDirectory()) {
                assert f.size() == fh.size();
                countFile++;
            }
        }

        return countFile;
    }
}
