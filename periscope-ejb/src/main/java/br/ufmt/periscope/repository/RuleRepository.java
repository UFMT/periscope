package br.ufmt.periscope.repository;

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
        query.retrievedFields(true, "_id", "name", "acronym", "substitutions", "country", "state", "type");
        return query.asList();
    }

    public List<Rule> getAllRule(Project project) {
//        Long ini = System.currentTimeMillis();
        try {
            return ds.find(Rule.class)
                    .field("project").equal(project)
                    .retrievedFields(true, "_id", "name", "acronym", "substitutions", "country", "state", "type")
                    .asList();
        } finally {
//            System.out.println("Tempo " + (System.currentTimeMillis() - ini));
        }
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

    public Rule findByName(String name) {
        return ds.find(Rule.class).field("name").equal(name).get();
    }

    public Rule findById(String id) {
        return ds.get(Rule.class, new ObjectId(id));
    }

    public void delete(String id) {
        System.out.println("oi");
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