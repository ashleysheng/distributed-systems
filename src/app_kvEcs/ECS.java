package app_kvEcs;

import common.Metadata;
import common.communication.ECSListener;
import logger.LogSetup;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.io.*;
import java.util.ArrayList;

public class ECS implements ECSCommInterface{

    public ArrayList<mKVS> serverPool;
    public ArrayList<mKVS> standbyPool;
    public ArrayList<mKVS> activePool;
    public ArrayList<mKVS> deadPool;
    public Metadata metadata;
    public State state;
    public ECSListener listener;

    public enum State{
        DOWN,
        STANDBY,
        ACTIVE
    }

    private static Logger logger = Logger.getRootLogger();

    public ECS(String file_path){
        try {
            this.state = State.DOWN;

            this.serverPool = new ArrayList<>();
            this.standbyPool = new ArrayList<>();
            this.activePool = new ArrayList<>();
            this.deadPool = new ArrayList<>();
            this.metadata = new Metadata();

            File f = new File(file_path);
            BufferedReader b = new BufferedReader(new FileReader(f));
            String readLine = "";

            while ((readLine = b.readLine()) != null) {
                String[] params = readLine.split("\\s+");
                mKVS s = new mKVS(params[0],params[1],Integer.parseInt(params[2]));
                serverPool.add(s);

                logger.info("Added the server "+readLine+" to server pool");
            }
            
            b.close();
        } catch (IOException ioe) {
            System.out.println("Error: can't read file");
        }
        logger.info("Successfully added all configs");

        listener = new ECSListener(this);
        listener.start();
    }

