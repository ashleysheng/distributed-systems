package testing;

import app_kvEcs.ECS;
import app_kvServer.KVServer;
import client.KVStore;
import common.communication.ECSCommModule;
import junit.framework.TestCase;
import logger.LogSetup;
import org.apache.log4j.Level;
import org.junit.Test;

import java.io.IOException;

/**
 * Created by ashleysheng on 2017-03-05.
 */
public class ClientServerConnectionTest extends TestCase {
    private ECSCommModule comm;
    private KVStore client;
    private ECS ecs;

    protected void setUp() {

        try{
            new LogSetup("test/ClientServerConnectionTest.log", Level.ALL);
        } catch (IOException ioe){
            ioe.printStackTrace();
        }
        ecs=new ECS("ecs_large.config");
        client = new KVStore("127.0.0.1",50000);

    }


    @Test
    public void testPutandGet() throws Exception {
        ecs.initService(5,5,"FIFO");
        ecs.start();

        client.connect(); //connect to server 1

        client.put("we","switch"); //hash value: ff1ccf57e98c817df1efcd9fe44a8aeb   server 1
        client.get("we");
        client.put("091","switch");//d2716515bd05082789a31002d4bbc958  //server 1
        client.put("fs","no");//bc7b36fe4d2924e49800d9b3dc4a325c   //server 3
        client.put("shrt","switch");//f6122c971aeb03476bf01623b09ddfd4    //server 2
        client.put("wre","switch");//80743387e7cd5fb5cc71693b16859e80  //server 3
        client.put("tu","no");//b6b4ce6df035dcfaa26f3bc32fb89e6a    //server 3
        client.put("csw","no");//7da518a55e2d9c4c217a921aea54d45c //server 3
        client.put("po","switch");//f6122c971aeb03476bf01623b09ddfd4    //server 2
        client.put("ndr","switch");//d74e97d017d1d868c59d07f022613670//server 1

        client.get("shrt");//f6122c971aeb03476bf01623b09ddfd4    //server 2
        client.get("wre");//80743387e7cd5fb5cc71693b16859e80  //server 3
        client.get("tu");//b6b4ce6df035dcfaa26f3bc32fb89e6a    //server 3
        client.get("abs");//get error
        client.get("po");//f6122c971aeb03476bf01623b09ddfd4    //server 2
        client.get("ndr");//d74e97d017d1d868c59d07f022613670//server 1
        ecs.shutDown();
    }
}