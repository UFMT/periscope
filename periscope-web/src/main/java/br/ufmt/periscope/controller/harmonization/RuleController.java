package br.ufmt.periscope.controller.harmonization;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import javax.faces.context.Flash;
import javax.inject.Inject;

import br.ufmt.periscope.harmonization.Harmonization;
import br.ufmt.periscope.model.Project;
import br.ufmt.periscope.model.Rule;
import br.ufmt.periscope.qualifier.CurrentProject;
import br.ufmt.periscope.repository.RuleRepository;
import javax.inject.Named;

@ManagedBean
@ViewScoped
@Named
public class RuleController implements Serializable{
	
	private static final long serialVersionUID = 7744517674295407077L;
	
	private @Inject Logger log;
	private @Inject Harmonization harmonization;
	private @Inject @CurrentProject Project currentProject;
	private @Inject RuleRepository ruleRepository;
	private List<Rule> applicantRules = new ArrayList<Rule>();
	private List<Rule> inventorRules = new ArrayList<Rule>();
	
	@PostConstruct
	public void init(){		
		applicantRules = ruleRepository.getApplicantRule(currentProject);
		inventorRules = ruleRepository.getInventorRule(currentProject);
	}
				
	public String apply(String id){
		Rule rule = ruleRepository.findById(id);
		harmonization.applyRule(rule);
		Flash flash = FacesContext.getCurrentInstance().  
                getExternalContext().getFlash();
		flash.put("success", "Regra aplicada com sucesso");
		return "listRule";
	}
	
	public String delete(String id){
		ruleRepository.delete(id);
		Flash flash = FacesContext.getCurrentInstance().  
                getExternalContext().getFlash();
		flash.put("info", "Deletado com sucesso");
		return "listRule";
	}	
				
	public List<Rule> getApplicantRules() {
		return applicantRules;
	}

	public void setApplicantRules(List<Rule> applicantRules) {
		this.applicantRules = applicantRules;
	}

	public List<Rule> getInventorRules() {
		return inventorRules;
	}

	public void setInventorRules(List<Rule> inventorRules) {
		this.inventorRules = inventorRules;
	}
}
