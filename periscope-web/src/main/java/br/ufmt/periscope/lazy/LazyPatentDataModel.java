package br.ufmt.periscope.lazy;

import br.ufmt.periscope.model.Patent;
import br.ufmt.periscope.repository.PatentRepository;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Named;
import org.primefaces.model.LazyDataModel;
import org.primefaces.model.SortOrder;

@Named
public class LazyPatentDataModel extends LazyDataModel<Patent>{
    private @Inject PatentRepository repo;
    private List<Patent> datasource;

    @Override
    public int getRowCount() {
        return repo.getRowCount();
    }

    @Override
    public Object getRowKey(Patent object) {
        return object.getId();
    }

    @Override
    public Patent getRowData(String rowkey) {
        for (Patent patent : datasource) {
            if (patent.getId().toString().equals(rowkey)) {
                return patent;
            }
        }
        return null;
    }
    
    
    @Override
    public List<Patent> load(int first, int pageSize, String sortField, SortOrder sortOrder, Map<String, String> filters) {
        datasource = repo.load(first, pageSize, sortField, sortOrder.ordinal(), filters);
        return datasource;
        
    }

    public PatentRepository getRepo() {
        return repo;
    }

    public void setRepo(PatentRepository repo) {
        this.repo = repo;
    }

    
}
