/**
 * Created by igor on 1/5/16.
 * based on http://habrahabr.ru/post/70690/
 *          http://habrahabr.ru/sandbox/22757/
 *          http://www.javaportal.ru/java/articles/java_http_web/article02.html
 *
 *          http://javatutor.net/books/tiej/socket
 *          http://howtodoinjava.com/2015/03/24/java-thread-pool-executor-example/
 *
 *          ...
 */

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

    TCPServer() {
        PORT = 1080;
        init();
    }

    TCPServer(int port) {
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

//public class TCPServer extends Thread {
//    /**
//     * Порт
//     */
//    static int port = 1080;
//    /**
//     * Хост
//     */
//    static InetAddress host;
//
//    Socket s;
//    int num;
//
//
//    /**
//     * Дополнительная информация цепляемая к каждому ключу {@link SelectionKey}
//     *
//     * @author dgreen
//     * @date 19.09.2009
//     *
//     */
//    static class Attachment {
//        /**
//         * Буфер для чтения, в момент проксирования становится буфером для
//         * записи для ключа хранимого в peer
//         *
//         * ВАЖНО: При парсинге Socks4 заголовком мы предполагаем что размер
//         * буфера, больше чем размер нормального заголовка, у браузера Mozilla
//         * Firefox, размер заголовка равен 12 байт 1 версия + 1 команда + 2 порт +
//         * 4 ip + 3 id (MOZ) + 1 \0
//         */
//
//        ByteBuffer in;
//        /**
//         * Буфер для записи, в момент проксирования равен буферу для чтения для
//         * ключа хранимого в peer
//         */
//        ByteBuffer out;
//        /**
//         * Куда проксируем
//         */
//        SelectionKey peer;
//
//    }
//
//    public static void main(String[] args) {
//        port = 1080;
//
////        TCPServer server = new TCPServer();
////        server.host = "127.0.0.1";
////        server.port = 1080;
////        server.run();
//
//
//        try {
//            host = InetAddress.getByName("localhost");
//            // Создаём Selector
//            Selector selector = SelectorProvider.provider().openSelector();
//            // Открываем серверный канал
//            ServerSocketChannel ssc = ServerSocketChannel.open();
//            // Убираем блокировку
//            ssc.configureBlocking(false);
//            // Вешаемся на порт
//            ssc.socket().bind(new InetSocketAddress(host, port));
//            // Регистрация в селекторе
//            ssc.register(selector, ssc.validOps());
//            System.out.println("Listening on port " + port);
//
//            int clientNum = 0;
//            // Основной цикл работу неблокирующего сервер
//            // Этот цикл будет одинаковым для практически любого неблокирующего
//            // сервера
//            while (selector.select() > -1)
//            {
//                // Получаем ключи на которых произошли события в момент
//                // последней выборки
//                Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
//
//                while (iterator.hasNext())
//                {
//                    SelectionKey key = iterator.next();
//                    iterator.remove();
//                    if (key.isValid()) {
//                        // Обработка всех возможнных событий ключа
//                        try {
//                            if (key.isAcceptable()) {
//                                // Принимаем соединение
//                                new TCPServer(clientNum, ssc.socket().accept());
//                                clientNum++;
////                                accept(key);
//                            }
////                            else if (key.isConnectable()) {
////                                // Устанавливаем соединение
////                                connect(key);
////                            } else if (key.isReadable()) {
////                                // Читаем данные
////                                read(key);
////                            } else if (key.isWritable()) {
////                                // Пишем данные
////                                write(key);
////                            }
//                        } catch (Exception e) {
//                            e.printStackTrace();
////                            close(key);
//                        }
//                    }
//                }
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//            throw new IllegalStateException(e);
//        }
//
//    }
//
//    public TCPServer(int num, Socket s)
//    {
//        // копируем данные
//        this.num = num;
//        this.s = s;
//
//        // и запускаем новый вычислительный поток (см. ф-ю run())
//        setDaemon(true);
//        setPriority(NORM_PRIORITY);
//        start();
//    }
//
//    /**
//     * Сердце неблокирующего сервера, практически не меняется от приложения к
//     * приложению, разве что при использование неблокирующего сервера в
//     * многопоточном приложение, и работе с ключами из других потоков, надо
//     * будет добавить некий KeyChangeRequest, но нам в этом приложение это без
//     * надобности
//     */
//    @Override
//    public void run() {
//        try
//        {
//            // из сокета клиента берём поток входящих данных
//            InputStream is = s.getInputStream();
//            // и оттуда же - поток данных от сервера к клиенту
//            OutputStream os = s.getOutputStream();
//
//            // буффер данных в 64 килобайта
//            byte buf[] = new byte[64*1024];
//            // читаем 64кб от клиента, результат - кол-во реально принятых данных
//            int r = is.read(buf);
//
//            // создаём строку, содержащую полученную от клиента информацию
//            String data = new String(buf, 0, r);
//
//            // добавляем данные об адресе сокета:
//            data = ""+num+": "+"\n"+data;
//
//            // выводим данные:
//            os.write(data.getBytes());
//
//            // завершаем соединение
//            s.close();
//        }
//        catch(Exception e)
//        {System.out.println("init error: "+e);} // вывод исключений
//    }
//
//    /**
//     * Функция принимает соединение, регистрирует ключ с интересуемым действием
//     * чтение данных (OP_READ)
//     *
//     * @param key
//     *            ключ на котором произошло событие
//     * @throws IOException
//     * @throws ClosedChannelException
//     */
//    private void accept(SelectionKey key) throws IOException, ClosedChannelException {
//        // Приняли
//        SocketChannel newChannel = ((ServerSocketChannel) key.channel()).accept();
//        // Неблокирующий
//        newChannel.configureBlocking(false);
//        // Регистрируем в селекторе
//        newChannel.register(key.selector(), SelectionKey.OP_READ);
//    }
//
//    /**
//     * No Comments
//     *
//     * @param key
//     * @throws IOException
//     */
//    private void close(SelectionKey key) throws IOException {
//        key.cancel();
//        key.channel().close();
//        SelectionKey peerKey = ((Attachment) key.attachment()).peer;
//        if (peerKey != null) {
//            ((Attachment)peerKey.attachment()).peer=null;
//            if((peerKey.interestOps()&SelectionKey.OP_WRITE)== 0 ) {
//                ((Attachment)peerKey.attachment()).out.flip();
//            }
//            peerKey.interestOps(SelectionKey.OP_WRITE);
//        }
//    }
//}
