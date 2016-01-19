import org.junit.Test

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

class TCPServerTest extends GroovyTestCase {
    @Test
    void testSomething() {
        final ExecutorService threadPool = Executors.newFixedThreadPool(6);

        TCPServer serv = new TCPServer(1080);
        threadPool.submit(serv);
        Thread.sleep(10);

        assert serv.testActiveHandlersNumber() == 0;

        LinkedList<TCPClient> clients = new LinkedList<TCPClient>();
        for (int i = 0; i < 5; i++) {
            clients.push(new TCPClient(1080, 1881 + i));
            clients[i].fileName = "file" + (1881 + i);
            threadPool.submit(clients[i]);
        }

        Thread.sleep(1000);

        testForAsyncRequest(serv, clients);

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

    static void testForAsyncRequest(TCPServer serv, LinkedList<TCPClient> clients) {
        int countSuccess = 0;
        for (TCPClient client : clients)
            if (client.success)
                countSuccess ++;

        setTestRequest(clients, "echo");
        Thread.sleep(1000);
        int countEcho = getTestAnswerCount(clients, "echo");

        assert countSuccess >= serv.testActiveHandlersNumber();
        assert serv.testActiveHandlersNumber() >= countEcho;

        if (isFileExists()) {
            setTestRequest(clients, "file");
            Thread.sleep(10000);

            int countFile = testForAsyncFileTranser(clients);

            assert countFile == 5;
        }

        setTestRequest(clients, " END");
        Thread.sleep(1000);
        int countEND = getTestAnswerCount(clients, " END");

        Thread.sleep(1000);

        assert serv.testActiveHandlersNumber() == 0;
        assert countEcho == countEND;

        System.out.print("\ncountSuccess: " + countSuccess + "\ncountEcho: " + countEcho + "\ncountEND: " + countEND);
        if (countEcho < 5)
            System.out.print("\nTest result is not reliable");
        else
            System.out.print("\nTest success");

        assert countEcho == 5;
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
