package common.communication;

import common.messages.KVAdminMsg;
import common.messages.KVMessageObj;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

public class ECSCommModule {

    Logger logger = Logger.getRootLogger();
    boolean running;
    Socket clientSocket;
    public OutputStream output;
    public InputStream input;
    public ClientSocketListener listener;

    static final int BUFFER_SIZE = 1024;
    static final int DROP_SIZE = 1024 * BUFFER_SIZE;

    public ECSCommModule(String address, int port) throws UnknownHostException {
        IOException cantConnectYet;

        try {
            Exception i;
            do {
                i=null;
                try{
                    clientSocket = new Socket(address, port);
                } catch (IOException ioe){
                    i = ioe;
                }
            } while(i!=null);

//            System.out.println(clientSocket==null);
            output = clientSocket.getOutputStream();
            input = clientSocket.getInputStream();

            logger.info("Connection established");
        } catch (IOException ioe) {
            if(ioe instanceof UnknownHostException){
                throw new UnknownHostException();
            }
            else {
                logger.error("Connection could not be established!");
            }
        }
    }

    public ECSCommModule(Socket s) {
        this.clientSocket = s;
        try {
            output = clientSocket.getOutputStream();
            input = clientSocket.getInputStream();
        } catch (IOException e) {
            logger.error("Connection could not be established!");
            e.printStackTrace();
        }

        logger.info("Connection established");
    }

    public KVAdminMsg receiveMessage() throws Exception{
        KVAdminMsg received = this.receive();
        return received;
    }

    public void sendMessage(KVAdminMsg msg) throws Exception{
        this.send(msg.toRegularMsg());
    }

    public void tearDownConnection() throws IOException {
        logger.debug("tearing down the connection ...");
        if (clientSocket != null) {
            clientSocket.close();
            clientSocket = null;
            logger.debug("connection closed!");
        }
    }

    /**
     * Calls tear down and handles the IOException thrown by Teardown
     */
    public synchronized void closeConnection() {
        logger.info("try to close connection ...");

        try {
            tearDownConnection();
        } catch (IOException ioe) {
            logger.error("Unable to close connection!");
        }
    }

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

    public KVAdminMsg receive() throws Exception {

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
        KVAdminMsg msg = new KVAdminMsg();
        msg.toObject(msgBytes);
        return msg;
    }
}
