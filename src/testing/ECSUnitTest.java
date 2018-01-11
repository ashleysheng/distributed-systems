package testing;

import app_kvEcs.ECS;
import client.KVStore;
import junit.framework.TestCase;
import logger.LogSetup;
import org.apache.log4j.Level;
import org.junit.Test;

import java.io.IOException;
import java.net.UnknownHostException;

public class ECSUnitTest extends TestCase {

    private ECS ecs;

    protected void setUp() {

        try{
            new LogSetup("test/metadatatest.log", Level.ALL);
        } catch (IOException ioe){
            ioe.printStackTrace();
        }
        ecs=new ECS("ecs.config");
    }

    @Test
    public void testWithClient(){

        ecs.initService(3,5,"FIFO");
        KVStore client;
        try {
            ecs.start();
            client = new KVStore("127.0.0.1",50000);
            client.connect();
            client.put("abc","hahahaha");
            client.put("ddd","hahahaha");
            client.put("efg","hahahaha");
            client.put("hehehe","nope");

            ecs.addNode(10,"FIFO");

            client.put("ber","hahahaha");
            client.put("fw","hahahaha");
            client.put("efgbh","hahahaha");
            client.put("uyt","nope");
            ecs.addNode(3,"LFU");
            ecs.addNode(4,"LRU");
            client.put("23","hahahaha");
            client.put("9876","hahahaha");
            client.put("53","hahahaha");
            client.put("074","nope");
            ecs.addNode(5,"FIFO");
//TODO: uncomment this after remove is done
//            ecs.removeNode(0);
            ecs.shutDown();
        } catch (Exception e){
            e.printStackTrace();
        }
    }
}
