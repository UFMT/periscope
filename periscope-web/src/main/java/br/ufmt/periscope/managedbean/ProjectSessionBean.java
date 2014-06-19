package br.ufmt.periscope.managedbean;

import br.ufmt.periscope.model.Patent;
import java.io.Serializable;

import javax.annotation.PostConstruct;
import javax.enterprise.context.SessionScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Named;

import org.bson.types.ObjectId;

import br.ufmt.periscope.model.Project;
import br.ufmt.periscope.qualifier.CurrentProject;

import com.github.jmkgreen.morphia.Datastore;
import com.github.jmkgreen.morphia.mapping.lazy.DatastoreHolder;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import java.util.List;

@Named("projectSession")
@SessionScoped
public class ProjectSessionBean implements Serializable {

    private static final long serialVersionUID = -202445705543842694L;

    private Datastore ds;
    private Project projectInstance;

    @PostConstruct
    public void init() {
        ds = DatastoreHolder.getInstance().get();
    }

    public String openProject(String idProject) {
      //  projectInstance = ds.get(Project.class, new ObjectId(idProject));
        projectInstance = new Project();
        
        DBObject resultProject = ds.getCollection(Project.class).findOne(new BasicDBObject("_id", new ObjectId(idProject)), new BasicDBObject("patents", new BasicDBObject("$slice", 1)));
       
        projectInstance.setDescription((String) resultProject.get("description"));
        projectInstance.setTitle((String) resultProject.get("title"));
        projectInstance.setId((ObjectId) resultProject.get("_id"));
        projectInstance.setIsPublic((Boolean) resultProject.get("isPublic"));
        projectInstance.setPatents((List<Patent>) resultProject.get("patents"));
      
        if (isProjectSelected()) {
            return "projectHome";
        } else {
            return null;
        }
    }

    public boolean isProjectSelected() {
        return projectInstance != null;
    }

    @Named
    @Produces
    @CurrentProject
    public Project getCurrentProject() {
        return ds.get(Project.class, projectInstance.getId());
    }

}
