package testing;

import common.MD5Hasher;
import common.Metadata;
import junit.framework.TestCase;
import logger.LogSetup;
import org.apache.log4j.Level;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

public class MetadataTest extends TestCase {

    private Metadata meta;
    private String m1;
    private String big;

    protected void setUp() {

        try{
            new LogSetup("test/metadatatest.log", Level.ALL);
        } catch (IOException ioe){
            ioe.printStackTrace();
        }

        meta = new Metadata();
        meta.addToMeta("1123abbacde","127.0.0.1","50000");
        meta.addToMeta("66666666666","127.1.1.1","50001");

        m1= "1111,google.com,50000"+"::"+
                "2222,google.com,50000"+"::"+
                "5555,google.com,50000"+"::"+
                "aabd,google.com,50000"+"::"+
                "8765,google.com,50000";

        big = "1111,google.com,50000"+"::"+
                "2222,google.com,50000"+"::"+
                "3333,google.com,50000"+"::"+
                "4444,google.com,50000"+"::"+
                "5555,google.com,50000"+"::"+
                "aaabc,google.com,50000"+"::"+
                "aaabd,google.com,50000"+"::"+
                "ffff,google.com,50000"+"::"+
                "8765,google.com,50000";
    }

    @Test
    public void testStringify() throws Exception {
        String parsed=meta.stringify();
        String expected="1123abbacde,127.0.0.1,50000::66666666666,127.1.1.1,50001";
        assertEquals(expected,parsed);
    }

    @Test
    public void testUpdate() throws Exception {
        String m1 = "1123abbacde,127.0.0.1,44444::66666666666,127.1.1.1,55555";
        meta.update(m1);
        assertEquals(m1,meta.stringify());

        m1="02989,127.0.0.1,50000::2341543,127.1.1.1,50001::29834u5t4392,127.0.0.1,23414";
        meta.update(m1);
        assertEquals(m1,meta.stringify());
    }

    @Test
    public void testOrdering() {
        meta.update(big);

        String actual = meta.order.toString();
        String expected = "[1111, 2222, 3333, 4444, 5555, 8765, aaabc, aaabd, ffff]";
        assertEquals(expected,actual);
    }

    @Test
    public void testSetLowerBound() throws Exception{
        meta.update(m1);

        meta.setSelf("8765");
        meta.setLowerBound();
        assertEquals("5555",meta.getLowerBound());

        meta.setSelf("1111");
        meta.setLowerBound();
        assertEquals("aabd",meta.getLowerBound());
    }

    @Test
    public void testIsResponsible() throws Exception {

        meta.update(m1);

        meta.setSelf("aabd");
        meta.setLowerBound();

        assertTrue(meta.isResponsible("aaaa"));
        assertTrue(meta.isResponsible("9012"));
        assertTrue(!meta.isResponsible("2346"));

        meta.setSelf("1111");
        meta.setLowerBound();

        assertTrue(meta.isResponsible("0123"));
        assertTrue(meta.isResponsible("ffff"));
        assertTrue(!meta.isResponsible("2346"));
    }

    @Test
    public void testFindResponsible() throws Exception {
        MD5Hasher md5 = new MD5Hasher();

        String actual_hashes = "b59c67bf196a4758191e42f76670ceba"+",google.com,50000::"+
                "934b535800b1cba8f96a5d72f72f1611"+",google.com,50001::"+
                "6074c6aa3488f3c2dddff2a7ca821aab"+",google.com,50002::"+
                "2be9bd7a3434f7038ca27d1918de58bd"+",google.com,50003";

        meta.update(actual_hashes);

        String[] results;
        System.out.println("5555: "+md5.hashString("5555"));
        results = meta.findResponsibleServer("5555");
        System.out.println(results[1]);
        System.out.println("6666: "+md5.hashString("6666"));
        results = meta.findResponsibleServer("6666");
        System.out.println(results[1]);
        System.out.println("7777: "+md5.hashString("7777"));
        results = meta.findResponsibleServer("7777");
        System.out.println(results[1]);
        System.out.println("qwe: "+md5.hashString("qwe"));
        results = meta.findResponsibleServer("qwe");
        System.out.println(results[1]);
    }

}