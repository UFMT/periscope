package br.ufmt.periscope.lazy;

import br.ufmt.periscope.model.Inventor;
import br.ufmt.periscope.repository.InventorRepository;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Named;
import org.primefaces.model.LazyDataModel;
import org.primefaces.model.SortOrder;

@Named
public class LazyInventorDataModel extends LazyDataModel<Inventor> {

    private @Inject
    InventorRepository inventorRepository;
    private List<Inventor> datasource;
    private List<Inventor> selectedInventors;

    @Override
    public int getRowCount() {
        return super.getRowCount(); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Inventor getRowData(String key) {
        for (Inventor inventor : datasource) {
            if (inventor.getName().equals(key)) {
                return inventor;
            }
        }
        return null;
    }

    @Override
    public Object getRowKey(Inventor object) {
        return object.getName();
    }

    @Override
    public List<Inventor> load(int first, int pageSize, String sortField, SortOrder sortOrder, Map<String, String> filters) {
        datasource = inventorRepository.load(first, pageSize, sortField, sortOrder.ordinal(), filters, this.selectedInventors);
        setRowCount(inventorRepository.getCount());
        for (Inventor inventor : datasource) {
            if (this.selectedInventors.contains(inventor)) {
                inventor.setSelected(true);
            }
        }
        return datasource;
    }

    public InventorRepository getInventorRepository() {
        return inventorRepository;
    }

    public void setInventorRepository(InventorRepository inventorRepository) {
        this.inventorRepository = inventorRepository;
    }

    public List<Inventor> getSelectedInventors() {
        return selectedInventors;
    }

    public void setSelectedInventors(List<Inventor> selectedInventors) {
        this.selectedInventors = selectedInventors;
    }

    public boolean verify(Inventor newInventor) {
        return inventorRepository.exists(newInventor);
    }
}
