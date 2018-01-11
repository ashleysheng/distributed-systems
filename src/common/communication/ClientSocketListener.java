package common.communication;

import common.messages.KVMessage.StatusType;
import common.messages.KVMessageObj;

import org.apache.log4j.Logger;


/**
 * Class for handling Communication Module's responses and connection statuses
 */
public class ClientSocketListener {

    public enum SocketStatus{DISCONNECTED, CONNECTION_LOST}
    private static Logger logger = Logger.getRootLogger();
    public boolean received;
    public final Object msgLock = new Object();
    public KVMessageObj latestMsg;

    ClientSocketListener(){
        latestMsg = null;
        received = false;
    }

    /**
     * Prints out the correct server response on the UI and wake up KVStore who was waiting for the response
     * @param msg the message received from the communication module
     */
    void handleNewMessage(KVMessageObj msg){

        StatusType status = msg.getStatus();
        StringBuilder response = new StringBuilder();

        response.append(status.toString()).append(": ");

        if(
                status != StatusType.GET_ERROR &&
                status != StatusType.DELETE_ERROR &&
                status != StatusType.PUT_ERROR
                )
        {
            if (status == StatusType.GET_SUCCESS) {
                response.append("successfully retrieved KV Pair ");
            }
            else if (status == StatusType.PUT_SUCCESS) {
                response.append("successfully inserted KV Pair ");
            }
            else if(status == StatusType.PUT_UPDATE) {
                response.append("successfully updated KV Pair to ");
            }
            else if(status == StatusType.DELETE_SUCCESS) {
                response.append("successfully deleted KV Pair ");
            }

            response.append(msg.getKey()).append(" ").append(msg.getValue());
            logger.info(response);
        }
        else {
            response.append("Unable to process previous request");
            logger.error(response);
        }

        synchronized (this.msgLock){
            this.latestMsg = msg;
            this.received = true;
            this.msgLock.notify();
        }

    }

    /**
     * Handles the connection status given
     * @param status
     *          The new connection status
     */
    void handleStatus(SocketStatus status){
        if (status == SocketStatus.DISCONNECTED) {
            logger.info("Connection terminated");

        } else if (status == SocketStatus.CONNECTION_LOST) {
            logger.warn("Connection lost.");
        }
    }
}