package br.ufmt.periscope.controller.harmonization;

import br.ufmt.periscope.lazy.LazyApplicantDataModel;
import br.ufmt.periscope.lazy.LazyPatentDataModel;
import br.ufmt.periscope.model.Applicant;
import br.ufmt.periscope.model.ApplicantType;
import br.ufmt.periscope.model.Country;
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
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import org.primefaces.context.RequestContext;

@ManagedBean
@ViewScoped
public class ApplicantHarmonizationController implements Serializable {

    private static final long serialVersionUID = 7744517674295407077L;
    private @Inject
    Logger log;
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
    private @Inject
    LazyPatentDataModel patents;
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
    private Boolean harmonized = false;
    private Boolean sugHarmonized = false;
    private Applicant selectedRadio;
    private Rule originalRule;
    private Integer searchType;
    private @Inject
    RuleController ruleController;

    public ApplicantHarmonizationController() {
//        System.out.println("App Harminization Controller");
    }

    @PostConstruct
    public void init() {
        applicantTypes = typeRepository.getAll();
        countries = countryRepository.getAll();
        applicants.getApplicantRepository().setCurrentProject(currentProject);
        selectedApplicants.clear();
        applicants.setSelectedApplicants(selectedApplicants);
        applicants.setHarmonization(true);
        patents.getRepo().setCurrentProject(currentProject);
        defaultCountry = countryRepository.getCountryByAcronym(acronymDefault);
        rule.setCountry(defaultCountry);
        states = defaultCountry.getStates();
        setSearchType(1);
        originalRule = null;
        Collections.sort(states);
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletRequest req = (HttpServletRequest) context.getExternalContext().getRequest();
        if (req.getParameter("ruleId") != null) {
            rule = ruleRepository.findById(req.getParameter("ruleId"));
            originalRule = rule;
            for (String pa : rule.getSubstitutions()) {
                Applicant ap = applicantRepository.getApplicantByName(pa);
                if (ap == null) {
                    selectedApplicants.add(new Applicant(pa));
                } else {
                    selectedApplicants.add(ap);
                }
            }
        }
    }

    public void updateHarmonized() {
        harmonized = false;
        for (Applicant applicant : selectedApplicants) {
            if (!applicant.getName().equals(rule.getName())) {
                harmonized = harmonized || applicant.getHarmonized();
            }
        }
    }

    public void onSelectApplicant(Applicant pa) {
        if (pa.getSelected()) {
            selectedApplicants.add(pa);
        } else {
            selectedApplicants.remove(pa);
        }
        if (originalRule != null) {
            updateHarmonized();
        }
    }

    public String unselect(Applicant pa) {
        pa.setSelected(false);
        selectedApplicants.remove(pa);
        if (originalRule != null) {
            updateHarmonized();
            RequestContext.getCurrentInstance().reset(":formAll:applicants");
        }
        return "";
    }

    public void loadDocs(String name) {
        patents.setType("applicant");
        patents.setName(name);
    }

    public void selectListener(ValueChangeEvent event) {
        String acronym = (String) event.getNewValue();
        searchState(acronym);
    }

    private void searchState(String acronym) {
        Country country;
        if (!acronym.contentEquals(""))
            country = countryRepository.getCountryByAcronym(acronym);
        else
            country = countryRepository.getCountryByAcronym("BR");
        states = country.getStates();
        Collections.sort(states);
    }

