package br.ufmt.periscope.controller;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.model.DataModel;
import javax.faces.model.ListDataModel;
import javax.inject.Inject;

import br.ufmt.periscope.model.Patent;
import br.ufmt.periscope.model.Project;
import br.ufmt.periscope.qualifier.CurrentProject;

import com.github.jmkgreen.morphia.Datastore;

@ManagedBean
@ViewScoped
public class PatentController {		

	private @Inject Datastore ds; 
	private @Inject @CurrentProject Project currentProject;
	private DataModel<Patent> patents = null;	
	
	@PostConstruct
	public void init(){

	}
	
	public DataModel<Patent> getPatents() {
		if(patents == null){			
			patents = new ListDataModel<Patent>(ds.createQuery(Patent.class).field("project").equal(currentProject).asList());
		}
		return patents;
	}

	public void setPatents(DataModel<Patent> patents) {
		this.patents = patents;
	}
	
}
