package br.ufmt.periscope.controller;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import javax.faces.model.DataModel;
import javax.faces.model.ListDataModel;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

import org.bson.types.ObjectId;

import br.ufmt.periscope.model.Project;
import br.ufmt.periscope.model.User;

import com.github.jmkgreen.morphia.Datastore;
import com.mongodb.WriteResult;

@ManagedBean
@ViewScoped
public class ProjectController {		

	private @Inject Datastore ds; 
	private DataModel<Project> projects = null;
	private Project project = new Project();
	private List<User> freeUsers = null;
	private String selectedUser = null;
	private boolean editing = false;
	
	@PostConstruct
	public void init(){
		FacesContext context = FacesContext.getCurrentInstance();
		HttpServletRequest req = (HttpServletRequest) context.getExternalContext().getRequest();		
		if(req.getParameter("projectId") != null){
			editing = true;
			project = ds.get(Project.class,new ObjectId(req.getParameter("projectId")));
			
		}
	}
		
	public void adduser(){
		
		if(selectedUser != null && selectedUser.trim().length() > 0){
		
			ObjectId key = new ObjectId(selectedUser);
			User user = ds.get(User.class,key);
			if(user != null){
				
				this.project.getObservers().add(user);
				freeUsers = null;
			}
		}
		
	}
	public String create(){	
		project.setCreatedAt(new Date());
		project.setUpdateAt(new Date());
		ds.save(project);		
		return "projectList";
	}
	
	public String save(){
		project.setUpdateAt(new Date());		
		ds.save(project);		
		return "projectList";
	}

	public String delete(String id){
		WriteResult result = ds.delete(Project.class,new ObjectId(id));		
		return "projectList";		
	}
	
	public DataModel<Project> getProjects() {
		if(projects == null){
			projects = new ListDataModel<Project>(ds.find(Project.class).asList());
		}
		return projects;
	}

	public void setProjects(DataModel<Project> projects) {
		this.projects = projects;
	}

	public Project getProject() {
		return project;
	}

	public void setProject(Project project) {
		this.project = project;
	}

	public List<User> getFreeUsers() {
		if(freeUsers == null){
			List<ObjectId> keys = new ArrayList<ObjectId>();
			for(User u : project.getObservers()){
				keys.add(u.getId());
			}
			if(keys.isEmpty()){
				freeUsers = ds.createQuery(User.class).asList();
			}else{
				freeUsers = ds.createQuery(User.class).field("id").notIn(keys).asList();
			}			
		}
		return freeUsers;
	}

	public void setFreeUsers(List<User> freeUsers) {
		this.freeUsers = freeUsers;
	}

	public String getSelectedUser() {
		return selectedUser;
	}

	public void setSelectedUser(String selectedUser) {
		this.selectedUser = selectedUser;
	}

	public boolean isEditing() {
		return editing;
	}

	public void setEditing(boolean editing) {
		this.editing = editing;
	}

	
	
	
	
}
