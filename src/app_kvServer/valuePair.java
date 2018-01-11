package app_kvServer;


public class valuePair {
    private String value;
    private int counter;

    public valuePair(String value_, int counter_) {
        this.value = value_;
        this.counter = counter_;
    }

    public void setValue(String value_){
        this.value = value_;
    }
    public String getValue() {
        return value;
    }

    public int getCount() {
        return counter;
    }

    public void incrementCount() {
        this.counter++;
    }
}