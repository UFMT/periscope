package br.ufmt.periscopeEsb.exception;

public class IncompleteResponseException extends Exception{

	/**
	 * 
	 */
	private static final long serialVersionUID = -1152464914220210734L;

	private static String msg="Response has not all necessary fields complete";
	
	public IncompleteResponseException(String msg){
		super(msg);
	}
	
	public IncompleteResponseException(){
		super(msg);
	}

	public String getMsg() {
		return msg;
	}

	public void setMsg(String message) {
		msg = message;
	}
	
	
}
