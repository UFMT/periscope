package br.ufmt.periscope.controller;

import br.ufmt.periscope.model.Project;
import br.ufmt.periscope.qualifier.CurrentProject;
import com.github.jmkgreen.morphia.Datastore;
import com.mongodb.BasicDBObject;
import javax.faces.bean.ManagedBean;
import javax.inject.Inject;

@ManagedBean(name = "sidebarController")
public class SidebarController {

    private @Inject @CurrentProject
    Project currentProject;
    
    private @Inject Datastore ds;

    public boolean isEmptyPatent() {
        return ds.getCollection(Project.class).findOne(new BasicDBObject("_id", currentProject.getId()), new BasicDBObject("patents", new BasicDBObject("$slice", 1))).get("patents") == null;
    }

}
