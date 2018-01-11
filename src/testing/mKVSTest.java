package testing;

import app_kvEcs.mKVS;
import common.Metadata;
import common.communication.ECSCommModule;
import common.messages.KVAdminMsg;
import junit.framework.TestCase;

import java.io.IOException;
import java.net.UnknownHostException;

import logger.LogSetup;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

public class mKVSTest extends TestCase {
    private mKVS controller;
    private Metadata meta;

    protected void setUp() {
    	try{
        	new LogSetup("test/mkvs.log", Level.ALL);
    	} catch (IOException ioe){
    		ioe.printStackTrace();
    	}
    	Logger logger = Logger.getRootLogger();
        controller= new mKVS("Server1","127.0.0.1",50000);
        meta = new Metadata();
        meta.addToMeta("aabc1334567","127.0.0.1",Integer.toString(50000));
    }

    public void testInit(){

        controller.initKVServer(meta.stringify(), 10, "FIFO");
        try{
        	Thread.sleep(1000);
        } catch(Exception e){
        	e.printStackTrace();
        }
        
        KVAdminMsg receive=null;
        try{
            ECSCommModule comm = new ECSCommModule("127.0.0.1",50000);
            KVAdminMsg message = new KVAdminMsg(
                    controller.getPassword(),
                    "values",
                    KVAdminMsg.StatusType.START
            );

            comm.sendMessage(message);
            receive = comm.receiveMessage();

            System.out.println(receive.getKey());
        } catch(UnknownHostException e){
            e.printStackTrace();
        } catch(Exception e){
            e.printStackTrace();
        }
        
        assertEquals(receive.getKey(),controller.getPassword());

    }
}
