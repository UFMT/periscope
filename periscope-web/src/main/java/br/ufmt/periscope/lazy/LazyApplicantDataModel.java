package br.ufmt.periscope.lazy;

import br.ufmt.periscope.model.Applicant;
import br.ufmt.periscope.repository.ApplicantRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Named;
import org.primefaces.model.LazyDataModel;
import org.primefaces.model.SortOrder;

@Named
public class LazyApplicantDataModel extends LazyDataModel<Applicant> {

    private @Inject
    ApplicantRepository applicantRepository;
    private List<Applicant> datasource;
    private List<Applicant> selectedApplicants;
    private Integer searchType;
    private Boolean harmonization = false;

    public LazyApplicantDataModel() {
//        System.out.println("Lazy Applicant Data Model");
    }

    
    
    
    @Override
    public int getRowCount() {
        return super.getRowCount();
    }

    @Override
    public Applicant getRowData(String key) {
        for (Applicant applicant : datasource) {
            if (applicant.getName().equals(key)) {
                return applicant;
            }
        }
        return null;
    }

    @Override
    public Object getRowKey(Applicant object) {
        return object.getName();
    }

    @Override
    public List<Applicant> load(int first, int pageSize, String sortField, SortOrder sortOrder, Map<String, String> filters) {
        applicantRepository.setSearchType(searchType);
        if (harmonization) {
            datasource = applicantRepository.load(first, pageSize, sortField, sortOrder.ordinal(), filters);
        } else {
            datasource = applicantRepository.load(first, pageSize, sortField, sortOrder.ordinal(), filters, this.selectedApplicants);
        }
        setRowCount(applicantRepository.getCount());
        for (Applicant applicant : datasource) {
            if (this.selectedApplicants != null && this.selectedApplicants.contains(applicant)) {

                applicant.setSelected(true);
            }
        }
        return datasource;
    }

    public ApplicantRepository getApplicantRepository() {
        return applicantRepository;
    }

    public void setApplicantRepository(ApplicantRepository applicantRepository) {
        this.applicantRepository = applicantRepository;
    }

    public List<Applicant> getSelectedApplicants() {
        return selectedApplicants;
    }

    public void setSelectedApplicants(List<Applicant> selectedApplicants) {
        this.selectedApplicants = selectedApplicants;
    }

    public Integer getSearchType() {
        return searchType;
    }

    public void setSearchType(Integer searchType) {
        this.searchType = searchType;
    }

    public Boolean getHarmonization() {
        return harmonization;
    }

    public void setHarmonization(Boolean harmonization) {
        this.harmonization = harmonization;
    }

    public boolean verify(Applicant newApplicant) {
        return applicantRepository.exists(newApplicant);
    }
}
