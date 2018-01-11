package testing;

import app_kvEcs.ECS;
import client.KVStore;
import junit.framework.TestCase;
import logger.LogSetup;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Test;
import java.io.IOException;

/**
 * Created by blade on 2017-03-25.
 */
public class AddNodeTest extends TestCase {

    private ECS ecs;
    private String key, value;

    private static Logger logger = Logger.getRootLogger();

    protected void setUp() {
        try {
            LogSetup logger = new LogSetup("test/addNodeTest.log", Level.ALL);

        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        ecs = new ECS("ecs.config");
    }

    @Test
    public void testAddNode() {

        logger.debug("Begin add node replication test ... ");
        ecs.initService(5,10,"FIFO");


        KVStore client;
        try {

            ecs.start();
            client = new KVStore("127.0.0.1",50000);
            client.connect();

            key = "6";
            value = "6";

            client.put(key, value);

            key = "xxx";
            value = "xxx";

            client.put(key, value);

            key = "yes";
            value = "yes";

            client.put(key, value);

            key = "no";
            value = "no";

            client.put(key, value);

            key = "umm";
            value = "umm";

            client.put(key, value);

            key = "hello";
            value = "hello";
            client.put(key, value);

            ecs.addNode(10, "FIFO");
            ecs.addNode(10, "FIFO");
            ecs.addNode(10, "FIFO");

            ecs.shutDown();
        } catch (Exception e){
//            ecs.shutDown();
            e.printStackTrace();
        }
        logger.debug("End add node replication test ... ");
    }

}