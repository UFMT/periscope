package br.ufmt.periscope.controller.harmonization;

import br.ufmt.periscope.lazy.LazyApplicantDataModel;
import br.ufmt.periscope.model.Applicant;
import br.ufmt.periscope.model.ApplicantType;
import br.ufmt.periscope.model.Country;
import br.ufmt.periscope.model.Patent;
import br.ufmt.periscope.model.Project;
import br.ufmt.periscope.model.Rule;
import br.ufmt.periscope.model.RuleType;
import br.ufmt.periscope.model.State;
import br.ufmt.periscope.qualifier.CurrentProject;
import br.ufmt.periscope.repository.ApplicantRepository;
import br.ufmt.periscope.repository.ApplicantTypeRepository;
import br.ufmt.periscope.repository.CountryRepository;
import br.ufmt.periscope.repository.RuleRepository;
import br.ufmt.periscope.util.SelectObject;
import com.github.jmkgreen.morphia.Datastore;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
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
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.DataModel;
import javax.faces.model.ListDataModel;
import javax.inject.Inject;

@ManagedBean
@ViewScoped
public class ApplicantHarmonizationController implements Serializable {

    private static final long serialVersionUID = 7744517674295407077L;
    private @Inject
    Logger log;
    private @Inject
    Datastore ds;
    private @Inject
    @CurrentProject
    Project currentProject;
    private @Inject
    ApplicantRepository applicantRepository;
    private @Inject
    RuleRepository ruleRepository;
    private @Inject
    ApplicantTypeRepository typeRepository;
    private @Inject
    CountryRepository countryRepository;
    private @Inject
    LazyApplicantDataModel applicants;
    private List<Applicant> selectedApplicants = new ArrayList<Applicant>();
    private List<Applicant> selectedApplicantSugestions = new ArrayList<Applicant>();
    private List<SelectObject<Applicant>> applicantSugestions = null;
    private List<SelectObject<Applicant>> selectedsApplicantSugestions = null;
    private List<Country> countries = new ArrayList<Country>();
    private List<ApplicantType> applicantTypes = new ArrayList<ApplicantType>();
    private List<State> states;
    private Country defaultCountry;
    private Rule rule = new Rule();
    private String acronymDefault = "BR";
    private Applicant selectedRadio;
    private Integer searchType;

    @PostConstruct
    public void init() {
//		List<Applicant> pas = applicantRepository.getApplicants(currentProject);
        applicantTypes = typeRepository.getAll();
        countries = countryRepository.getAll();
        applicants.getApplicantRepository().setCurrentProject(currentProject);
        selectedApplicants.clear();
        applicants.setSelectedApplicants(selectedApplicants);

        defaultCountry = countryRepository.getCountryByAcronym(acronymDefault);
        rule.setCountry(defaultCountry);
        states = defaultCountry.getStates();
        setSearchType(1);
        Collections.sort(states);
//		applicants = new ListDataModel<SelectObject<Applicant>>(SelectObject.convertToSelectObjects(pas));		
    }

    public void onSelectApplicant(Applicant pa) {

        if (pa.getSelected()) {
            selectedApplicants.add(pa);
        } else {
            selectedApplicants.remove(pa);
        }

    }

    public void selectListener(ValueChangeEvent event) {
        String acronym = (String) event.getNewValue();
        searchState(acronym);
    }

    private void searchState(String acronym) {
        Country country = countryRepository.getCountryByAcronym(acronym);
        states = country.getStates();
        Collections.sort(states);
    }

    public void onSelectApplicantSugestion() {
        Iterator<SelectObject<Applicant>> it = applicantSugestions.iterator();
        selectedApplicantSugestions.clear();
        while (it.hasNext()) {
            SelectObject<Applicant> pa = it.next();
            if (pa.isSelected()) {
                selectedApplicantSugestions.add(pa.getObject());
            }
        }
    }

    public void onSelectApplicantFill() {
        rule.setName(null);
        rule.setAcronym(null);
        rule.setCountry(defaultCountry);
        rule.setNature(null);
        rule.setState(null);
        if (selectedRadio != null) {
//            System.out.println("Selecionado:" + selectedRadio.getName());
            if (selectedRadio.getName() != null) {
                rule.setName(selectedRadio.getName());
            }
            if (selectedRadio.getAcronym() != null) {
                rule.setAcronym(selectedRadio.getAcronym());
            }
            if (selectedRadio.getCountry() != null) {
                selectedRadio.getCountry().setStates(null);
                rule.setCountry(selectedRadio.getCountry());
                searchState(selectedRadio.getCountry().getAcronym());
            }
            if (selectedRadio.getType() != null) {
                rule.setNature(selectedRadio.getType());
            }
            if (selectedRadio.getState() != null) {
                rule.setState(selectedRadio.getState());
            }
//            System.out.println("RuleName:" + rule.getName());
        }
    }

