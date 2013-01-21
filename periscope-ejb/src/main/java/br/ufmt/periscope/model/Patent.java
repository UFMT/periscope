package br.ufmt.periscope.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.bson.types.ObjectId;

import com.github.jmkgreen.morphia.annotations.Embedded;
import com.github.jmkgreen.morphia.annotations.Entity;
import com.github.jmkgreen.morphia.annotations.Id;
import com.github.jmkgreen.morphia.annotations.Reference;

@Entity
public class Patent {
	
	@Id
	private ObjectId id;
	
	private String titleSelect;	
	private String abstractSelect;
		
	private @Embedded Classification mainClassification;
	private @Embedded List<Classification> classifications = new ArrayList<Classification>();

	private String language;
	
	private String publicationNumber;
	private Date publicationDate;
	private @Embedded Country publicationCountry;
	
	private String applicationNumber;
	private Date applicationDate;
	private @Embedded Country applicationCountry;
		
	private @Embedded List<Applicant> applicants = new ArrayList<Applicant>();
	
	private Boolean blacklisted = false; 
	private Boolean completed = false;
	@Reference private Project project;
	
	public ObjectId getId() {
		return id;
	}
	public void setId(ObjectId id) {
		this.id = id;
	}
	public String getTitleSelect() {
		return titleSelect;
	}
	public void setTitleSelect(String titleSelect) {
		this.titleSelect = titleSelect;
	}
	public String getAbstractSelect() {
		return abstractSelect;
	}
	public void setAbstractSelect(String abstractSelect) {
		this.abstractSelect = abstractSelect;
	}
	public String getPublicationNumber() {
		return publicationNumber;
	}
	public void setPublicationNumber(String publicationNumber) {
		this.publicationNumber = publicationNumber;
	}
	public Project getProject() {
		return project;
	}
	public void setProject(Project project) {
		this.project = project;
	}
	public Classification getMainClassification() {
		return mainClassification;
	}
	public void setMainClassification(Classification mainClassification) {
		this.mainClassification = mainClassification;
	}
	public List<Classification> getClassifications() {
		return classifications;
	}
	public void setClassifications(List<Classification> classifications) {
		this.classifications = classifications;
	}
	public Boolean getBlacklisted() {
		return blacklisted;
	}
	public void setBlacklisted(Boolean blacklisted) {
		this.blacklisted = blacklisted;
	}
	public Date getPublicationDate() {
		return publicationDate;
	}
	public void setPublicationDate(Date publicationDate) {
		this.publicationDate = publicationDate;
	}
	public Country getPublicationCountry() {
		return publicationCountry;
	}
	public void setPublicationCountry(Country publicationCountry) {
		this.publicationCountry = publicationCountry;
	}
	public String getApplicationNumber() {
		return applicationNumber;
	}
	public void setApplicationNumber(String applicationNumber) {
		this.applicationNumber = applicationNumber;
	}
	public Date getApplicationDate() {
		return applicationDate;
	}
	public void setApplicationDate(Date applicationDate) {
		this.applicationDate = applicationDate;
	}
	public Country getApplicationCountry() {
		return applicationCountry;
	}
	public void setApplicationCountry(Country applicationCountry) {
		this.applicationCountry = applicationCountry;
	}
	public Boolean getCompleted() {
		return completed;
	}
	public void setCompleted(Boolean completed) {
		this.completed = completed;
	}
	public String getLanguage() {
		return language;
	}
	public void setLanguage(String language) {
		this.language = language;
	}
	public List<Applicant> getApplicants() {
		return applicants;
	}
	public void setApplicants(List<Applicant> applicants) {
		this.applicants = applicants;
	}
	

}
