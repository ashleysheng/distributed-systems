package app_kvServer;

import java.util.*;

//import java.lang.Object;

import java.util.HashMap;

import common.messages.KVMessage.*;
import common.messages.KVMessageObj;
import org.apache.log4j.Logger;

public class LFUCache extends Cache {
    Logger logger = Logger.getRootLogger();

    private int cacheSize;
    private HashMap hm;

    public LFUCache(int size_) {
        cacheSize = size_;
        hm = new HashMap(cacheSize + 1);   //?????????????????????????????????????????????????
    }

    public HashMap getMap() {
        return hm;
    }

    /**
     *
     * @param kvm
     *      kvm contains the request type GET/PUT
     *          key
     *          value (only for put)
     * @return
     *
     */
    public KVMessageObj processMessage(KVMessageObj kvm) {

        if (kvm.getStatus().equals(StatusType.GET)) {
            String key_ = kvm.getKey();
            //try to get it from cache
            if (hm.get(key_) != null) {
                //key exists in cache
                valuePair ve = (valuePair) hm.get(key_);
                String value_ = ve.getValue();
                ve.incrementCount();
                hm.put(key_, ve);
                logger.info("GET success value "+value_+ " at key "+key_);
                kvm.setStatus(StatusType.GET_SUCCESS);
                kvm.setValue(value_);
                return kvm;
            } else {
                //check if key is in FS
                String value_ = getValueFromFS(key_);
                if (value_ == null) {//its not in FS
                    logger.error("GET error at key: "+key_);
                    kvm.setStatus(StatusType.GET_ERROR);
                    return kvm;
                } else {//not in cache, in FS
                    //put key_, value_ in cache, it for sure does NOT exist in cache
                    if (hm.size() == cacheSize) {
                        //if cache full, evict
                        //loop through cache, find the minimum count
                        Iterator iter = hm.keySet().iterator();
                        int min = (int) Double.POSITIVE_INFINITY;
                        String minKey = "";
                        while (iter.hasNext()) {
                            String currKey = iter.next().toString();
                            valuePair currEntry = (valuePair) hm.get(currKey);
                            int currCount = currEntry.getCount();
                            if (currCount < min) {
                                min = currCount;
                                minKey = currKey;
                            }
                            if (min == 0) {
                                break;
                            }
                        }
                        hm.remove(minKey);
//                        String valueToE = ((valuePair) hm.get(minKey)).getValue();
//                        putToFS(minKey, valueToE);
                    }
                    //put the curr one in cache
                    valuePair currVE = new valuePair(value_, 1);
                    hm.put(key_, currVE);
                    kvm.setValue(value_);
                    logger.info("GET success value "+value_+ " at key "+key_);
                    kvm.setStatus(StatusType.GET_SUCCESS);
                    return kvm;
                }
            }
        } else if (kvm.getStatus().equals(StatusType.PUT)) {
            String key_ = kvm.getKey();
            String value_ = kvm.getValue();
            if (value_ == null) {//DELETE
                KVMessageObj temp = new KVMessageObj(null, null, StatusType.DELETE_ERROR);
                if (hm.containsKey(key_)) {//key in hash table, delete in hash table and FS
                    hm.remove(key_);
                } // key not in HT, try to delete it in FS
                boolean success = deleteFromFS(key_);
                if (success) {
                    logger.info("DELETE success at key "+key_);
                    kvm.setStatus(StatusType.DELETE_SUCCESS);
                } else {
                    logger.error("DELETE failure at key "+key_);
                    kvm.setStatus(StatusType.DELETE_ERROR);
                }
                return kvm;
            } else {
                if (hm.containsKey(key_)) {//key in cache, UPDATE both
                    valuePair veToUpdate = (valuePair) hm.get(key_);
                    veToUpdate.incrementCount();
                    veToUpdate.setValue(value_);
                    hm.put(key_, veToUpdate);//replace the old one
                    putToFS(key_, value_);
                    logger.info("PUT update value "+value_+" at key "+key_);
                    kvm.setStatus(StatusType.PUT_UPDATE);
                    return kvm;
                } else {//key not in cache
                    //key in FS, UPDATE FS, insert in cache,
                    int ret = putToFS(key_, value_);
                    //now put in cache, first evict
                    if (hm.size() >= cacheSize) {
                        //if cache full, evict, put it in fs
                        //loop through cache, find the minimum count
                        Iterator iter = hm.keySet().iterator();
                        int min = (int) Double.POSITIVE_INFINITY;
                        String minKey = "";
                        while (iter.hasNext()) {
                            String currKey = iter.next().toString();
                            valuePair currEntry = (valuePair) hm.get(currKey);
                            int currCount = currEntry.getCount();
                            if (currCount < min) {
                                min = currCount;
                                minKey = currKey;
                            }
                            if (min == 0) {
                                break;
                            }
                        }
                        hm.remove(minKey);
                    }
                    //put the curr one in cache
                    valuePair currVE = new valuePair(value_, 1);
                    hm.put(key_, currVE);
                    if (ret == 0) {
                        logger.info("PUT success value "+value_+" at key "+key_);
                        kvm.setStatus(StatusType.PUT_SUCCESS);
                    } else if (ret == 1) {
                        logger.info("PUT update value "+value_+" at key "+key_);

                        kvm.setStatus(StatusType.PUT_UPDATE);

                    } else {
                        logger.error("PUT failure at key "+key_);

                        kvm.setStatus(StatusType.PUT_ERROR);
                    }
                    return kvm;
                }
            }
        } else {
            System.out.println("Error! Invalid request type!");
            return null;
        }
    }
}
