package br.ufmt.periscope.model;

import com.github.jmkgreen.morphia.annotations.Embedded;
import com.github.jmkgreen.morphia.annotations.Transient;
import java.io.Serializable;

@Embedded
public class Inventor implements Serializable, Comparable<Inventor> {

    private String name;
    private String acronym;
    @Embedded
    private History history;
    @Embedded
    private Country country;
    @Embedded
    private State state;
    private Boolean harmonized = false;
    @Transient
    private Boolean selected = false;

    private Integer documentCount = 0;

    public Inventor() {
        country = new Country();
        state = new State();
        history = new History();
    }

    public Inventor(String name) {
        this.name = name;
        this.history = new History();
        this.history.setName(name);
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public String getName() {
        if (name != null) {
            name = name.toUpperCase();
        }
        return name;
    }

    public void setName(String name) {
        if (name != null) {
            name = name.toUpperCase();
        }
        this.name = name;
    }

    public History getHistory() {
        return history;
    }

    public void setHistory(History history) {
        this.history = history;
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

    public Integer getDocumentCount() {
        return documentCount;
    }

    public void setDocumentCount(Integer documentCount) {
        this.documentCount = documentCount;
    }

    public String getAcronym() {
        return acronym;
    }

    public void setAcronym(String acronym) {
        this.acronym = acronym;
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
        if (o instanceof Inventor) {
            Inventor inventor = (Inventor) o;
            if (inventor.getName().equals(this.getName())) {
                if (inventor.country != null) {
                    if (inventor.country.getAcronym().equals(this.country.getAcronym())) {
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
    public int compareTo(Inventor o) {
        int ret = this.name.compareTo(o.name);
        if (ret == 0) {
            if (this.country != null) {
                ret = this.country.getAcronym().compareTo(o.getCountry().getAcronym());
            }
        }
        return ret;
    }

}
