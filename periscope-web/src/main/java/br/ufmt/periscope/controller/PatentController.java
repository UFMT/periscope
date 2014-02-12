package br.ufmt.periscope.controller;

import br.ufmt.periscope.model.Applicant;
import br.ufmt.periscope.model.Classification;
import br.ufmt.periscope.model.Country;
import br.ufmt.periscope.model.Inventor;
import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import javax.faces.model.DataModel;
import javax.faces.model.ListDataModel;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

import br.ufmt.periscope.model.Patent;
import br.ufmt.periscope.model.Priority;
import br.ufmt.periscope.model.Project;
import br.ufmt.periscope.qualifier.CurrentProject;
import br.ufmt.periscope.repository.ApplicantRepository;
import br.ufmt.periscope.repository.CountryRepository;
import br.ufmt.periscope.repository.InventorRepository;
import br.ufmt.periscope.repository.PatentRepository;
import java.util.ArrayList;
import java.util.List;
import org.bson.types.ObjectId;

@ManagedBean
@ViewScoped
public class PatentController {

    private @Inject
    @CurrentProject
    Project currentProject;
    private @Inject
    PatentRepository patentRepository;
    private DataModel<Patent> patents = null;
    private String type = "complete";
    private String[] filters = {"complete", "incomplete", "darklist"};
    private int totalCount = 0;
    private Patent selectedPatent;
    private @Inject
    CountryRepository countryRepository;
    private List<Country> countries = new ArrayList<Country>();
    private @Inject
    ApplicantRepository applicantRepository;
    private List<Applicant> applicants = new ArrayList<Applicant>();
    private @Inject
    InventorRepository inventorRepository;
    private List<Inventor> inventors = new ArrayList<Inventor>();
    private @Inject
    Applicant newApplicant;
    private @Inject
    Priority newPriority;
    private @Inject
    Inventor newInventor;

    @PostConstruct
    public void init() {
        patents = new ListDataModel<Patent>(patentRepository.getAllPatents(currentProject));
        totalCount = patents.getRowCount();
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletRequest req = (HttpServletRequest) context.getExternalContext().getRequest();
        if (req.getParameter("patentId") != null) {
            selectedPatent = patentRepository.getPatentWithId(currentProject, new ObjectId(req.getParameter("patentId"))).get(0);
        }
        countries = countryRepository.getAll();
        applicants = applicantRepository.getApplicants(currentProject);
        inventors = inventorRepository.getInventors(currentProject);
        updateList();
    }

    public String save() {
        System.out.println(selectedPatent.getTitleSelect());
        patentRepository.savePatent(selectedPatent);
        return "listPatent";
    }

    public void doPriority() {
        newPriority = new Priority();
    }

    public String addPriority() {
        List<Priority> list = selectedPatent.getPriorities();
        newPriority.setCountry(countryRepository.getCountryByAcronym(newPriority.getCountry().getAcronym()));
        list.add(newPriority);
        selectedPatent.setPriorities(list);
        return "";
    }

    public String deletePriority(Priority priority) {
        List<Priority> list = selectedPatent.getPriorities();
        list.remove(priority);
        selectedPatent.setPriorities(list);
        return "";
    }

    public void doApplicant() {
        newApplicant = new Applicant();
    }

    public String newApplicant() {
        List<Applicant> list = selectedPatent.getApplicants();
        newApplicant.setCountry(countryRepository.getCountryByAcronym(newApplicant.getCountry().getAcronym()));
        if (!applicants.contains(newApplicant)) {
            list.add(newApplicant);
            selectedPatent.setApplicants(list);
        }
        return "";
    }

    public String addApplicant(Applicant applicant) {
        List<Applicant> list = selectedPatent.getApplicants();
        if (!list.contains(applicant)) {
            System.out.println(applicant.getName());
            list.add(applicant);
            selectedPatent.setApplicants(list);
        }
        return "";
    }

    public String deleteApplicant(Applicant applicant) {
        List<Applicant> list = selectedPatent.getApplicants();
        list.remove(applicant);
        selectedPatent.setApplicants(list);
        return "";
    }

    public void doInventor() {
        newInventor = new Inventor();
    }

    public String newInventor() {
        List<Inventor> list = selectedPatent.getInventors();
        newInventor.setCountry(countryRepository.getCountryByAcronym(newInventor.getCountry().getAcronym()));
        if (!inventors.contains(newInventor)) {
            list.add(newInventor);
            selectedPatent.setInventors(list);
        }
        return "";
    }

    public String addInventor(Inventor inventor) {
        List<Inventor> list = selectedPatent.getInventors();
        if (!list.contains(inventor)) {
            list.add(inventor);
            selectedPatent.setInventors(list);
        }
        return "";
    }

    public String deleteInventor(Inventor inventor) {
        List<Inventor> list = selectedPatent.getInventors();
        list.remove(inventor);
        selectedPatent.setInventors(list);
        return "";
    }

//    public void editPatent() {
//        System.out.println("sim");
//        selectedPatent = patents.getRowData();
//        System.out.println(selectedPatent.getTitleSelect());
////        System.out.println(selectedPatent.getApplicantsToString());
////        System.out.println(selectedPatent.getMainClassification().getValue());
//    }
    public void updateList() {
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletRequest req = (HttpServletRequest) context
                .getExternalContext().getRequest();
        String param = req.getParameter("listType");
        if (param != null) {
            type = req.getParameter("listType");
        }
        if (type.contentEquals("complete")) {
            patents = new ListDataModel<Patent>(patentRepository.getPatentsComplete(currentProject, true));
        } else if (type.contentEquals("incomplete")) {
            patents = new ListDataModel<Patent>(patentRepository.getPatentsComplete(currentProject, false));
        } else if (type.contentEquals("darklist")) {
            patents = new ListDataModel<Patent>(patentRepository.getPatentsDarklist(currentProject, true));
        } else {
            patents = new ListDataModel<Patent>(patentRepository.getPatentsDarklist(currentProject, false));
        }
    }

    public void updateBlackListPatent() {
        patentRepository.sendPatentToBlacklist(patents.getRowData());
        updateList();
    }

    public DataModel<Patent> getPatents() {
        return patents;
    }

    public void setPatents(DataModel<Patent> patents) {
        this.patents = patents;
    }

    public String[] getFilters() {
        return filters;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public int getPartialCount() {
        return patents.getRowCount();
    }

    public String getType() {
        return type;
    }

    public Patent getSelectedPatent() {
        return selectedPatent;
    }

    public void setSelectedPatent(Patent selectedPatent) {
        this.selectedPatent = selectedPatent;
    }

    public Priority getNewPriority() {
        return newPriority;
    }

    public void setNewPriority(Priority newPriority) {
        this.newPriority = newPriority;
    }

    public List<Country> getCountries() {
        return countries;
    }

    public void setCountries(List<Country> countries) {
        this.countries = countries;
    }

    public Applicant getNewApplicant() {
        return newApplicant;
    }

    public void setNewApplicant(Applicant newApplicant) {
        this.newApplicant = newApplicant;
    }

    public Inventor getNewInventor() {
        return newInventor;
    }

    public void setNewInventor(Inventor newInventor) {
        this.newInventor = newInventor;
    }

    public List<Applicant> getApplicants() {
        return applicants;
    }

    public void setApplicants(List<Applicant> applicants) {
        this.applicants = applicants;
    }

    public List<Inventor> getInventors() {
        return inventors;
    }

    public void setInventors(List<Inventor> inventors) {
        this.inventors = inventors;
    }
}
