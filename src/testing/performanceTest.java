package testing;
import app_kvEcs.ECS;
import app_kvServer.KVServer;
import client.KVStore;
import common.communication.ECSCommModule;
import junit.framework.TestCase;
import logger.LogSetup;
import org.apache.log4j.Level;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

/**
 * Created by ashleysheng on 2017-03-25.
 */
public class performanceTest extends TestCase {


    private ECS ecs;

    protected void setUp() {

        try{
            new LogSetup("test/metadatatest.log", Level.ERROR);
        } catch (IOException ioe){
            ioe.printStackTrace();
        }
        ecs=new ECS("ecs_large.config");
    }

    @Test
    public void testPerform() throws Exception {
        int numberOfServer = 20;
        int numberOfClients = 5;

        ecs.initService(numberOfServer,5,"FIFO");
        List<KVStore> client_list = new ArrayList<KVStore>();
        try {
            ecs.start();

            for (int k = 0;k<numberOfClients;k++) {
                KVStore client = new KVStore("127.0.0.1",50000);
                client.connect();
                client_list.add(client);
            }

            File folder = new File("allen_data/");
            File[] listOfFiles = folder.listFiles();
            int numOfFiles = listOfFiles.length;
            int i = 0;
            while(i<100) {
//                while(i<numOfFiles) {
                for (KVStore client : client_list) {
                    String fileName = listOfFiles[i].getName();
                    if (!fileName.contains("DS_Store")) {
                        Scanner s = new Scanner(new File("allen_data/" + fileName));
                        String content = s.nextLine();
                        fileName = fileName.substring(0, fileName.length() - 4);
                        client.put(fileName, content);
                    }
                    i++;
//                    if(i==8){
//                        ecs.removeNode(1);
//                    }
                }
            }
            long start = System.currentTimeMillis();

//            i=0;
//            Random rand = new Random();
//            while (i<50){
//                int rn = rand.nextInt(100);
//                String fn = listOfFiles[rn].getName();
//                fn = fn.substring(0, fn.length() - 4);
//                client_list.get(0).get(fn);
//                i++;
//            }
//ecs.addNode(5,"FIFO");
//            ecs.addNode(5,"FIFO");
//            ecs.addNode(5,"FIFO");
//            ecs.addNode(5,"FIFO");
//            ecs.addNode(5,"FIFO");

            ecs.removeNode(0);
            ecs.removeNode(0);
            ecs.removeNode(0);
            ecs.removeNode(0);
            ecs.removeNode(0);

            long end = System.currentTimeMillis();
            System.out.println("TIMEEEEEEEE: "+(end-start));

            ecs.shutDown();
        } catch (Exception e){
            ecs.shutDown();
            e.printStackTrace();
        }


    }
}
