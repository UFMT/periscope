package br.ufmt.periscopeEsb.rest;

import javax.ejb.EJB;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.json.JSONException;
import org.json.JSONObject;

import br.ufmt.periscopeEsb.dao.ServiceDao;
import br.ufmt.periscopeEsb.exception.IncompleteResponseException;
import br.ufmt.periscopeEsb.model.Service;
import br.ufmt.periscopeEsb.util.ResponseUtil;
import br.ufmt.periscopeEsb.util.TokenUtils;

@Path("/add")
public class ServiceAdd {

	@EJB
	private ServiceDao dao;
	
	@EJB
	private TokenUtils tokenUtils;
	
	@PUT
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response add( @QueryParam("serviceName") String serviceName, 
			@QueryParam("path") String path){
		Response response = null;
		
		if(serviceName == null || !(serviceName.length() > 0) || serviceName.contentEquals("null")){
			response = buildMissingServiceNameResponse();
			return response;
		}
		
		if(path == null || !(path.length() > 0) || path.contentEquals("null")){
			response = buildMissingPathResponse();
			return response;
		}
		
		Service service = dao.findByName(serviceName);
		if(service != null){
			response = buildServiceAlreadyExistResponse(serviceName);
			return response;
		}
		
		
		service = new Service();
		service.setName(serviceName);
		service.setRealPath(path);
		service.setToken(tokenUtils.generateToken());
		dao.save(service);
		
		String msg = "Service registered with sucess. Please hold your token to manage your service";
		JSONObject obj = new JSONObject();
		try {
			obj.put("serviceName", service.getName());
			obj.put("path", service.getRealPath());
			obj.put("token", service.getToken());
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		ResponseUtil util = new ResponseUtil();
		util.setMetaCode(Status.OK.getStatusCode());
		util.setMetaMessage(msg);
		util.setResponse(obj);
		
		try {
			response = util.build();
		} catch (IncompleteResponseException e) {
			e.printStackTrace();
		} 
		return response;
	}

	private Response buildServiceAlreadyExistResponse(String serviceName) {
		JSONObject obj = new JSONObject();
		String msg = "Sorry, the service ["+serviceName+"] is already registered";
		
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
	
	private Response buildMissingPathResponse() {
		JSONObject obj = new JSONObject();
		String msg = "Sorry, it's missing the path of the service in the request";
		
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
