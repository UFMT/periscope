package br.ufmt.periscopeEsb.util;

import javax.ws.rs.core.Response;

import org.json.JSONException;
import org.json.JSONObject;

import br.ufmt.periscopeEsb.exception.IncompleteResponseException;

public class ResponseUtil {

	private int metaCode=-1;
	private String metaMessage="";
	private JSONObject response = null;
	
	public ResponseUtil(){
		
	}
	
	public Response build() throws IncompleteResponseException{
		if(metaCode == -1)
			throw new IncompleteResponseException("metaCode from response is not filled");
		if(metaMessage.length() == 0)
			throw new IncompleteResponseException("metaMessage from response is not filled");
			
		JSONObject obj = new JSONObject();
		JSONObject objMeta = new JSONObject();
		try {
			objMeta.put("code", metaCode);
			objMeta.put("message", metaMessage);
			obj.put("meta", objMeta);
			if(response != null)
				obj.put("response", response);
			else
				obj.put("response", "");
		} catch (JSONException e) {
			e.printStackTrace();
		}
		try {
			return Response.ok(obj.toString(2)).status(metaCode).build();
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return null;
	}

	public int getMetaCode() {
		return metaCode;
	}

	public void setMetaCode(int metaCode) {
		this.metaCode = metaCode;
	}

	public String getMetaMessage() {
		return metaMessage;
	}

	public void setMetaMessage(String metaMessage) {
		this.metaMessage = metaMessage;
	}

	public JSONObject getResponse() {
		return response;
	}

	public void setResponse(JSONObject response) {
		this.response = response;
	}
	
	
	
	
}
