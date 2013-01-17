package br.ufmt.periscope.controller;

import java.util.Arrays;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import javax.faces.context.Flash;
import javax.faces.model.DataModel;
import javax.faces.model.ListDataModel;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

import org.bson.types.ObjectId;

import br.ufmt.periscope.model.User;
import br.ufmt.periscope.model.UserLevel;

import com.github.jmkgreen.morphia.Datastore;
import com.mongodb.WriteResult;

@ManagedBean
@ViewScoped
public class UserController {		

	private @Inject Datastore ds; 
	private DataModel<User> users = null;
	private User user = new User();
	private boolean editing = false;
	private List<UserLevel> levels = Arrays.asList(UserLevel.values());
	
	@PostConstruct
	public void init(){
		FacesContext context = FacesContext.getCurrentInstance();
		HttpServletRequest req = (HttpServletRequest) context.getExternalContext().getRequest();		
		if(req.getParameter("userId") != null){
			editing = true;
			user = ds.get(User.class,new ObjectId(req.getParameter("userId")));
			
		}
	}
		
	public String save(){		
		ds.save(user);	
		Flash flash = FacesContext.getCurrentInstance().  
                getExternalContext().getFlash();
		flash.put("success", "Salvo com Sucesso");		
		return "userList";
	}

	public String delete(String id){
		WriteResult result = ds.delete(User.class,new ObjectId(id));
		Flash flash = FacesContext.getCurrentInstance().  
                getExternalContext().getFlash();
		flash.put("info", "Deletado com sucesso");
		return "userList";		
	}
	
	public DataModel<User> getUsers() {		
		if(users == null){
			users = new ListDataModel<User>(ds.find(User.class).asList());
		}
		return users;
	}

	public void setUsers(DataModel<User> users) {
		this.users = users;
	}

	public boolean isEditing() {
		return editing;
	}

	public void setEditing(boolean editing) {
		this.editing = editing;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public List<UserLevel> getLevels() {
		return levels;
	}

	public void setLevels(List<UserLevel> levels) {
		this.levels = levels;
	}
	
}
