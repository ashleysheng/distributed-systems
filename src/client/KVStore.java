package client;


import common.Metadata;
import common.communication.ClientCommModule;
import common.messages.KVMessage;
import common.messages.KVMessageObj;

import java.io.IOException;
import java.net.InterfaceAddress;
import java.net.UnknownHostException;

import org.apache.log4j.Logger;

public class KVStore implements KVCommInterface {

	private String addr;
	private int p;
	private ClientCommModule commModule;
	private static Logger logger = Logger.getRootLogger();
	private Metadata clientMeta;

	/**
	 * Initialize KVStore with address and port of KVServer
	 * @param address the address of the KVServer
	 * @param port the port of the KVServer
	 */
	public KVStore(String address, int port) {
		addr = address;
		p = port;
		clientMeta = new Metadata();
	}

	/**
	 * Initializes empty KVStore that's note connected to a server
	 */
	public KVStore(){
		addr = null;
		p = -1;
		clientMeta = new Metadata();
	}

	/**
	 * Initializes an empty KVStore with server address and port
	 * @param address
	 * 				Server Address
	 * @param port
	 * 				Server port number
	 */
	public void init(String address, int port) {
		this.addr = address;
		this.p = port;
	}

	/**
	 * Try to establish a socket connection with the server
	 * with specified address and port number in the current KVStore
	 * @throws UnknownHostException Throws exception if the address name is unknown
	 */
	@Override
	public void connect()
			throws UnknownHostException {
		commModule = new ClientCommModule(addr, p);
		commModule.start();
	}

	/**
	 * Tries to disconnect from the server and unset all values in KVStore
	 */
	@Override
	public void disconnect() {
		if(commModule!=null) {
			commModule.closeConnection();
			commModule = null;
		}
		addr = null;
		p = -1;
	}

	/**
	 * Use the Communication module to send a PUT message, wait for the communication module to respond
	 * If exception occurs while sending message, will try to disconnect
	 * @param key
	 *            the key that identifies the given value.
	 * @param value
	 *            the value that is indexed by the given key.
	 * @return
	 * 			  the KVMessage with the correct response from the server
	 * @throws Exception
	 * 			  If it cannot properly send the message via the communication module
	 */
	@Override
	synchronized public KVMessage put(String key, String value) throws Exception {
		if(!isConnected()){
			throw new Exception();
		}
		try {
			KVMessageObj temp = null;
			while(temp==null || temp.getStatus().equals(KVMessage.StatusType.SERVER_WRITE_LOCK)){
//				while(temp==null || temp.getStatus().equals(KVMessage.StatusType.SERVER_WRITE_LOCK)
//					||temp.getStatus().equals(KVMessage.StatusType.SERVER_NOT_RESPONSIBLE)){

				if (temp==null || temp.getStatus().equals(KVMessage.StatusType.SERVER_WRITE_LOCK)){
					temp =sendMessage(key,value,"PUT");
				}

				if(temp.getStatus().equals(KVMessage.StatusType.SERVER_NOT_RESPONSIBLE)){
					clientMeta.update(temp.getValue());
					String server[] = clientMeta.findResponsibleServer(key);

					this.disconnect();
					this.init(server[0], Integer.parseInt(server[1]));
					this.connect();
					temp = sendMessage(key,value,"PUT");
				}
			}


			return temp;

		} catch (Exception e) {
			logger.warn("Unable to send message!");
			disconnect();
			return null;
		}

	}

	private KVMessageObj sendMessage(String key, String value, String type) throws Exception{
		KVMessageObj temp;
		if (type.equals("GET")){
			commModule.send(new KVMessageObj(key, KVMessage.StatusType.GET));
		}

		else if(type.equals("PUT")){
			if (value==null || value.equals("null")) {
				commModule.send(new KVMessageObj(key,null,KVMessage.StatusType.PUT));
			}
			else{
				commModule.send(new KVMessageObj(key,value, KVMessage.StatusType.PUT));
			}
		}
		synchronized (commModule.listener.msgLock) {
			while(!commModule.listener.received) {
				this.commModule.listener.msgLock.wait();
			}
			temp = commModule.listener.latestMsg;
			commModule.listener.received = false;
			commModule.listener.latestMsg = null;
			return temp;
		}
	}




	/**
	 * Use communication module to retrieve the value of a key in the server
	 * Wait for communication module to have the result available before returning
	 * @param key
	 *            the key that identifies the value.
	 * @return
	 * 			  the KVMessage returned by the server containing the status and value
	 * @throws Exception
	 */
	@Override
	synchronized public KVMessage get(String key) throws Exception {
		if(!isConnected()){
			throw new Exception();
		}
		try {
			KVMessageObj temp = null;
			while(temp==null || temp.getStatus().equals(KVMessage.StatusType.SERVER_WRITE_LOCK)){
				if (temp==null || temp.getStatus().equals(KVMessage.StatusType.SERVER_WRITE_LOCK)){
					temp =sendMessage(key,"","GET");
				}

				if(temp.getStatus().equals(KVMessage.StatusType.SERVER_NOT_RESPONSIBLE)){
					clientMeta.update(temp.getValue());
					String server[] = clientMeta.findResponsibleServer(key);

					this.disconnect();
					this.init(server[0], Integer.parseInt(server[1]));
					this.connect();
					temp = sendMessage(key,"","GET");
				}
			}
			return temp;
		} catch (IOException e) {
			logger.warn("Unable to send message!");
			disconnect();
			return null;
		}
	}

	public boolean isConnected() {
		return (addr!=null) && (p > 0) && (commModule != null);
	}

}