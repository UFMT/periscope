package br.ufmt.periscope.model;

import java.io.Serializable;

import com.github.jmkgreen.morphia.annotations.Embedded;
import com.github.jmkgreen.morphia.annotations.Transient;

@Embedded
public class Applicant implements Serializable, Comparable<Applicant> {

    private static final long serialVersionUID = 8189474165213004815L;
    private String name;
    private String acronym;
    @Embedded
    private Country country;
    @Embedded
    private State state;
    @Embedded
    private ApplicantType type;
    private Boolean harmonized = false;
    @Transient
    private Integer documentCount = 0;
    @Transient
    private Boolean selected = false;

    public Applicant() {
        country = new Country();
    }

    public Applicant(String name) {
        this.name = name;
    }

    public String getName() {
        if(name != null){
            name = name.toUpperCase();
        }
        return name;
    }

    public void setName(String name) {
        if(name != null){
            name = name.toUpperCase();
        }
        this.name = name;
    }

    public String getAcronym() {
        return acronym;
    }

    public void setAcronym(String acronym) {
        this.acronym = acronym;
    }
    
    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }
    
    public Country getCountry() {
        return country;
    }

    public void setCountry(Country country) {
        this.country = country;
    }

    public Boolean getHarmonized() {
        return harmonized;
    }

    public void setHarmonized(Boolean harmonized) {
        this.harmonized = harmonized;
    }

    public ApplicantType getType() {
        return type;
    }

    public void setType(ApplicantType type) {
        this.type = type;
    }

    public Integer getDocumentCount() {
        return documentCount;
    }

    public void setDocumentCount(Integer documentCount) {
        this.documentCount = documentCount;
    }

    public Boolean getSelected() {
        return selected;
    }

    public void setSelected(Boolean selected) {
        this.selected = selected;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Applicant) {
            Applicant applicant = (Applicant) o;
            if (applicant.getName().equals(this.getName())) {
                if (applicant.country != null) {
                    if (applicant.country.getAcronym().equals(this.country.getAcronym())) {
                        return true;
                    }
                } else if (this.country == null) {
                    return true;
                }
                return false;
            }
        }
        return false;
    }

    @Override
    public int compareTo(Applicant o) {
        int ret;
        ret = this.name.compareTo(o.name);
        if (ret == 0) {
            if (this.country != null) {
                ret = this.country.compareTo(o.country);
                if (ret == 0) {
                    if (this.acronym != null) {
                        return this.acronym.compareTo(o.acronym);
                    }
                }
            }
        }
        return ret;
    }
}
