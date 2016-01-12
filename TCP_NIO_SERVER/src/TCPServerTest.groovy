
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
            threadPool.submit(clients[i]);
        }

        Thread.sleep(1000);

        int countSuccess = 0;
        for (int i = 0; i < 5; i++)
            if (clients[i].success)
                countSuccess ++;

        for (int i = 0; i < 5; i++)
            clients[i].testRequest = "echo";

        Thread.sleep(1000);

        int countEcho = 0;
        for (int i = 0; i < 5; i++)
            if (clients[i].testAnswer == "echo")
                countEcho ++;

        assert countSuccess >= serv.testActiveHandlersNumber();

        assert serv.testActiveHandlersNumber() >= countEcho;

        for (int i = 0; i < 5; i++)
            clients[i].testRequest = " END";

        Thread.sleep(1000);

        int countEND = 0;
        for (int i = 0; i < 5; i++)
            if (clients[i].testAnswer == " END")
                countEND ++;

        Thread.sleep(1000);

        assert serv.testActiveHandlersNumber() == 0;

        assert countEcho == countEND;

        System.out.print("\ncountSuccess: " + countSuccess + "\ncountEcho: " + countEcho + "\ncountEND: " + countEND);
        if (countEcho < 5)
            System.out.print("\nTest failed");
        else
            System.out.print("\nTest success");

        serv.stop();
        threadPool.awaitTermination(1000, TimeUnit.MILLISECONDS);
    }
}
