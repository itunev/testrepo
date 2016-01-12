// http://serverfault.com/questions/5729/file-transfer-using-telnet
// You should be able to use Kermit to transfer files over telnet, you can use C-Kermit as the telnet client to do so. Its only availible for Unix/Linux based systems though.
// http://www.cyberforum.ru/java-networks/thread503897.html

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;

/**
 * 2 way for collision problem solving
 * 1 - synchronyse the requests (consequently request-answer, request-answer, ...)
 * 2 - asynchronyous requests by different sockets on client
 * here not realized no one way because it is not needed in task
 *
 * 2 ways for file transfer
 * 1 - via buffer (http://stackoverflow.com/questions/10677130/java-transferring-big-files-over-channels-nio)
 * 2 - via transferTo method
 * here realized 2nd way
 *
 * 2 way of clients handling
 * 1 - in separated theads for every client
 * 2 - in common separated thread for all clients (by non blocking sockets)
 * here realized 1nd way
 */
public class Handler implements Runnable {
    private SocketChannel       sChannel;
    private Selector            sel;
    private FileTransfer        fHandler;
    private TCPServer           serv;
    private boolean             done;
    private ByteBuffer          buffer;
    private String              response;

    private class FileTransfer {
        private FileInputStream fromFileStream;
        private FileChannel     fChannel;
        private final int       BUFFER_SIZE = 1024;
        private boolean         fileSending;

        public FileTransfer() {
            fileSending = false;
        }

        public boolean processing( ) {
            return fileSending;
        }

        public void processing(boolean seeding) {
            fileSending = seeding;
        }

        /**
         * Opens channel for file sending and send size of file by text format (send format - 16 bytes like 0000000012345678)
         * @return true if success
         * @throws IOException
         * @throws InterruptedException
         */
        public boolean PrepareFileChannelForSending( ) throws IOException, InterruptedException {
            long size = 0;
            if (isFileExists("file"))
                size = openFileChannel();

            // send 16 bytes with info about size of file
            String sizeString = "" + size;
            while (sizeString.length() < 16)
                sizeString = "0" + sizeString;

            sChannel.write(ByteBuffer.wrap(sizeString.getBytes()));
            System.out.print("\nfile size: " + size);
            System.out.println("\n.STARTING");
            return (size != 0);
        }

        /**
         *  file existing checking
         */
        public boolean isFileExists( String fileName ) {
            File f = new File(fileName);
            return (f.exists() && !f.isDirectory());
        }

        /**
         * Opens channel for file sending
         * @return size of file in bytes
         * @throws IOException
         * @throws InterruptedException
         */
        public long openFileChannel( ) throws IOException, InterruptedException {
        /* preparing of variables to file sending */
            fromFileStream = new FileInputStream("file");//http://www.programcreek.com/java-api-examples/index.php?source_dir=my-ministry-assistant-master/src/com/myMinistry/util/FileUtils.java
            fChannel = fromFileStream.getChannel();
            fChannel.position(0);

            return fChannel.size();
        }

        /**
         * Send next portion of file
         * @return true if sending is not finished
         * @throws IOException
         * @throws InterruptedException
         */
        public boolean sendFilePortion( ) throws IOException, InterruptedException {
            long bytesSent = fChannel.transferTo(fChannel.position(), BUFFER_SIZE, sChannel);
            fChannel.position(fChannel.position() + bytesSent);
            System.out.println("sent: " + ((double)fChannel.position() / (double)fChannel.size() * 100.0) + "%");

            return (fChannel.size() != fChannel.position());
        }

        /**
         * closes file channel and stream
         * @throws IOException
         */
        public void closeFileChannel( ) throws IOException {
            fChannel.close();
            fromFileStream.close();
        }

    }

    public Handler(SocketChannel ch, TCPServer ssc) throws IOException {
        buffer      = ByteBuffer.allocate(16);
        response    = "";
        done        = false;
        serv        = ssc;
        sChannel    = ch;
        sel         = Selector.open();
        fHandler    = new FileTransfer();
    }

    public void run( ) {
        try {
            sChannel.register(sel, sChannel.validOps());
            while (!done && !serv.isStopped()) {
                sel.select();
                Iterator it = sel.selectedKeys().iterator();
                while (it.hasNext() && !serv.isStopped()) {
                    SelectionKey key = (SelectionKey) it.next();
                    it.remove();
                    handleEvent(key);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                closeChannel();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void handleEvent(SelectionKey key) throws IOException, InterruptedException {
        if (fHandler.processing()) {
            if (!key.isWritable())
                return;

            fHandler.processing(fHandler.sendFilePortion());
            if (!fHandler.processing())
                fHandler.closeFileChannel();
        }
        else if (key.isReadable()) {
            sChannel.read(buffer);

            response = TCPServer.CS.decode((ByteBuffer) buffer.flip()).toString();
            if (response.length() == 0)
                done = true;
        }
        else if (key.isWritable() && response.length() > 0) {
            System.out.print(" response : \"" + response + "\"\n");
            handleResponse(response);
            response    = "";
            buffer.clear();
        }
    }

    private void handleResponse(String response) throws IOException, InterruptedException {
        if (response.contains("file"))
            fHandler.processing(fHandler.PrepareFileChannelForSending());
        else if (response.contains("echo") || response.contains("END"))
            sChannel.write(ByteBuffer.wrap(response.getBytes()));
        else if (response.contains("who"))
            sChannel.write(ByteBuffer.wrap(GetSystemInfo().getBytes()));
    }

    private void closeChannel( ) throws IOException {
        String addr = sChannel.socket().toString();
        sChannel.close();
        System.out.println(" Channel closed." + addr);
    }

    /**
     * Prepare basic system info
     * @return string with basic system info
     */
    public static String GetSystemInfo( ) {
    /* System name */
        String info = ("\n" + System.getProperty("os.name"));;

    /* Total number of processors or cores available to the JVM */
        info = "\nAvailable processors (cores): " + Runtime.getRuntime().availableProcessors();

    /* Total amount of free memory available to the JVM */
        info += "\nFree memory (bytes): " + Runtime.getRuntime().freeMemory();

    /* This will return Long.MAX_VALUE if there is no preset limit */
        long maxMemory = Runtime.getRuntime().maxMemory();
    /* Maximum amount of memory the JVM will attempt to use */
        info += "\nMaximum memory (bytes): " + (maxMemory == Long.MAX_VALUE ? "no limit" : maxMemory);

    /* Total memory currently available to the JVM */
        info += "\nTotal memory available to JVM (bytes): " + Runtime.getRuntime().totalMemory();

        return info;
    }
}
