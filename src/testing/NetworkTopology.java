package testing;

import app_kvEcs.ECS;
import junit.framework.TestCase;
import logger.LogSetup;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Test;
import common.MD5Hasher;
import java.util.Arrays;

import java.io.*;
import java.util.ArrayList;

/**
 * Created by blade on 2017-03-25.
 */
public class NetworkTopology extends TestCase {

    private ECS ecs;
    private MD5Hasher MD5;
    private static int num_servers = 8;
    private static Logger logger = Logger.getRootLogger();
    private static String storagePath = "src/storage/";
    private static File storageDir = new File(storagePath);
    private static String[] serverHashCodes;
    private static String absoluteStoragePath = storageDir.getAbsolutePath();

    protected void setUp() {
        try {
            LogSetup logger = new LogSetup("test/NetworkTopologyTest.log", Level.ALL);

        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        ecs = new ECS("ecs.config");
        MD5 = new MD5Hasher();
    }

    @Test
    public void testNetworkTopology() {

        logger.debug("Begin Network Topology test ... ");
        serverHashCodes = new String[num_servers];
        for (int i = 0; i < num_servers; i++) {
            serverHashCodes[i] = MD5.hashString("127.0.0.15000" + Integer.toString(i));
        }
        Arrays.sort(serverHashCodes);

        logger.debug("Order of hash codes: ");
        for (int i = 0; i < num_servers; i++) {
            logger.debug(serverHashCodes[i]);
        }
        ecs.initService(num_servers,10,"FIFO");

        searchDir(storageDir);
        ecs.shutDown();
        logger.debug("End Network Topology test ... ");
    }

    public void searchDir(File f) {
        if (f.isDirectory()) {
//            for (File c : f.listFiles()) {
//                searchDir(c);
//            }

            //KVServer upper storage directory
            if (f.getParentFile().getAbsolutePath().equals(absoluteStoragePath)) {
            }

            //Upper directory
            else if (f.getAbsolutePath().equals(absoluteStoragePath)) {

            }

            //KVServer lower storage directory
            else {
                int hashCodeIndex = f.getAbsolutePath().lastIndexOf("/")+1;
                String hashCode = f.getAbsolutePath().substring(hashCodeIndex);
                int[] predecessorsIndex = new int[2];
                logger.debug(f.getAbsolutePath());
                logger.debug(hashCode);
                logger.debug(serverHashCodes[predecessorsIndex[0]]);
                logger.debug(serverHashCodes[predecessorsIndex[1]]);
//                assert(hashCode.equals(serverHashCodes[predecessorsIndex[0]]) ||
//                    hashCode.equals(serverHashCodes[predecessorsIndex[1]])
//                );

            }
        }
    }

    public static int[] getPredecessorsIndex(String hashCode) {
        int hashCodeIndex = 0;
        int[] ret = new int[2];
        for (int i = 0; i < num_servers; i++) {
            if (serverHashCodes[i].equals(hashCode)) {
                hashCodeIndex = i;
            }
        }
        if (hashCodeIndex == 0) {
            ret[0] = serverHashCodes.length - 1;
            ret[1] = serverHashCodes.length - 2;
        }
        else if (hashCodeIndex == 1) {
            ret[0] = 0;
            ret[1] = serverHashCodes.length - 1;
        }
        else {
            ret[0] = hashCodeIndex - 1;
            ret[1] = hashCodeIndex - 2;
        }

        return ret;
    }

}