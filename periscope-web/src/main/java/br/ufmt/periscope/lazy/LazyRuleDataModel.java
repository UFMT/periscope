package br.ufmt.periscope.lazy;

import br.ufmt.periscope.model.Applicant;
import br.ufmt.periscope.model.Inventor;
import br.ufmt.periscope.model.Rule;
import br.ufmt.periscope.repository.ApplicantRepository;
import br.ufmt.periscope.repository.InventorRepository;
import br.ufmt.periscope.repository.RuleRepository;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Named;
import org.primefaces.model.LazyDataModel;
import org.primefaces.model.SortOrder;

@Named
public class LazyRuleDataModel extends LazyDataModel<Rule>{
    
    private @Inject
    RuleRepository ruleRepository;
    private List<Rule> rules;
    private Integer searchType;
    private @Inject
    ApplicantRepository applicantRepository;
    private @Inject
    InventorRepository inventorRepository;

    
    @Override
    public int getRowCount() {
        return ruleRepository.getRowCount();
    }

    @Override
    public Object getRowKey(Rule object) {
        return object.getId();
    }

    @Override
    public Rule getRowData(String rowkey) {
        for (Rule rule : rules) {
            if (rule.getId().toString().equals(rowkey)) {
                return rule;
            }
        }
        return null;
    }
    
    
    @Override
    public List<Rule> load(int first, int pageSize, String sortField, SortOrder sortOrder, Map<String, String> filters) {
        
        ruleRepository.setSearchType(this.searchType);
        
        List<Rule> regras;
        rules = ruleRepository.load(first, pageSize, sortField, sortOrder.ordinal(), filters);
        regras = new ArrayList<Rule>();
        if (this.searchType == 1) {
            for (Rule rule : rules) {
                rule.setAppSugestions(loadApplicantSugestions(rule));
                regras.add(rule);
            }    
        }else{
            for (Rule rule : rules) {
                rule.setInvSugestions(loadInventorSugestions(rule));
                regras.add(rule);
            }
        }
        return regras;
    }
    
    public List<Applicant> loadApplicantSugestions(Rule rule) {
        String[] names = new String[rule.getSubstitutions().size()];
        int i = 0;
        for (Iterator<String> it = rule.getSubstitutions().iterator(); it.hasNext();) {
            names[i] = it.next();
            i++;
        }
        Set<String> sugestions = applicantRepository.getApplicantSugestions(ruleRepository.getCurrentProject(), 100, names);
        ArrayList<Applicant> aplicants = new ArrayList<Applicant>();
        for (String sugestion : sugestions) {
            aplicants.add(new Applicant(sugestion));
        }
        rule.setAppSugestions(aplicants);
        return aplicants;
    }
    
    public List<Inventor> loadInventorSugestions(Rule rule) {
        String[] names = new String[rule.getSubstitutions().size()];
        int i = 0;
        for (Iterator<String> it = rule.getSubstitutions().iterator(); it.hasNext();) {
            names[i] = it.next();
            i++;
        }
        Set<String> sugestions = inventorRepository.getInventorSugestions(ruleRepository.getCurrentProject(), 100, names);
        ArrayList<Inventor> inventors = new ArrayList<Inventor>();
        for (String sugestion : sugestions) {
            inventors.add(new Inventor(sugestion));
        }
        rule.setInvSugestions(inventors);
        return inventors;
    }

    public Integer getSearchType() {
        return searchType;
    }

    public void setSearchType(Integer searchType) {
        this.searchType = searchType;
    }

    public RuleRepository getRuleRepository() {
        return ruleRepository;
    }

    public void setRuleRepository(RuleRepository ruleRepository) {
        this.ruleRepository = ruleRepository;
    }

    public List<Rule> getRules() {
        return rules;
    }

    public void setRules(List<Rule> rules) {
        this.rules = rules;
    }
    
    
}
