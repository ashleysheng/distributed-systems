package app_kvServer;

import java.io.*;
import java.lang.reflect.Array;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.Semaphore;
import java.net.Socket;

import common.MD5Hasher;
import common.Metadata;
import common.messages.KVAdminMsg;
import common.messages.KVMessage;
import common.messages.KVMessageObj;
import org.apache.log4j.*;
import common.communication.ServerCommModule;

/**
 * Represents a connection end point for a particular client that is
 * connected to the server. This class is responsible for message reception
 * and sending.
 * The class also implements the echo functionality. Thus whenever a message
 * is received it is going to be echoed back to the client.
 */
public class ClientConnection implements Runnable {

    private static Logger logger = Logger.getRootLogger();

    private boolean isOpen;
    private ServerCommModule commModule;
    private Cache c;
    private Semaphore s;
    private Metadata meta;
    private lockList serverLL;
    private ServerSocket ss;

    /**
     * Constructs a new ClientConnection object for a given TCP socket.
     *
     * @param clientSocket the Socket object for the client connection.
     */
    ClientConnection(Socket clientSocket, Cache c_, Semaphore s_, Metadata meta, lockList ll,
                     ServerSocket ss) {
        this.commModule = new ServerCommModule(clientSocket);
        this.isOpen = true;
        this.c = c_;
        this.s = s_;
        this.meta = meta;
        this.serverLL = ll;
        this.ss = ss;
    }

    /**
     * Initializes and starts the client connection.
     * Loops until the connection is closed or aborted by the client.
     */
    public void run() {
        try {
            commModule.initializeIO();

            while (isOpen) {
                try {
                    KVMessageObj latestMsg = commModule.receive();
                    System.out.println("just received a KVMessage...");
                    if (latestMsg.getKey().equals(meta.getEcsPassword())) {
                        KVAdminMsg adminMsg = new KVAdminMsg(latestMsg);
                        System.out.println("Received a KVAdminMsg");
                        processAdminMessage(adminMsg);
                    } else {
                        s.acquire();
                        MD5Hasher md5 = new MD5Hasher();
                        // handle a client request, first check if server is responsible/writeLock/running
                        if (this.serverLL.getActive() == false) {
                            latestMsg.setStatus(KVMessage.StatusType.SERVER_STOPPED);
                        } else if (this.serverLL.getWriteLock() == true) {
                            latestMsg.setStatus(KVMessage.StatusType.SERVER_WRITE_LOCK);
                        } else if (!this.meta.isResponsible(md5.hashString(latestMsg.getKey()))) {
                            latestMsg.setValue(this.meta.stringify());
                            latestMsg.setStatus(KVMessage.StatusType.SERVER_NOT_RESPONSIBLE);
                        } else {
                            latestMsg = c.processMessage(latestMsg);
                        }
                        s.release();
                        commModule.send(latestMsg);
                    }

				/* connection either terminated by the client or lost due to 
                 * network problems*/
                } catch (IOException ioe) {
                    logger.error("Error! Connection lost!");
                    isOpen = false;
                } catch (Exception e) {
                    logger.error("Connection lost!");
                    isOpen = false;
                }
            }
        } catch (IOException ioe) {
            logger.error("Error! Connection could not be established!", ioe);

        } finally {

            try {
                commModule.closeConnection();
            } catch (IOException ioe) {
                logger.error("Error! Unable to tear down connection!", ioe);
            }
        }
    }

