package testing;

import common.messages.KVMessage;
import common.messages.KVMessageObj;
import junit.framework.TestCase;
import org.junit.Test;
import app_kvServer.*;
import java.util.Iterator;

import static org.junit.Assert.*;

/**
 * Created by ashleysheng on 2017-01-27.
 */
public class LFUCacheTest extends TestCase {

    public void testProcessLFU() throws Exception {
        System.out.println("Begin LFU Testing.");

        LFUCache lfuCache;
        lfuCache = new LFUCache(5);
        //INSERTION
        KVMessageObj kvm1 = new KVMessageObj("1", "a", KVMessage.StatusType.PUT);
        KVMessageObj kvm2 = new KVMessageObj("2", "b", KVMessage.StatusType.PUT);
        KVMessageObj kvm3 = new KVMessageObj("3", "c", KVMessage.StatusType.PUT);
        KVMessageObj kvm4 = new KVMessageObj("4", "d", KVMessage.StatusType.PUT);
        KVMessageObj kvm5 = new KVMessageObj("5", "e", KVMessage.StatusType.PUT);
        lfuCache.processMessage(kvm1);
        lfuCache.processMessage(kvm2);
        lfuCache.processMessage(kvm3);
        lfuCache.processMessage(kvm4);
        lfuCache.processMessage(kvm5);

        System.out.println("INSERT 1-5 into cache & FS");
        Iterator iter = lfuCache.getMap().keySet().iterator();
        while (iter.hasNext()) {
            String currKey = iter.next().toString();
            valuePair currEntry = (valuePair) lfuCache.getMap().get(currKey);
            System.out.println(currKey+" ----- "+currEntry.getValue()+" ---- "+currEntry.getCount());
        }


        //DELETE
        System.out.println("DELETE 3 from cache & FS");
        kvm3.setKey("3");
        kvm3.setValue(null);
        kvm3.setStatus(KVMessage.StatusType.PUT);
        lfuCache.processMessage(kvm3);

        iter = lfuCache.getMap().keySet().iterator();
        while (iter.hasNext()) {
            String currKey = iter.next().toString();
            valuePair currEntry = (valuePair) lfuCache.getMap().get(currKey);
            System.out.println(currKey+" ----- "+currEntry.getValue()+" ---- "+currEntry.getCount());
        }


        //REPLACEMENT
        System.out.println("GET 1 from cache");
        kvm1.setKey("1");
        kvm1.setStatus(KVMessage.StatusType.GET);
        lfuCache.processMessage(kvm1);

        iter = lfuCache.getMap().keySet().iterator();
        while (iter.hasNext()) {
            String currKey = iter.next().toString();
            valuePair currEntry = (valuePair) lfuCache.getMap().get(currKey);
            System.out.println(currKey+" ----- "+currEntry.getValue()+" ---- "+currEntry.getCount());
        }

        //UPDATE
        System.out.println("UPDATE 2 at cache & FS");
        kvm2.setKey("2");
        kvm2.setValue("UPDATED");
        kvm2.setStatus(KVMessage.StatusType.PUT);
        lfuCache.processMessage(kvm2);

        iter = lfuCache.getMap().keySet().iterator();
        while (iter.hasNext()) {
            String currKey = iter.next().toString();
            valuePair currEntry = (valuePair) lfuCache.getMap().get(currKey);
            System.out.println(currKey+" ----- "+currEntry.getValue()+" ---- "+currEntry.getCount());
        }

        System.out.println("INSERT 6,7,8 into cache & FS, cache eviction");
        KVMessageObj kvm6 = new KVMessageObj("6", "f", KVMessage.StatusType.PUT);
        KVMessageObj kvm7 = new KVMessageObj("7", "g", KVMessage.StatusType.PUT);
        KVMessageObj kvm8 = new KVMessageObj("8", "h", KVMessage.StatusType.PUT);
        KVMessageObj kvm6pr = lfuCache.processMessage(kvm6);
        KVMessageObj kvm7pr = lfuCache.processMessage(kvm7);
        KVMessageObj kvm8pr = lfuCache.processMessage(kvm8);

        iter = lfuCache.getMap().keySet().iterator();
        while (iter.hasNext()) {
            String currKey = iter.next().toString();
            valuePair currEntry = (valuePair) lfuCache.getMap().get(currKey);
            System.out.println(currKey+" ----- "+currEntry.getValue()+" ---- "+currEntry.getCount());
        }

        System.out.println("UPDATE 4 from FS (4 is not in cache)");
        kvm4.setStatus(KVMessage.StatusType.PUT);
        kvm4.setKey("4");
        kvm4.setValue("UPDATE4");
        lfuCache.processMessage(kvm4);

        iter = lfuCache.getMap().keySet().iterator();
        while (iter.hasNext()) {
            String currKey = iter.next().toString();
            valuePair currEntry = (valuePair) lfuCache.getMap().get(currKey);
            System.out.println(currKey+" ----- "+currEntry.getValue()+" ---- "+currEntry.getCount());
        }

        System.out.println("DELETE 5 from FS (5 is not in cache)");
        kvm5.setStatus(KVMessage.StatusType.PUT);
        kvm5.setValue(null);
        kvm5.setKey("5");
        lfuCache.processMessage(kvm5);

        iter = lfuCache.getMap().keySet().iterator();
        while (iter.hasNext()) {
            String currKey = iter.next().toString();
            valuePair currEntry = (valuePair) lfuCache.getMap().get(currKey);
            System.out.println(currKey+" ----- "+currEntry.getValue()+" ---- "+currEntry.getCount());
        }

    }

}