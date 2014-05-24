package br.ufmt.periscope.model;

import com.github.jmkgreen.morphia.annotations.Embedded;
import java.io.Serializable;

@Embedded
public class Inventor implements Serializable {

    private String name;
    @Embedded
    private Country country;
    @Embedded
    private State state;
    private Boolean harmonized = false;
    
    private Integer documentCount = 0;

    public Inventor() {
        country = new Country();
    }

    public Inventor(String name) {
        this.name = name;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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
    
    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if(o instanceof Inventor){
            Inventor inventor = (Inventor) o;
            if(inventor.name.equals(this.name) && inventor.country.getAcronym().equals(this.country.getAcronym())){
                return true;
            }
        }
        return false;
    }
    
    
}
