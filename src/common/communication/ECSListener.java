package common.communication;

import app_kvEcs.ECS;
import app_kvEcs.mKVS;
import app_kvServer.ClientConnection;
import common.messages.KVAdminMsg;
import common.messages.KVMessage;
import common.messages.KVMessageObj;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class ECSListener extends Thread{
    public ServerSocket ss;
    private boolean running;
    private static Logger logger = Logger.getRootLogger();
    private ECS ecs;

    public ECSListener(ECS ecs){
        this.ecs=ecs;
        this.running=false;
    }

    public void run(){
        try {
            this.ss = new ServerSocket(8888);
            logger.info("ECS listening on port: " + ss.getLocalPort());
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.setRunning(true);

        if (ss != null) {
            while (isRunning()) {
                try {
                    Socket kvServerSocket = ss.accept();
                    ECSCommModule comm = new ECSCommModule(kvServerSocket);
                    KVAdminMsg adminMsg = comm.receive();
                    logger.info("received warning message from a KVServer");
                    this.processWarning(adminMsg);
                }
                catch (IOException e) {
                    logger.info("Socket is closed");
//                    setRunning(false);
                } catch (Exception e){
                }
            }
//            try {
//                logger.info("CLOSING LISTENER");
//                ss.close();
//                ss=null;
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
        }
    }

    public boolean isRunning() {
        return running;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    public void processWarning(KVAdminMsg msg) throws Exception{
        logger.info("Failed server is: "+msg.getValue());
        String failed_hash=msg.getValue();

        ArrayList<mKVS> pool = null;
        if(ecs.state.equals(ECS.State.STANDBY)){
            pool = ecs.standbyPool;
        } else if(ecs.state.equals(ECS.State.ACTIVE)){
            pool = ecs.activePool;
        }
        if (pool==null) throw new Exception();

        // make sure that the server is actually dead
        // if it's really dead, then send the replica data to the responsible person
        mKVS failed_node=null;
        for (mKVS node:pool) {
            if (node.hashUpperBound.equals(failed_hash)){
                failed_node=node;
            }
        }
        if(failed_node==null) throw new Exception();

        ArrayList<String>replicas = ecs.metadata.nextTwoHashes(failed_hash);
        String recovery_recipient = ecs.metadata.getNextHash(failed_hash);

        mKVS recoveryNode=null;
        for(mKVS node:pool){
            if(node.hashUpperBound.equals(recovery_recipient)){
                recoveryNode=node;
            }
            if(node.hashUpperBound.equals(replicas.get(0))){
                recoveryNode=node;
            }
            else if(node.hashUpperBound.equals(replicas.get(1))){
                recoveryNode=node;
            }
        }
        if(recoveryNode==null) throw new Exception();

        recoveryNode.recoverData(failed_hash,recovery_recipient);

        // update the metadata of everyone similar to removeNode
        int removeIndex = ecs.metadata.order.indexOf(failed_hash);
        String A_hash = ecs.metadata.order.get((ecs.metadata.order.size()+removeIndex-2)%ecs.metadata.order.size());
        String B_hash = ecs.metadata.order.get((ecs.metadata.order.size()+removeIndex-1)%ecs.metadata.order.size());
        String D_hash = ecs.metadata.order.get((ecs.metadata.order.size()+removeIndex+1)%ecs.metadata.order.size());
        String E_hash = ecs.metadata.order.get((ecs.metadata.order.size()+removeIndex+2)%ecs.metadata.order.size());

        logger.debug("A_Hash is "+A_hash);
        logger.debug("B_Hash is "+B_hash);
        logger.debug("D_Hash is "+D_hash);
        logger.debug("E_Hash is "+E_hash);

        mKVS A=null,B=null, D =null, E=null;


        for(mKVS node: pool){
//            if(node.hashUpperBound.equals(successorHash)){successor = node;}
            if(node.hashUpperBound.equals(A_hash)){
                A = node;
            }
            if(node.hashUpperBound.equals(B_hash)){
                B = node;
            }
            if(node.hashUpperBound.equals(D_hash)){
                D = node;
            }
            if(node.hashUpperBound.equals(E_hash)){
                E = node;
            }
        }
        ecs.metadata.removeFromMeta(failed_node);
        A.lockWrite();
        B.lockWrite();
        D.lockWrite();
        E.lockWrite();

        D.update(ecs.metadata.stringify());
        E.update(ecs.metadata.stringify());
        A.update(ecs.metadata.stringify());
        B.update(ecs.metadata.stringify());


        pool.remove(failed_node);
        for(mKVS node:pool){
            node.update(ecs.metadata.stringify());
        }

        A.unlockWrite();
        B.unlockWrite();
        D.unlockWrite();
        E.unlockWrite();

        ecs.deadPool.add(failed_node);

        // call AddNode()
        ecs.addNode(50,"FIFO");
    }

//    public void stop(){
//
//    }
}
