package br.ufmt.periscope.controller.harmonization;

import br.ufmt.periscope.harmonization.Harmonization;
import br.ufmt.periscope.lazy.LazyRuleDataModel;
import br.ufmt.periscope.model.Applicant;
import br.ufmt.periscope.model.Inventor;
import br.ufmt.periscope.model.Project;
import br.ufmt.periscope.model.Rule;
import br.ufmt.periscope.qualifier.CurrentProject;
import br.ufmt.periscope.repository.RuleRepository;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import javax.faces.context.Flash;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

@ManagedBean
@ViewScoped
public class RuleController implements Serializable {

    private static final long serialVersionUID = 7744517674295407077L;
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
    private Integer tabIndex;

    public RuleController() {
//        System.out.println("Rule Controller");
    }

    @PostConstruct
    public void init() {
        lazyApplicants.setSearchType(1);
        lazyApplicants.getRuleRepository().setCurrentProject(currentProject);
        lazyInventors.setSearchType(2);
        lazyInventors.getRuleRepository().setCurrentProject(currentProject);
        tabIndex = 0;
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletRequest req = (HttpServletRequest) context.getExternalContext().getRequest();
        if (req.getParameter("tabIndex") != null) {
            String parameter = req.getParameter("tabIndex");
            tabIndex = Integer.parseInt(parameter);
        }
    }

    public void tabIndexChanger() {
        tabIndex = tabIndex % 1;
        //tabView.setActiveIndex(tabIndex);
        System.out.println("indexChanger " + tabIndex);
    }

    public String applyAll() {
//        long in = System.currentTimeMillis();
        List<Rule> rules = ruleRepository.getAllRule(currentProject);
//        System.out.println("Busca : "+(System.currentTimeMillis() - in));
//        in = System.currentTimeMillis();
        for (Rule rule : rules) {
            rule.setProject(currentProject);
            harmonization.applyRule(rule);
        }
//        System.out.println("Aplicar "+(System.currentTimeMillis() - in));
        Flash flash = FacesContext.getCurrentInstance().
                getExternalContext().getFlash();
        flash.put("success", "Regras aplicada com sucesso");
        return "listRule";
    }

    public String applyApplicant(Rule rule) {
        if (rule.getAppSugestions() != null) {
            List<String> subs = new ArrayList<String>();
            for (Applicant applicant : rule.getAppSugestions()) {
                if (applicant.getSelected()) {
                    subs.add(applicant.getName());
                }
            }
            if (!subs.isEmpty()) {
                rule.setSubstitutions(new HashSet<String>(subs));
                rule.setProject(currentProject);
                ruleRepository.save(rule);
            }
        }
        return apply(rule.getId().toString());
    }

    public String applyInventor(Rule rule) {
        if (rule.getInvSugestions() != null) {
            List<String> subs = new ArrayList<String>();
            for (Inventor inventor : rule.getInvSugestions()) {
                if (inventor.getSelected()) {
                    subs.add(inventor.getName());
                }
            }
            if (!subs.isEmpty()) {
                rule.setSubstitutions(new HashSet<String>(subs));
                rule.setProject(currentProject);
                ruleRepository.save(rule);
            }
        }
        return apply(rule.getId().toString());
    }

    public String apply(String id) {
        Rule rule = ruleRepository.findById(id);
        harmonization.applyRule(rule);
        Flash flash = FacesContext.getCurrentInstance().
                getExternalContext().getFlash();
        flash.put("success", "Regra aplicada com sucesso");
        return "listRule";
    }

    public String delete(String id) {
        ruleRepository.delete(id);
        Flash flash = FacesContext.getCurrentInstance().
                getExternalContext().getFlash();
        flash.put("info", "Deletado com sucesso");
        return "listRule";
    }

    public String deleteAppRule(String name, String id) {
        undoAppRule(name);
        return delete(id);
    }

    public String deleteInvRule(String name, String id) {
        undoInvRule(name);
        return delete(id);
    }

    public void undoAppRule(String name) {
        ruleRepository.undoApplicantRule(currentProject, name);
    }

    public void undoInvRule(String name) {
        ruleRepository.undoInventorRule(currentProject, name);
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

    public Integer getTabIndex() {
        return tabIndex;
    }

    public void setTabIndex(Integer tabIndex) {
        this.tabIndex = tabIndex;
    }
}
