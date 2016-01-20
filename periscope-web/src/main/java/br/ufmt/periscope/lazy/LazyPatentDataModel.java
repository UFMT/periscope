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
    private String type;
    private String name;

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
        if (this.type == null){
            datasource = repo.load(first, pageSize, sortField, sortOrder.ordinal(), filters);
        }else if (this.type.equals("applicant")) {
            datasource = repo.loadApplicantDocs(first, pageSize, sortField, sortOrder.ordinal(), filters, this.name);
        }else if(this.type.equals("inventor")){
            datasource = repo.loadInventorDocs(first, pageSize, sortField, sortOrder.ordinal(), filters, this.name);
        }
        return datasource;
        
    }

    public PatentRepository getRepo() {
        return repo;
    }

    public void setRepo(PatentRepository repo) {
        this.repo = repo;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public void setRowIndex(int rowIndex) {
        if (getPageSize() == 0)
            rowIndex = -1;
        super.setRowIndex(rowIndex); //To change body of generated methods, choose Tools | Templates.
    }

    
}
