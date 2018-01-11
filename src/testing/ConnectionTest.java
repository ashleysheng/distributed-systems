package testing;

import java.io.IOException;
import java.net.UnknownHostException;

import app_kvEcs.ECS;
import client.KVStore;

import junit.framework.TestCase;
import logger.LogSetup;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;


public class ConnectionTest extends TestCase {

	private ECS ecs;
	private String key, value;
	private static Logger logger = Logger.getRootLogger();

	protected void setUp() {
		try {
			LogSetup logger = new LogSetup("test/connectionTest.log", Level.ALL);

		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
		ecs = new ECS("ecs.config");
	}

	public void testConnection(){
		ecs.initService(5,10,"FIFO");
		ecs.start();

		Exception ex = null;

		KVStore kvClient = new KVStore("localhost", 50000);
		try {
			kvClient.connect();
		} catch (Exception e) {
			ex = e;
		}

		assertNull(ex);

		ex = null;
		kvClient = new KVStore("unknown", 50000);

		try {
			kvClient.connect();
		} catch (Exception e) {
			ex = e;
		}

		assertTrue(ex instanceof UnknownHostException);

		ex = null;
		kvClient = new KVStore("unknown", 50000);

		try {
			kvClient.connect();
		} catch (Exception e) {
			ex = e;
		}

		assertTrue(ex instanceof UnknownHostException);

		ex = null;
		kvClient = new KVStore("localhost", 123456789);

		try {
			kvClient.connect();
		} catch (Exception e) {
			ex = e;
		}

		assertTrue(ex instanceof IllegalArgumentException);

		ex = null;
		kvClient = new KVStore("127.0.0.1", 50000);
		try {
			kvClient.connect();
			kvClient.disconnect();
			kvClient.init("127.0.0.1", 50001);
			kvClient.connect();
		} catch (Exception e) {
			ex = e;
		}

		assertNull(ex);

		ecs.shutDown();
	}

//	public void testConnectionSuccess() {
//
//		Exception ex = null;
//
//		KVStore kvClient = new KVStore("localhost", 50000);
//		try {
//			kvClient.connect();
//		} catch (Exception e) {
//			ex = e;
//		}
//
//		assertNull(ex);
//	}


//	public void testUnknownHost() {
//		Exception ex = null;
//		KVStore kvClient = new KVStore("unknown", 50000);
//
//		try {
//			kvClient.connect();
//		} catch (Exception e) {
//			ex = e;
//		}
//
//		assertTrue(ex instanceof UnknownHostException);
//	}


//	public void testIllegalPort() {
//		Exception ex = null;
//		KVStore kvClient = new KVStore("localhost", 123456789);
//
//		try {
//			kvClient.connect();
//		} catch (Exception e) {
//			ex = e;
//		}
//
//		assertTrue(ex instanceof IllegalArgumentException);
//	}
//
//	public void testDisconnect(){        //TODO: CHANGE THIS LATER WE NEED TWO SERVERS
//
//		Exception ex = null;
//		KVStore kvClient = new KVStore("127.0.0.1", 50000);
//		try {
//			kvClient.connect();
//			kvClient.disconnect();
//			kvClient.init("127.0.0.1", 50001);
//			kvClient.connect();
//		} catch (Exception e) {
//			ex = e;
//		}
//
//		assertNull(ex);
//	}
}