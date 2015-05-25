package br.ufmt.periscope.repository;

import br.ufmt.periscope.indexer.PatentIndexer;
import br.ufmt.periscope.model.Patent;
import br.ufmt.periscope.model.Project;
import br.ufmt.periscope.model.Rule;
import br.ufmt.periscope.model.User;
import com.github.jmkgreen.morphia.Datastore;
import com.github.jmkgreen.morphia.query.Query;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.DBRef;
import com.mongodb.Mongo;
import com.mongodb.gridfs.GridFS;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.inject.Named;
import org.bson.types.ObjectId;

@Named
public class ProjectRepository {

    private @Inject
    Datastore ds;
    private @Inject
    PatentIndexer patentIndexer;
    private @Inject
    Instance<GridFS> fsProvider;

    public List<Project> getProjectList(User user) {
        Query<Project> query = ds.createQuery(Project.class);
        query.retrievedFields(false, "patents");
        query.order("title");
        if (user.getUserLevel().getAccessLevel() != 10) {

            query.or(
                    query.criteria("owner").equal(user),
                    query.criteria("observers").hasThisElement(user),
                    query.criteria("isPublic").equal(true));
        }
        List<Project> projetos = query.asList();
        return projetos;
    }

    public List<String> getProjectFiles(Project project) {
        DBObject matchProj = new BasicDBObject();
        matchProj.put("project.$id", project.getId());

        DBObject param = new BasicDBObject("presentationFile", 1);
        param.put("patentInfo", 1);
        param.put("_id", 0);

        List<String> lista = new ArrayList<String>();
        DBCursor c = ds.getCollection(Patent.class).find(matchProj, param);

        while (c.hasNext()) {
            DBObject novo = c.next();
            DBRef preFile = (DBRef) novo.get("presentationFile");
            DBRef pInfo = (DBRef) novo.get("patentInfo");
            if (preFile != null) {
                lista.add(preFile.getId().toString());
            }
            if (pInfo != null) {
                lista.add(pInfo.getId().toString());
            }
        }
        if (lista.size() > 0) {
            return lista;
        }
        return null;
    }

    public void deleteProject(String id) throws UnknownHostException {
        Project p = new Project();
        p.setId(new ObjectId(id));
        patentIndexer.deleteIndexesForProject(p);
        List<String> files = getProjectFiles(p);

        if (files != null) {
            GridFS fs = fsProvider.get();
            ObjectId _id;
            for (String file : files) {
                _id = new ObjectId(file);
                fs.remove(_id);
                _id = null;
            }
        }

        deleteProject(p);
    }

//    public GridFS getFs() throws UnknownHostException {
//        Mongo mongo = new Mongo("localhost", 27017);
//        DB db = mongo.getDB("Periscope");
//        GridFS fs = new GridFS(db);
//        return fs;
//    }
    public boolean isEmptyPatent(Project currentProject) {
        return ds.getCollection(Project.class).findOne(new BasicDBObject("_id", currentProject.getId()), new BasicDBObject("patents", new BasicDBObject("$slice", 1))).get("patents") == null;
    }

    public void deleteProject(Project project) {
        ds.delete(ds.createQuery(Patent.class).field("project").equal(project));
        ds.delete(ds.createQuery(Rule.class).field("project").equal(project));
        ds.delete(project);
    }
}