    public void onSelectApplicantSugestion() {
        Iterator<SelectObject<Applicant>> it = applicantSugestions.iterator();
        selectedApplicantSugestions.clear();
        sugHarmonized = false;
        while (it.hasNext()) {
            SelectObject<Applicant> pa = it.next();
            if (pa.isSelected()) {
                sugHarmonized = sugHarmonized || pa.getObject().getHarmonized();
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
        }
    }

    public String updateRule() {
        ruleRepository.delete(rule.getId().toString());
        return createRule() + "listRule";
    }

    public String createRule() {
        rule.setType(RuleType.APPLICANT);
        rule.setProject(currentProject);
        if (selectedApplicantSugestions != null) {
            selectedApplicants.addAll(selectedApplicantSugestions);
        }
        List<String> substitutions = new ArrayList<String>();
        List<String> deletions = new ArrayList<String>();
        for (Applicant pa : selectedApplicants) {
            substitutions.add(pa.getName());
            if (pa.getHarmonized()) {
                deletions.add(pa.getName());
            }
        }

        if (originalRule != null) {
            for (String pa : originalRule.getSubstitutions()) {
                if (!substitutions.contains(pa)) {
                    ruleRepository.unbindApplicantFromRule(currentProject, pa);
                }
            }
        }
        
        if (rule.getNature() == null|| rule.getNature().getName() == null || rule.getNature().getName().contentEquals("")) {
            rule.setNature(null);
        }
        rule.setCountry(countryRepository.getCountryByAcronym(rule.getCountry().getAcronym()));
        for (State state : rule.getCountry().getStates()) {
            if (state.getAcronym().equals(rule.getState().getAcronym())) {
                rule.setState(state);
            }
        }
        rule.getCountry().setStates(null);
        if (overwrite()) {
            for (String deletion : deletions) {
                Rule rul = ruleRepository.findByName(deletion);
                if (rul != null) {
                    substitutions.addAll(rul.getSubstitutions());
                    ruleRepository.delete(rul.getId().toString());
                }
            }
        }
        rule.setSubstitutions(new HashSet<String>(substitutions));
        ruleRepository.save(rule);
        ruleController.apply(rule.getId().toString());
        selectedApplicants.clear();
        rule = new Rule();

        Flash flash = FacesContext.getCurrentInstance().getExternalContext().getFlash();
        flash.put("success", "Regra criada e aplicada com sucesso");
        return "";
    }

    public void loadSugestions() {
        String[] names = new String[selectedApplicants.size()];
        selectedRadio = null;
        selectedRadio = selectedApplicants.get(0);
        onSelectApplicantFill();
        int i = 0;
        sugHarmonized = harmonized = false;
        for (Applicant pa : selectedApplicants) {
            names[i] = pa.getName();
            harmonized = harmonized || pa.getHarmonized();
            i++;
        }
        Set<String> sugestions = applicantRepository.getApplicantSugestions(currentProject, 100, names);
        List<Applicant> aplicants = new ArrayList<Applicant>();
        Applicant app;
        for (String sugestion : sugestions) {
            app = new Applicant(sugestion);
            if (ruleRepository.isRule(sugestion)) {
                app.setHarmonized(true);
            }
            aplicants.add(app);
        }
        System.out.println("Sugestoes carregadas");
        setApplicantSugestions(new ArrayList<SelectObject<Applicant>>(SelectObject.convertToSelectObjects(aplicants)));

    }

    public void metodo(Applicant pa) {
        if (selectedApplicants.contains(pa)) {
            pa.setSelected(true);
        } else {
            pa.setSelected(false);
        }
    }

    public Boolean overwrite() {
        return harmonized || sugHarmonized;
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

    public Boolean getHarmonized() {
        return harmonized;
    }

    public void setHarmonized(Boolean harmonized) {
        this.harmonized = harmonized;
    }

    public Boolean getSugHarmonized() {
        return sugHarmonized;
    }

    public void setSugHarmonized(Boolean sugHarmonized) {
        this.sugHarmonized = sugHarmonized;
    }

    public LazyPatentDataModel getPatents() {
        return patents;
    }

    public void setPatents(LazyPatentDataModel patents) {
        this.patents = patents;
    }

    public Rule getOriginalRule() {
        return originalRule;
    }

    public void setOriginalRule(Rule originalRule) {
        this.originalRule = originalRule;
    }
}
