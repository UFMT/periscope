package br.ufmt.periscope.model;

import com.github.jmkgreen.morphia.annotations.Embedded;
import com.github.jmkgreen.morphia.annotations.Entity;
import java.util.ArrayList;
import java.util.List;

@Embedded
@Entity
public class Country implements Comparable<Country> {

    private String acronym;
    private String name;
    private List<Region> region = new ArrayList<Region>();

    public Country() {
    }

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

    public List<Region> getRegion() {
        return region;
    }

    public void setRegion(List<Region> region) {
        this.region = region;
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
