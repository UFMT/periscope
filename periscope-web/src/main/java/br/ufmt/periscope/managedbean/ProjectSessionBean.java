package br.ufmt.periscope.managedbean;

import br.ufmt.periscope.model.Project;
import br.ufmt.periscope.qualifier.CurrentProject;
import com.github.jmkgreen.morphia.Datastore;
import com.github.jmkgreen.morphia.mapping.lazy.DatastoreHolder;
import com.github.jmkgreen.morphia.query.Query;
import java.io.Serializable;
import javax.annotation.PostConstruct;
import javax.enterprise.context.SessionScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Named;
import org.bson.types.ObjectId;

@Named("projectSession")
@SessionScoped
public class ProjectSessionBean implements Serializable {

    private static final long serialVersionUID = -202445705543842694L;

    private Datastore ds;
    private Project currentProject;

    @PostConstruct
    public void init() {
        ds = DatastoreHolder.getInstance().get();
    }

    public String openProject(String idProject) {
        Query<Project> query = ds.createQuery(Project.class);
        query.retrievedFields(false, "patents").field("_id").equal(new ObjectId(idProject));
        currentProject = query.asList().get(0);

        if (isProjectSelected()) {
            return "projectHome";
        } else {
            return null;
        }
    }

    public boolean isProjectSelected() {
        return currentProject != null;
    }

    @Named
    @Produces
    @CurrentProject
    public Project getCurrentProject() {
        return currentProject;
    }

}
