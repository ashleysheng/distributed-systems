package app_kvServer;

import java.util.concurrent.Semaphore;

import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.IOException;

import common.Metadata;
import logger.LogSetup;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

public class KVServer extends Thread {
    private static Logger logger = Logger.getRootLogger();
    private int port;
    private int cacheSize;
    private String strategy;
    public Metadata metadata;
    private HeartBeat heartBeat;

    private boolean running;
    private ServerSocket serverSocket;
    public lockList ll;


    public KVServer(int port, int cacheSize, String strategy) {
        this.port = port;
        this.cacheSize = cacheSize;
        this.strategy = strategy;
    }

    public KVServer(int port, int cacheSize, String strategy, String password, String id) {
        this.port = port;
        this.cacheSize = cacheSize;
        this.strategy = strategy;
        this.ll = new lockList();
        this.metadata = new Metadata();
        this.metadata.setEcsPassword(password);
        this.metadata.setSelf(id);
        logger.debug("Password added as: "+ password);
        logger.debug("Set self as: " + id);
    }


    private boolean isRunning() {
        return running;
    }

    public void run() {
        this.running = initializeServer();
        this.ll.setActive(false);
        this.ll.setWriteLock(false);
        Cache c;

        Semaphore s = new Semaphore(1, true);
        if (strategy.equals("LRU")) {
            c = new LRUCache(cacheSize);
        } else if (strategy.equals("LFU")) {
            c = new LFUCache(cacheSize);
        } else if (strategy.equals("FIFO")) {
            c = new FIFOCache(cacheSize);
        } else {
            c= new FIFOCache(cacheSize);
        }
        c.setPath(metadata.getSelf());

        this.heartBeat = new HeartBeat(this.metadata);
        heartBeat.start();

        if (serverSocket != null) {
            while (isRunning()) {
                try {
                    Socket client = serverSocket.accept();
                    ClientConnection connection =
                            new ClientConnection(client, c,s, metadata,ll,serverSocket);
                    new Thread(connection).start();

                    logger.info("Connected to "
                            + client.getInetAddress().getHostName()
                            + " on port " + client.getPort());
                } catch (IOException e) {
                    logger.error("Error! " +
                            "Unable to establish connection. \n", e);
                }
            }
        }
        logger.info("Server stopped.");
    }

    private boolean initializeServer() {
        logger.info("Initializing this server ...");

        try {
            serverSocket = new ServerSocket(port);
            logger.info("Server listening on port: "
                    + serverSocket.getLocalPort());
            return true;

        } catch (IOException e) {
            logger.error("Error! Cannot open server socket:");
            if (e instanceof BindException) {
                logger.error("Port " + port + " is already bound!");
            }
            return false;
        }
    }

    public void shutDown(){
        logger.info("Shutting down per admin request");
        running=false;
    }

    public static void main(String[] args) {

        try {
            System.out.println(args.length);
            if (args.length!=5) {
                System.out.println("Error! Invalid number of arguments!");
                System.out.println("Usage: Server <port> <cacheSize> <strategy>!");
            } else {
                int port = Integer.parseInt(args[0]);
                int cacheSize = Integer.parseInt(args[1]);
                String strategy = args[2];
                logger.info("Metadata: "+ args[3]);
                String password=args[3];
                String id=args[4];
                new LogSetup("logs/server_"+id+".log", Level.ALL);
                new KVServer(port, cacheSize, strategy, password,id).start();

            }
        } catch (IOException e) {
            System.out.println("Error! Unable to initialize logger!");
            e.printStackTrace();
            System.exit(1);
        } catch (NumberFormatException nfe) {
            System.out.println("Error! Invalid argument <port>/<cacheSize>! Not a number!");
            System.out.println("Usage: Server <port> <cacheSize> <strategy>!");
            System.exit(1);
        }
    }

}
