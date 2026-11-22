package br.ufmt.periscope.repository;

import br.ufmt.periscope.importer.PatentImporter;
import br.ufmt.periscope.importer.decorator.PatentValidator;
import br.ufmt.periscope.indexer.PatentIndexer;
import br.ufmt.periscope.model.Patent;
import br.ufmt.periscope.model.Project;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.Updates;
import dev.morphia.Datastore;
import dev.morphia.query.FindOptions;
import dev.morphia.query.Query;
import static dev.morphia.query.filters.Filters.eq;
import static dev.morphia.query.filters.Filters.regex;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import jakarta.inject.Inject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;
import org.bson.Document;
import org.bson.types.ObjectId;

@ApplicationScoped
@Named
public class PatentRepository {

    @Inject
    private Datastore ds;
    @Inject
    private PatentIndexer patentIndexer;
    private Project currentProject;
    @Inject
    private PatentValidator validator;
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
                patentIndexer.indexPatents(patentsCache, project);
                patentsCache.clear();
                cont = 0;
            }
        }
        if (cont > 0) {
            ds.save(patentsCache);
            ds.save(project);
            patentIndexer.indexPatents(patentsCache, project);
            patentsCache.clear();
        }

    }

    public void savePatentToDatabase(Patent p, Project project) {
        p.setProject(project);
        if (project.getPatents() == null) {
            project.setPatents(new ArrayList<Patent>());
        }
        if (!patentExistsForProject(p, project)) {
            validator.validate(p);
            project.getPatents().add(p);
            ds.save(p);
            ds.save(project);
            patentIndexer.indexPatent(p);
        }
    }

    public boolean patentExistsForProject(Patent patent, Project project) {
        if (patent.getPublicationNumber() != null && !patent.getPublicationNumber().equals("")) {
            return ds.find(Patent.class)
                    .filter(
                            eq("publicationNumber", patent.getPublicationNumber()),
                            eq("project", project))
                    .count() > 0;
        }
        return ds.find(Patent.class)
                .filter(
                        eq("applicationNumber", patent.getApplicationNumber()),
                        eq("project", project))
                .count() > 0;
    }

    public void sendPatentToBlacklist(Patent patent) {
        MongoCollection<Document> coll = ds.getDatabase()
                .getCollection(ds.getMapper().getEntityModel(Patent.class).getCollectionName());
        coll.updateOne(
                Filters.eq("_id", patent.getId()),
                Updates.set("blacklisted", !patent.getBlacklisted()));
    }

    public void savePatent(Patent patent) {
        validator.validate(patent);
        patentIndexer.indexPatent(patent);
        ds.save(patent);
    }

    public List<Patent> getPatentsComplete(Project project, Boolean complete) {
        return ds.find(Patent.class)
                .filter(
                        eq("completed", complete),
                        eq("blacklisted", false),
                        eq("project", project))
                .iterator().toList();
    }

    public List<Patent> getPatentsDarklist(Project project, Boolean darklist) {
        return ds.find(Patent.class)
                .filter(
                        eq("blacklisted", darklist),
                        eq("project", project))
                .iterator().toList();
    }

    public List<Patent> getAllPatents(Project project) {
        return ds.find(Patent.class)
                .filter(eq("project", project))
                .iterator().toList();
    }

    public List<Patent> getPatentWithId(Project project, ObjectId id) {
        return ds.find(Patent.class)
                .filter(
                        eq("project", project),
                        eq("_id", id))
                .iterator().toList();
    }

    public List<Patent> getPatentWithApplicant(Project project, String applicantName) {
        return ds.find(Patent.class)
                .filter(
                        eq("project", project),
                        eq("applicants.name", applicantName))
                .iterator().toList();
    }

    public Date getMinDate(Project currentProject, int selectedDate) {
        String date = selectedDate == 2 ? "applicationDate" : "publicationDate";
        MongoCollection<Document> coll = ds.getDatabase()
                .getCollection(ds.getMapper().getEntityModel(Patent.class).getCollectionName());
        Document doc = coll.find(Filters.and(
                        Filters.eq("project.$id", currentProject.getId()),
                        Filters.eq("blacklisted", false)))
                .sort(Sorts.ascending(date))
                .limit(1)
                .first();
        return doc == null ? null : doc.getDate(date);
    }

    public Date getMaxDate(Project currentProject, int selectedDate) {
        String date = selectedDate == 2 ? "applicationDate" : "publicationDate";
        MongoCollection<Document> coll = ds.getDatabase()
                .getCollection(ds.getMapper().getEntityModel(Patent.class).getCollectionName());
        Document doc = coll.find(Filters.and(
                        Filters.eq("project.$id", currentProject.getId()),
                        Filters.eq("blacklisted", false)))
                .sort(Sorts.descending(date))
                .limit(1)
                .first();
        return doc == null ? null : doc.getDate(date);
    }

    public List<Patent> load(int first, int pageSize, String sortField, int sortOrder, Map<String, String> filters) {
        Query<Patent> query = ds.find(Patent.class)
                .filter(
                        eq("project", this.currentProject),
                        eq("blacklisted", Boolean.TRUE.equals(this.blacklisted)));
        if (this.completed != null) {
            query.filter(eq("completed", this.completed));
        }
        applyContainsFilters(query, filters);
        setRowCount((int) query.count());

        FindOptions options = new FindOptions().skip(first).limit(pageSize);
        applySort(options, sortField, sortOrder);
        // Do NOT include "project": Morphia @Reference resolves it while the
        // outer cursor is open and exhausts the Mongo connection pool.
        options.projection().include(
                "_id", "applicationCountry", "titleSelect", "mainClassification",
                "publicationDate", "publicationNumber", "applicationDate", "applicationNumber",
                "applicants", "inventors",
                "blacklisted", "presentationFile", "patentInfo");
        return query.iterator(options).toList();
    }

    public List<Patent> loadApplicantDocs(int first, int pageSize, String sortField, int sortOrder,
            Map<String, String> filters, String name) {
        Query<Patent> query = ds.find(Patent.class)
                .filter(
                        eq("project", this.currentProject),
                        eq("applicants.name", name));
        applyContainsFilters(query, filters);
        setRowCount((int) query.count());

        FindOptions options = new FindOptions().skip(first).limit(pageSize);
        applySort(options, sortField, sortOrder);
        options.projection().include("titleSelect", "applicationNumber", "applicants", "inventors");
        return query.iterator(options).toList();
    }

    public List<Patent> loadInventorDocs(int first, int pageSize, String sortField, int sortOrder,
            Map<String, String> filters, String name) {
        Query<Patent> query = ds.find(Patent.class)
                .filter(
                        eq("project", this.currentProject),
                        eq("inventors.name", name));
        applyContainsFilters(query, filters);
        setRowCount((int) query.count());

        FindOptions options = new FindOptions().skip(first).limit(pageSize);
        applySort(options, sortField, sortOrder);
        options.projection().include("titleSelect", "applicationNumber", "applicants", "inventors");
        return query.iterator(options).toList();
    }

    public List<Patent> loadBrazilian(int first, int pageSize, String sortField, int sortOrder,
            Map<String, String> filters) {
        PatentPage page = loadBrazilianPage(
                this.currentProject,
                Boolean.TRUE.equals(this.blacklisted),
                first, pageSize, sortField, sortOrder, filters);
        setRowCount((int) page.total());
        return page.content();
    }

    public PatentPage loadBrazilianPage(Project project, boolean blacklistedFlag,
            int first, int pageSize, String sortField, int sortOrder,
            Map<String, String> filters) {
        Query<Patent> query = ds.find(Patent.class)
                .filter(
                        eq("project", project),
                        eq("blacklisted", blacklistedFlag),
                        eq("priorities.country.acronym", "BR"));
        applyContainsFilters(query, filters);
        long total = query.count();

        FindOptions options = new FindOptions().skip(first).limit(pageSize);
        applySort(options, sortField, sortOrder);
        options.projection().include(
                "_id", "titleSelect", "mainClassification", "publicationDate", "publicationNumber",
                "applicationDate", "applicationNumber", "applicationCountry",
                "applicants", "inventors", "blacklisted", "completed",
                "presentationFile", "patentInfo");
        return new PatentPage(query.iterator(options).toList(), total);
    }

    public record PatentPage(List<Patent> content, long total) {
    }

    public String findProjectIdByFileId(ObjectId fileId) {
        if (fileId == null) {
            return null;
        }
        MongoCollection<Document> coll = ds.getDatabase()
                .getCollection(ds.getMapper().getEntityModel(Patent.class).getCollectionName());
        Document raw = coll.find(Filters.or(
                        Filters.eq("presentationFile.$id", fileId),
                        Filters.eq("patentInfo.$id", fileId),
                        Filters.eq("presentationFile", fileId),
                        Filters.eq("patentInfo", fileId)))
                .projection(com.mongodb.client.model.Projections.include("project"))
                .first();
        if (raw == null) {
            return null;
        }
        return extractRefId(raw.get("project"));
    }

    private static String extractRefId(Object ref) {
        if (ref == null) {
            return null;
        }
        if (ref instanceof com.mongodb.DBRef dbRef) {
            Object id = dbRef.getId();
            return id != null ? id.toString() : null;
        }
        if (ref instanceof Document doc) {
            Object id = doc.get("$id");
            if (id == null) {
                id = doc.get("_id");
            }
            return id != null ? id.toString() : null;
        }
        if (ref instanceof ObjectId oid) {
            return oid.toString();
        }
        return null;
    }

    private void applyContainsFilters(Query<Patent> query, Map<String, String> filters) {
        if (filters == null) {
            return;
        }
        for (Map.Entry<String, String> entry : filters.entrySet()) {
            Pattern pattern = Pattern.compile(Pattern.quote(entry.getValue()), Pattern.CASE_INSENSITIVE);
            query.filter(regex(entry.getKey(), pattern));
        }
    }

    private void applySort(FindOptions options, String sortField, int sortOrder) {
        if (sortField == null) {
            return;
        }
        if (sortOrder == 1) {
            options.sort(dev.morphia.query.Sort.descending(sortField));
        } else {
            options.sort(dev.morphia.query.Sort.ascending(sortField));
        }
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
            rowCount = (int) ds.find(Patent.class)
                    .filter(eq("project", this.currentProject))
                    .count();
        }
        return rowCount;
    }

    public void setRowCount(int rowCount) {
        this.rowCount = rowCount;
    }

}
