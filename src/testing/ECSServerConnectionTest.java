package testing;

import app_kvServer.KVServer;
import common.communication.ECSCommModule;
import common.messages.KVAdminMsg;
import junit.framework.TestCase;
import logger.LogSetup;
import org.apache.log4j.Level;
import org.junit.Test;

import java.io.IOException;
import java.net.UnknownHostException;

/**
 * Created by ashleysheng on 2017-03-04.
 */
public class ECSServerConnectionTest extends TestCase{
    private KVServer server;
    private ECSCommModule comm;
    final Object lock = new Object();
    boolean cv=false;

    protected void setUp(){
        try{
            new LogSetup("test/clientconn.log", Level.ALL);
        } catch (IOException ioe){
            ioe.printStackTrace();
        }
        try {
            server = new KVServer(7777,10,"FIFO","APPLE","549b5ccf4de733a1d6adf578c4648689");
            server.start();
            Thread.sleep(1000);
            comm = new ECSCommModule("127.0.0.1",7777);
        }
        catch (UnknownHostException e){
            e.printStackTrace();
        }
        catch (InterruptedException e){
            e.printStackTrace();
        }
    }
    @Test
    public void testProcessAdmin() throws Exception {

        KVAdminMsg adminMsg = new KVAdminMsg("APPLE","l",KVAdminMsg.StatusType.START);
        comm.sendMessage(adminMsg);
        Thread.sleep(1000);
        KVAdminMsg receive = comm.receive();
        System.out.println(receive.getAdminStatus().toString());
        assert(server.ll.getActive());

        adminMsg.setStatus(KVAdminMsg.StatusType.STOP);
        comm.sendMessage(adminMsg);
        Thread.sleep(1000);
        receive = comm.receive();
        assert(!server.ll.getActive());

        adminMsg.setStatus(KVAdminMsg.StatusType.LOCK_WRITE);
        comm.sendMessage(adminMsg);
        Thread.sleep(1000);
        receive = comm.receive();
        assert(server.ll.getWriteLock());

        adminMsg.setStatus(KVAdminMsg.StatusType.UNLOCK_WRITE);
        comm.sendMessage(adminMsg);
        Thread.sleep(1000);
        receive = comm.receive();
        assert(!server.ll.getWriteLock());


        adminMsg.setValue("ddc3fd80341f8d8f3aa6792ddea83d9e,127.0.0.1,50000::30a6197ecbfce50b497429c2e402cac5,127.1.1.1,50001"+
                "::1752af20beba3cdb5e87f5be2dd3c5de,127.0.0.1,50002"
        );
        adminMsg.setStatus(KVAdminMsg.StatusType.UPDATE);
        comm.sendMessage(adminMsg);
        Thread.sleep(1000);
        System.out.println(server.metadata.stringify());
    }
}