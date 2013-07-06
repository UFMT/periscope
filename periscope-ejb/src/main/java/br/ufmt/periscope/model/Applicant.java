package br.ufmt.periscope.model;

import java.io.Serializable;

import com.github.jmkgreen.morphia.annotations.Embedded;
import com.github.jmkgreen.morphia.annotations.Transient;

@Embedded
public class Applicant implements Serializable{
	
	private static final long serialVersionUID = 8189474165213004815L;
	
	private String name;
	private String acronym;
	@Embedded private Country country;
	@Embedded private ApplicantType type;
	private Boolean harmonized = false;
	
	@Transient
	private Integer documentCount = 0;
		
	public Applicant() {
	
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
	@Override
	public String toString(){ 
		return name;
	}
	
}
