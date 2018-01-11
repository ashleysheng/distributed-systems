package app_kvEcs;

import app_kvClient.KVClient;
import logger.LogSetup;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.io.*;
import java.util.ArrayList;



public class ECSClient {

    ArrayList<mKVS> serverPool;
    private static Logger logger = Logger.getRootLogger();
    private static final String PROMPT = ">";
    private boolean stop = false;
    private BufferedReader stdin;
    
    private static ECS client;

    public ECSClient(){
    	client = new ECS("ecs.config");
    }

    private void run() {
        while(!stop) {
            stdin = new BufferedReader(new InputStreamReader(System.in));
            System.out.print(PROMPT);

            try {
                String cmdLine = stdin.readLine();
                this.handleCommand(cmdLine);
            } catch (IOException e) {
                stop = true;
                printError("CLI does not respond - Application terminated ");
            } catch (Exception e) {
                stop = true;
                printError("Error with command. Application Terminated");
            }
        }
    }

    private void handleCommand(String cmdLine) throws Exception{
        String[] tokens = cmdLine.split("\\s+");

        if (tokens[0].equals("initService")) {
        	if (tokens.length != 4) {
        		System.out.println("Error: Invalid number of parameters, ECS not initialized");
        	} else {
        		int numberOfNodes = Integer.parseInt(tokens[1]);
        		int cacheSize = Integer.parseInt(tokens[2]);
        		String replacementStrategy = tokens[3];
        		client.initService(numberOfNodes, cacheSize, replacementStrategy);
        	}

        } else if (tokens[0].equals("start")) {
        	client.start();

        } else if(tokens[0].equals("stop")) {
        	client.stop();

        } else if(tokens[0].equals("shutDown")) {
        	client.shutDown();

        } else if(tokens[0].equals("addNode")) {
        	int cacheSize = Integer.parseInt(tokens[1]);
        	String replacementStrategy = tokens[2];
		try{
        		client.addNode(cacheSize, replacementStrategy);
		} catch (Exception e){
			logger.info("Unable to add node");
		}


        } else if (tokens[0].equals("removeNode")) {
        	int nodeIndex = Integer.parseInt(tokens[1]);
		try{
			client.removeNode(nodeIndex);
		}catch (Exception e) {
			logger.info("Unable to remove node");
		}
        } else {
            printError("Unknown command");
            printHelp();
        }
    }

    private void printError(String error){
        System.out.println(PROMPT + "Error! " +  error);
    }

    private void printHelp() {
        StringBuilder sb = new StringBuilder();
        sb.append(PROMPT).append("KV-ECS HELP (Usage):\n");
        sb.append(PROMPT);
        sb.append("::::::::::::::::::::::::::::::::");
        sb.append("::::::::::::::::::::::::::::::::\n");
        sb.append(PROMPT).append("initService NUMBEROFNODES CACHESIZE REPLACEMENTSTRATEGY \n");
        sb.append(PROMPT).append("start\n");
        sb.append(PROMPT).append("stop\n");
        sb.append(PROMPT).append("addNode CACHESIZE STRATEGY\n");
        sb.append(PROMPT).append("removeNode INDEX\n");
	sb.append(PROMPT).append("shutdown\n");

        sb.append(PROMPT).append("\t\t\t\t\t\t ");

        sb.append(PROMPT).append("quit ");
        sb.append("\t\t\t\t\t exits the program");
        System.out.println(sb.toString());
    }
    /**
     * Main entry point for the echo server application.
     * @param args contains the port number at args[0].
     */
    public static void main(String [] args) {
        try {
            new LogSetup("logs/ecs.log", Level.ALL);

            ECSClient ecs = new ECSClient();
	    ecs.run();

        }catch (IOException e) {
            System.out.println("Error! Unable to initialize ECSClient!");
            e.printStackTrace();
            System.exit(1);
        }
        catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
        logger.info("successfully ran ECS");

    }

    public static void initService(int numberOfNodes, int cacheSize, String replacementStrategy) {

    }
}
