package testing;

import app_kvServer.LRUCache;
import common.messages.KVMessage.*;
import common.messages.KVMessageObj;
import junit.framework.TestCase;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Created by ashleysheng on 2017-01-24.
 */
public class LRUCacheTest extends TestCase {
    public void testProcessLRU() throws Exception {
        System.out.println("Begin LRU Testing.");

        LRUCache lruCache;
        lruCache = new LRUCache(5);
        //INSERTION
        KVMessageObj kvm1 = new KVMessageObj("1", "a", StatusType.PUT);
        KVMessageObj kvm2 = new KVMessageObj("2", "b", StatusType.PUT);
        KVMessageObj kvm3 = new KVMessageObj("3", "c", StatusType.PUT);
        KVMessageObj kvm4 = new KVMessageObj("4", "d", StatusType.PUT);
        KVMessageObj kvm5 = new KVMessageObj("5", "e", StatusType.PUT);
        lruCache.processMessage(kvm1);
        lruCache.processMessage(kvm2);
        lruCache.processMessage(kvm3);
        lruCache.processMessage(kvm4);
        lruCache.processMessage(kvm5);

        System.out.println("INSERT 1-5 into cache & FS");
        System.out.println(lruCache.getMap());

        //DELETE
        System.out.println("DELETE 3 from cache & FS");
        kvm3.setKey("3");
        kvm3.setValue(null);
        kvm3.setStatus(StatusType.PUT);
        lruCache.processMessage(kvm3);
        System.out.println(lruCache.getMap());

        //REPLACEMENT
        System.out.println("GET 1 from cache");
        kvm1.setKey("1");
        kvm1.setStatus(StatusType.GET);
        lruCache.processMessage(kvm1);
        System.out.println(lruCache.getMap());

        //UPDATE
        System.out.println("UPDATE 2 at cache & FS");
        kvm2.setKey("2");
        kvm2.setValue("UPDATED");
        kvm2.setStatus(StatusType.PUT);
        lruCache.processMessage(kvm2);
        System.out.println(lruCache.getMap());

        System.out.println("INSERT 6,7,8 into cache & FS, cache eviction");
        KVMessageObj kvm6 = new KVMessageObj("6", "f", StatusType.PUT);
        KVMessageObj kvm7 = new KVMessageObj("7", "g", StatusType.PUT);
        KVMessageObj kvm8 = new KVMessageObj("8", "h", StatusType.PUT);
        KVMessageObj kvm6pr = lruCache.processMessage(kvm6);
        KVMessageObj kvm7pr = lruCache.processMessage(kvm7);
        KVMessageObj kvm8pr = lruCache.processMessage(kvm8);
        System.out.println(lruCache.getMap());


        System.out.println("UPDATE 4 from FS (4 is not in cache)");
        kvm4.setStatus(StatusType.PUT);
        kvm4.setKey("4");
        kvm4.setValue("UPDATE4");
        lruCache.processMessage(kvm4);
        System.out.println(lruCache.getMap());

        System.out.println("DELETE 5 from FS (5 is not in cache)");
        kvm5.setStatus(StatusType.PUT);
        kvm5.setValue(null);
        kvm5.setKey("5");
        lruCache.processMessage(kvm5);
        System.out.println(lruCache.getMap());

    }

}