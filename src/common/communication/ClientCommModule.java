package common.communication;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

import common.messages.KVMessageObj;
import common.communication.ClientSocketListener.SocketStatus;


/**
 * Communication module used by the client.
 * Is a thread by itself, and just polls the socket.
 */
public class ClientCommModule extends CommModule {

    /**
     * Instantiates a client communication module
     * @param address
     *          Address of the server
     * @param port
     *          Port of the server
     * @throws UnknownHostException
     *          If the connection could not be established with the host
     */
    public ClientCommModule(String address, int port) throws UnknownHostException {
      try {
          clientSocket = new Socket(address, port);
          output = clientSocket.getOutputStream();
          input = clientSocket.getInputStream();
          listener = new ClientSocketListener();
          setRunning(true);
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

    /**
     * Keeps on trying to receive from the server socket.
     * Asks the listener to ClientSocketListener to handle the received message.
     * Disconnects when the socket is invalid
     */
    public void run() {
      while(isRunning()) {
        try {
            KVMessageObj msg = receive();
            listener.handleNewMessage(msg);
        } catch (Exception ioe) {
            if(isRunning()) {
                logger.error("Connection lost!");
                try {
                    tearDownConnection();
                    listener.handleStatus(SocketStatus.CONNECTION_LOST);
                } catch (IOException e) {
                    logger.error("Unable to close connection!");
                }
            }
        }
      }
    }

    /**
     * Try to tear down the connection by closing the socket
     * @throws IOException
     *      If socket is invalid
     */
    public void tearDownConnection() throws IOException {
    setRunning(false);
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
            listener.handleStatus(SocketStatus.DISCONNECTED);
        } catch (IOException ioe) {
            logger.error("Unable to close connection!");
        }
    }

    /**
     * Getters and setters for the running variable
     * @param new_running the new boolean status of running
     */
    private void setRunning(boolean new_running) {running = new_running;}
    private boolean isRunning(){return running;}
}