package br.ufmt.periscope.managedbean;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;
import javax.faces.context.Flash;
import javax.inject.Inject;

import br.ufmt.periscope.controller.LoginController;
import br.ufmt.periscope.model.User;

import com.github.jmkgreen.morphia.Datastore;

@ManagedBean
@SessionScoped
public class SessionBean {

	private @Inject Datastore ds;
	private @Inject LoginController loginController;
	private User loggedUser;
	
	public String login(){
		User u = ds.find(User.class)
					.field("username").equal(loginController.getLogin())
					.field("password").equal(loginController.getPassword()).get();
		
		if (u != null) {			
			loggedUser = u;
			Flash flash = FacesContext.getCurrentInstance().  
	                getExternalContext().getFlash();	
			flash.put("success", "Bem vindo ao Periscope");
			flash.keep("success");
			return "login";
		}else {
			FacesMessage msg = new FacesMessage("Usuário/Senha inválidos.","Erro");  
	        FacesContext.getCurrentInstance().addMessage(null, msg);
	        return null;		
		}
	}

	public String logout() {	
		loggedUser = null;
		return "logout";
	}

	public boolean isLoggedIn() {
		return loggedUser != null;
	}

	public User getCurrentUser() {
		return loggedUser;
	}

}
