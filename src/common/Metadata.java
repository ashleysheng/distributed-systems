package common;

import app_kvEcs.mKVS;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.lang.StringBuilder;
import org.apache.log4j.Logger;


public class Metadata {

    // LinkedHashMap of hashed value of server --> [address, port] of server
    public LinkedHashMap<String, String []> data;
    // ArrayList of server hash values. Ordered in alphabetical (ring) order
    public ArrayList<String> order;
    // Private code populated by the ECS
    private String ecsPassword;
    //self is the server's hash value (upperBound)
    private String lowerBound;
    //lowerBound is the hash value of the previous server
    private String self;
    public boolean isEmpty;

    private static Logger logger = Logger.getRootLogger();
    
    public Metadata(){
        data = new LinkedHashMap<>();
        order = new ArrayList<>();
        ecsPassword="";
        self="";
        lowerBound="";
        isEmpty=true;
    }

    public String getEcsPassword(){return this.ecsPassword;}
    public void setEcsPassword(String pwd){this.ecsPassword=pwd;}

    public String getSelf() {return self;}
    public void setSelf(String self) {this.self = self;}

    // This should only be used at ECS
    public void addToMeta(mKVS node){
        String[] d = new String[2];

        d[0]= node.addr;
        d[1]=Integer.toString(node.port);

        data.put(node.hashUpperBound,d);
        order.add(node.hashUpperBound);

        this.reOrder();
    }

    // Essentially used in both the server and client
    public void addToMeta(String upperBound,String addr, String port){
        String[] d = new String[2];
        d[0]= addr;
        d[1]=port;

        data.put(upperBound,d);
        order.add(upperBound);

        this.reOrder();
    }

    public void removeFromMeta(mKVS node){
        data.remove(node.hashUpperBound);
        order.remove(node.hashUpperBound);

        this.reOrder();
    }

    /**
     * @return string version of all the server and address & ports
     * Format:
     * HashVal_1,addr_1,port_1::HashVal_2,addr_2,port_2 ...
     */
    public String stringify(){
        String retVal="";
        String[] value;
        for(String key : data.keySet()){
            value = data.get(key);
            retVal=retVal+key+","+value[0]+","+value[1]+"::";
        }
        retVal = (data.size()==0)? "" : retVal.substring(0,retVal.length()-2);
        return retVal;
    }

    /**
     * @param newMeta
     * Updates the metadata of a server.
     * 1. populates metadata
     * 2. reorder metadata
     * 3. calculate range
     */
    public void updateServer(String newMeta){
        this.update(newMeta);
        if(!this.order.contains(self)){
            return;
        }
        this.reCalculateRange();
        this.isEmpty=false;
        logger.debug("New Metadata updated as: "+ this.stringify());
        logger.debug("Order of metadata set as "+ this.order.toString());
        logger.debug("Server "+this.self + " Set predecessor as: "+ this.lowerBound);
    }

    /**
     * @param newMeta
     * populates metadata and reorder metadata
     */
    public void update(String newMeta) {
        data.clear();
        order.clear();

        String[] tokens = newMeta.split("::");
        String[]current;
        String temp;
        int i;
        for(i=0;i<tokens.length;i++){
            temp = tokens[i];
            current=temp.split(",");
            this.addToMeta(current[0],current[1],current[2]);
        }

        this.reOrder();
    }

    /**
     * Find the index of the previous server and set lowerBound to this hash value
     */
    public void setLowerBound(){
        int prevIndex;
        int myIndex = order.indexOf(self);

        if (myIndex==0){
            prevIndex = order.size()-1;
        }
        else {
            prevIndex = myIndex - 1;
        }

        this.lowerBound = order.get(prevIndex);
    }

    public String getLowerBound(){
        return lowerBound;
    }

