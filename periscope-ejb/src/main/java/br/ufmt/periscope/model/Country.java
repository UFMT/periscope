package br.ufmt.periscope.model;

import com.github.jmkgreen.morphia.annotations.Embedded;
import com.github.jmkgreen.morphia.annotations.Entity;

@Embedded
@Entity
public class Country implements Comparable<Country> {

    private String acronym;
    private String name;

    public String getAcronym() {
        return acronym;
    }

    public void setAcronym(String acronym) {
        this.acronym = acronym;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public int compareTo(Country o) {
        int ret;
        ret = this.name.compareTo(o.name);
        if (ret == 0) {
            if (this.acronym != null) {
                return this.acronym.compareTo(o.acronym);
            }
        }
        return ret;
    }

}
