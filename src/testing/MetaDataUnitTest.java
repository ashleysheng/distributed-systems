package testing;

import app_kvEcs.mKVS;
import common.Metadata;
import junit.framework.TestCase;
import logger.LogSetup;
import org.apache.log4j.Level;

import java.io.IOException;

public class MetaDataUnitTest extends TestCase {
    private Metadata meta;
    protected void setUp() {

        try{
            new LogSetup("test/metadatatest.log", Level.ALL);
        } catch (IOException ioe){
            ioe.printStackTrace();
        }

        meta = new Metadata();
        meta.addToMeta("1123abbacde","127.0.0.1","50000");
        meta.addToMeta("66666666666","127.1.1.1","50001");
    }

    public void testParsing(){
        String parsed=meta.stringify();
        String expected="1123abbacde,127.0.0.1,50000::66666666666,127.1.1.1,50001";
        assertEquals(expected,parsed);
    }

    public void testUpdate(){
        meta.update("1123abbacde,127.0.0.1,44444::66666666666,127.1.1.1,55555");
        System.out.println(meta.stringify());

        meta.update( "02989,127.0.0.1,50000::2341543,127.1.1.1,50001::29834u5t4392,127.0.0.1,23414");
        System.out.println(meta.stringify());

    }
}
