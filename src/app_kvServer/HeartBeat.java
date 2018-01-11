package app_kvServer;

import common.MD5Hasher;
import common.Metadata;
import common.communication.ServerCommModule;
import common.messages.KVAdminMsg;
import common.messages.KVMessage;
import common.messages.KVMessageObj;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.*;

public class HeartBeat extends Thread {
    private ServerCommModule comm;
    KVMessage retVal;
    private ExecutorService executor;
    private Callable<KVMessageObj> task;
    private Metadata meta;
    private String currentlyBrokenHash;
    private boolean isBroken;

    private static Logger logger = Logger.getRootLogger();

    public HeartBeat(Metadata m){
        this.meta = m;
        this.executor = Executors.newCachedThreadPool();

        this.task = new Callable<KVMessageObj>() {
            KVMessageObj ret;
            public KVMessageObj call() {
                try{
                     ret = comm.receive();
                } catch(Exception e){
                    e.printStackTrace();
                }
                return ret;
            }
        };
    }

    public void run(){
        logger.debug("inside heartbeat run function");
        while(true){
            String[] info=null;
            try{
                if(!this.meta.isEmpty){
                    //should look at metadata and put the host and port of the receiver
                    info = this.meta.getNextServer();
                    // logger.debug("Next server for heartbeat is: "+ info[0]+" on port: "+info[1]);
                    Socket s = new Socket(info[0],Integer.parseInt(info[1]));
                    this.comm = new ServerCommModule(s);
                    this.comm.initializeIO();

//                    logger.debug("Sending heartbeat message");
                    //send echo message to the next server
                    this.comm.send(new KVMessageObj(KVMessage.StatusType.ECHO));

                    KVMessageObj result;
                    Future<KVMessageObj> future = executor.submit(this.task);
                    try {
                        result= future.get(10, TimeUnit.SECONDS);
                        logger.debug("Received heartbeat ack");
                    } catch (TimeoutException ex) {
                        logger.warn("Heartbeat timed out. Sending ECS Warning message");
                        // TODO: send ECS Message here
                    }
                    // close the connection
                    this.comm.closeConnection();
                    this.comm = null;

                    Thread.sleep(10000);
                }
                else{
                    // logger.debug("heartbeat doesn't have metadata yet");
                    Thread.sleep(5000);
                }
            } catch (IOException e){
                MD5Hasher md5 = new MD5Hasher();
                String failedHash = md5.hashString(info[0]+Integer.parseInt(info[1]));

                if(currentlyBrokenHash==null || !failedHash.equals(currentlyBrokenHash)){
                    logger.warn("New heartbeat socket is broken. Sending ECS warning message");
                    try {
                        currentlyBrokenHash = failedHash;
                        logger.debug("Hash of failed server: "+currentlyBrokenHash);

                        logger.debug("Trying to connect with ECS");
                        Socket s = new Socket("127.0.0.1",8888);
                        this.comm = new ServerCommModule(s);
                        this.comm.initializeIO();
                        logger.debug("Successfully connected to ECS");

                        KVAdminMsg msg = new KVAdminMsg(
                                meta.getEcsPassword(),
                                currentlyBrokenHash,
                                KVAdminMsg.StatusType.FAIL
                        );
                        comm.send(msg);
                        comm.closeConnection();
                    } catch (IOException e1) {
                        logger.error("Error in connecting with ECS: ", e1);
                    } catch (Exception e2){
                        logger.error("Error in sending message to the ECS: ", e2);
                    }
                }

            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
