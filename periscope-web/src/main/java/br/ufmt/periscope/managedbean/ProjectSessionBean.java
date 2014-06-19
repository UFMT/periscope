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
    private Project openProject, currentProject;

    @PostConstruct
    public void init() {
        ds = DatastoreHolder.getInstance().get();
    }

    public String openProject(String idProject) {
      //  projectInstance = ds.get(Project.class, new ObjectId(idProject));
        
        openProject = new Project();
        DBObject resultProject = ds.getCollection(Project.class).findOne(new BasicDBObject("_id", new ObjectId(idProject)), new BasicDBObject("patents", new BasicDBObject("$slice", 1)));
       
        openProject.setDescription((String) resultProject.get("description"));
        openProject.setTitle((String) resultProject.get("title"));
        openProject.setId((ObjectId) resultProject.get("_id"));
        openProject.setIsPublic((Boolean) resultProject.get("isPublic"));
        openProject.setPatents((List<Patent>) resultProject.get("patents"));
       
        if (isProjectSelected()) {
            return "projectHome";
        } else {
            return null;
        }
    }

    public boolean isProjectSelected() {
        return openProject != null;
    }
    
    private boolean currentProjectIsNull(){
        return currentProject == null;
    }
    
    @Named
    @Produces
    @CurrentProject
    public Project getCurrentProject() {
        if(currentProjectIsNull())
            currentProject = ds.get(Project.class, openProject.getId());
        return currentProject;
    }

}