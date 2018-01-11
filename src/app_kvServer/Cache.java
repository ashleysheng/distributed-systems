package app_kvServer;

import java.nio.file.Files;
import java.util.ArrayList;
import java.io.File;
import java.io.IOException;
import java.io.StreamCorruptedException;
import java.io.FileOutputStream;

import common.messages.KVMessageObj;

import java.util.Scanner;
import org.apache.log4j.Logger;

abstract class Cache {
    abstract KVMessageObj processMessage(KVMessageObj kvm);

    public String getPath() {
        return localPath;
    }

    String serverHash_;
    String localPath;
    String globalPath;
    String localStoragePath;
    String localFirstPredecessorPath;
    String localSecondPredecessorPath;
    String globalFirstSuccessorPath;
    String globalSecondSuccessorPath;
    private static Logger logger = Logger.getRootLogger();

    /**
     * When the cache is initialized, set the paths to:
     * 1. its local directory
     * 2. the replica directories it holds
     * 3. the directories it replicates to
     *
     * This will only use placeholder names for its local replicas,
     * as there are no metadata upon the cache's initialization
     * @param serverHash
     */
    public void setPath(String serverHash){
        this.serverHash_ = serverHash;
        this.localPath = "src/storage/"+serverHash+"/";
        this.globalPath = "src/storage/";
        this.localStoragePath = "src/storage/" + serverHash + "/" + serverHash + "/";
        this.localFirstPredecessorPath = this.localPath + "localFirstPredecessorPath/";
        this.localSecondPredecessorPath = this.localPath + "localSecondPredecessorPath/";
        this.globalFirstSuccessorPath = this.globalPath + "globalFirstSuccessorPath/";
        this.globalSecondSuccessorPath = this.globalPath + "globalSecondSuccessorPath/";
        File globalStorageDirectory = new File(this.globalPath);
        if (!globalStorageDirectory.exists()) {
            globalStorageDirectory.mkdir();
        }

        File upperDirectory = new File(this.localPath);
        File localStorageDirectory = new File(this.localStoragePath);
        File firstPredecessorDirectory = new File(this.localFirstPredecessorPath);
        File secondPredecessorDirectory = new File(this.localSecondPredecessorPath);

        upperDirectory.mkdir();
        localStorageDirectory.mkdir();
        firstPredecessorDirectory.mkdir();
        secondPredecessorDirectory.mkdir();

        logger.debug(this.serverHash_);
        logger.debug(this.localPath);
        logger.debug(this.globalPath);
        logger.debug(this.localStoragePath);
        logger.debug(this.localFirstPredecessorPath);
        logger.debug(this.localSecondPredecessorPath);
        logger.debug(this.globalFirstSuccessorPath);
        logger.debug(this.globalSecondSuccessorPath);

        System.out.println(this.localPath);
    }

    /**
     * This gets called after the server receives metadata for the first time,
     * it renames the placeholder directories, and sets the 4 paths to appropriate values
     * @param predecessorHashes Hash values of the servers replicating on itself onto the current server
     * @param successorHashes Hash values of the servers that the current server will be replicating to
     */
    public void updatePath(ArrayList<String> predecessorHashes, ArrayList<String> successorHashes) {

        File firstPredecessorDirectory = new File(this.localFirstPredecessorPath);
        File secondPredecessorDirectory = new File(this.localSecondPredecessorPath);
        if (firstPredecessorDirectory.exists()){
            this.localFirstPredecessorPath = this.localPath + predecessorHashes.get(0) + "/";
            firstPredecessorDirectory.renameTo(new File(this.localFirstPredecessorPath));
        }

        if (secondPredecessorDirectory.exists()){
            this.localSecondPredecessorPath = this.localPath + predecessorHashes.get(1) + "/";
            secondPredecessorDirectory.renameTo(new File(this.localSecondPredecessorPath));
        }

        this.globalFirstSuccessorPath = this.globalPath + successorHashes.get(0) + "/" + this.serverHash_ + "/";
        logger.debug("first replica set to " + globalFirstSuccessorPath);
        this.globalSecondSuccessorPath = this.globalPath + successorHashes.get(1) + "/" + this.serverHash_ + "/";
        logger.debug("second replica set to " + globalSecondSuccessorPath);
    }

    /**
     * Find the successor that's outdated and set its path to null first
     * @param outdated_replicate
     */
    void clear_replicate(String outdated_replicate) {
        String to_clear = this.globalPath + outdated_replicate + "/" + this.serverHash_ + "/";

        if(to_clear.equals(this.globalFirstSuccessorPath)){
            this.globalFirstSuccessorPath =null;
        }

        else if(to_clear.equals(this.globalSecondSuccessorPath)){
            this.globalSecondSuccessorPath =null;
        }
    }

