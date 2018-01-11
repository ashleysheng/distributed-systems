package app_kvServer;

/**
 * Created by ashleysheng on 2017-03-04.
 */
public class lockList {
    public boolean getActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    boolean active;

    public boolean getWriteLock() {
        return writeLock;
    }

    public void setWriteLock(boolean writeLock) {
        this.writeLock = writeLock;
    }

    boolean writeLock;


}
