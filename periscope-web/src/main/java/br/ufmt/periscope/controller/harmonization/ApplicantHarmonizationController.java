package br.ufmt.periscope.controller.harmonization;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.model.DataModel;
import javax.faces.model.ListDataModel;
import javax.inject.Inject;

import br.ufmt.periscope.model.Applicant;
import br.ufmt.periscope.model.Project;
import br.ufmt.periscope.model.Rule;
import br.ufmt.periscope.qualifier.CurrentProject;
import br.ufmt.periscope.repository.ApplicantRepository;
import br.ufmt.periscope.util.SelectObject;

import com.github.jmkgreen.morphia.Datastore;

@ManagedBean
@ViewScoped
public class ApplicantHarmonizationController implements Serializable{
	
	private static final long serialVersionUID = 7744517674295407077L;
		
	private @Inject Datastore ds;
	private @Inject @CurrentProject Project currentProject;
	private @Inject ApplicantRepository applicantRepository;
	private DataModel<SelectObject<Applicant>> applicants = null;
	private List<Applicant> selectedApplicants = new ArrayList<Applicant>();
	private Set<String> applicantSugestions = new HashSet<String>();
	private Rule rule = new Rule();
	
	@PostConstruct
	public void init(){		
		List<Applicant> pas = applicantRepository.getApplicants(currentProject);
		applicants = new ListDataModel<SelectObject<Applicant>>(SelectObject.convertToSelectObjects(pas));		
	}

	public void onSelectApplicant(){
		Iterator<SelectObject<Applicant>> it = applicants.iterator();
		selectedApplicants.clear();
		while(it.hasNext()){
			SelectObject<Applicant> pa = it.next();
			if(pa.isSelected()){
				selectedApplicants.add(pa.getObject());
			}
		}
	}
	
	public void createRule(){
		
		
		
	}
	
	public void loadSugestions(){		
		String[] names = new String[selectedApplicants.size()];
		int i = 0;
		for(Applicant pa : selectedApplicants){
			names[i] = pa.getName();
			i++;
		}
		applicantSugestions = applicantRepository.getApplicantSugestions(currentProject, 10, names);
	}
	
	public DataModel<SelectObject<Applicant>> getApplicants() {
		return applicants;
	}

	public void setApplicants(DataModel<SelectObject<Applicant>> applicants) {
		this.applicants = applicants;
	}

	public List<Applicant> getSelectedApplicants() {
		return selectedApplicants;
	}

	public void setSelectedApplicants(List<Applicant> selectedApplicants) {
		this.selectedApplicants = selectedApplicants;
	}

	public Set<String> getApplicantSugestions() {
		return applicantSugestions;
	}

	public void setApplicantSugestions(Set<String> applicantSugestions) {
		this.applicantSugestions = applicantSugestions;
	}
		
	
	
}
