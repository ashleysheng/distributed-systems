package testing;

import app_kvEcs.ECS;
import client.KVStore;
import java.util.ArrayList;
import java.util.Collections;
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
import java.util.Comparator;

/**
 * Created by blade on 2017-03-25.
 */
public class AddNodeTestAssert extends TestCase {

    private ECS ecs;
    private MD5Hasher hash;
    private String expectedServerHash,
            expectedLocalKeyValuePath,
            expectedFirstSuccessorKeyValuePath,
            expectedSecondSuccessorKeyValuePath,
            key,
            value;
    private int numServers = 5;
    private int numAddedServers = 3;
    private ArrayList<String> serverHashValues = new ArrayList<>(numServers);
    private ArrayList<String> serverHashValuesNew = new ArrayList<>(numServers+numAddedServers);
    private ArrayList<String> resultPaths = new ArrayList<>(3);
    private static Logger logger = Logger.getRootLogger();
    File dir;

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

        dir = new File("src/storage/");
        logger.debug("Begin add node replication test ... ");
        ecs.initService(5,10,"FIFO");
        for (int i = 0; i < numServers; i++) {
            serverHashValues.add(hash.hashString("127.0.0.15000" + Integer.toString(i)));
        }
        Collections.sort(serverHashValues, new Comparator<String>(){
            public int compare(String s1, String s2){
                return s1.compareTo(s2);
            }
        });