    public void processAdminMessage(KVAdminMsg adminMsg) {
        if (adminMsg.getAdminStatus().equals(KVAdminMsg.StatusType.START)) {
            this.serverLL.setActive(true);
            adminMsg.setStatus(KVAdminMsg.StatusType.SUCCESS);
            logger.info("Successfully set server status to START");
            try {
                commModule.send(adminMsg);
            } catch (IOException ioe) {
                logger.error("Error! Unable to send adminMsg back!", ioe);
            } catch (Exception e) {
                logger.error("Connection lost!");
                isOpen = false;
            }

        } else if (adminMsg.getAdminStatus().equals(KVAdminMsg.StatusType.STOP)) {
            this.serverLL.setActive(false);
            logger.info("Successfully set server status to STOP");
            adminMsg.setStatus(KVAdminMsg.StatusType.SUCCESS);
            try {
                commModule.send(adminMsg);
            } catch (IOException ioe) {
                logger.error("Error! Unable to send adminMsg back!", ioe);
            } catch (Exception e) {
                logger.error("Connection lost!");
                isOpen = false;
            }
        } else if (adminMsg.getAdminStatus().equals(KVAdminMsg.StatusType.LOCK_WRITE)) {
            this.serverLL.setWriteLock(true);
            adminMsg.setStatus(KVAdminMsg.StatusType.SUCCESS);
            try {
                commModule.send(adminMsg);
            } catch (IOException ioe) {
                logger.error("Error! Unable to send adminMsg back!", ioe);
            } catch (Exception e) {
                logger.error("Connection lost!");
                isOpen = false;
            }
        } else if (adminMsg.getAdminStatus().equals(KVAdminMsg.StatusType.UNLOCK_WRITE)) {
            this.serverLL.setWriteLock(false);
            adminMsg.setStatus(KVAdminMsg.StatusType.SUCCESS);
            try {
                commModule.send(adminMsg);
            } catch (IOException ioe) {
                logger.error("Error! Unable to send adminMsg back!", ioe);
            } catch (Exception e) {
                logger.error("Connection lost!");
                isOpen = false;
            }
        } else if (adminMsg.getAdminStatus().equals(KVAdminMsg.StatusType.UPDATE)) {
            try {
                this.s.acquire();
                logger.debug("Begin updating metadata");
                ArrayList<String> old_pred, old_suc, new_pred, new_suc;

                if(meta.isEmpty){
                    meta.updateServer(adminMsg.getValue());
                    if(meta.order.contains(meta.getSelf())){
                        c.updatePath(meta.getPredecessorHashes(), meta.getSuccessorHashes());
                    }
                }
                else {
                    logger.debug("updating existing metadata");
                    String new_replicate, new_coord, outdated_replicate, outdated_coord;

                    old_pred = meta.getPredecessorHashes();
                    old_suc = meta.getSuccessorHashes();

                    logger.debug("old predecessor :"+old_pred.toString());
                    logger.debug("old successor: "+old_suc.toString());

                    meta.updateServer(adminMsg.getValue());

                    //if it contains itself, it's business as usual. else, we're on the node that's being removed
                    if(meta.order.contains(meta.getSelf())){
                        logger.debug("performing update on a surviving ring member");
                        new_pred = meta.getPredecessorHashes();
                        new_suc = meta.getSuccessorHashes();

                        logger.debug("new predecessor :"+new_pred.toString());
                        logger.debug("new successor: "+new_suc.toString());

                        this.serverLL.setWriteLock(true);


                        for (String i: old_pred) {
                            if (!new_pred.contains(i)){
                                //delete the old folder containing this guy's data
                                //set path
                                outdated_coord = i;
                                logger.debug("found outdated coordinator "+outdated_coord);
                                c.clear_coordinator(outdated_coord);
                                logger.debug("outdated coordinator path and data cleared");
                            }
                        }
                        for (String i: new_pred) {
                            if (!old_pred.contains(i)){
                                new_coord = i;
                                //make local directory and set path here
                                logger.debug("found outdated coordinator "+new_coord);
                                c.add_coordinator(new_coord);
                                logger.debug("set local coordinator path and made new directory");
                            }
                        }
                        for (String i: old_suc) {
                            if (!new_suc.contains(i)){
                                outdated_replicate = i;
                                logger.debug("found outdated replicate "+outdated_replicate);
                                //empty out the outdated path
                                c.clear_replicate(outdated_replicate);
                                logger.debug("outdated replicate path cleared");
                            }
                        }
                        for (String i: new_suc) {
                            if (!old_suc.contains(i)){
                                // copy all its data to the new one
                                new_replicate = i;
                                // copies the files and set the path here
                                logger.debug("found new successor "+new_suc);
                                c.add_replicate(new_replicate);
                                logger.debug("successor successfully replicated data");
                            }
                        }

                        this.serverLL.setWriteLock(false);
                    } else{
                        logger.debug("no opt because this node will be removed");
                    }
//                    c.updatePath(meta.getPredecessorHashes(), meta.getSuccessorHashes());
                }

                logger.debug("Finish updating metadata");
                this.s.release();
            } catch (Exception e) {
//                logger.error("Interrupted@@!!!!");
                logger.error("Error in updating metadata. full stack track: ", e);
                isOpen = false;
            }
            try {
                adminMsg.setStatus(KVAdminMsg.StatusType.UPDATE);
                commModule.send(adminMsg);
            } catch (IOException ioe) {
                logger.error("Error! Unable to send adminMsg back!", ioe);
            } catch (Exception e) {
                logger.error("Connection lost!");
                isOpen = false;
            }
        } else if (adminMsg.getAdminStatus().equals(KVAdminMsg.StatusType.SHUTDOWN)) {
            adminMsg.setStatus(KVAdminMsg.StatusType.SHUTDOWN);
            try {
                commModule.send(adminMsg);
                logger.info("Successfully received shutdown request from admin. shutting down...");
                if(ss!=null){
                    ss.close();
                }
                System.exit(0);
            } catch (IOException ioe) {
                logger.error("Error! Unable to send adminMsg back!", ioe);
            } catch (Exception e) {
                logger.error("Connection lost!");
                isOpen = false;
            }
        } else if (adminMsg.getAdminStatus().equals(KVAdminMsg.StatusType.MOVE_DATA)) {
            logger.debug("INSIDE MOVE DATA BLOCK");
            //clean up
            if (adminMsg.getValue().equals("")) {
                logger.debug("clean up begins");
                try {
                    File dir = new File(c.localStoragePath);
                    File[] directoryListing = dir.listFiles();
                    logger.debug("Files in directory before cleanup: " + dir.listFiles().toString());
                    if (directoryListing != null) {
                        MD5Hasher md5 = new MD5Hasher();
                        for (File child : directoryListing) {
                            String name = child.getName().substring(0, child.getName().length() - 4);
                            String hashName = md5.hashString(name);
                            if (!meta.isResponsible(hashName) || !meta.order.contains(meta.getSelf())) {
                                //delete them in your replicas
                                ArrayList<String>replicas = meta.getSuccessorHashes();
                                logger.debug("deleting from the following replica servers: "+
                                replicas.toString());
                                for(String d : replicas){
                                    String path_name="src/storage/"+d+"/"+meta.getSelf()+"/"+child.getName();
                                    logger.debug("Trying to find replica path name: "+path_name);
                                    File d1 = new File(path_name);
                                    if(d1.exists()){
                                        logger.debug("Found existing file to delete. Deleting...");
                                        d1.delete();
                                    }
                                }
                                child.delete();
                            }
                        }
                    } else {
                        logger.error("FS empty, nothing to clean up");

                        // Handle the case where dir is not really a directory.
                        // Checking dir.isDirectory() above would not be sufficient
                        // to avoid race conditions with another process that deletes
                        // directories.
                    }
                    System.out.println("File is copied successful!");
                } catch (Exception e) {
                    logger.error("Error occured during cleanup");
                    e.printStackTrace();
                }
            }
//            //move data
             else if (!adminMsg.getValue().equals("")) {
                logger.debug("move begins");
                logger.debug("received message: " + adminMsg.getValue());
                String[] range_recipient = adminMsg.getValue().split(",");
                logger.debug("split range reci success");
                //range anythin under or equal, send out
                //reci sent to
                InputStream inStream = null;
                OutputStream outStream = null;
                try {
                    File dir=null;
                    logger.debug("range_recipient length: "+Integer.toString(range_recipient.length));
                    if(range_recipient.length==2){
                        logger.info("moving local data");
                        dir = new File(c.localStoragePath);
                    }
                    else if (range_recipient.length==3){
                        logger.info("recovering data");
                        String path=c.localPath+range_recipient[0]+"/";
                        logger.debug("recovering from directory: "+path);
                        dir = new File(path);
                    }
                    if (dir==null) throw new Exception();
                    File[] directoryListing = dir.listFiles();
                    if (directoryListing != null) {
                        MD5Hasher md5 = new MD5Hasher();
                        for (File child : directoryListing) {
                            logger.debug(child.getName());
                            logger.debug("without .txt: " + child.getName().substring(0, child.getName().length() - 4));
                            String name = child.getName().substring(0, child.getName().length() - 4);
                            String hashName = md5.hashString(name);
                            logger.debug("hashed name of current key: " +hashName);
                            if (!meta.isResponsible(hashName)) {
                                logger.debug("moving one element");
//                                File newDir = new File(range_recipient[1]);
//                                while(!newDir.exists()){}
                                File bfile = new File(
                                        "src/storage/"+
                                                range_recipient[1] +
                                                "/"+range_recipient[1] +
                                                "/"+child.getName()
                                );
                                inStream = new FileInputStream(child);
                                outStream = new FileOutputStream(bfile);
                                byte[] buffer = new byte[1024];

                                int length;
                                //copy the file content in bytes
                                while ((length = inStream.read(buffer)) > 0) {
                                    outStream.write(buffer, 0, length);
                                }
                                inStream.close();
                                outStream.close();

                                // Send the data to the new node's replicas
                                int index = meta.order.indexOf(range_recipient[1]);

                                ArrayList<String> destinations = new ArrayList<>();
                                destinations.add(meta.order.get((meta.order.size()+index+1)%meta.order.size()));
                                destinations.add(meta.order.get((meta.order.size()+index+2)%meta.order.size()));
                                logger.debug("replicant destinations for transferring data: "+destinations.toString());
                                for (String d: destinations) {
                                    bfile = new File("src/storage/"+d+"/"+range_recipient[1]+"/"+child.getName());
                                    inStream = new FileInputStream(child);
                                    outStream = new FileOutputStream(bfile);
                                    buffer = new byte[1024];
                                    //copy the file content in bytes
                                    while ((length = inStream.read(buffer)) > 0) {
                                        outStream.write(buffer, 0, length);
                                    }
                                    inStream.close();
                                    outStream.close();
                                }

                            }
                        }

                    } else {
                        logger.error("Error occured during move!!!!!!!!");
                        // Handle the case where dir is not really a directory.
                        // Checking dir.isDirectory() above would not be sufficient
                        // to avoid race conditions with another process that deletes
                        // directories.
                    }
                    System.out.println("File is copied successful!");
                }
//                catch (IOException e) {
//                    logger.error("Error occured during move");
//                    e.printStackTrace();}
                catch (Exception e) {
                    logger.error("Error in moving files. full stack track: ", e);
                }
            }
            try {
                adminMsg.setStatus(KVAdminMsg.StatusType.SUCCESS);
                commModule.send(adminMsg);
                logger.info("Successfully moved/deleted files");

            } catch (IOException ioe) {
                logger.error("Error! Unable to send adminMsg back!", ioe);
            } catch (Exception e) {
                logger.error("Connection lost!");
                isOpen = false;
            }
        }
    }
}