    public String createRule() {

//        System.out.println("criando rule");
        rule.setType(RuleType.APPLICANT);
        rule.setProject(currentProject);
        if (selectedApplicantSugestions != null) {
            selectedApplicants.addAll(selectedApplicantSugestions);
        }
        List<String> substitutions = new ArrayList<String>();
        for (Applicant pa : selectedApplicants) {
            substitutions.add(pa.getName());
        }
        if (rule.getNature().getName().contentEquals("")) {
            rule.setNature(null);
        }
        rule.setCountry(countryRepository.getCountryByAcronym(rule.getCountry().getAcronym()));
        for (State state : rule.getCountry().getStates()) {
            if (state.getAcronym().equals(rule.getState().getAcronym())) {
                rule.setState(state);
            }
        }

        rule.setSubstitutions(new HashSet<String>(substitutions));
        ruleRepository.save(rule);
        Flash flash = FacesContext.getCurrentInstance().getExternalContext().getFlash();
        flash.put("success", "Regra criada com sucesso");
        return "listRule";
    }

    public void loadSugestions() {
//        System.out.println("Load");
        String[] names = new String[selectedApplicants.size()];
        selectedRadio = null;
        selectedRadio = selectedApplicants.get(0);
        onSelectApplicantFill();
        int i = 0;
        for (Applicant pa : selectedApplicants) {
            names[i] = pa.getName();
            i++;
        }
        Set<String> sugestions = applicantRepository.getApplicantSugestions(currentProject, 100, names);
        List<Applicant> aplicants = new ArrayList<Applicant>();
        for (String sugestion : sugestions) {
            aplicants.add(new Applicant(sugestion));
        }
        setApplicantSugestions(new ArrayList<SelectObject<Applicant>>(SelectObject.convertToSelectObjects(aplicants)));

    }

    public void metodo(Applicant pa) {
        if (selectedApplicants.contains(pa)) {
            pa.setSelected(true);
        } else {
            pa.setSelected(false);
        }
    }

    public DataModel<Applicant> getApplicants() {

        return applicants;
    }

    public void setApplicants(LazyApplicantDataModel applicants) {
        this.applicants = applicants;
    }

    public List<Applicant> getSelectedApplicants() {
        return selectedApplicants;
    }

    public void setSelectedApplicants(List<Applicant> selectedApplicants) {
        this.selectedApplicants = selectedApplicants;
    }

    public Rule getRule() {
        return rule;
    }

    public void setRule(Rule rule) {
        this.rule = rule;
    }

    public List<SelectObject<Applicant>> getApplicantSugestions() {
        return applicantSugestions;
    }

    public void setApplicantSugestions(List<SelectObject<Applicant>> applicantSugestions) {
        this.applicantSugestions = applicantSugestions;
    }

    public List<Country> getCountries() {
        return countries;
    }

    public void setCountries(List<Country> countries) {
        this.countries = countries;
    }

    public List<ApplicantType> getApplicantTypes() {
        return applicantTypes;
    }

    public void setApplicantTypes(List<ApplicantType> applicantTypes) {
        this.applicantTypes = applicantTypes;
    }

    public List<State> getStates() {
        return states;
    }

    public void setStates(List<State> states) {
        this.states = states;
    }

    public Integer getSearchType() {
        return searchType;
    }

    public void setSearchType(Integer searchType) {
        this.searchType = searchType;
        applicants.setSearchType(searchType);
    }
    

    public Applicant getSelectedRadio() {
        return selectedRadio;
    }

    public void setSelectedRadio(Applicant selectedRadio) {
        this.selectedRadio = selectedRadio;
    }

    public List<SelectObject<Applicant>> getSelectedsApplicantSugestions() {
        return selectedsApplicantSugestions;
    }

    public void setSelectedsApplicantSugestions(List<SelectObject<Applicant>> selectedsApplicantSugestions) {
        this.selectedsApplicantSugestions = selectedsApplicantSugestions;
    }

}
