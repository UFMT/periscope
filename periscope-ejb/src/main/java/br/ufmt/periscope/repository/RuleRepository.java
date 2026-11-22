package br.ufmt.periscope.repository;

import br.ufmt.periscope.model.Applicant;
import br.ufmt.periscope.model.History;
import br.ufmt.periscope.model.Inventor;
import br.ufmt.periscope.model.Patent;
import br.ufmt.periscope.model.Project;
import br.ufmt.periscope.model.Rule;
import br.ufmt.periscope.model.RuleType;
import dev.morphia.Datastore;
import dev.morphia.query.FindOptions;
import dev.morphia.query.Query;
import dev.morphia.query.Sort;
import jakarta.inject.Inject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;
import java.util.List;
import java.util.Map;
import org.bson.types.ObjectId;

import static dev.morphia.query.filters.Filters.eq;

@ApplicationScoped
@Named
public class RuleRepository {

    private @Inject
    Datastore ds;
    private Integer rowCount = null;
    private Project currentProject;
    private Integer searchType = null;

    public List<Rule> load(int first, int pageSize, String sortField, int sortOrder,
            Map<String, String> filters) {

        Query<Rule> query;
        if (this.searchType != null && this.searchType == 1) {
            query = ds.find(Rule.class)
                    .filter(eq("project", this.currentProject),
                            eq("type", RuleType.APPLICANT));
        } else {
            query = ds.find(Rule.class)
                    .filter(eq("project", this.currentProject),
                            eq("type", RuleType.INVENTOR));
        }

        FindOptions options = new FindOptions().skip(first).limit(pageSize);
        if (sortField != null) {
            options.sort(sortOrder == 1
                    ? Sort.descending(sortField)
                    : Sort.ascending(sortField));
        }
        for (Map.Entry<String, String> entry : filters.entrySet()) {
            String column = entry.getKey();
            String value = entry.getValue();
            query.filter(dev.morphia.query.filters.Filters.regex(column,
                    ".*" + java.util.regex.Pattern.quote(value) + ".*")
                    .caseInsensitive());
        }
        setRowCount((int) query.count());

        if (this.searchType != null && this.searchType == 1) {
            options.projection().include("_id", "name", "acronym", "substitutions",
                    "country", "state", "type", "nature");
        } else {
            options.projection().include("_id", "name", "acronym", "substitutions",
                    "country", "state", "type");
        }
        return query.iterator(options).toList();
    }

    public List<Rule> getAllRule(Project project) {
        FindOptions options = new FindOptions();
        options.projection().include("_id", "name", "acronym", "substitutions",
                "country", "state", "type", "nature");
        return ds.find(Rule.class)
                .filter(eq("project", project))
                .iterator(options)
                .toList();
    }

    public void undoApplicantRule(Project project, String name) {
        List<Patent> patents = ds.find(Patent.class)
                .filter(eq("project", project), eq("applicants.name", name))
                .iterator()
                .toList();
        for (Patent patent : patents) {
            if (patent.getApplicants() == null) {
                continue;
            }
            boolean changed = false;
            for (Applicant applicant : patent.getApplicants()) {
                if (nameEquals(applicant.getName(), name)) {
                    restoreApplicantFromHistory(applicant);
                    changed = true;
                }
            }
            if (changed) {
                ds.save(patent);
            }
        }
    }

    public void unbindApplicantFromRule(Project project, String name) {
        List<Patent> patents = ds.find(Patent.class)
                .filter(eq("project", project), eq("applicants.history.name", name))
                .iterator()
                .toList();
        for (Patent patent : patents) {
            if (patent.getApplicants() == null) {
                continue;
            }
            boolean changed = false;
            for (Applicant applicant : patent.getApplicants()) {
                History history = applicant.getHistory();
                if (history != null && nameEquals(history.getName(), name)) {
                    restoreApplicantFromHistory(applicant);
                    changed = true;
                }
            }
            if (changed) {
                ds.save(patent);
            }
        }
    }

    public void undoInventorRule(Project project, String name) {
        List<Patent> patents = ds.find(Patent.class)
                .filter(eq("project", project), eq("inventors.name", name))
                .iterator()
                .toList();
        for (Patent patent : patents) {
            if (patent.getInventors() == null) {
                continue;
            }
            boolean changed = false;
            for (Inventor inventor : patent.getInventors()) {
                if (nameEquals(inventor.getName(), name)) {
                    restoreInventorFromHistory(inventor);
                    changed = true;
                }
            }
            if (changed) {
                ds.save(patent);
            }
        }
    }

