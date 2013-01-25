package br.ufmt.periscope.model;

import com.github.jmkgreen.morphia.annotations.Embedded;

@Embedded
public class Inventor {

	private String name;
	@Embedded private Country country;
	private Boolean harmonized = false;
	
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
	
	@Override
	public String toString(){ 
		return name;
	}
	
}
