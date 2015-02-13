package br.ufmt.periscope.model;

import java.util.Date;

import com.github.jmkgreen.morphia.annotations.Embedded;

@Embedded
public class Priority {

    private String value;
    @Embedded
    private Country country;
    private Date date;

    public Priority() {
        country = new Country();
    }

    public String getValue() {
        if (value != null) {
            value = value.toUpperCase();
        }
        return value;
    }

    public void setValue(String value) {
        if (value != null) {
            value = value.toUpperCase();
        }
        this.value = value;
    }

    public Country getCountry() {
        return country;
    }

    public void setCountry(Country country) {
        this.country = country;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    @Override
    public String toString() {
        return value;
    }
}