    public void initService(int numberOfNodes, int cacheSize, String replacementStrategy) {

        try {
            File storage = new File("src/storage/");
            if (storage.exists())
                deleteFile(storage);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

        try{

            if (numberOfNodes > serverPool.size()){
                logger.error("Exceeds server pool limit");
                throw new Exception();
            }

            for (int i =0; i<numberOfNodes;i++){
                mKVS controller = serverPool.remove(0);
                standbyPool.add(controller);
                metadata.addToMeta(controller);
            }
            for (mKVS controller: standbyPool) {
                controller.initKVServer(metadata.stringify(),cacheSize,replacementStrategy);
            }

            this.state = State.STANDBY;

        }catch (IOException e) {
            logger.error("Could not complete SSH command");
            e.printStackTrace();
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public void start(){
        int numberToStart = standbyPool.size();
        mKVS current;

        for (int i = 0; i < numberToStart; i++) {
            current = standbyPool.remove(0);
            activePool.add(current);
        }

        logger.debug("Metadata Stringified: " + metadata.stringify());
        logger.debug("Current order of hashes: " + metadata.order.toString());

        for (mKVS node: activePool) {
            node.start();
        }

        this.state = State.ACTIVE;
    }

    public void stop(){
        if(activePool.size()==0) return;

        mKVS node;
        int totalActive = activePool.size();
        for(int i = 0; i<totalActive;i++){
            node = activePool.remove(0);
            node.stop();
            standbyPool.add(node);
        }

        this.state = State.STANDBY;
    }

    public void shutDown(){
        this.stop();

        mKVS node;
        int totalStandby=standbyPool.size();
        for(int i = 0; i<totalStandby;i++){
            node = standbyPool.remove(0);
            node.shutDown();
            serverPool.add(node);
        }
        logger.debug("INSIDE ECS SHUTDOWN, ABOUT TO SET LISTENER RUNNING TO FALSE");
        this.listener.setRunning(false);
        try {
            this.listener.ss.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.state = State.DOWN;
    }

    public void addNode(int cacheSize, String replacementStrategy) throws Exception{
        logger.info("Adding node...");
        if(serverPool.size()==0){
            logger.error("Don't have any more servers available");
            throw new Exception();
        }
        if(state.equals(State.DOWN)){
            logger.error("Must first initialize service!");
            throw new Exception();
        }

        mKVS toAdd = serverPool.remove(0);

        metadata.addToMeta(toAdd);

        toAdd.initKVServer(metadata.stringify(),cacheSize,replacementStrategy);

        int index = metadata.order.indexOf(toAdd.hashUpperBound);
        String successorHash = metadata.order.get((index+1)%metadata.order.size());

        System.out.println("A Hash index "+ (metadata.order.size()+index-2)%metadata.order.size());
        String A_hash = metadata.order.get((metadata.order.size()+index-2)%metadata.order.size());
        String B_hash = metadata.order.get((metadata.order.size()+index-1)%metadata.order.size());
        String D_hash = metadata.order.get((metadata.order.size()+index+1)%metadata.order.size());
        String E_hash = metadata.order.get((metadata.order.size()+index+2)%metadata.order.size());

        logger.debug("A_Hash is "+A_hash);
        logger.debug("B_Hash is "+B_hash);
        logger.debug("D_Hash is "+D_hash);
        logger.debug("E_Hash is "+E_hash);


        ArrayList<mKVS> pool = null;
        if(this.state.equals(State.STANDBY)){
            pool = standbyPool;
        } else if(this.state.equals(State.ACTIVE)){
            pool = activePool;
        }

        mKVS successor=null,A=null,B=null, D =null, E=null;

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
                successor = node;
            }
            if(node.hashUpperBound.equals(E_hash)){
                E = node;
            }
        }
        if(successor==null) throw new Exception();

        successor.lockWrite();
        toAdd.lockWrite();
        A.lockWrite();
        B.lockWrite();
        D.lockWrite();
        E.lockWrite();

        A.update(metadata.stringify());
        B.update(metadata.stringify());
        D.update(metadata.stringify());
        E.update(metadata.stringify());

        //this update is basically redundant
//        successor.update(metadata.stringify());

        //tell toAdd to move its new data to D and E, or just let successor.moveData() to do it.
        successor.moveData(toAdd.hashUpperBound,toAdd.hashUpperBound);

        for(mKVS node:pool){
            node.update(metadata.stringify());
        }

        successor.unlockWrite();
        successor.cleanUp();

        toAdd.unlockWrite();
        A.unlockWrite();
        B.unlockWrite();
        D.unlockWrite();
        E.unlockWrite();

        pool.add(toAdd);
        if(pool.equals(activePool)){
            toAdd.start();
        }
    }
    
    public void removeNode(int index) throws Exception{
        logger.info("Removing node...");
        ArrayList<mKVS> pool = null;
        if(this.state.equals(State.STANDBY)){
            pool = standbyPool;
        } else if(this.state.equals(State.ACTIVE)){
            pool = activePool;
        } else {
            logger.error("Service not initialized");
            throw new Exception();
        }

        mKVS toRemove = pool.get(index);

        int removeIndex = metadata.order.indexOf(toRemove.hashUpperBound);
        String successorHash = metadata.order.get((removeIndex+1)%metadata.order.size());
        String A_hash = metadata.order.get((metadata.order.size()+removeIndex-2)%metadata.order.size());
        String B_hash = metadata.order.get((metadata.order.size()+removeIndex-1)%metadata.order.size());
        String D_hash = metadata.order.get((metadata.order.size()+removeIndex+1)%metadata.order.size());
        String E_hash = metadata.order.get((metadata.order.size()+removeIndex+2)%metadata.order.size());

        logger.debug("A_Hash is "+A_hash);
        logger.debug("B_Hash is "+B_hash);
        logger.debug("D_Hash is "+D_hash);
        logger.debug("E_Hash is "+E_hash);

        mKVS successor=null,A=null,B=null, D =null, E=null;


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
                successor = node;
            }
            if(node.hashUpperBound.equals(E_hash)){
                E = node;
            }
        }
        if(successor==null) throw new Exception();
        metadata.removeFromMeta(toRemove);
        toRemove.lockWrite();
        A.lockWrite();
        B.lockWrite();
        D.lockWrite();
        E.lockWrite();

        toRemove.update(metadata.stringify());
        successor.update(metadata.stringify());
        D.update(metadata.stringify());
        E.update(metadata.stringify());
        A.update(metadata.stringify());
        B.update(metadata.stringify());

        toRemove.moveData(toRemove.hashUpperBound,successor.hashUpperBound);

        pool.remove(toRemove);
        for(mKVS node:pool){
            node.update(metadata.stringify());
        }

        toRemove.cleanUp();

        A.unlockWrite();
        B.unlockWrite();
        D.unlockWrite();
        E.unlockWrite();

        toRemove.shutDown();
        serverPool.add(toRemove);
    }

    void deleteFile(File f) throws IOException {
        if (f.isDirectory()) {
            for (File c : f.listFiles()) {
                deleteFile(c);
            }
        }
        if (!f.delete()){
            throw new FileNotFoundException("Failed to delete file: " + f);
        }
    }

}
