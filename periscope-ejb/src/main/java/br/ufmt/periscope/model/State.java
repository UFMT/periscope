package br.ufmt.periscope.model;

import com.github.jmkgreen.morphia.annotations.Embedded;
import java.io.Serializable;

@Embedded
public class State implements Serializable, Comparable<State> {

    private String name;
    private String acronym;
    private String region;

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

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    @Override
    public int compareTo(State o) {
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
