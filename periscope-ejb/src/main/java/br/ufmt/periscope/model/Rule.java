package br.ufmt.periscope.model;

import com.github.jmkgreen.morphia.annotations.Embedded;
import com.github.jmkgreen.morphia.annotations.Entity;
import com.github.jmkgreen.morphia.annotations.Id;
import com.github.jmkgreen.morphia.annotations.Reference;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import org.bson.types.ObjectId;

@Entity
public class Rule implements Serializable {

    @Id
    private ObjectId id;
    private String name;
    private String acronym;

    @Embedded
    private Set<String> substitutions;

    @Embedded
    private Country country;

    @Embedded
    private State state;

    @Embedded
    private ApplicantType nature;
    private RuleType type;

    @Reference
    private Project project;

    public Rule() {
        country = new Country();
        state = new State();
        nature = new ApplicantType();
        substitutions = new HashSet<String>();
    }

    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }

    public String getName() {
        if(name != null){
            name = name.toUpperCase();
        }
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAcronym() {
        if(acronym != null){
            acronym = acronym.toUpperCase();
        }
        return acronym;
    }

    public void setAcronym(String acronym) {
        this.acronym = acronym;
    }

    public Set<String> getSubstitutions() {
        return substitutions;
    }

    public void setSubstitutions(Set<String> substitutions) {
        this.substitutions = substitutions;
    }

    public Country getCountry() {
        return country;
    }

    public void setCountry(Country country) {
        this.country = country;
    }

    public ApplicantType getNature() {
        if(nature == null){
            nature = new ApplicantType();
        }
        return nature;
    }

    public void setNature(ApplicantType nature) {
        this.nature = nature;
    }

    public RuleType getType() {
        return type;
    }

    public void setType(RuleType type) {
        this.type = type;
    }

    public Project getProject() {
        return project;
    }

    public State getState() {
        if(state == null){
            state = new State();
        }
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public void setProject(Project project) {
        this.project = project;
    }  
}
