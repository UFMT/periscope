package br.ufmt.periscope.model;

import com.github.jmkgreen.morphia.annotations.Embedded;

@Embedded
public class Applicant {

	private String name;
	private String acronym;
	@Embedded private Country country;
	private Boolean harmonized = false;
	
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
	
	
}
