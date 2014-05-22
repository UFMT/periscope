package br.ufmt.periscope.report;

public class Pair {

    private Object key;
    private Number value;
    private String description;

    public Pair() {

    }

    public Pair(Object key, Number value) {
        super();
        this.key = key;
        this.value = value;
    }

    public Pair(Object key, Number value, String description) {
        this.key = key;
        this.value = value;
        this.description = description;
    }

    public Object getKey() {
        return key;
    }

    public void setKey(Object key) {
        this.key = key;
    }

    public Number getValue() {
        return value;
    }

    public void setValue(Number value) {
        this.value = value;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

}
