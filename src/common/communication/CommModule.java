package common.communication;

import common.messages.KVMessageObj;
import org.apache.log4j.Logger;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class CommModule extends Thread {

    Logger logger = Logger.getRootLogger();
    boolean running;
    Socket clientSocket;
    public OutputStream output;
    public InputStream input;
    public ClientSocketListener listener;

    static final int BUFFER_SIZE = 1024;
    static final int DROP_SIZE = 1024 * BUFFER_SIZE;

    /**
     * Calls the serializing function for KVMessage and send the corresponding byte array
     * @param msg
     *      The KVMessage to send
     * @throws Exception
     *      If the message couldn't be sent out
     */
    public void send(KVMessageObj msg) throws Exception {
        byte[] msgBytes = msg.toByteArray();
        output.write(msgBytes, 0, msgBytes.length);
        output.flush();
    }

    /**
     * Receive a byte array, deserialize it to KVMessage
     * @return a KVMessage for the client/server communication module
     * @throws Exception Could not properly read all the bytes from the socket
     */
    public KVMessageObj receive() throws Exception {

        int index = 0;
        byte[] msgBytes = null;
        byte[] tmp;
        byte[] bufferBytes = new byte[BUFFER_SIZE];

      /* read first char from stream */
        byte read = (byte) input.read();
        boolean reading = true;

        while(read != 0x03 && reading) {/* end text byte */
          /* if buffer filled, copy to msg array */
            if(index == BUFFER_SIZE) {
                if(msgBytes == null){
                    tmp = new byte[BUFFER_SIZE];
                    System.arraycopy(bufferBytes, 0, tmp, 0, BUFFER_SIZE);
                } else {
                    tmp = new byte[msgBytes.length + BUFFER_SIZE];
                    System.arraycopy(msgBytes, 0, tmp, 0, msgBytes.length);
                    System.arraycopy(bufferBytes, 0, tmp, msgBytes.length,BUFFER_SIZE);
                }
                msgBytes = tmp;
                bufferBytes = new byte[BUFFER_SIZE];
                index = 0;
            }

          /* stop reading is DROP_SIZE is reached */
            if(msgBytes != null && msgBytes.length + index >= DROP_SIZE) {
                reading = false;
            }

            bufferBytes[index] = read;
            index++;

          /* read next char from stream */
            read = (byte) input.read();
        }

        if(msgBytes == null){
            tmp = new byte[index];
            System.arraycopy(bufferBytes, 0, tmp, 0, index);
        } else {
            tmp = new byte[msgBytes.length + index];
            System.arraycopy(msgBytes, 0, tmp, 0, msgBytes.length);
            System.arraycopy(bufferBytes, 0, tmp, msgBytes.length, index);
        }

        msgBytes = tmp;

        /* build final String */
        KVMessageObj msg = new KVMessageObj();
        msg.toObject(msgBytes);
        return msg;
    }

}