    /**
     * delete the old replication folder, and clear the coordinator path to null
     * @param outdated_coord the coordinator to clear
     */
    void clear_coordinator(String outdated_coord) {
        String path_to_clear = this.localPath + outdated_coord + "/";

        if(path_to_clear.equals(this.localFirstPredecessorPath)){
            this.localFirstPredecessorPath = null;
        }
        else{
            this.localSecondPredecessorPath = null;
        }

        File delete_directory = new File(path_to_clear);
        String[] files_to_delete = delete_directory.list();
        for (int i=0; i<files_to_delete.length; i++) {
            File myFile = new File(delete_directory, files_to_delete[i]);
            myFile.delete();
        }
        delete_directory.delete();
    }

    public void add_replicate(String new_replicate) {
        String to_add=this.globalPath + new_replicate + "/" + this.serverHash_ + "/";
        //sets path of replicate
        if(this.localFirstPredecessorPath==null){
            this.localFirstPredecessorPath=to_add;
        } else{
            this.localSecondPredecessorPath = to_add;
        }

        //copy all the local data to the replicate path
        File localDir= new File(this.localStoragePath);
        for (File f: localDir.listFiles()) {
            try {
                Files.copy(f.toPath(),(new File(to_add+f.getName()).toPath()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Creates a new folder locally to store others' replicas
     * @param new_coord
     */
    public void add_coordinator(String new_coord) {
        String to_add =this.localPath + new_coord + "/";
        if(localFirstPredecessorPath==null){
            localFirstPredecessorPath =to_add;
        }
        else{
            localSecondPredecessorPath=to_add;
        }

        File make_new_dir = new File(to_add);
        make_new_dir.mkdir();
    }

    String getValueFromFS(String key_) {
        //return null if no in FS
        // return value if is
        boolean exist = new File(this.localStoragePath, key_ + ".txt").exists();
        if (exist) {
            try {
                String content = new Scanner(new File(this.localStoragePath+key_ + ".txt")).useDelimiter("\\Z").next();
                return content;
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        } else {
            return null;
        }

    }

    boolean deleteFromFS(String key_) {
        boolean ret = deleteFromFSHelper(this.localStoragePath, key_);
        if (ret) {
            deleteFromFSHelper(this.globalFirstSuccessorPath, key_);
            deleteFromFSHelper(this.globalSecondSuccessorPath, key_);
        }
        return ret;
    }

    boolean deleteFromFSHelper(String path, String key_) {
        boolean exist = new File(path, key_ + ".txt").exists();
        if (exist) {
            try {
                File file = new File(path + key_ + ".txt");
                if (file.delete()) {
                    return true;
                } else {
                    return false;
                }
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        } else {
            return false;
        }
    }


    public int putToFS(String key_, String value_) {
        logger.debug("trying to put into FS for <"+key_+":"+value_+">");
        int ret = putToFSHelper(this.localStoragePath, key_, value_);
        logger.debug("Finished putting to local path at +"+this.localStoragePath);
        if (ret == 0 || ret == 1){
            putToFSHelper(this.globalFirstSuccessorPath, key_, value_);
            putToFSHelper(this.globalSecondSuccessorPath, key_, value_);
        }
        return ret;
    }
    //insert 0
    //update 1
    //error 2
    int putToFSHelper(String path, String key_, String value_) {
        boolean exist = new File(path, key_ + ".txt").exists();
        //exists, delete, make a new one
//not, make a new one
        if (exist) {
            try {
                File file = new File(path + key_ + ".txt");
                if (file.delete()) {

                } else {
                    return 2;
                }
            } catch (Exception e) {
                e.printStackTrace();
                return 2;
            }
            //make a new one
            try {
                File file = new File(path + key_ + ".txt");
                if (file.createNewFile()) {
                    FileOutputStream fop = null;
                    try {
                        fop = new FileOutputStream(file);
                        // get the content in bytes
                        byte[] contentInBytes = value_.getBytes();

                        fop.write(contentInBytes);
                        fop.flush();
                        fop.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        try {
                            if (fop != null) {
                                fop.close();
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    return 2;
                }
            } catch (IOException e) {
                e.printStackTrace();
                return 2;
            }
            return 1;

        } else {

            try {
                File file = new File(path + key_ + ".txt");
                if (file.createNewFile()) {
                    FileOutputStream fop = null;
                    try {
                        fop = new FileOutputStream(file);
                        // get the content in bytes
                        byte[] contentInBytes = value_.getBytes();
                        fop.write(contentInBytes);
                        fop.flush();
                        fop.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        try {
                            if (fop != null) {
                                fop.close();
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    return 2;
                }
            } catch (IOException e) {
                e.printStackTrace();
                return 2;
            }
            return 0;
        }
    }
}