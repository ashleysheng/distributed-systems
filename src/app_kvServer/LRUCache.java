package app_kvServer;

import java.util.*;

import common.messages.KVMessageObj;
import common.messages.KVMessage.StatusType;
import org.apache.log4j.Logger;

public class LRUCache extends Cache {
    Logger logger = Logger.getRootLogger();

    private int cacheSize;
    private LinkedHashMap lhm;

    public LinkedHashMap getMap() {
        return lhm;
    }

    public LRUCache(int size_) {
        this.cacheSize = size_;
        lhm = new LinkedHashMap(size_, 2.0f, true) { //????????????????????????????????????
            protected boolean removeEldestEntry(Map.Entry eldest) {
                return size() > cacheSize;
            }
        };
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
            String value_ = (lhm.get(key_) == null) ? null : lhm.get(key_).toString();
            if (value_ != null) {
                kvm.setValue(value_);
                kvm.setStatus(StatusType.GET_SUCCESS);
                logger.info("GET success value "+value_+ " at key "+key_);
                return kvm;
            } else {
                //if not search fs
                value_ = getValueFromFS(key_);
                if (value_ == null) {
                    //not in cache or fs
                    logger.error("GET error at key: "+key_);
                    kvm.setStatus(StatusType.GET_ERROR);
                    return kvm;
                } else {//not in cache but in FSs
                    lhm.put(key_, value_);
                    kvm.setValue(value_);
                    logger.info("GET success value "+value_+ " at key "+key_);
                    kvm.setStatus(StatusType.GET_SUCCESS);
                    return kvm;
                }
            }
        } else if (kvm.getStatus().equals(StatusType.PUT)) {
            String key_ = kvm.getKey();
            String value_ = kvm.getValue();
            if (value_ == null) { //DELETE
                KVMessageObj temp = new KVMessageObj(null, null, StatusType.DELETE_ERROR);
                if (lhm.containsKey(key_)) {//key in hash table, delete in hash table and FS
                    lhm.remove(key_);
                }  // key not in HT, try to delete it in FS
                boolean success = deleteFromFS(key_);
                if (success) {
                    logger.info("DELETE success at key "+key_);
                    kvm.setStatus(StatusType.DELETE_SUCCESS);
                } else {
                    logger.error("DELETE failure at key "+key_);
                    kvm.setStatus(StatusType.DELETE_ERROR);
                }
                return kvm;

            } else {// INSERT OR UPDATE
                lhm.put(key_, value_);
                int ret = putToFS(key_, value_);
                if (ret == 0) {
                    logger.info("PUT success value "+value_+" at key "+key_);
                    kvm.setStatus(StatusType.PUT_SUCCESS);
                } else if (ret == 1) {
                    logger.info("PUT update value "+value_+" at key "+key_);
                    kvm.setStatus(StatusType.PUT_UPDATE);
                } else {
                    logger.error("PUT error at key "+key_);
                    kvm.setStatus(StatusType.PUT_ERROR);
                }
            }
            return kvm;
        } else

        {
            System.out.println("Error! Invalid request type!");
            return null;
        }
    }
}
