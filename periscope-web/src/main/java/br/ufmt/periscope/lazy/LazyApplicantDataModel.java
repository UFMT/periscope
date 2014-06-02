package br.ufmt.periscope.lazy;

import br.ufmt.periscope.model.Applicant;
import br.ufmt.periscope.repository.ApplicantRepository;
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

    @Override
    public int getRowCount() {
        return super.getRowCount(); //To change body of generated methods, choose Tools | Templates.
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
        datasource = applicantRepository.load(first, pageSize, sortField, sortOrder.ordinal(), filters, this.selectedApplicants);
        setRowCount(applicantRepository.getCount());
        for (Applicant applicant : datasource) {
//            System.out.println(applicant.getName()+" "+applicant.getCountry().getAcronym());
            if (this.selectedApplicants.contains(applicant)) {
                
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
}
