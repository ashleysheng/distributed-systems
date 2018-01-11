package app_kvEcs;

import common.MD5Hasher;
import common.ScriptGenerator;
import common.communication.ECSCommModule;
import common.messages.KVAdminMsg;
import org.apache.log4j.Logger;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

public class mKVS {

    public String name;
    public String addr;
    public int port;
    Socket conn;
    String password;
    public String hashUpperBound;
    public ECSCommModule cm;
    
    Logger logger = Logger.getRootLogger();
    MD5Hasher md5 = new MD5Hasher();
    ScriptGenerator generator = new ScriptGenerator();

    public mKVS(String name, String addr, int port){
        this.name=name;
        this.addr=addr;
        this.port=port;
        this.hashUpperBound = md5.hashString(this.addr+Integer.toString(this.port));
        this.password = Long.toHexString(Double.doubleToLongBits(Math.random()));
    }

    public void initKVServer(String metadata, int cacheSize, String strategy){
        try{
            generator.makeLaunchScript(
                    strategy,
                    cacheSize,
                    this.password,
                    this,
                    this.hashUpperBound
            );
        } catch (IOException ioe){
            logger.error("Error: Can't make script file");
            ioe.printStackTrace();
        }
        try{
            Runtime run = Runtime.getRuntime();
//            run.exec("chmod +x init_servers.sh");
            Process launch = run.exec("sh init_servers.sh");
            int doneExec = launch.waitFor();
        }
        catch (IOException ioe){
            logger.error("Error: can't perform SSH Call");
            ioe.printStackTrace();
        } catch (InterruptedException e) {
            logger.error("Erro: couldn't finish waiting for launch call");
            e.printStackTrace();
        }

        try{
            ECSCommModule comm = new ECSCommModule(addr,port);
            this.cm = comm;

            this.update(metadata);
        } catch (UnknownHostException e){
            e.printStackTrace();
        }
    }

    void start(){
        // start a socket, send a adminkvMessage indicating "START SERVER" --statusType
        // Effectively, the server should just change the bool value.
        try{
            KVAdminMsg message = new KVAdminMsg(getPassword(),"",KVAdminMsg.StatusType.START);
            cm.sendMessage(message);
            KVAdminMsg receive = null;
            while(receive==null){
                receive = cm.receiveMessage();
            }
            System.out.println(receive.getAdminStatus());
        } catch(UnknownHostException e){
            e.printStackTrace();
        } catch(Exception e){
            e.printStackTrace();
        }
    }

    void stop(){
        try{
            KVAdminMsg message = new KVAdminMsg(getPassword(),"",KVAdminMsg.StatusType.STOP);

            this.cm.sendMessage(message);

            KVAdminMsg receive = null;
            while(receive==null){
                receive = cm.receiveMessage();
            }
            System.out.println(receive.getAdminStatus().toString());
        } catch(UnknownHostException e){
            e.printStackTrace();
        } catch(Exception e){
            e.printStackTrace();
        }
    }

    public void shutDown(){
        try{
            KVAdminMsg message = new KVAdminMsg(getPassword(),"",KVAdminMsg.StatusType.SHUTDOWN);
            this.cm.sendMessage(message);
            KVAdminMsg receive = null;

            while(receive==null){
                receive = cm.receiveMessage();
            }

            System.out.println(receive.getAdminStatus().toString());
        } catch(UnknownHostException e){
            e.printStackTrace();
        } catch(Exception e){
            e.printStackTrace();
        }
    }

    public void lockWrite(){
        try{
            KVAdminMsg message = new KVAdminMsg(getPassword(),"",KVAdminMsg.StatusType.LOCK_WRITE);
            this.cm.sendMessage(message);
            KVAdminMsg receive = null;
            while(receive==null){
                receive = cm.receiveMessage();
            }
            System.out.println(receive.getAdminStatus().toString());
        } catch(UnknownHostException e){
            e.printStackTrace();
        } catch(Exception e){
            e.printStackTrace();
        }
    }
    public void unlockWrite(){
        try{
            KVAdminMsg message = new KVAdminMsg(getPassword(),"",KVAdminMsg.StatusType.UNLOCK_WRITE);
            this.cm.sendMessage(message);
            KVAdminMsg receive = null;
            while(receive==null){
                receive = cm.receiveMessage();
            }
            System.out.println(receive.getAdminStatus().toString());
        } catch(UnknownHostException e){
            e.printStackTrace();
        } catch(Exception e){
            e.printStackTrace();
        }
    }

    void moveData(String range, String recipient){
        try{
            KVAdminMsg admsg = new KVAdminMsg(
                    getPassword(),
                    range+","+recipient,
                    KVAdminMsg.StatusType.MOVE_DATA
            );
            this.cm.sendMessage(admsg);
            KVAdminMsg receive = null;
            while(receive==null){
                receive = cm.receiveMessage();
            }
            System.out.println(receive.getAdminStatus().toString());
        } catch(UnknownHostException e){
            e.printStackTrace();
        } catch(Exception e){
            e.printStackTrace();
        }
    }

    public void recoverData(String range, String recipient){
        try{
            KVAdminMsg admsg = new KVAdminMsg(
                    getPassword(),
                    range+","+recipient+",RECOVER",
                    KVAdminMsg.StatusType.MOVE_DATA
            );
            this.cm.sendMessage(admsg);
            KVAdminMsg receive = null;
            while(receive==null){
                receive = cm.receiveMessage();
            }
            System.out.println(receive.getAdminStatus().toString());
        } catch(UnknownHostException e){
            e.printStackTrace();
        } catch(Exception e){
            e.printStackTrace();
        }
    }

    void cleanUp(){

        try{
            KVAdminMsg admsg = new KVAdminMsg(
                    getPassword(),
                    "",
                    KVAdminMsg.StatusType.MOVE_DATA
            );
            this.cm.sendMessage(admsg);
            KVAdminMsg receive = null;
            while(receive==null){
                receive = cm.receiveMessage();
            }
            System.out.println(receive.getAdminStatus().toString());
        } catch(UnknownHostException e){
            e.printStackTrace();
        } catch(Exception e){
            e.printStackTrace();
        }
    }
    // send kvServer a new metadata string
    public void update(String meta){
        try{
            KVAdminMsg message = new KVAdminMsg(getPassword(),meta,KVAdminMsg.StatusType.UPDATE);
            this.cm.sendMessage(message);
            KVAdminMsg receive = null;
            while(receive==null){
                receive = cm.receiveMessage();
            }
            System.out.println(receive.getAdminStatus().toString());
        } catch(UnknownHostException e){
            e.printStackTrace();
        } catch(Exception e){
            e.printStackTrace();
        }
    }

    public String getPassword(){return this.password;}
}
