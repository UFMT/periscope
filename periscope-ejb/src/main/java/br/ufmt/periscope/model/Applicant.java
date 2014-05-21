package br.ufmt.periscope.model;

import java.io.Serializable;

import com.github.jmkgreen.morphia.annotations.Embedded;
import com.github.jmkgreen.morphia.annotations.Transient;

@Embedded
public class Applicant implements Serializable {

    private static final long serialVersionUID = 8189474165213004815L;
    private String name;
    private String acronym;
    @Embedded
    private Country country;
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
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAcronym() {
        return acronym;
    }

    public void setAcronym(String acronym) {
        this.acronym = acronym;
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
            if (applicant.name.equals(this.name)) {
                if (applicant.country != null) {
                    if (applicant.country.getAcronym().equals(this.country.getAcronym())) {
                        return true;
                    }
                }else if(this.country == null) {
                    return true;
                }
                return true;
            }
        }
        return false;
    }
}
