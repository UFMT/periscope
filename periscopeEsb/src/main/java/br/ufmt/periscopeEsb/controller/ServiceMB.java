package br.ufmt.periscopeEsb.controller;

import java.util.List;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.model.DataModel;
import javax.faces.model.ListDataModel;

import org.primefaces.context.RequestContext;

import br.ufmt.periscopeEsb.dao.ServiceDao;
import br.ufmt.periscopeEsb.model.Service;
import br.ufmt.periscopeEsb.util.TokenUtils;

@ManagedBean
@ViewScoped
public class ServiceMB {

	@EJB
	public ServiceDao sDao;
	
	@EJB
	public TokenUtils utils;
	
	private DataModel<Service> services;
	private List<Service> filteredServices;
	private Service service;
	private String token = "";
	private static final String[] availableOperations={"edit","exclude"};
	private String currentOperation="";
	
	@PostConstruct
	public void init(){
		loadServices();
	}
	
	public void prepareNew(){
		service = new Service();
		token="";
	}
	
	public void loadServices(){
		services = new ListDataModel<Service>(sDao.findAllOrdered());
	}
	
	public void saveNew(){
		service.setToken(utils.generateToken());
		sDao.save(service);
		services = new ListDataModel<Service>(sDao.findAllOrdered());
		RequestContext request = RequestContext.getCurrentInstance();
		request.execute("dlgShowToken.show();");
		FacesContext context = FacesContext.getCurrentInstance();
		context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO,"service registered with sucess","sucess"));
	}
	
	public void deleteConfirmed( ){
		sDao.delete(service.getId());
		loadServices();
		FacesContext context = FacesContext.getCurrentInstance();
		context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "service deleted sucessfully", "sucess"));
		currentOperation="";
		service = null;
		token = "";
	}
	
	public void editConfirmed( ){
		sDao.update(service);
		FacesContext context = FacesContext.getCurrentInstance();
		context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "service edited sucessfully", "sucess"));
		currentOperation="";
		service = null;
		token = "";
	}
	
	public void operationIsDelete(){
		currentOperation = availableOperations[1];
		token = "";
	}
	
	public void operationIsEdit(){
		currentOperation = availableOperations[0];
		token = "";
	}
	
	public void processInsertTokenDialog(){
		RequestContext request = RequestContext.getCurrentInstance();
		if(currentOperation.contentEquals(availableOperations[0])){
			request.execute("dlgEditService.show();");
		}else{
			if(currentOperation.contentEquals(availableOperations[1])){
				request.execute("dlgDeleteService.show();");
			}
		}
		
	}
	
	public void validateServiceName(FacesContext context, UIComponent toValidate, Object value){
		    String newServiceName = (String) value;    
		  
		    Service s = sDao.findByName(newServiceName);
		    
		    if (s != null) {  
		        ((UIInput)toValidate).setValid(false);  
		  
		        FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR,"serviceName already used","error");  
		        context.addMessage(toValidate.getClientId(context), message);  
		    }  
	}
	
	public void validateToken(FacesContext context, UIComponent toValidate, Object value){
	    String insertedToken = (String) value;    
	  
	    
	    if (! insertedToken.contentEquals(service.getToken())) {  
	        ((UIInput)toValidate).setValid(false);  
	  
	        FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR,"token is invalid","error");  
	        context.addMessage(toValidate.getClientId(context), message);  
	    }  
	}
	
	public void clean(){
		service = null;
	}
	
	
	public DataModel<Service> getServices() {
		return services;
	}



	public void setServices(DataModel<Service> services) {
		this.services = services;
	}

	

	public List<Service> getFilteredServices() {
		return filteredServices;
	}



	public void setFilteredServices(List<Service> filteredServices) {
		this.filteredServices = filteredServices;
	}



	public Service getService() {
		return service;
	}

	public void setService(Service service) {
		this.service = service;
	}
	
	

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public ServiceMB(){
		
	}
	
	
}
