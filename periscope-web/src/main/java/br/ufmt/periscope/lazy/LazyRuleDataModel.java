package br.ufmt.periscope.lazy;

import br.ufmt.periscope.model.Applicant;
import br.ufmt.periscope.model.Rule;
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
        rules = ruleRepository.load(first, pageSize, sortField, sortOrder.ordinal(), filters);
        return rules;
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
