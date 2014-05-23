package br.ufmt.periscope.model;

import com.github.jmkgreen.morphia.annotations.Embedded;
import java.io.Serializable;

@Embedded
public class State implements Serializable{
    
    private String name;
    private String acronym;

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
    
}