        KVStore client;
        try {

            ecs.start();
            client = new KVStore("127.0.0.1",50000);
            client.connect();

            key = "6";
            value = "6";
            expectedServerHash = getServerHash(key, serverHashValues);
            expectedLocalKeyValuePath = "src/storage/" + getServerHash(key, serverHashValues) + "/" + getServerHash(key, serverHashValues) + "/" + key;
            expectedFirstSuccessorKeyValuePath = "src/storage/" + getSuccessors(getServerHash(key, serverHashValues), serverHashValues).get(0) + "/" + getServerHash(key, serverHashValues) + "/" + key;
            expectedSecondSuccessorKeyValuePath = "src/storage/" + getSuccessors(getServerHash(key, serverHashValues), serverHashValues).get(1) + "/" + getServerHash(key, serverHashValues) + "/" + key;
            client.put(key, value);
            logger.debug("Attempt to insert <" + key + "," + value + "> into storage server.");
            logger.debug("Checking result ... ");
            logger.debug("Expected server hash for <" + key + "," + value + "> in " + expectedServerHash);
            logger.debug("Expecting to find <" + key + "," + value + "> in " + expectedLocalKeyValuePath);
            logger.debug("Expecting to find <" + key + "," + value + "> in " + expectedFirstSuccessorKeyValuePath);
            logger.debug("Expecting to find <" + key + "," + value + "> in " + expectedSecondSuccessorKeyValuePath);
            searchDir(dir, getServerHash(key, serverHashValues), value);
            assertPaths();
            resultPaths.clear();

            key = "xxx";
            value = "xxx";
            expectedServerHash = getServerHash(key, serverHashValues);
            expectedLocalKeyValuePath = "src/storage/" + getServerHash(key, serverHashValues) + "/" + getServerHash(key, serverHashValues) + "/" + key;
            expectedFirstSuccessorKeyValuePath = "src/storage/" + getSuccessors(getServerHash(key, serverHashValues), serverHashValues).get(0) + "/" + getServerHash(key, serverHashValues) + "/" + key;
            expectedSecondSuccessorKeyValuePath = "src/storage/" + getSuccessors(getServerHash(key, serverHashValues), serverHashValues).get(1) + "/" + getServerHash(key, serverHashValues) + "/" + key;
            client.put(key, value);
            logger.debug("Attempt to insert <" + key + "," + value + "> into storage server.");
            logger.debug("Checking result ... ");
            logger.debug("Expected server hash for <" + key + "," + value + "> in " + expectedServerHash);
            logger.debug("Expecting to find <" + key + "," + value + "> in " + expectedLocalKeyValuePath);
            logger.debug("Expecting to find <" + key + "," + value + "> in " + expectedFirstSuccessorKeyValuePath);
            logger.debug("Expecting to find <" + key + "," + value + "> in " + expectedSecondSuccessorKeyValuePath);
            searchDir(dir, getServerHash(key, serverHashValues), value);
            assertPaths();
            resultPaths.clear();

            key = "yes";
            value = "yes";
            expectedServerHash = getServerHash(key, serverHashValues);
            expectedLocalKeyValuePath = "src/storage/" + getServerHash(key, serverHashValues) + "/" + getServerHash(key, serverHashValues) + "/" + key;
            expectedFirstSuccessorKeyValuePath = "src/storage/" + getSuccessors(getServerHash(key, serverHashValues), serverHashValues).get(0) + "/" + getServerHash(key, serverHashValues) + "/" + key;
            expectedSecondSuccessorKeyValuePath = "src/storage/" + getSuccessors(getServerHash(key, serverHashValues), serverHashValues).get(1) + "/" + getServerHash(key, serverHashValues) + "/" + key;
            client.put(key, value);
            logger.debug("Attempt to insert <" + key + "," + value + "> into storage server.");
            logger.debug("Checking result ... ");
            logger.debug("Expected server hash for <" + key + "," + value + "> in " + expectedServerHash);
            logger.debug("Expecting to find <" + key + "," + value + "> in " + expectedLocalKeyValuePath);
            logger.debug("Expecting to find <" + key + "," + value + "> in " + expectedFirstSuccessorKeyValuePath);
            logger.debug("Expecting to find <" + key + "," + value + "> in " + expectedSecondSuccessorKeyValuePath);
            searchDir(dir, getServerHash(key, serverHashValues), value);
            assertPaths();
            resultPaths.clear();

            key = "no";
            value = "no";
            expectedServerHash = getServerHash(key, serverHashValues);
            expectedLocalKeyValuePath = "src/storage/" + getServerHash(key, serverHashValues) + "/" + getServerHash(key, serverHashValues) + "/" + key;
            expectedFirstSuccessorKeyValuePath = "src/storage/" + getSuccessors(getServerHash(key, serverHashValues), serverHashValues).get(0) + "/" + getServerHash(key, serverHashValues) + "/";
            expectedSecondSuccessorKeyValuePath = "src/storage/" + getSuccessors(getServerHash(key, serverHashValues), serverHashValues).get(1) + "/" + getServerHash(key, serverHashValues) + "/";
            client.put(key, value);
            logger.debug("Attempt to insert <" + key + "," + value + "> into storage server.");
            logger.debug("Checking result ... ");
            logger.debug("Expected server hash for <" + key + "," + value + "> in " + expectedServerHash);
            logger.debug("Expecting to find <" + key + "," + value + "> in " + expectedLocalKeyValuePath);
            logger.debug("Expecting to find <" + key + "," + value + "> in " + expectedFirstSuccessorKeyValuePath);
            logger.debug("Expecting to find <" + key + "," + value + "> in " + expectedSecondSuccessorKeyValuePath);
            searchDir(dir, getServerHash(key, serverHashValues), value);
            assertPaths();
            resultPaths.clear();

            key = "umm";
            value = "umm";
            expectedServerHash = getServerHash(key, serverHashValues);
            expectedLocalKeyValuePath = "src/storage/" + getServerHash(key, serverHashValues) + "/" + getServerHash(key, serverHashValues) + "/" + key;
            expectedFirstSuccessorKeyValuePath = "src/storage/" + getSuccessors(getServerHash(key, serverHashValues), serverHashValues).get(0) + "/" + getServerHash(key, serverHashValues) + "/" + key;
            expectedSecondSuccessorKeyValuePath = "src/storage/" + getSuccessors(getServerHash(key, serverHashValues), serverHashValues).get(1) + "/" + getServerHash(key, serverHashValues) + "/" + key;
            client.put(key, value);
            logger.debug("Attempt to insert <" + key + "," + value + "> into storage server.");
            logger.debug("Checking result ... ");
            logger.debug("Expected server hash for <" + key + "," + value + "> in " + expectedServerHash);
            logger.debug("Expecting to find <" + key + "," + value + "> in " + expectedLocalKeyValuePath);
            logger.debug("Expecting to find <" + key + "," + value + "> in " + expectedFirstSuccessorKeyValuePath);
            logger.debug("Expecting to find <" + key + "," + value + "> in " + expectedSecondSuccessorKeyValuePath);
            searchDir(dir, getServerHash(key, serverHashValues), value);
            assertPaths();
            resultPaths.clear();

            key = "hello";
            value = "hello";
            expectedServerHash = getServerHash(key, serverHashValues);
            expectedLocalKeyValuePath = "src/storage/" + getServerHash(key, serverHashValues) + "/" + getServerHash(key, serverHashValues) + "/";
            expectedFirstSuccessorKeyValuePath = "src/storage/" + getSuccessors(getServerHash(key, serverHashValues), serverHashValues).get(0) + "/" + getServerHash(key, serverHashValues) + "/" + key;
            expectedSecondSuccessorKeyValuePath = "src/storage/" + getSuccessors(getServerHash(key, serverHashValues), serverHashValues).get(1) + "/" + getServerHash(key, serverHashValues) + "/" + key;
            client.put(key, value);
            logger.debug("Attempt to insert <" + key + "," + value + "> into storage server.");
            logger.debug("Checking result ... ");
            logger.debug("Expected server hash for <" + key + "," + value + "> in " + expectedServerHash);
            logger.debug("Expecting to find <" + key + "," + value + "> in " + expectedLocalKeyValuePath);
            logger.debug("Expecting to find <" + key + "," + value + "> in " + expectedFirstSuccessorKeyValuePath);
            logger.debug("Expecting to find <" + key + "," + value + "> in " + expectedSecondSuccessorKeyValuePath);
            searchDir(dir, getServerHash(key, serverHashValues), value);
            assertPaths();
            resultPaths.clear();

            ecs.addNode(10, "FIFO");
            ecs.addNode(10, "FIFO");
            ecs.addNode(10, "FIFO");

            numServers = numServers + numAddedServers;
            serverHashValuesNew = new ArrayList<>(numServers);
            for (int i = 0; i < numServers; i++) {
                serverHashValuesNew.add(i,hash.hashString("127.0.0.15000" + i));
            }
            Collections.sort(serverHashValuesNew, new Comparator<String>(){
                public int compare(String s1, String s2){
                    return s1.compareTo(s2);
                }
            });

            key = "6";
            value = "6";
            expectedServerHash = getServerHash(key, serverHashValuesNew);
            expectedLocalKeyValuePath = "src/storage/" + getServerHash(key, serverHashValuesNew) + "/" + getServerHash(key, serverHashValuesNew) + "/";
            expectedFirstSuccessorKeyValuePath = "src/storage/" + getSuccessors(getServerHash(key, serverHashValuesNew), serverHashValuesNew).get(0) + "/" + getServerHash(key, serverHashValuesNew) + "/" + key;
            expectedSecondSuccessorKeyValuePath = "src/storage/" + getSuccessors(getServerHash(key, serverHashValuesNew), serverHashValuesNew).get(1) + "/" + getServerHash(key, serverHashValuesNew) + "/" + key;
            logger.debug("Expected server hash for <" + key + "," + value + "> in " + expectedServerHash);
            logger.debug("Expecting to find <" + key + "," + value + "> in " + expectedLocalKeyValuePath);
            logger.debug("Expecting to find <" + key + "," + value + "> in " + expectedFirstSuccessorKeyValuePath);
            logger.debug("Expecting to find <" + key + "," + value + "> in " + expectedSecondSuccessorKeyValuePath);
            searchDir(dir, getServerHash(key, serverHashValuesNew), value);
            assertPaths();
            resultPaths.clear();


            key = "xxx";
            value = "xxx";
            expectedServerHash = getServerHash(key, serverHashValuesNew);
            expectedLocalKeyValuePath = "src/storage/" + getServerHash(key, serverHashValuesNew) + "/" + getServerHash(key, serverHashValuesNew) + "/";
            expectedFirstSuccessorKeyValuePath = "src/storage/" + getSuccessors(getServerHash(key, serverHashValuesNew), serverHashValuesNew).get(0) + "/" + getServerHash(key, serverHashValuesNew) + "/" + key;
            expectedSecondSuccessorKeyValuePath = "src/storage/" + getSuccessors(getServerHash(key, serverHashValuesNew), serverHashValuesNew).get(1) + "/" + getServerHash(key, serverHashValuesNew) + "/" + key;
            logger.debug("Expected server hash for <" + key + "," + value + "> in " + expectedServerHash);
            logger.debug("Expecting to find <" + key + "," + value + "> in " + expectedLocalKeyValuePath);
            logger.debug("Expecting to find <" + key + "," + value + "> in " + expectedFirstSuccessorKeyValuePath);
            logger.debug("Expecting to find <" + key + "," + value + "> in " + expectedSecondSuccessorKeyValuePath);
            searchDir(dir, getServerHash(key, serverHashValuesNew), value);
            assertPaths();
            resultPaths.clear();

            key = "yes";
            value = "yes";
            expectedServerHash = getServerHash(key, serverHashValuesNew);
            expectedLocalKeyValuePath = "src/storage/" + getServerHash(key, serverHashValuesNew) + "/" + getServerHash(key, serverHashValuesNew) + "/";
            expectedFirstSuccessorKeyValuePath = "src/storage/" + getSuccessors(getServerHash(key, serverHashValuesNew), serverHashValuesNew).get(0) + "/" + getServerHash(key, serverHashValuesNew) + "/" + key;
            expectedSecondSuccessorKeyValuePath = "src/storage/" + getSuccessors(getServerHash(key, serverHashValuesNew), serverHashValuesNew).get(1) + "/" + getServerHash(key, serverHashValuesNew) + "/" + key;
            logger.debug("Expected server hash for <" + key + "," + value + "> in " + expectedServerHash);
            logger.debug("Expecting to find <" + key + "," + value + "> in " + expectedLocalKeyValuePath);
            logger.debug("Expecting to find <" + key + "," + value + "> in " + expectedFirstSuccessorKeyValuePath);
            logger.debug("Expecting to find <" + key + "," + value + "> in " + expectedSecondSuccessorKeyValuePath);
            searchDir(dir, getServerHash(key, serverHashValuesNew), value);
            assertPaths();
            resultPaths.clear();

            key = "no";
            value = "no";
            expectedServerHash = getServerHash(key, serverHashValuesNew);
            expectedLocalKeyValuePath = "src/storage/" + getServerHash(key, serverHashValuesNew) + "/" + getServerHash(key, serverHashValuesNew) + "/";
            expectedFirstSuccessorKeyValuePath = "src/storage/" + getSuccessors(getServerHash(key, serverHashValuesNew), serverHashValuesNew).get(0) + "/" + getServerHash(key, serverHashValuesNew) + "/" + key;
            expectedSecondSuccessorKeyValuePath = "src/storage/" + getSuccessors(getServerHash(key, serverHashValuesNew), serverHashValuesNew).get(1) + "/" + getServerHash(key, serverHashValuesNew) + "/" + key;
            logger.debug("Expected server hash for <" + key + "," + value + "> in " + expectedServerHash);
            logger.debug("Expecting to find <" + key + "," + value + "> in " + expectedLocalKeyValuePath);
            logger.debug("Expecting to find <" + key + "," + value + "> in " + expectedFirstSuccessorKeyValuePath);
            logger.debug("Expecting to find <" + key + "," + value + "> in " + expectedSecondSuccessorKeyValuePath);
            searchDir(dir, getServerHash(key, serverHashValuesNew), value);
            assertPaths();
            resultPaths.clear();

            key = "umm";
            value = "umm";
            expectedServerHash = getServerHash(key, serverHashValuesNew);
            expectedLocalKeyValuePath = "src/storage/" + getServerHash(key, serverHashValuesNew) + "/" + getServerHash(key, serverHashValuesNew) + "/";
            expectedFirstSuccessorKeyValuePath = "src/storage/" + getSuccessors(getServerHash(key, serverHashValuesNew), serverHashValuesNew).get(0) + "/" + getServerHash(key, serverHashValuesNew) + "/" + key;
            expectedSecondSuccessorKeyValuePath = "src/storage/" + getSuccessors(getServerHash(key, serverHashValuesNew), serverHashValuesNew).get(1) + "/" + getServerHash(key, serverHashValuesNew) + "/" + key;
            logger.debug("Expected server hash for <" + key + "," + value + "> in " + expectedServerHash);
            logger.debug("Expecting to find <" + key + "," + value + "> in " + expectedLocalKeyValuePath);
            logger.debug("Expecting to find <" + key + "," + value + "> in " + expectedFirstSuccessorKeyValuePath);
            logger.debug("Expecting to find <" + key + "," + value + "> in " + expectedSecondSuccessorKeyValuePath);
            searchDir(dir, getServerHash(key, serverHashValuesNew), value);
            assertPaths();
            resultPaths.clear();

            key = "hello";
            value = "hello";
            expectedServerHash = getServerHash(key, serverHashValuesNew);
            expectedLocalKeyValuePath = "src/storage/" + getServerHash(key, serverHashValuesNew) + "/" + getServerHash(key, serverHashValuesNew) + "/";
            expectedFirstSuccessorKeyValuePath = "src/storage/" + getSuccessors(getServerHash(key, serverHashValuesNew), serverHashValuesNew).get(0) + "/" + getServerHash(key, serverHashValuesNew) + "/" + key;
            expectedSecondSuccessorKeyValuePath = "src/storage/" + getSuccessors(getServerHash(key, serverHashValuesNew), serverHashValuesNew).get(1) + "/" + getServerHash(key, serverHashValuesNew) + "/" + key;
            logger.debug("Expected server hash for <" + key + "," + value + "> in " + expectedServerHash);
            logger.debug("Expecting to find <" + key + "," + value + "> in " + expectedLocalKeyValuePath);
            logger.debug("Expecting to find <" + key + "," + value + "> in " + expectedFirstSuccessorKeyValuePath);
            logger.debug("Expecting to find <" + key + "," + value + "> in " + expectedSecondSuccessorKeyValuePath);
            searchDir(dir, getServerHash(key, serverHashValuesNew), value);
            assertPaths();
            resultPaths.clear();


            ecs.shutDown();
        } catch (Exception e){
            e.printStackTrace();
        } finally {
            ecs.shutDown();
        }
        logger.debug("End add node replication test ... ");
    }

    public void searchDir(File f, String hashCode, String value) {
        if (f.isDirectory()) {
            for (File c : f.listFiles()) {
                searchDir(c, hashCode, value);
            }
        }
        else {
            try {
                BufferedReader reader = new BufferedReader(new FileReader(f));
                if (reader.readLine().equals(value)) {
                    String path = f.getAbsolutePath();
                    logger.info("Found <" + key + ", " + value + "> in file " + f.getAbsolutePath());
                    resultPaths.add(path.substring(path.indexOf(hashCode)));
                }
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
    }

    public ArrayList<String> getSuccessors(String selfHashValue, ArrayList<String> hashValues) {
        ArrayList<String> successors = new ArrayList<>(2);
        int selfIndex = hashValues.indexOf(selfHashValue);
        if (selfIndex == numServers - 1){
            successors.add(hashValues.get(0));
            successors.add(hashValues.get(1));
        }
        else if (selfIndex == numServers - 2) {
            successors.add(hashValues.get(numServers - 1));
            successors.add(hashValues.get(0));
        }
        else {
            successors.add(hashValues.get(selfIndex+1));
            successors.add(hashValues.get(selfIndex+2));
        }
        return successors;
    }

    public String getServerHash(String key, ArrayList<String> hashValues) {
        for (int i = 0; i < numServers; i++) {
            if (hashValues.get(i).compareTo(hash.hashString(key)) >= 0) {
                return hashValues.get(i);
            }
        }
        return hashValues.get(0);

        /*The result is a negative integer if this String object
        lexicographically precedes the argument string. The result
        is a positive integer if this String object lexicographically
        follows the argument string. The result is zero if the
        strings are equal; compareTo returns 0 exactly when the
        equals(Object) method would return true.*/
    }

    public void assertPaths() {
        for (int i = 0; i < resultPaths.size(); i++) {
            assert(
                    resultPaths.get(i).equals(expectedLocalKeyValuePath) ||
                            resultPaths.get(i).equals(expectedFirstSuccessorKeyValuePath) ||
                            resultPaths.get(i).equals(expectedSecondSuccessorKeyValuePath)
            );
        }
    }

}