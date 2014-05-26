package br.ufmt.periscope.repository;

import br.ufmt.periscope.importer.PatentImporter;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import br.ufmt.periscope.indexer.PatentIndexer;
import br.ufmt.periscope.model.Patent;
import br.ufmt.periscope.model.Project;

import com.github.jmkgreen.morphia.Datastore;
import com.github.jmkgreen.morphia.query.Query;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCursor;
import com.mongodb.Mongo;
import com.mongodb.gridfs.GridFS;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.Map;
import org.bson.types.ObjectId;

@Named
public class PatentRepository {

    @Inject
    private Datastore ds;
    private @Inject
    PatentIndexer patentIndexer;
    private @Inject
    Project currentProject;
    private Boolean completed;
    private Boolean blacklisted;
    private Integer rowCount = null;

    public void savePatentToDatabase(PatentImporter patents, Project project) {
        List<Patent> patentsCache = new ArrayList<Patent>();
        int cont = 0;
        if (project.getPatents() == null) {
            project.setPatents(new ArrayList<Patent>());
        }
        while (patents.hasNext()) {
            Patent p = patents.next();
            if (p == null) {
                continue;
            }
            p.setProject(project);

            if (!patentExistsForProject(p, project)) {
                project.getPatents().add(p);
                patentsCache.add(p);
                cont++;
            }
            if (cont >= 30) {
                ds.save(patentsCache);
                ds.save(project);
                patentIndexer.indexPatents(patentsCache);
                patentsCache.clear();
                cont = 0;
            }
        }
        if (cont > 0) {
            ds.save(patentsCache);
            ds.save(project);
            patentIndexer.indexPatents(patentsCache);
            patentsCache.clear();
        }

    }

    public boolean patentExistsForProject(Patent patent, Project project) {
        return ds.find(Patent.class)
                .field("publicationNumber").equal(patent.getPublicationNumber())
                .field("project").equal(project)
                .countAll() > 0;
    }

    public void sendPatentToBlacklist(Patent patent) {
        patent.setBlacklisted(!patent.getBlacklisted());
        ds.save(patent);
    }

    public void savePatent(Patent patent) {
        ds.save(patent);
    }

    public List<Patent> getPatentsComplete(Project project, Boolean complete) {
        return ds.find(Patent.class)
                .field("completed").equal(complete)
                .field("blacklisted").equal(false)
                .field("project").equal(project)
                .asList();
    }

    public List<Patent> getPatentsDarklist(Project project, Boolean darklist) {
        return ds.find(Patent.class)
                .field("blacklisted").equal(darklist)
                .field("project").equal(project)
                .asList();
    }

    public List<Patent> getAllPatents(Project project) {
        return ds.find(Patent.class)
                .field("project").equal(project)
                .asList();
    }

    public List<Patent> getPatentWithId(Project project, ObjectId id) {

        return ds.find(Patent.class)
                .field("project").equal(project)
                .field("_id").equal(id)
                .asList();

    }

    public List<Patent> getPatentWithApplicant(Project project, String applicantName) {
        return ds.find(Patent.class)
                .field("project").equal(project)
                .field("applicants.name").equal(applicantName)
                .asList();
    }

    public Date getMinDate(Project currentProject) {
        DBCursor dbc = ds.getCollection(Patent.class).find(new BasicDBObject("project.$id", currentProject.getId())).sort(new BasicDBObject("applicationDate", 1)).limit(1);
        Date data = (Date) dbc.next().get("applicationDate");
        System.out.println(data);
        return data;
    }

    public Date getMaxDate(Project currentProject) {
        DBCursor dbc = ds.getCollection(Patent.class).find(new BasicDBObject("project.$id", currentProject.getId())).sort(new BasicDBObject("publicationDate", -1)).limit(1);
        Date data = (Date) dbc.next().get("publicationDate");
        return data;
    }

    public GridFS getFs() throws UnknownHostException {
        Mongo mongo = new Mongo("localhost", 27017);
        DB db = mongo.getDB("Periscope");
        GridFS fs = new GridFS(db);
        return fs;
    }

    public List<Patent> load(int first, int pageSize, String sortField, int sortOrder, Map<String, String> filters) {
        Query query = ds.find(Patent.class)
                .field("project").equal(this.currentProject)
                .field("completed").equal(this.completed)
                .field("blacklisted").equal(this.blacklisted);

        if (sortField != null) {
            query = query.order((sortOrder == 1 ? "-" : "") + sortField);
        }
        for (Map.Entry<String, String> entry : filters.entrySet()) {
            String column = entry.getKey();
            String value = entry.getValue();
            query.field(column).containsIgnoreCase(value);
        }
        setRowCount((int) ds.getCount(query));
        System.out.println("TOTAL:"+getRowCount());
        query.offset(first).limit(pageSize);

        return query.asList();
    }

    public List<Patent> loadBrazilian(int first, int pageSize, String sortField, int sortOrder, Map<String, String> filters) {
        Query query = ds.find(Patent.class)
                .field("project").equal(this.currentProject)
                .field("completed").equal(this.completed)
                .field("blacklisted").equal(this.blacklisted)
                .field("priorities.country.acronym").equal("BR");
        if (sortField != null) {
            query = query.order((sortOrder == 1 ? "-" : "") + sortField);
        }
        for (Map.Entry<String, String> entry : filters.entrySet()) {
            String column = entry.getKey();
            String value = entry.getValue();
            query.field(column).containsIgnoreCase(value);
        }
        setRowCount((int) ds.getCount(query));

        query.offset(first).limit(pageSize);

        return query.asList();
    }

    public Project getCurrentProject() {
        return currentProject;
    }

    public void setCurrentProject(Project currentProject) {
        this.currentProject = currentProject;
    }

    public Boolean getCompleted() {
        return completed;
    }

    public void setCompleted(Boolean completed) {
        this.completed = completed;
    }

    public Boolean getBlacklisted() {
        return blacklisted;
    }

    public void setBlacklisted(Boolean blacklisted) {
        this.blacklisted = blacklisted;
    }

    public int getRowCount() {
        if (rowCount == null) {
            Query query = ds.find(Patent.class)
                    .field("project").equal(this.currentProject)
                    .field("completed").equal(this.completed)
                    .field("blacklisted").equal(this.blacklisted);  
            
            rowCount = (int) ds.getCount(query);
        }
        return rowCount;
    }

    public void setRowCount(int rowCount) {
        this.rowCount = rowCount;
    }

}