    public void unbindInventorFromRule(Project project, String name) {
        List<Patent> patents = ds.find(Patent.class)
                .filter(eq("project", project), eq("inventors.history.name", name))
                .iterator()
                .toList();
        for (Patent patent : patents) {
            if (patent.getInventors() == null) {
                continue;
            }
            boolean changed = false;
            for (Inventor inventor : patent.getInventors()) {
                History history = inventor.getHistory();
                if (history != null && nameEquals(history.getName(), name)) {
                    restoreInventorFromHistory(inventor);
                    changed = true;
                }
            }
            if (changed) {
                ds.save(patent);
            }
        }
    }

    private void restoreApplicantFromHistory(Applicant applicant) {
        History history = applicant.getHistory();
        if (history == null) {
            return;
        }
        applicant.setName(history.getName());
        applicant.setHarmonized(false);
        if (history.getCountry() != null) {
            if (applicant.getCountry() == null) {
                applicant.setCountry(history.getCountry());
            } else {
                applicant.getCountry().setName(history.getCountry().getName());
                applicant.getCountry().setAcronym(history.getCountry().getAcronym());
            }
        }
    }

    private void restoreInventorFromHistory(Inventor inventor) {
        History history = inventor.getHistory();
        if (history == null) {
            return;
        }
        inventor.setName(history.getName());
        inventor.setHarmonized(false);
        if (history.getCountry() != null) {
            if (inventor.getCountry() == null) {
                inventor.setCountry(history.getCountry());
            } else {
                inventor.getCountry().setName(history.getCountry().getName());
                inventor.getCountry().setAcronym(history.getCountry().getAcronym());
            }
        }
    }

    private static boolean nameEquals(String left, String right) {
        if (left == null) {
            return right == null;
        }
        return left.equals(right);
    }

    public List<Rule> getApplicantRule(Project project) {
        return ds.find(Rule.class)
                .filter(eq("project", project), eq("type", RuleType.APPLICANT))
                .iterator()
                .toList();
    }

    public List<Rule> getInventorRule(Project project) {
        return ds.find(Rule.class)
                .filter(eq("project", project), eq("type", RuleType.INVENTOR))
                .iterator()
                .toList();
    }

    public void save(Rule rule) {
        Rule existing = rule.getProject() != null
                ? findByNameAndProject(rule.getName(), rule.getProject())
                : findByName(rule.getName());
        if (existing != null && (rule.getId() == null || !existing.getId().equals(rule.getId()))) {
            delete(existing.getId().toString());
            if (existing.getSubstitutions() != null) {
                if (rule.getSubstitutions() == null) {
                    rule.setSubstitutions(new java.util.HashSet<>());
                }
                rule.getSubstitutions().addAll(existing.getSubstitutions());
            }
        }
        ds.save(rule);
    }

    public void update(Rule rule) {
        ds.save(rule);
    }

    public Boolean isRule(String name) {
        return ds.find(Rule.class).filter(eq("name", name)).first() != null;
    }

    public Rule findByName(String name) {
        return ds.find(Rule.class).filter(eq("name", name)).first();
    }

    public Rule findByNameAndProject(String name, Project project) {
        if (name == null || name.isBlank() || project == null) {
            return null;
        }
        return ds.find(Rule.class)
                .filter(eq("project", project),
                        dev.morphia.query.filters.Filters.regex("name",
                                "^" + java.util.regex.Pattern.quote(name.trim()) + "$")
                                .caseInsensitive())
                .first();
    }

    public Rule findById(String id) {
        if (id == null || !ObjectId.isValid(id)) {
            return null;
        }
        return ds.find(Rule.class).filter(eq("_id", new ObjectId(id))).first();
    }

    public void delete(String id) {
        Rule rule = findById(id);
        if (rule != null) {
            ds.delete(rule);
        }
    }

    public int getRowCount() {
        if (rowCount == null) {
            Query<Rule> query = ds.find(Rule.class)
                    .filter(eq("project", this.currentProject));
            rowCount = (int) query.count();
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
