package br.ufmt.periscope.repository;

import br.ufmt.periscope.importer.PatentImporter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import br.ufmt.periscope.indexer.PatentIndexer;
import br.ufmt.periscope.model.Patent;
import br.ufmt.periscope.model.Project;

import com.github.jmkgreen.morphia.Datastore;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
import java.util.Date;

@Named
public class PatentRepository {

    @Inject
    private Datastore ds;
    private @Inject
    PatentIndexer patentIndexer;

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

    public List<Patent> getPatentWithApplicant(Project project, String applicantName) {
        return ds.find(Patent.class)
                .field("project").equal(project)
                .field("applicants.name").equal(applicantName)
                .asList();
    }
    
    public Date getMinDate(Project project){
        DBCursor dbc = ds.getCollection(Patent.class).find(new BasicDBObject("project.$id" ,project.getId())).sort(new BasicDBObject("applicationDate", 1)).limit(1);
        Date data = (Date) dbc.next().get("applicationDate");
        return data;
    }
    
    public Date getMaxDate(Project project) {
        DBCursor dbc = ds.getCollection(Patent.class).find(new BasicDBObject("project.$id" ,project.getId())).sort(new BasicDBObject("publicationDate", -1)).limit(1);
        Date data = (Date) dbc.next().get("publicationDate");
        return data;
    }
}
