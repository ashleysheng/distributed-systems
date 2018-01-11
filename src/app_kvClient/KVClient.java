package app_kvClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.UnknownHostException;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import client.KVStore;
import logger.LogSetup;

public class KVClient {

    static Logger logger = Logger.getRootLogger();
    private static final String PROMPT = ">";
    private BufferedReader stdin;
    private boolean stop = false;

    private String serverAddress;
    private int serverPort;
    private KVStore client;

    private final int MAX_KEY = 20;
    private final int MAX_VALUE = 120000;

    /**
     * Generates a new KVClient
     */
    public KVClient(){
        client = new KVStore();
    }


    /**
     * Starts running the KVClient.
     * Will keep on prompting for command until system crashes
     */
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

    /**
     * Calls the appropriate KVStore API depending on the command
     * @param cmdLine The command that the user enters
     * @throws Exception If the client crashes
     */
    private void handleCommand(String cmdLine) throws Exception{
        String[] tokens = cmdLine.split("\\s+");

        if(tokens[0].equals("quit")) {
            stop = true;
            client.disconnect();
            System.out.println(PROMPT + "Application exit!");

        } else if (tokens[0].equals("connect")){
            if(tokens.length == 3) {
                try{
                    serverAddress = tokens[1];
                    if (serverAddress.equals("localhost")){
                        serverAddress = "127.0.0.1";
                    }
                    serverPort = Integer.parseInt(tokens[2]);
                    client.init(serverAddress,serverPort);
                    client.connect();
                } catch(NumberFormatException nfe) {
                    printError("No valid address. Port must be a number!");
                    logger.info("Unable to parse argument <port>", nfe);
                } catch (UnknownHostException e) {
                    printError("Unknown Host!");
                    logger.info("Unknown Host!", e);
                }
            } else {
                printError("Invalid number of parameters!");
            }

        } else if(tokens[0].equals("disconnect")) {
            client.disconnect();

        } else if(tokens[0].equals("logLevel")) {
            if(tokens.length == 2) {
                String level = setLevel(tokens[1]);
                if(level.equals(LogSetup.UNKNOWN_LEVEL)) {
                    printError("No valid log level!");
                    printPossibleLogLevels();
                } else {
                    System.out.println(PROMPT +
                            "Log level changed to level " + level);
                }
            } else {
                printError("Invalid number of parameters!");
            }


        } else if(tokens[0].equals("help")) {
            printHelp();


        } else if (tokens[0].equals("get")) {

            try {
                if(!client.isConnected()) throw new Exception();
                if(tokens.length != 2) throw new Exception();
                if(tokens[1].length() > MAX_KEY) throw new Exception();

                StringBuilder msg = new StringBuilder();
                msg.append("Query to server to get entry with key: ");
                msg.append(tokens[1]);
                logger.info(msg);
                client.get(tokens[1]);

            } catch (Exception e) {
                if (!client.isConnected()) {
                    logger.error("Not connected to any servers.");
                } else if (tokens.length != 2) {
                    logger.error("Invalid number of arguments");
                } else if (tokens[1].length()>MAX_KEY) {
                    logger.error("Key size exceeds max key size");
                }
            }
        }
        else if (tokens[0].equals("put")) {
            boolean exceed_max_value = false;
            try {
                if(!client.isConnected()) throw new Exception();
                if(tokens.length < 2) throw new Exception();
                if(tokens[1].length() > MAX_KEY) throw new Exception();

                //insert or update or delete
                if (tokens.length > 2) {

                    int size = 0;
                    String buildValue = new String();
                    String temp;

                    for(int i =2; i< tokens.length;i++) {
                        size += tokens[i].length();
                        temp=tokens[i];
                        if (i != tokens.length-1) {
                            size++;
                            temp+=" ";
                        }

                        if(size > MAX_VALUE) {
                            exceed_max_value = true;
                            throw new Exception();
                        }
                        buildValue = buildValue + temp;
                    }

                    StringBuilder msg = new StringBuilder();
                    if (buildValue.equals("null")) {
                        msg.append("Query to delete entry in table with key: ");
                        msg.append(tokens[1]);
                        logger.info(msg);
                        client.put(tokens[1],null);
                    }
                    else {
                        msg.append("Query server to insert/update entry in table with ");
                        msg.append("key value pair <");
                        msg.append(tokens[1]).append(", ");
                        msg.append(buildValue).append(">");
                        logger.info(msg);
                        client.put(tokens[1],buildValue);
                    }
                }

                else {
                    StringBuilder msg = new StringBuilder();
                    msg.append("Query to delete entry in table with key: ");
                    msg.append(tokens[1]);
                    logger.info(msg);
                    client.put(tokens[1],null);
                }

            } catch (Exception e) {
                if(!client.isConnected()){
                    printError("Not connected to any servers.");
                    logger.error("Not connected to any servers.");
                }
                else if (tokens.length < 2) {
                    printError("Invalid number of arguments");
                    logger.error("Invalid number of arguments");
                }
                else if (tokens[1].length()>MAX_KEY) {
                    printError("Key size exceeds max key size");
                    logger.error("Key size exceeds max key size");
                }
                else if (exceed_max_value){
                    printError("Value size exceeds max value size");
                    logger.error("Value size exceeds max value size");
                }
            }
        }
        else {
            printError("Unknown command");
            printHelp();
        }
    }

