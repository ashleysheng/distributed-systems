package common;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MD5Hasher {
	public String hashString(String in){
    	try{
        	MessageDigest m = MessageDigest.getInstance("MD5");
        	m.reset();
        	m.update(in.getBytes());
        	byte[] digest = m.digest();
        	BigInteger bigInt = new BigInteger(1,digest);
        	String hashtext = bigInt.toString(16);
        	
        	return hashtext;
    	} catch(NoSuchAlgorithmException e){
    		e.printStackTrace();
    	}
    	return null;
    }
}
