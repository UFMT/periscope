package br.ufmt.periscope.controller.harmonization;

import br.ufmt.periscope.harmonization.Harmonization;
import br.ufmt.periscope.lazy.LazyRuleDataModel;
import br.ufmt.periscope.model.Applicant;
import br.ufmt.periscope.model.Project;
import br.ufmt.periscope.model.Rule;
import br.ufmt.periscope.qualifier.CurrentProject;
import br.ufmt.periscope.repository.ApplicantRepository;
import br.ufmt.periscope.repository.RuleRepository;
import br.ufmt.periscope.util.SelectObject;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import javax.faces.context.Flash;
import javax.inject.Inject;
import javax.inject.Named;

@ManagedBean
@ViewScoped
public class RuleController implements Serializable {

    private static final long serialVersionUID = 7744517674295407077L;

    private @Inject
    Logger log;
    private @Inject
    Harmonization harmonization;
    private @Inject
    @CurrentProject
    Project currentProject;
    private @Inject
    RuleRepository ruleRepository;
    private @Inject
    LazyRuleDataModel lazyApplicants;
    private @Inject
    LazyRuleDataModel lazyInventors;
    private List<Rule> applicantRules = new ArrayList<Rule>();
    private List<Rule> inventorRules = new ArrayList<Rule>();
    private @Inject
    ApplicantRepository applicantRepository;

    @PostConstruct
    public void init() {
        
        lazyApplicants.setSearchType(1);
        lazyApplicants.getRuleRepository().setCurrentProject(currentProject);
        lazyInventors.setSearchType(2);
        lazyInventors.getRuleRepository().setCurrentProject(currentProject);
        System.out.println("Novo");
        inventorRules = ruleRepository.getInventorRule(currentProject);
    }

    public String apply(String id) {
        
        Rule rule = ruleRepository.findById(id);
        harmonization.applyRule(rule);
        Flash flash = FacesContext.getCurrentInstance().
                getExternalContext().getFlash();
        flash.put("success", "Regra aplicada com sucesso");
        return "listRule";
    }
    
    /*
    public String apply(Rule rule){
        System.out.println("G: "+rule.getApplicantSugestions().size());
        for (Applicant pa : rule.getApplicantSugestions()) {
            if (pa.getSelected()) {
                System.out.println("teste: "+pa.toString());
            }else{
                System.out.println("NO!");
            }
        }
        Flash flash = FacesContext.getCurrentInstance().
                getExternalContext().getFlash();
        flash.put("success", "Regra aplicada com sucesso");
        return "listRule";
    }*/

    public String delete(String id) {
        System.out.println("Oi");
        ruleRepository.delete(id);
        Flash flash = FacesContext.getCurrentInstance().
                getExternalContext().getFlash();
        flash.put("info", "Deletado com sucesso");
        return "listRule";
    }

    
    public ArrayList<Applicant> loadApplicantSugestions(Rule rule) {
//        System.out.println("Load");
        String[] names = new String[rule.getSubstitutions().size()];
//        System.out.println("Tamanho: "+rule.getSubstitutions().size());
        int i = 0;
        for (Iterator<String> it = rule.getSubstitutions().iterator(); it.hasNext();) {
            names[i] = it.next();
//            System.out.println(i+" : "+names[i]);
            i++;
        }
//        System.out.println("pre-in: "+names.toString());
        Set<String> sugestions = applicantRepository.getApplicantSugestions(currentProject, 100, names);
//        System.out.println("saiu");
        ArrayList<Applicant> aplicants = new ArrayList<Applicant>();
        for (String sugestion : sugestions) {
            aplicants.add(new Applicant(sugestion));
        }
        return aplicants;

    }

    public void print(Applicant rule){
        System.out.println("APP "+rule.getName());
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

    public LazyRuleDataModel getLazyApplicants() {
        return lazyApplicants;
    }

    public void setLazyApplicants(LazyRuleDataModel lazyApplicants) {
        this.lazyApplicants = lazyApplicants;
    }

    public LazyRuleDataModel getLazyInventors() {
        return lazyInventors;
    }

    public void setLazyInventors(LazyRuleDataModel lazyInventors) {
        this.lazyInventors = lazyInventors;
    }
    
    
}
