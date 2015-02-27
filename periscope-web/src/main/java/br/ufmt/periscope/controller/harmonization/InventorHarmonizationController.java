package br.ufmt.periscope.controller.harmonization;

import br.ufmt.periscope.lazy.LazyInventorDataModel;
import br.ufmt.periscope.model.Country;
import br.ufmt.periscope.model.Inventor;
import br.ufmt.periscope.model.Project;
import br.ufmt.periscope.model.Rule;
import br.ufmt.periscope.model.RuleType;
import br.ufmt.periscope.model.State;
import br.ufmt.periscope.qualifier.CurrentProject;
import br.ufmt.periscope.repository.CountryRepository;
import br.ufmt.periscope.repository.InventorRepository;
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

@ManagedBean
@ViewScoped
public class InventorHarmonizationController implements Serializable {

    private static final long serialVersionUID = 7744517674295407077L;
    private @Inject
    Logger log;
    private @Inject
    @CurrentProject
    Project currentProject;
    private @Inject
    InventorRepository inventorRepository;
    private @Inject
    LazyInventorDataModel inventors;
    private List<Inventor> selectedInventors = new ArrayList<Inventor>();
    private List<SelectObject<Inventor>> inventorSugestions = null;
    private List<Inventor> selectedInventorSugestions = new ArrayList<Inventor>();
    private @Inject
    RuleRepository ruleRepository;
    private @Inject
    CountryRepository countryRepository;
    private List<Country> countries = new ArrayList<Country>();
    private List<State> states;
    private Rule rule = new Rule();
    private Country defaultCountry;
    private String acronymDefault = "BR";
    private Boolean harmonized = false;
    private Boolean sugHarmonized = false;
    private Inventor selectedRadio;
    private Integer searchType;
    private @Inject
    RuleController ruleController;

    @PostConstruct
    public void init() {
        setSearchType(1);
        countries = countryRepository.getAll();
        inventors.getInventorRepository().setCurrentProject(currentProject);
        inventors.setHarmonization(true);
        selectedInventors.clear();
        inventors.setSelectedInventors(selectedInventors);

        defaultCountry = countryRepository.getCountryByAcronym(acronymDefault);
        rule.setCountry(defaultCountry);
        states = defaultCountry.getStates();
        Collections.sort(states);
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

    public void onSelectInventor(Inventor pa) {
        if (pa.getSelected()) {
            selectedInventors.add(pa);
        } else {
            selectedInventors.remove(pa);
        }

    }

    public String unselect(Inventor pa) {
        pa.setSelected(false);
        selectedInventors.remove(pa);
        return "";

    }

    public void onSelectInventorSugestion() {
        Iterator<SelectObject<Inventor>> it = inventorSugestions.iterator();
        selectedInventorSugestions.clear();
        sugHarmonized = false;
        while (it.hasNext()) {
            SelectObject<Inventor> pa = it.next();
            if (pa.isSelected()) {
                sugHarmonized = sugHarmonized || pa.getObject().getHarmonized();
                selectedInventorSugestions.add(pa.getObject());
            }
        }
    }

    public void onSelectInventorFill() {
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
            if (selectedRadio.getState() != null) {
                rule.setState(selectedRadio.getState());
            }
        }
    }

    public String createRule() {

        rule.setType(RuleType.INVENTOR);
        rule.setProject(currentProject);
        selectedInventors.addAll(selectedInventorSugestions);
        List<String> substitutions = new ArrayList<String>();
        List<String> deletions = new ArrayList<String>();
        for (Inventor pa : selectedInventors) {
            substitutions.add(pa.getName());
            if (pa.getHarmonized()) {
                deletions.add(pa.getName());
            }
        }
        rule.setCountry(countryRepository.getCountryByAcronym(rule.getCountry().getAcronym()));
        for (State state : rule.getCountry().getStates()) {
            if (state.getAcronym().equals(rule.getState().getAcronym())) {
                rule.setState(state);
            }
        }
        rule.getCountry().setStates(null);
        rule.setSubstitutions(new HashSet<String>(substitutions));
        if (harmonized || sugHarmonized) {
            for (String deletion : deletions) {
                String id = ruleRepository.findByName(deletion).getId().toString();
                ruleRepository.delete(id);
            }
        }
        ruleRepository.save(rule);

        ruleController.apply(rule.getId().toString());
        selectedInventors.clear();
        Flash flash = FacesContext.getCurrentInstance().getExternalContext().getFlash();
        flash.put("success", "Regra criada com sucesso");
        return "listRule";
    }

    public void loadSugestions() {
        String[] names = new String[selectedInventors.size()];

        selectedRadio = selectedInventors.get(0);
        onSelectInventorFill();

        int i = 0;
        harmonized = false;
        for (Inventor pa : selectedInventors) {
            names[i] = pa.getName();
            harmonized = harmonized || pa.getHarmonized();
            i++;
        }
        Set<String> sugestions = inventorRepository.getInventorSugestions(currentProject, 10, names);
        List<Inventor> inventores = new ArrayList<Inventor>();
        for (String sugestion : sugestions) {
            inventores.add(inventorRepository.getInventorByName(sugestion));
            //inventores.add(new Inventor(sugestion));
        }
        setInventorSugestions(new ArrayList<SelectObject<Inventor>>(SelectObject.convertToSelectObjects(inventores)));
    }

    public void metodo(Inventor pa) {
        if (selectedInventors.contains(pa)) {
            pa.setSelected(true);
        } else {
            pa.setSelected(false);
        }
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

    public DataModel<Inventor> getInventors() {
        return inventors;
    }

    public void setInventors(LazyInventorDataModel inventors) {
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

    public List<State> getStates() {
        return states;
    }

    public void setStates(List<State> states) {
        this.states = states;
    }

    public Inventor getSelectedRadio() {
        return selectedRadio;
    }

    public void setSelectedRadio(Inventor selectedRadio) {
        this.selectedRadio = selectedRadio;
    }

    public Integer getSearchType() {
        return searchType;
    }

    public void setSearchType(Integer searchType) {
        this.searchType = searchType;
        inventors.setSearchType(searchType);
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

}
