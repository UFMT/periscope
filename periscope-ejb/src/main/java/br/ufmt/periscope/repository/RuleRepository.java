package br.ufmt.periscope.repository;

import br.ufmt.periscope.model.Patent;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.bson.types.ObjectId;

import br.ufmt.periscope.model.Project;
import br.ufmt.periscope.model.Rule;
import br.ufmt.periscope.model.RuleType;

import com.github.jmkgreen.morphia.Datastore;
import com.github.jmkgreen.morphia.Key;
import com.github.jmkgreen.morphia.query.Query;
import com.mongodb.DB;
import java.util.Map;

@Named
public class RuleRepository {

    private @Inject
    Datastore ds;
    private Integer rowCount = null;
    private @Inject
    Project currentProject;
    private Integer searchType = null;

    public List<Rule> load(int first, int pageSize, String sortField, int sortOrder, Map<String, String> filters) {

        Query query;
        if (this.searchType != null && this.searchType == 1) {
            query = ds.find(Rule.class)
                    .field("project").equal(this.currentProject)
                    .field("type").equal(RuleType.APPLICANT);
        } else {
            query = ds.find(Rule.class)
                    .field("project").equal(this.currentProject)
                    .field("type").equal(RuleType.INVENTOR);
        }

        if (sortField != null) {
            query = query.order((sortOrder == 1 ? "-" : "") + sortField);
        }
        for (Map.Entry<String, String> entry : filters.entrySet()) {
            String column = entry.getKey();
            String value = entry.getValue();
            query.field(column).containsIgnoreCase(value);
        }
        setRowCount((int) query.countAll());
        query.offset(first).limit(pageSize);
        if (this.searchType != null && this.searchType == 1) {
            query.retrievedFields(true, "_id", "name", "acronym", "substitutions", "country", "state", "type", "nature");
        } else {
            query.retrievedFields(true, "_id", "name", "acronym", "substitutions", "country", "state", "type");
        }
        return query.asList();
    }

    public List<Rule> getAllRule(Project project) {
//        Long ini = System.currentTimeMillis();
        try {
            return ds.find(Rule.class)
                    .field("project").equal(project)
                    .retrievedFields(true, "_id", "name", "acronym", "substitutions", "country", "state", "type", "nature")
                    .asList();
        } finally {
//            System.out.println("Tempo " + (System.currentTimeMillis() - ini));
        }
    }
    
    public void undoApplicantRule(Project project, String name){
        DB db = ds.getCollection(Patent.class).getDB();
        String fnc = "function(project, name){"
                +       "db.Patent.find({\"project.$id\": project"
                +       ", \"applicants.name\" : name}).forEach(function(pa){"
                +           "var newAps = [];"
                +           "pa.applicants.forEach(function(ap){"
                +               "if(ap.name = name){"
                +                   "ap.name = ap.history.name;"
                +                   "ap.harmonized = false;"
                +                   "ap.country.name = ap.history.country.name;"
                +                   "ap.country.acronym = ap.history.country.acronym;"
                +               "}"
                +               "newAps.push(ap);"
                +           "});"
                +           "db.Patent.update({ _id: pa._id },{ \"$set\": { \"applicants\": newAps } });"
                +       "});"
                +   "};";
        System.out.println(db.eval(fnc, project.getId(), name));
    }

    public void undoInventorRule(Project project, String name){
        DB db = ds.getCollection(Patent.class).getDB();
        String fnc = "function(project, name){"
                +       "db.Patent.find({\"project.$id\": project"
                +       ", \"inventors.name\" : name}).forEach(function(pa){"
                +           "var newInvs = [];"
                +           "pa.inventors.forEach(function(inv){"
                +               "if(inv.name = name){"
                +                   "inv.name = inv.history.name;"
                +                   "inv.harmonized = false;"
                +                   "inv.country.name = inv.history.country.name;"
                +                   "inv.country.acronym = inv.history.country.acronym;"
                +               "}"
                +               "newInvs.push(inv);"
                +           "});"
                +           "db.Patent.update({ _id: pa._id },{ \"$set\": { \"inventors\": newInvs } });"
                +       "});"
                +   "};";
        System.out.println(db.eval(fnc, project.getId(), name));
    }
    
    public List<Rule> getApplicantRule(Project project) {
        return ds.find(Rule.class)
                .field("project").equal(project)
                .field("type").equal(RuleType.APPLICANT)
                .asList();
    }

    public List<Rule> getInventorRule(Project project) {
        return ds.find(Rule.class)
                .field("project").equal(project)
                .field("type").equal(RuleType.INVENTOR)
                .asList();
    }

    public void save(Rule rule) {
        Rule r = findByName(rule.getName());
        if (r != null) {
            delete(r.getId().toString());
            rule.getSubstitutions().addAll(r.getSubstitutions());

        }
        Key k = ds.save(rule);
    }
    
    public Boolean isRule(String name){
        return !(ds.find(Rule.class).field("name").equal(name).get() == null);
    }

    public Rule findByName(String name) {
        return ds.find(Rule.class).field("name").equal(name).get();
    }

    public Rule findById(String id) {
        return ds.get(Rule.class, new ObjectId(id));
    }

    public void delete(String id) {
        ds.delete(Rule.class, new ObjectId(id));
    }

    public int getRowCount() {
        if (rowCount == null) {

            Query query = ds.find(Rule.class)
                    .field("project").equal(this.currentProject);

            rowCount = (int) query.countAll();
        }
        return rowCount;
    }

    public void setRowCount(int rowCount) {
        this.rowCount = rowCount;
    }

    public Datastore getDs() {
        return ds;
    }

    public void setDs(Datastore ds) {
        this.ds = ds;
    }

    public Project getCurrentProject() {
        return currentProject;
    }

    public void setCurrentProject(Project currentProject) {
        this.currentProject = currentProject;
    }

    public Integer getSearchType() {
        return searchType;
    }

    public void setSearchType(Integer searchType) {
        this.searchType = searchType;
    }

}
