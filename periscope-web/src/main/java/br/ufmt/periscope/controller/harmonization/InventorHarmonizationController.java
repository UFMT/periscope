package br.ufmt.periscope.controller.harmonization;

import br.ufmt.periscope.model.Applicant;
import br.ufmt.periscope.model.ApplicantType;
import br.ufmt.periscope.model.Country;
import br.ufmt.periscope.model.Inventor;
import br.ufmt.periscope.model.Project;
import br.ufmt.periscope.model.Rule;
import br.ufmt.periscope.model.RuleType;
import br.ufmt.periscope.qualifier.CurrentProject;
import br.ufmt.periscope.report.Pair;
import br.ufmt.periscope.repository.ApplicantRepository;
import br.ufmt.periscope.repository.ApplicantTypeRepository;
import br.ufmt.periscope.repository.CountryRepository;
import br.ufmt.periscope.repository.InventorRepository;
import br.ufmt.periscope.repository.RuleRepository;
import br.ufmt.periscope.util.Filters;
import br.ufmt.periscope.util.SelectObject;
import com.github.jmkgreen.morphia.Datastore;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import javax.faces.context.Flash;
import javax.faces.model.DataModel;
import javax.faces.model.ListDataModel;
import javax.inject.Inject;


@ManagedBean
@ViewScoped
public class InventorHarmonizationController implements Serializable{
    
	private static final long serialVersionUID = 7744517674295407077L;
	
	private @Inject Logger log;
	private @Inject Datastore ds;
	private @Inject @CurrentProject Project currentProject;
	private @Inject ApplicantRepository applicantRepository;
        
        private @Inject InventorRepository inventorRepository;
        private DataModel<SelectObject<Inventor>> inventors = null;
        private List<Inventor> selectedInventors = new ArrayList<Inventor>();
        private List<SelectObject<Inventor>> inventorSugestions = null;
        private List<Inventor> selectedInventorSugestions = new ArrayList<Inventor>();
        
        
	private @Inject RuleRepository ruleRepository;
	private @Inject CountryRepository countryRepository;
	private List<Country> countries = new ArrayList<Country>();
	private Rule rule = new Rule();
        
	
	@PostConstruct
	public void init(){
		ArrayList<Inventor> pas = inventorRepository.getInventors(currentProject);
		countries = countryRepository.getAll();		
		inventors = new ListDataModel<SelectObject<Inventor>>(SelectObject.convertToSelectObjects(pas));		
	}

	public void onSelectInventor(){
		Iterator<SelectObject<Inventor>> it = inventors.iterator();
		selectedInventors.clear();
		while(it.hasNext()){
			SelectObject<Inventor> pa = it.next();
			if(pa.isSelected()){
				selectedInventors.add(pa.getObject());
			}
		}
	}
	
	public void onSelectInventorSugestion(){
		Iterator<SelectObject<Inventor>> it = inventorSugestions.iterator();
		selectedInventorSugestions.clear();
		while(it.hasNext()){
			SelectObject<Inventor> pa = it.next();
			if(pa.isSelected()){
				selectedInventorSugestions.add(pa.getObject());
			}
		}
	}
	
	public String createRule(){

		rule.setType(RuleType.INVENTOR);
		rule.setProject(currentProject);
		selectedInventors.addAll(selectedInventorSugestions);
		List<String> substitutions = new ArrayList<String>();
		for(Inventor pa : selectedInventors){
			substitutions.add(pa.getName());
		}	
		if(rule.getNature().getName().contentEquals("")){
			rule.setNature(null);
		}								
		rule.setCountry(countryRepository.getCountryByAcronym(rule.getCountry().getAcronym()));
		rule.setSubstitutions(new HashSet<String>(substitutions));				
		ruleRepository.save(rule);		
		Flash flash = FacesContext.getCurrentInstance().getExternalContext().getFlash();
		flash.put("success","Regra criada com sucesso");
		return "listRule";
	}
	
	public void loadSugestions(){		
		String[] names = new String[selectedInventors.size()];
		int i = 0;
		for(Inventor pa : selectedInventors){
			names[i] = pa.getName();
			i++;
		}
		Set<String> sugestions = inventorRepository.getInventorSugestions(currentProject, 10, names);
		List<Inventor> inventores = new ArrayList<Inventor>();
		for(String sugestion : sugestions){
			inventores.add(new Inventor(sugestion));
		}
		setInventorSugestions(new ArrayList<SelectObject<Inventor>>(SelectObject.convertToSelectObjects(inventores)));
	}
	


	public Rule getRule() {
		return rule;
	}

	public void setRule(Rule rule) {
		this.rule = rule;
	}

	public List<Country> getCountries() {
		return countries;
	}

	public void setCountries(List<Country> countries) {
		this.countries = countries;
	}


    public DataModel<SelectObject<Inventor>> getInventors() {
        return inventors;
    }

    public void setInventors(DataModel<SelectObject<Inventor>> inventors) {
        this.inventors = inventors;
    }

    public List<Inventor> getSelectedInventors() {
        return selectedInventors;
    }

    public void setSelectedInventors(List<Inventor> selectedInventors) {
        this.selectedInventors = selectedInventors;
    }

    public List<SelectObject<Inventor>> getInventorSugestions() {
        return inventorSugestions;
    }

    public void setInventorSugestions(List<SelectObject<Inventor>> inventorSugestions) {
        this.inventorSugestions = inventorSugestions;
    }
        
        
        
	
}
