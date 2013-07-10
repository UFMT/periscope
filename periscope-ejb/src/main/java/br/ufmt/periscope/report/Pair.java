package br.ufmt.periscope.report;

public class Pair {

	private Object key;
	private Number value;
	
	public Pair(){
		
	}

	public Pair(Object key, Number value) {
		super();
		this.key = key;
		this.value = value;
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

}
