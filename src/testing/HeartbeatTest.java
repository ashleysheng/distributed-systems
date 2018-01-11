package testing;

import app_kvEcs.ECS;
import client.KVStore;
import junit.framework.TestCase;
import logger.LogSetup;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Test;

import java.io.*;

/**
 * Created by blade on 2017-03-25.
 */
public class HeartbeatTest extends TestCase {

    private ECS ecs;
    private static Logger logger = Logger.getRootLogger();
    private static String logsPath = "logs/";
    private static File logsDir = new File(logsPath);

    protected void setUp() {
        try {
            LogSetup logger = new LogSetup("test/HeartbeatTest.log", Level.ALL);

        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        ecs = new ECS("ecs.config");
    }

    @Test
    public void testHeartbeat() {

        logger.debug("Begin add node replication test ... ");
        ecs.initService(5,10,"FIFO");

        KVStore client;
        try {
            ecs.start();
            logger.info("Waiting 20 seconds for heartbeats occur");
            Thread.sleep(20000);
            ecs.stop();
            ecs.shutDown();
        } catch (Exception e){
            ecs.stop();
            ecs.shutDown();
            e.printStackTrace();
        } finally {
            ecs.stop();
            ecs.shutDown();
        }

        try {
            for (File f : logsDir.listFiles()) {

                if(!f.isDirectory()){
                    BufferedReader reader = new BufferedReader(new FileReader(f));
                    try {
                        for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                            if (line.contains("heartbeat")) {
                                logger.debug("Server with hash code " +
                                    f.getAbsolutePath().substring(f.getAbsolutePath().indexOf("_"),
                                            f.getAbsolutePath().indexOf(".")) +
                                    " logged ACK for Heartbeat");
                                //logger.debug("Found log of ACK for Heartbeat");

                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();

                    }}

            }
        } catch (FileNotFoundException f) {
            f.printStackTrace();
        }

        logger.debug("End add node replication test ... ");
    }

}