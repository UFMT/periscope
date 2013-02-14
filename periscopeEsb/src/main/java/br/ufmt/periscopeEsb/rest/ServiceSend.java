package br.ufmt.periscopeEsb.rest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;

import javax.ejb.EJB;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;

import org.json.JSONException;
import org.json.JSONObject;

import br.ufmt.periscopeEsb.dao.ServiceDao;
import br.ufmt.periscopeEsb.model.Service;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;

@Path("/send")
public class ServiceSend {
	private final String PARAM_FROM="periscopeEsb-from",PARAM_TO="periscopeEsb-to",PARAM_FROM_TOKEN="periscopeEsb-from-token"
			,PARAM_METHOD="periscopeEsb-method",PARAM_PROCESSED="periscopeEsb-processed";

	@Context
	HttpServletRequest request;
	
	@EJB
	private ServiceDao sDao;
	
	@POST
	@GET
	@PUT
	@DELETE
	public Response sendData(){
		
		String requestBody = decode(request);
		JSONObject obj = null;
		String to="",from="",token="",method="";
		try {
			obj = new JSONObject(requestBody);
		} catch (JSONException e) {
			e.printStackTrace();
			return  buildJsonNotRecognizedResponse();
		}
		
		boolean foundProcessed = false;
		try {
			foundProcessed = obj.getBoolean(PARAM_PROCESSED);
		} catch (JSONException e) {}
		if(foundProcessed){
			return Response.ok("are you fucking kidding?\nthe service \"to\" is pointing to the esb\n"
					+"this cause a loopback").status(Status.OK).build();
		}
		
		try {
			from = obj.getString(PARAM_FROM);
		} catch (JSONException e) {
			e.printStackTrace();
			return buildPropertyFromNotFoundResponse();
		}
		
		try {
			to = obj.getString(PARAM_TO);
		} catch (JSONException e) {
			e.printStackTrace();
			return buildPropertyToNotFoundResponse();
		}
		
		try {
			token = obj.getString(PARAM_FROM_TOKEN);
		} catch (JSONException e) {
			e.printStackTrace();
			return buildPropertyFromTokenNotFoundResponse();
		}
		
		try {
			method = obj.getString(PARAM_METHOD);
		} catch (JSONException e) {
		}
		
		obj.remove(PARAM_FROM_TOKEN);

		
		
		try {
			obj.put(PARAM_PROCESSED, true);
		} catch (JSONException e1) {
			e1.printStackTrace();
		}

		
		Service s  = sDao.findByName(from);
		if(s == null){
			return buildFromNotFoundResponse(from);
		}
		
		
		if(!s.getToken().contentEquals(token)){
			return buildTokenMismatchResponse(from);
		}
		
		Service s2 = sDao.findByName(to);
		if(s2 == null){
			return buildToNotFoundResponse(to);
		}
				
		
		
		//everything is right, so send the message
		ClientConfig  clientConfig = new DefaultClientConfig();
		Client client = Client.create(clientConfig);
		WebResource service = client.resource(getUriBase(s2.getRealPath()));
		ClientResponse response = null;
		try {
			if(method.toLowerCase().contentEquals("get")){
				response = service.type(MediaType.APPLICATION_JSON).get(ClientResponse.class);
			}else{
				if(method.toLowerCase().contentEquals("post")){
					response = service.type(MediaType.APPLICATION_JSON).post(ClientResponse.class,obj.toString(2));
				}else{
					if(method.toLowerCase().contentEquals("put")){
						response = service.type(MediaType.APPLICATION_JSON).put(ClientResponse.class,obj.toString(2));
					}else{
						if(method.toLowerCase().contentEquals("delete")){
							response = service.type(MediaType.APPLICATION_JSON).delete(ClientResponse.class,obj.toString(2));
						}else{
							response = service.type(MediaType.APPLICATION_JSON).post(ClientResponse.class,obj.toString(2));
						}
					}
				}
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			return Response.ok("something bad happepend :(\n"+e.getMessage()+
					"\nCaused by: "+e.getCause()).status(Status.OK).build();
		}
		
		if(response == null){
			String msg = "something bad happened :(";
			return Response.ok(msg).status(Status.OK).build();
		}else{
			return Response.ok(response.getEntity(String.class)).status(Status.OK).build();
		}
	}
	
	public URI getUriBase(String uriBase){
		return UriBuilder.fromUri(uriBase).build();
	}
	
	
	private Response buildToNotFoundResponse(String to) {
		String msg="the \"to\" service ["+to+"] is not registered in the system";
		return Response.ok(msg).status(Status.OK).build();	
	}

	
	private Response buildFromNotFoundResponse(String from) {
		String msg="the \"from\" service ["+from+"] is not registered in the system";
		return Response.ok(msg).status(Status.OK).build();	}

	private Response buildTokenMismatchResponse(String from) {
		String msg="the token informed mismatch with the \"from\" service ["+from+"]token registered in system";
		return Response.ok(msg).status(Status.OK).build();
	}

	private Response buildPropertyFromNotFoundResponse() {
		String msg="property \""+PARAM_FROM+"\" not found in the body of the Json";
		return Response.ok(msg).status(Status.OK).build();
	}
	
	private Response buildPropertyToNotFoundResponse() {
		String msg="property \""+PARAM_TO+"\" not found in the body of the Json";
		return Response.ok(msg).status(Status.OK).build();
	}
	
	private Response buildPropertyFromTokenNotFoundResponse() {
		String msg="property \""+PARAM_FROM_TOKEN+"\" not found in the body of the Json";
		return Response.ok(msg).status(Status.OK).build();
	}

	private Response buildJsonNotRecognizedResponse() {
		String msg="the content of the body was not recognized as Json";
		return Response.ok(msg).status(Status.OK).build();
	}

	private String decode(HttpServletRequest request){
		BufferedReader bufferedReader = null;
		StringBuilder stringBuilder = new StringBuilder();
		try {
			InputStream inputStream = request.getInputStream();
			if (inputStream != null) {
				bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
				char[] charBuffer = new char[128];
				int bytesRead = -1;
				while ((bytesRead = bufferedReader.read(charBuffer)) > 0) {
					stringBuilder.append(charBuffer, 0, bytesRead);
				}
			} else {
				stringBuilder.append("");
			}
		} catch (IOException ex) {
			ex.printStackTrace();
		} finally {
			if (bufferedReader != null) {
				try {
					bufferedReader.close();
				} catch (IOException ex) {
					ex.printStackTrace();
				}
			}
	}
		return stringBuilder.toString();
	}
	
}
