package testing;
import app_kvServer.FIFOCache;
import common.messages.KVMessage;
import common.messages.KVMessageObj;

import junit.framework.TestCase;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Created by ashleysheng on 2017-01-27.
 */
public class FIFOCacheTest extends TestCase{

    public void testProcessFIFO() throws Exception {

        System.out.println("Begin FIFO Testing.");
        FIFOCache fifoCache;
        fifoCache = new FIFOCache(5);
//        fifoCache.setPath("Jupiter");
        //INSERTION
        KVMessageObj kvm1 = new KVMessageObj("1", "a", KVMessage.StatusType.PUT);
        KVMessageObj kvm2 = new KVMessageObj("2", "b", KVMessage.StatusType.PUT);
        KVMessageObj kvm3 = new KVMessageObj("3", "c", KVMessage.StatusType.PUT);
        KVMessageObj kvm4 = new KVMessageObj("4", "d", KVMessage.StatusType.PUT);
        KVMessageObj kvm5 = new KVMessageObj("5", "e", KVMessage.StatusType.PUT);
        fifoCache.processMessage(kvm1);
        fifoCache.processMessage(kvm2);
        fifoCache.processMessage(kvm3);
        fifoCache.processMessage(kvm4);
        fifoCache.processMessage(kvm5);

        System.out.println("INSERT 1-5 into cache & FS");
        System.out.println(fifoCache.getMap());

        //DELETE
        System.out.println("DELETE 3 from cache & FS");
        kvm3.setKey("3");
        kvm3.setValue(null);
        kvm3.setStatus(KVMessage.StatusType.PUT);
        KVMessageObj ret = fifoCache.processMessage(kvm3);
        System.out.println(fifoCache.getMap());
        System.out.println(ret.getStatus().toString());


        //REPLACEMENT
        System.out.println("GET 1 from cache");
        kvm1.setKey("1");
        kvm1.setStatus(KVMessage.StatusType.GET);
        fifoCache.processMessage(kvm1);
        System.out.println(fifoCache.getMap());

        //UPDATE
        System.out.println("UPDATE 2 at cache & FS");
        kvm2.setKey("2");
        kvm2.setValue("UPDATED");
        kvm2.setStatus(KVMessage.StatusType.PUT);
        fifoCache.processMessage(kvm2);
        System.out.println(fifoCache.getMap());

        System.out.println("INSERT 6,7,8 into cache & FS, cache eviction");
        KVMessageObj kvm6 = new KVMessageObj("6", "f", KVMessage.StatusType.PUT);
        KVMessageObj kvm7 = new KVMessageObj("7", "g", KVMessage.StatusType.PUT);
        KVMessageObj kvm8 = new KVMessageObj("8", "h", KVMessage.StatusType.PUT);
        KVMessageObj kvm6pr = fifoCache.processMessage(kvm6);
        KVMessageObj kvm7pr = fifoCache.processMessage(kvm7);
        KVMessageObj kvm8pr = fifoCache.processMessage(kvm8);
        System.out.println(fifoCache.getMap());

        System.out.println("UPDATE 1 from FS (1 is not in cache)");
        kvm1.setStatus(KVMessage.StatusType.PUT);
        kvm1.setKey("1");
        kvm1.setValue("UPDATE1");
        fifoCache.processMessage(kvm1);
        System.out.println(fifoCache.getMap());

        System.out.println("DELETE 4 from FS (4 is not in cache)");
        kvm4.setStatus(KVMessage.StatusType.PUT);
        kvm4.setValue(null);
        kvm4.setKey("4");
        fifoCache.processMessage(kvm4);
        System.out.println(fifoCache.getMap());
    }
}