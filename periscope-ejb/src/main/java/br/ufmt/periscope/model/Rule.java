package br.ufmt.periscope.model;

import java.util.ArrayList;
import java.util.List;

import org.bson.types.ObjectId;

import com.github.jmkgreen.morphia.annotations.Embedded;
import com.github.jmkgreen.morphia.annotations.Entity;
import com.github.jmkgreen.morphia.annotations.Id;
import com.github.jmkgreen.morphia.annotations.Reference;

@Entity
public class Rule {

	@Id
	private ObjectId id;
	private String name;
	private String acronym;
	
	@Embedded
	private List<String> substitutions;
	
	@Embedded
	private Country country;
	
	@Embedded
	private ApplicantType nature;
	private RuleType type;
		
	@Reference
	private Project project;
	
	public Rule(){
		country = new Country();
		nature = new ApplicantType();
		substitutions = new ArrayList<String>();
	}

	public ObjectId getId() {
		return id;
	}

	public void setId(ObjectId id) {
		this.id = id;
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

	public List<String> getSubstitutions() {
		return substitutions;
	}

	public void setSubstitutions(List<String> substitutions) {
		this.substitutions = substitutions;
	}

	public Country getCountry() {
		return country;
	}

	public void setCountry(Country country) {
		this.country = country;
	}

	public ApplicantType getNature() {
		return nature;
	}

	public void setNature(ApplicantType nature) {
		this.nature = nature;
	}

	public RuleType getType() {
		return type;
	}

	public void setType(RuleType type) {
		this.type = type;
	}

	public Project getProject() {
		return project;
	}

	public void setProject(Project project) {
		this.project = project;
	} 
}
