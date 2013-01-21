package br.ufmt.periscopeEsb.rest;

import javax.ejb.EJB;
import javax.ws.rs.DELETE;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.json.JSONException;
import org.json.JSONObject;

import br.ufmt.periscopeEsb.dao.ServiceDao;
import br.ufmt.periscopeEsb.exception.IncompleteResponseException;
import br.ufmt.periscopeEsb.model.Service;
import br.ufmt.periscopeEsb.util.ResponseUtil;

@Path("/remove")
public class ServiceRemove {

	@EJB
	private ServiceDao dao;
	
	@DELETE
	public Response delete(@QueryParam("serviceName") String serviceName,
			@QueryParam("token") String token){
		
		Response response = null;
		if(serviceName == null || !(serviceName.length() > 0) || serviceName.contentEquals("null")){
			response = buildMissingServiceNameResponse();
			return response;
		}
		
		if(token == null || !(token.length() > 0) || token.contentEquals("null")){
			response = buildMissingTokenResponse();
			return response;
		}
		
		Service service = dao.findByName(serviceName);
		
		if(service == null){
			response = buildMissingServiceNameRegisteredResponse(serviceName);
			return response;
		}
		
		if(service.getToken().contentEquals(token)){
			dao.delete(service.getId());
			response = buildDeletedSucessfullyResponse(serviceName);
		}else{
			response = buildTokenMismatchResponse();
		}
		
		return response;
	}
	
	
	
	private Response buildMissingServiceNameResponse() {
		JSONObject obj = new JSONObject();
		String msg = "Sorry, it's missing the serviceName in the request";
		
		try {
			obj.put("msg", msg);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		ResponseUtil util = new ResponseUtil();
		util.setMetaCode(Status.OK.getStatusCode());
		util.setMetaMessage(msg);
		
		try {
			return util.build();
		} catch (IncompleteResponseException e) {
			e.printStackTrace();
		}
		return null;
		
	}
	
	private Response buildDeletedSucessfullyResponse(String serviceName) {
		JSONObject obj = new JSONObject();
		String msg = "the service ["+serviceName+"] was sucessfully deleted";
		
		try {
			obj.put("msg", msg);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		ResponseUtil util = new ResponseUtil();
		util.setMetaCode(Status.OK.getStatusCode());
		util.setMetaMessage(msg);
		
		try {
			return util.build();
		} catch (IncompleteResponseException e) {
			e.printStackTrace();
		}
		return null;
		
	}
	
	private Response buildTokenMismatchResponse() {
		JSONObject obj = new JSONObject();
		String msg = "Sorry, the token is invalid";
		
		try {
			obj.put("msg", msg);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		ResponseUtil util = new ResponseUtil();
		util.setMetaCode(Status.OK.getStatusCode());
		util.setMetaMessage(msg);
		
		try {
			return util.build();
		} catch (IncompleteResponseException e) {
			e.printStackTrace();
		}
		return null;
		
	}
	
	private Response buildMissingServiceNameRegisteredResponse(String service) {
		JSONObject obj = new JSONObject();
		String msg = "Sorry, the service ["+service+"] was not found";
		
		try {
			obj.put("msg", msg);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		ResponseUtil util = new ResponseUtil();
		util.setMetaCode(Status.OK.getStatusCode());
		util.setMetaMessage(msg);
		
		try {
			return util.build();
		} catch (IncompleteResponseException e) {
			e.printStackTrace();
		}
		return null;
		
	}
	
	private Response buildMissingTokenResponse() {
		JSONObject obj = new JSONObject();
		String msg = "Sorry, it's missing the token to authenticate the request";
		
		try {
			obj.put("msg", msg);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		ResponseUtil util = new ResponseUtil();
		util.setMetaCode(Status.OK.getStatusCode());
		util.setMetaMessage(msg);
		
		try {
			return util.build();
		} catch (IncompleteResponseException e) {
			e.printStackTrace();
		}
		return null;
	}

	
	
}
