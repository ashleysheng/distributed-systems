package common.messages;

import java.util.ArrayList;

/**
 * Implements the KVMessage interface with more features
 */
public class KVMessageObj implements KVMessage {
	String key;
	String value;
	StatusType status;

	static final char START_TEXT = 0x02;
	static final char END_TEXT = 0x03;
	static final char NEW_LINE = 0x0A;
	static final char NULL = 0x00;
	static final char STUFF = 0x21;

	public KVMessageObj() {
		key = null;
		value = null;
	}

	public KVMessageObj (StatusType _status) {
		status = _status;
	}

	public KVMessageObj(String _key, StatusType _status){
		key = _key;
		status = _status;
	} 

	public KVMessageObj(String _key, String _value, StatusType _status) {
		this.key = _key;
		this.value = _value;
		this.status = _status;
	}

	/**
	 * Getters and setters for key, value and status
	 */
	public String getKey() {return key;}
	public String getValue() {return value;}
	public StatusType getStatus() {	return status;}
	public void setKey(String key_) {
		key = key_;
	}
    public void setValue(String value_) {
        value = value_;
    }
    public void setStatus(StatusType s) {
        status = s;
    }

	/**
	 * Converts the KVMessage object into a byte array
	 * @return the byte array version of the current KVMessage
	 */
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

	/**
	 * Deserialize a byte array and set the key value status of the current KVMessage to that of the array
	 * @param bytes the input byte array that contains key value and status
	 */
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

	/**
	 * Make sure that if there are special control characters in the key or value, they are properly flagged
	 * @param bytes the byte array version of a KvMessage
	 * @param ctrlIndex the indices of all the control bytes
	 * @return a newly stuffed byte array that is safe
	 */
	byte[] byteStuffing(byte[] bytes, int[] ctrlIndex) {
		byte current;
		int countSpecials = 0;
		ArrayList<Integer> toStuff=new ArrayList<>();

		for (int i = 0; i< bytes.length ; i++) {
			current = bytes[i];

			if ((current == START_TEXT
					|| current == END_TEXT
					|| current == NEW_LINE)
					&& !contains(ctrlIndex,i)) {
				countSpecials++;
				toStuff.add(i);
			}
		}

		byte[] stuffed = new byte[bytes.length + countSpecials];
		int j = 0;

		for (int i = 0; i< stuffed.length ; i++) {
			current = bytes[j];

			if ((current == START_TEXT || current == END_TEXT || current == NEW_LINE) && toStuff.contains(j)) {
				stuffed[i] = bytes[j];
				stuffed[i+1] = STUFF;
				i++;
			}
			else {
				stuffed[i] = bytes[j];
			}
			j++;
		}

		return stuffed;
	}

	public byte[] byteDestuffing(byte[] bytes) {
		byte current;
		int countSpecials = 0;

		for (int i = 0; i< bytes.length ; i++) {
			current = bytes[i];

			if (current == START_TEXT || current == END_TEXT || current == NEW_LINE) {
				countSpecials++;
			}
		}

		countSpecials-=5;

		byte[] destuffed = new byte[bytes.length - countSpecials];
		int j = 0;
		byte next;

		for (int i = 0; i< bytes.length ; i++) {
			current = bytes[i];
			next = (i == bytes.length - 1) ? (byte)1 : bytes[i+1];

			if((current == START_TEXT || current == END_TEXT ||
					current == NEW_LINE) && (next == STUFF)) {
//				System.out.println("Skipping stuffed byte at index i=" +i);
				destuffed[j] = bytes[i];
				i++;
			}
			else {
				destuffed[j] = bytes[i];
			}
			j++;
		}
		return destuffed;
	}

	private boolean contains(final int[] array,final int key) {
		boolean retval = false;

		for(int i = 0; i<array.length;i++) {
			if (key == array[i]) retval=true;
		}
		return retval;
	}
}