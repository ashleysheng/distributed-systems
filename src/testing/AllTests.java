package testing;

import java.io.IOException;

import app_kvServer.ClientConnection;
import org.apache.log4j.Level;

import app_kvServer.KVServer;
import junit.framework.Test;
import junit.framework.TestSuite;
import logger.LogSetup;


public class AllTests {

	static {
		try {
			new LogSetup("logs/testing/test.log", Level.ERROR);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	public static Test suite() {
		TestSuite clientSuite = new TestSuite("Basic Storage ServerTest-Suite");

		// new tests for m3
		clientSuite.addTestSuite(AddNodeTest.class);
		clientSuite.addTestSuite(RemoveNodeTest.class);
		clientSuite.addTestSuite(CacheReplicationTest.class);
		clientSuite.addTestSuite(HeartbeatTest.class);
		clientSuite.addTestSuite(NetworkTopology.class);
		// Expected exceptions will be thrown in ReconcileTest.
		clientSuite.addTestSuite(ReconcileTest.class);


		// old tests from m2
		clientSuite.addTestSuite(ECSUnitTest.class);
		clientSuite.addTestSuite(ECSServerConnectionTest.class);
		clientSuite.addTestSuite(ClientServerConnectionTest.class);
		clientSuite.addTestSuite(MetadataTest.class);

		// old tests from m1
		clientSuite.addTestSuite(FIFOCacheTest.class);
		clientSuite.addTestSuite(LRUCacheTest.class);
		clientSuite.addTestSuite(LFUCacheTest.class);
//		clientSuite.addTestSuite(ConnectionTest.class);


		return clientSuite;
	}
	
}
