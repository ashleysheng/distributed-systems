package common;

import app_kvEcs.mKVS;
import org.apache.log4j.Logger;

import java.io.*;

public class ScriptGenerator {
    private static Logger logger = Logger.getRootLogger();
    private static final String LAUNCH_CMD =
            "ssh -n -f <host> \"sh -c 'cd ~/ece419/m2/ScalableStorageService-stub;"+
                    " nohup java -jar ms3-server.jar <port> <size> <strat> <pass> <self> &'\"";
    private static final String LOCAL_LAUNCH =
            "nohup java -jar ms3-server.jar <port> <size> <strat> <pass> <self> &";

    public ScriptGenerator(){

    }

    public void makeLaunchScript(String strat, int cacheSize, String password, mKVS s, String self)
            throws IOException {

        String cmd;
        File fout = new File("init_servers.sh");

        if(fout.exists() && !fout.isDirectory()) {
            if(!fout.delete()){
                logger.error("ERROR: failed to delete server launch script");
                throw new IOException();
            }
            else{
                logger.debug("Successfully deleted previous launch script");
            }
        }

        FileOutputStream fos = new FileOutputStream(fout);
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));
        bw.write("#!/bin/sh");
        bw.newLine();

        cmd=(s.addr.equals("127.0.0.1"))? LOCAL_LAUNCH : LAUNCH_CMD;
        cmd=cmd.replaceAll("<host>",s.addr);
        cmd=cmd.replaceAll("<port>", Integer.toString(s.port));
        cmd=cmd.replaceAll("<size>",Integer.toString(cacheSize));
        cmd=cmd.replaceAll("<strat>",strat);
        cmd=cmd.replaceAll("<pass>", password);
        cmd = cmd.replaceAll("<self>", self);
        bw.write(cmd);
        logger.debug("Added server launch command "+cmd+" to launch script");
        bw.newLine();

        bw.close();
    }
}