    /**
     * @param hash
     * @return
     *
     * For a server to determine if its responsible for the hash string
     */
    public boolean isResponsible(String hash) {

        boolean greaterThanLowerBound;
        boolean lessThanUpperBound;

        if(!this.order.contains(self)){return false;}

        greaterThanLowerBound = lowerBound.compareTo(hash)<0;
        lessThanUpperBound = this.self.compareTo(hash) >=0;

        if (order.indexOf(self)==0) {
            return greaterThanLowerBound || lessThanUpperBound;
        }
        else {
            return greaterThanLowerBound && lessThanUpperBound;
        }
    }

    /**
     * @param key
     * @return [address, port] of the responsible server
     *
     * For a client to find the responsible server of a given key
     */
    public String[] findResponsibleServer(String key){
        MD5Hasher hasher = new MD5Hasher();
        String hashedVal = hasher.hashString(key);

        for (String serverHash : order){
            if(hashedVal.compareTo(serverHash) <= 0) {
                return data.get(serverHash);
            }
        }

        return data.get(order.get(0));
    }

    /**
     * This should ONLY be used by KVServer
     */
    public void reCalculateRange(){
        Collections.sort(order);
        this.setLowerBound();
    }

    public void reOrder(){
        Collections.sort(order);
    }

    // return the upper and lower bound of the two successor serves to this server
    public ArrayList<String> getSuccessorHashes(){

        ArrayList<String> twoSuccessorServers = null;
        int selfIndex = order.indexOf(self);
        if (selfIndex == order.size() - 1) {
            twoSuccessorServers = new ArrayList<String>(2);
            twoSuccessorServers.add((new StringBuilder(order.get(0))).toString());
            twoSuccessorServers.add((new StringBuilder(order.get(1))).toString());
        }
        else if (selfIndex == order.size() - 2) {
            twoSuccessorServers = new ArrayList<String>(2);
            twoSuccessorServers.add((new StringBuilder(order.get(order.size() - 1))).toString());
            twoSuccessorServers.add((new StringBuilder(order.get(0))).toString());
        }
        else {
            twoSuccessorServers = new ArrayList<String>(2);
            twoSuccessorServers.add((new StringBuilder(order.get(selfIndex+1))).toString());
            twoSuccessorServers.add((new StringBuilder(order.get(selfIndex+2))).toString());
        }
        return twoSuccessorServers;
    }

    public ArrayList<String> getPredecessorHashes(){

        ArrayList<String> twoPredecessorServers = null;
        int selfIndex = order.indexOf(self);
        if (selfIndex == 0) {
            twoPredecessorServers = new ArrayList<String>(2);
            twoPredecessorServers.add((new StringBuilder(order.get(order.size()-1))).toString());
            twoPredecessorServers.add((new StringBuilder(order.get(order.size()-2))).toString());
        }
        else if (selfIndex == 1) {
            twoPredecessorServers = new ArrayList<String>(2);
            twoPredecessorServers.add((new StringBuilder(order.get(0))).toString());
            twoPredecessorServers.add((new StringBuilder(order.get(order.size()-1))).toString());
        }
        else {
            twoPredecessorServers = new ArrayList<String>(2);
            twoPredecessorServers.add((new StringBuilder(order.get(selfIndex-1))).toString());
            twoPredecessorServers.add((new StringBuilder(order.get(selfIndex-2))).toString());
        }
        return twoPredecessorServers;
    }

    public String[] getNextServer(){
        int nextIndex;
        int myIndex = order.indexOf(self);

        if (myIndex==order.size()-1){
            nextIndex = 0;
        }
        else {
            nextIndex = myIndex + 1;
        }

        String nextHash = order.get(nextIndex);
        return this.data.get(nextHash);
    }

    public String getNextHash(String hash){
        int myIndex = order.indexOf(hash);
        int i1 = (myIndex+1)%order.size();

        String nextHash = order.get(i1);
        return nextHash;
    }
    public ArrayList<String> nextTwoHashes(String hash){
        int myIndex = order.indexOf(hash);
        int i1 = (myIndex+1)%order.size();
        int i2 = (myIndex+2)%order.size();

        ArrayList<String> retVal = new ArrayList<>();
        retVal.add(order.get(i1));
        retVal.add(order.get(i2));
        return retVal;
    }

}