package br.ufmt.periscopeEsb.rest;

import java.util.ArrayList;
import java.util.List;

import javax.ejb.EJB;
import javax.ws.rs.GET;
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

@Path("/list")
public class ServiceList {

	@EJB
	private ServiceDao sDao;
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response getServices(@QueryParam("name") String serviceName){
		List<Service> services = null;
		if(serviceName == null || !(serviceName.length() > 0) || serviceName.contentEquals("null")){
			services = sDao.findAllOrdered();
		}else{
			services = sDao.findLike(serviceName);
		}
		Response response = buildServicesResponse(services);
		return response;
	}
	
	public Response buildServicesResponse(List<Service> services){
		JSONObject obj = new JSONObject();
		String msg="see the available services";
		List<String> strServices = new ArrayList<String>();
		try {
			for (Service service : services) {
				strServices.add(service.getName());
				//obj.put(service.getName(), service.getName());
			}
			obj.put("services", strServices);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		ResponseUtil util = new ResponseUtil();
		util.setMetaCode(Status.OK.getStatusCode());
		util.setMetaMessage(msg);
		util.setResponse(obj);
		try {
			return util.build();
		} catch (IncompleteResponseException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	
}
