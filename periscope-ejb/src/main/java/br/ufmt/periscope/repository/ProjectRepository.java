package br.ufmt.periscope.repository;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.bson.types.ObjectId;

import br.ufmt.periscope.indexer.PatentIndexer;
import br.ufmt.periscope.model.Patent;
import br.ufmt.periscope.model.Project;
import br.ufmt.periscope.model.Rule;
import br.ufmt.periscope.model.User;

import com.github.jmkgreen.morphia.Datastore;
import com.github.jmkgreen.morphia.query.Query;

@Named
public class ProjectRepository {
	
	private @Inject Datastore ds;	
	private @Inject PatentIndexer patentIndexer;
		
	public List<Project> getProjectList(User user){
		Query<Project> query = ds.createQuery(Project.class);
                if (user.getUserLevel().getAccessLevel() != 10) {
                    
                    query.or(
                            query.criteria("owner").equal(user),
                            query.criteria("observers").hasThisElement(user),
                            query.criteria("isPublic").equal(true));
                }
		return query.asList();
	}
	
	public void deleteProject(String id){
		Project p = new Project();
		p.setId(new ObjectId(id));
		patentIndexer.deleteIndexesForProject(p);
		deleteProject(p);		
	}
	
	public void deleteProject(Project project){
		ds.delete(ds.createQuery(Patent.class).field("project").equal(project));
		ds.delete(ds.createQuery(Rule.class).field("project").equal(project));
		ds.delete(project);
	}
}
