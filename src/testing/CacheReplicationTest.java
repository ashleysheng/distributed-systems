package testing;

import app_kvEcs.ECS;
import client.KVStore;
import junit.framework.TestCase;
import logger.LogSetup;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Test;
import common.MD5Hasher;

import java.io.FileReader;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;

/**
 * Created by blade on 2017-03-25.
 */
public class CacheReplicationTest extends TestCase {

    private ECS ecs;
    private MD5Hasher hash;
    private String expectedServerHash, key, value;
    private static Logger logger = Logger.getRootLogger();
    File dir;

    protected void setUp() {
        try {
            LogSetup logger = new LogSetup("test/cacheReplicationTest.log", Level.ALL);

        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        ecs = new ECS("ecs.config");
    }

    public void searchDir(File f, String value) {
        if (f.isDirectory()) {
            for (File c : f.listFiles()) {
                searchDir(c, value);
            }
        }
        else {
            try {
                BufferedReader reader = new BufferedReader(new FileReader(f));
                if (reader.readLine().equals(value)) {
                    logger.info("Found value in file " + f.getAbsolutePath());
                }
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }

    }

    @Test
    public void testPutKeyValue() {
        dir = new File("src/storage/");
        logger.debug("Begin cache replication test ... ");
        ecs.initService(5,10,"FIFO");
        KVStore client;
        try {
            ecs.start();
            client = new KVStore("127.0.0.1",50000);
            client.connect();

            key = "hello";
            value = "hello";

            client.put(key, value);
            logger.debug("Attempt to insert <" + key + "," + value + "> into storage server.");
            logger.debug("Checking result ... ");
            searchDir(dir, value);


            key = "bye";
            value = "bye";

            client.put(key, value);
            logger.debug("Attempt to insert <" + key + "," + value + "> into storage server.");
            logger.debug("Checking result ... ");
            searchDir(dir, value);

            key = "yes";
            value = "yes";
            client.put(key, value);
            logger.debug("Attempt to insert <" + key + "," + value + "> into storage server.");
            logger.debug("Checking result ... ");
            searchDir(dir, value);


            key = "no";
            value = "no";
            client.put(key, value);
            logger.debug("Attempt to insert <" + key + "," + value + "> into storage server.");
            logger.debug("Checking result ... ");
            searchDir(dir, value);

            key = "abc";
            value = "abc";

            client.put(key, value);
            logger.debug("Attempt to insert <" + key + "," + value + "> into storage server.");
            logger.debug("Checking result ... ");
            searchDir(dir, value);
            ecs.shutDown();
        } catch (Exception e){
            ecs.shutDown();
            e.printStackTrace();
        }
        logger.debug("End cache replication test ... ");
    }

}
