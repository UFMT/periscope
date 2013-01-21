package br.ufmt.periscope.repository;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.bson.types.ObjectId;

import br.ufmt.periscope.model.Patent;
import br.ufmt.periscope.model.Project;
import br.ufmt.periscope.model.User;

import com.github.jmkgreen.morphia.Datastore;
import com.github.jmkgreen.morphia.query.Query;

@Named
public class ProjectRepository {

	@Inject
	private Datastore ds;
		
	public List<Project> getProjectList(User user){
		Query<Project> query = ds.createQuery(Project.class);
		query.or(
				query.criteria("owner").equal(user),				 
				query.criteria("observers").hasThisElement(user),
				query.criteria("isPublic").equal(true)
			);
		return query.asList();
	}
	
	public void deleteProject(String id){
		Project p = new Project();
		p.setId(new ObjectId(id));
		deleteProject(p);			
	}
	
	public void deleteProject(Project project){
		ds.delete(ds.createQuery(Patent.class).field("project").equal(project));
		ds.delete(project);
	}
}
