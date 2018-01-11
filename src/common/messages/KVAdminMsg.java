package common.messages;


import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class KVAdminMsg extends KVMessageObj {

    public enum StatusType{
        START,
        STOP,
        SHUTDOWN,
        LOCK_WRITE,
        UNLOCK_WRITE,
        MOVE_DATA,
        UPDATE,
        SUCCESS,
        ERROR,
        FAIL,
        RECOVER_DATA
    }

    private HashMap<KVMessage.StatusType,KVAdminMsg.StatusType> statusConverter=new HashMap<>();
    KVAdminMsg.StatusType status;

    public KVAdminMsg() {
        key = null;
        value = null;
        buildConverter();
    }

    public KVAdminMsg (KVAdminMsg.StatusType _status) {
        buildConverter();
        status = _status;
    }

    public KVAdminMsg(String _key, KVAdminMsg.StatusType _status){
        key = _key;
        status = _status;
        buildConverter();
    }

    public KVAdminMsg(String key, String value, KVAdminMsg.StatusType status) {
        buildConverter();
        this.key = key;
        this.value = value;
        this.status = status;
    }

    public KVAdminMsg(KVMessageObj original){
        buildConverter();
        this.key=original.key;
        this.value=original.value;
        this.status=statusConverter.get(original.status);
    }

    public KVAdminMsg.StatusType getAdminStatus(){
        return this.status;
    }

    public void setStatus(KVAdminMsg.StatusType status) {
        this.status= status;
    }
    public KVMessageObj toRegularMsg(){
        KVMessage.StatusType s = null;
        for (KVMessage.StatusType key : statusConverter.keySet()) {
            if (statusConverter.get(key)==this.status){
                s=key;
            }
        }
        return new KVMessageObj(this.key,this.value,s);
    }

    public void buildConverter(){
        statusConverter.put(KVMessage.StatusType.GET, StatusType.START);
        statusConverter.put(KVMessage.StatusType.GET_ERROR, StatusType.STOP);
        statusConverter.put(KVMessage.StatusType.GET_SUCCESS, StatusType.LOCK_WRITE);
        statusConverter.put(KVMessage.StatusType.PUT, StatusType.UNLOCK_WRITE);
        statusConverter.put(KVMessage.StatusType.PUT_SUCCESS, StatusType.MOVE_DATA);
        statusConverter.put(KVMessage.StatusType.PUT_UPDATE, StatusType.UPDATE);
        statusConverter.put(KVMessage.StatusType.PUT_ERROR, StatusType.SUCCESS);
        statusConverter.put(KVMessage.StatusType.DELETE_SUCCESS, StatusType.ERROR);
        statusConverter.put(KVMessage.StatusType.DELETE_ERROR, StatusType.SHUTDOWN);
        statusConverter.put(KVMessage.StatusType.SERVER_STOPPED, StatusType.FAIL);
        statusConverter.put(KVMessage.StatusType.SERVER_NOT_RESPONSIBLE, StatusType.RECOVER_DATA);
    }

    public byte[] toByteArray() {
        byte[] keyBytes = (key==null) ? new byte[]{NULL}: key.getBytes();
        byte[] valBytes = (value==null) ? new byte[]{NULL}: value.getBytes();
        byte[] staBytes = status.name().getBytes();
        byte[] tmp = new byte[keyBytes.length + valBytes.length + staBytes.length + 5];

        tmp[0] = START_TEXT;
        tmp[staBytes.length+1] = NEW_LINE;
        tmp[staBytes.length + keyBytes.length + 2] = NEW_LINE;
        tmp[staBytes.length + keyBytes.length + valBytes.length + 3] = NEW_LINE;
        tmp[tmp.length-1] = END_TEXT;


        System.arraycopy(staBytes,0,tmp,1,staBytes.length);
        System.arraycopy(keyBytes,0,tmp,1+staBytes.length+1,keyBytes.length);
        System.arraycopy(valBytes,0,tmp,1+staBytes.length+1+keyBytes.length+1,valBytes.length);

        int[] ctrlChars = new int[]{
                0,
                staBytes.length+1,
                staBytes.length + keyBytes.length + 2,
                staBytes.length + keyBytes.length + valBytes.length + 3,
                tmp.length-1
        };

        return byteStuffing(tmp,ctrlChars);
    }

    public void toObject(byte[] bytes) throws Exception{
        int[] ctrlChars = new int[5];
        int ctrlCounter = 0;
        int goodCtrl = 0;

        for (int i = 0; i < bytes.length; i++) {
            if (bytes[i] == START_TEXT) {
                goodCtrl = (i==0) ? 1 : -1;
            }
            else if (bytes[i] == NEW_LINE) {
                goodCtrl = (ctrlCounter<4) ? 1 : -1;
            }
            else if(bytes[i] == END_TEXT) {
                goodCtrl = (i == bytes.length -1) ? 1 : -1;
            }

            if (goodCtrl == 1) {
                ctrlChars[ctrlCounter] = i;
                ctrlCounter++;
                goodCtrl = 0;
            }
            else if (goodCtrl == -1) {
                System.exit(-1);
            }
        }

        byte[] statBytes = new byte[ctrlChars[1]-ctrlChars[0] + 1 - 2];
        byte[] keyBytes = new byte[ctrlChars[2]-ctrlChars[1] + 1 - 2];
        byte[] valBytes = new byte[ctrlChars[3]-ctrlChars[2] + 1 - 2];

        System.arraycopy(bytes,ctrlChars[0]+1,statBytes,0,statBytes.length);
        System.arraycopy(bytes,ctrlChars[1]+1,keyBytes,0,keyBytes.length);
        System.arraycopy(bytes,ctrlChars[2]+1,valBytes,0,valBytes.length);

        String statStr = new String(statBytes);
        String new_key = (keyBytes.length==1 && keyBytes[0]==0) ? null : new String(keyBytes);
        String new_value = (valBytes.length==1 && valBytes[0]==0) ? null : new String(valBytes);

        key = new_key;
        value = new_value;
        status = StatusType.valueOf(statStr);
    }
}