    /**
     * Prints out all the possible commands for the user
     */
    private void printHelp() {
        StringBuilder sb = new StringBuilder();
        sb.append(PROMPT).append("KV-CLIENT HELP (Usage):\n");
        sb.append(PROMPT);
        sb.append("::::::::::::::::::::::::::::::::");
        sb.append("::::::::::::::::::::::::::::::::\n");
        sb.append(PROMPT).append("connect <host> <port>");
        sb.append("\t establishes a connection to a server\n");
        sb.append(PROMPT).append("get <key>");
        sb.append("\t\t\t\t retrieve the data associated with a key \n");
        sb.append(PROMPT).append("put <key> <value>");
        sb.append("\t\t insert a new key value pair or modify an existing pair \n");
        sb.append(PROMPT).append("put <key>");
        sb.append("\t\t\t\t remove a key value pair \n");
        sb.append(PROMPT).append("disconnect");
        sb.append("\t\t\t\t disconnects from the server \n");

        sb.append(PROMPT).append("logLevel");
        sb.append("\t\t\t\t changes the logLevel \n");
        sb.append(PROMPT).append("\t\t\t\t\t\t ");
        sb.append("ALL | DEBUG | INFO | WARN | ERROR | FATAL | OFF \n");

        sb.append(PROMPT).append("quit ");
        sb.append("\t\t\t\t\t exits the program");
        System.out.println(sb.toString());
    }

    /**
     * Print out all the possible log levels for the user
     */
    private void printPossibleLogLevels() {
        System.out.println(PROMPT
                + "Possible log levels are:");
        System.out.println(PROMPT
                + "ALL | DEBUG | INFO | WARN | ERROR | FATAL | OFF");
    }

    /**
     * @param levelString The new level for the logger to abide by
     * @return the string version of the log levels
     */
    private String setLevel(String levelString) {

        if(levelString.equals(Level.ALL.toString())) {
            logger.setLevel(Level.ALL);
            return Level.ALL.toString();
        } else if(levelString.equals(Level.DEBUG.toString())) {
            logger.setLevel(Level.DEBUG);
            return Level.DEBUG.toString();
        } else if(levelString.equals(Level.INFO.toString())) {
            logger.setLevel(Level.INFO);
            return Level.INFO.toString();
        } else if(levelString.equals(Level.WARN.toString())) {
            logger.setLevel(Level.WARN);
            return Level.WARN.toString();
        } else if(levelString.equals(Level.ERROR.toString())) {
            logger.setLevel(Level.ERROR);
            return Level.ERROR.toString();
        } else if(levelString.equals(Level.FATAL.toString())) {
            logger.setLevel(Level.FATAL);
            return Level.FATAL.toString();
        } else if(levelString.equals(Level.OFF.toString())) {
            logger.setLevel(Level.OFF);
            return Level.OFF.toString();
        } else {
            return LogSetup.UNKNOWN_LEVEL;
        }
    }

    private void printError(String error){
        System.out.println(PROMPT + "Error! " +  error);
    }

    /**
     * Main entry point for the echo server application.
     * @param args contains the port number at args[0].
     */
    public static void main(String[] args) {
        try {
            new LogSetup("logs/client.log", Level.INFO);
            KVClient app = new KVClient();
            app.run();
        } catch (IOException e) {
            System.out.println("Error! Unable to initialize logger!");
            e.printStackTrace();
            System.exit(1);
        }
    }

}
