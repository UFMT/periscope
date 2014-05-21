package br.ufmt.periscope.controller;

import br.ufmt.periscope.lazy.LazyPatentBrazilianDataModel;
import br.ufmt.periscope.lazy.LazyPatentDataModel;
import br.ufmt.periscope.model.Applicant;
import br.ufmt.periscope.model.Country;
import br.ufmt.periscope.model.Files;
import br.ufmt.periscope.model.Inventor;
import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import javax.faces.model.DataModel;
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
import com.mongodb.gridfs.GridFS;
import com.mongodb.gridfs.GridFSDBFile;
import com.mongodb.gridfs.GridFSInputFile;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import javax.faces.application.FacesMessage;
import javax.faces.context.ExternalContext;
import org.bson.types.ObjectId;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;
import org.primefaces.model.UploadedFile;

/**
 *
 * @author PH
 */
@ManagedBean
@ViewScoped
public class PatentBrazilianController {

    private @Inject
    @CurrentProject
    Project currentProject;
    private @Inject
    PatentRepository patentRepository;
    private @Inject LazyPatentBrazilianDataModel patents;
    private String type = "complete";
    private String[] filters = {"complete", "incomplete", "darklist"};
    private int totalCount = 0;
    private Patent selectedPatent;
    private UploadedFile file;
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
    private StreamedContent download;

    /**
     * Pre-loads the patent's presentation file before user can download it
     *
     * @param patent
     * @throws UnknownHostException
     * @throws IOException
     */
    public void preDownloadPresentation(Patent patent) throws UnknownHostException, IOException {
        GridFS fs = patentRepository.getFs();
        GridFSDBFile gfile = fs.findOne(patent.getPresentationFile().getId());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        long writeTo = gfile.writeTo(out);
        byte[] data = out.toByteArray();
        ByteArrayInputStream istream = new ByteArrayInputStream(data);
        InputStream arquivo = istream;
        ExternalContext externalContext = FacesContext.getCurrentInstance().getExternalContext();
        setDownload(new DefaultStreamedContent(arquivo, externalContext.getMimeType(gfile.getFilename()), gfile.getFilename()));

    }

    /**
     * Pre-loads the patent's info file before user can download it
     *
     * @param patent
     * @throws UnknownHostException
     * @throws IOException
     */
    public void preDownloadPatent(Patent patent) throws UnknownHostException, IOException {
        GridFS fs = patentRepository.getFs();
        GridFSDBFile gfile = fs.findOne(patent.getPatentInfo().getId());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        long writeTo = gfile.writeTo(out);
        byte[] data = out.toByteArray();
        ByteArrayInputStream istream = new ByteArrayInputStream(data);
        InputStream arquivo = istream;
        ExternalContext externalContext = FacesContext.getCurrentInstance().getExternalContext();
        setDownload(new DefaultStreamedContent(arquivo, externalContext.getMimeType(gfile.getFilename()), gfile.getFilename()));

    }

    /**
     * Controller constructor<BR/>
     * Loads project's patents, countries, applicants and inventors
     *
     * @throws UnknownHostException
     */
    @PostConstruct
    public void init(){
        patents.getRepo().setBlacklisted(false);
        patents.getRepo().setCompleted(false);
        patents.getRepo().setCurrentProject(currentProject);
        totalCount = patents.getRowCount();
        countries = countryRepository.getAll();
        applicants = applicantRepository.getApplicants(currentProject);
        inventors = inventorRepository.getInventors(currentProject);
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletRequest req = (HttpServletRequest) context.getExternalContext().getRequest();
        System.out.println("edit");
        if (req.getParameter("patentId") != null) {
            selectedPatent = patentRepository.getPatentWithId(currentProject, new ObjectId(req.getParameter("patentId"))).get(0);
            System.out.println("selecpatent");
            updateApplicants();
            updateInventors();
        }

        updateList();
    }

    public void updateApplicants() {
        for (int i = 0; i < selectedPatent.getApplicants().size(); i++) {
            applicants.remove(selectedPatent.getApplicants().get(i));
        }
    }

    public void updateInventors() {
        for (int i = 0; i < selectedPatent.getInventors().size(); i++) {
            inventors.remove(selectedPatent.getInventors().get(i));

        }
    }

    /**
     * Save the edited patent
     *
     * @return
     */
    public String save() {
        patentRepository.savePatent(selectedPatent);
        return "listPatent";
    }

    /**
     * Instantiate a new priority when adding
     */
    public void doPriority() {
        newPriority = new Priority();
    }

    /**
     * Add new priority to patents's priorities list
     *
     * @return
     */
    public String addPriority() {
        List<Priority> list = selectedPatent.getPriorities();
        newPriority.setCountry(countryRepository.getCountryByAcronym(newPriority.getCountry().getAcronym()));
        list.add(newPriority);
        selectedPatent.setPriorities(list);
        return "";
    }

    /**
     * delete priority from patent's priorities list
     *
     * @param priority
     * @return
     */
    public String deletePriority(Priority priority) {
        List<Priority> list = selectedPatent.getPriorities();
        list.remove(priority);
        selectedPatent.setPriorities(list);
        return "";
    }

    /**
     * Instantiate the new applicant
     */
    public void doApplicant() {
        newApplicant = new Applicant();
    }

    /**
     * Adds new applicant to project's applicants list
     *
     * @return
     */
    public String newApplicant() {
        List<Applicant> list = selectedPatent.getApplicants();
        newApplicant.setCountry(countryRepository.getCountryByAcronym(newApplicant.getCountry().getAcronym()));
        if (!selectedPatent.getApplicants().contains(newApplicant) || !applicants.contains(newApplicant)) {
            list.add(newApplicant);
            selectedPatent.setApplicants(list);
        }
        return "";
    }

    /**
     * Adds a applicant to the patent's applicants list
     *
     * @param applicant
     * @return
     */
    public String addApplicant(Applicant applicant) {
        List<Applicant> list = selectedPatent.getApplicants();
        applicants.remove(applicant);
        list.add(applicant);
        selectedPatent.setApplicants(list);
        return "";
    }

    /**
     * Deletes a applicant from the patent's applicants list
     *
     * @param applicant
     * @return
     */
    public String deleteApplicant(Applicant applicant) {
        List<Applicant> list = selectedPatent.getApplicants();
        list.remove(applicant);
        applicants.add(applicant);
        selectedPatent.setApplicants(list);
        return "";
    }

    /**
     * Instantiate the new inventor
     */
    public void doInventor() {
        newInventor = new Inventor();
    }

    /**
     * Adds new inventor the the project's inventors list
     *
     * @return
     */
    public String newInventor() {
        List<Inventor> list = selectedPatent.getInventors();
        newInventor.setCountry(countryRepository.getCountryByAcronym(newInventor.getCountry().getAcronym()));
        if (!inventors.contains(newInventor) || !selectedPatent.getInventors().contains(newInventor)) {
            list.add(newInventor);
            selectedPatent.setInventors(list);
        }
        return "";
    }

    /**
     * Adds a inventor to the patent's inventors list
     *
     * @param inventor
     * @return
     */
    public String addInventor(Inventor inventor) {
        List<Inventor> list = selectedPatent.getInventors();
        if (!list.contains(inventor)) {
            list.add(inventor);
            inventors.remove(inventor);
            selectedPatent.setInventors(list);
        }
        return "";
    }

    /**
     * Deletes a inventor from the patent's inventors list
     *
     * @param inventor
     * @return
     */
    public String deleteInventor(Inventor inventor) {
        List<Inventor> list = selectedPatent.getInventors();
        list.remove(inventor);
        inventors.add(inventor);
        selectedPatent.setInventors(list);
        return "";
    }

    /**
     * Handles the uploading of the presentation file to the patent If the
     * pantet alredy has a presentation file the method will delete the file
     * from database before uploading a new one
     *
     * @param event
     * @throws UnknownHostException
     * @throws IOException
     */
    public void uploadPresentationFile(FileUploadEvent event) throws UnknownHostException, IOException {
        file = event.getFile();
        GridFS fs = patentRepository.getFs();
        System.out.println(file.getFileName());
        GridFSInputFile gfsFiles = fs.createFile(file.getInputstream());
        gfsFiles.setFilename(file.getFileName());
        gfsFiles.save();
        if (selectedPatent.getPresentationFile() == null) {
            Files novo = new Files((ObjectId) gfsFiles.getId());
            selectedPatent.setPresentationFile(novo);
        } else {
            fs.remove(selectedPatent.getPresentationFile().getId());
            Files novo = new Files((ObjectId) gfsFiles.getId());
            selectedPatent.setPresentationFile(novo);
        }
        save();

        FacesMessage msg = new FacesMessage("Sucesso", event.getFile().getFileName() + " foi enviado.");
        FacesContext.getCurrentInstance().addMessage(null, msg);

    }

    /**
     * Handles the uploading of the Patent Info file to the patent If the pantet
     * alredy has a Patent Info file the method will delete the file from
     * database before uploading a new one
     *
     * @param event
     * @throws UnknownHostException
     * @throws IOException
     */
    public void uploadPatentInfo(FileUploadEvent event) throws UnknownHostException, IOException {
        file = event.getFile();
        GridFS fs = patentRepository.getFs();
        System.out.println(file.getFileName());
        GridFSInputFile gfsFiles = fs.createFile(file.getInputstream());
        gfsFiles.setFilename(file.getFileName());
        gfsFiles.save();
        if (selectedPatent.getPatentInfo() == null) {
            Files novo = new Files((ObjectId) gfsFiles.getId());
            selectedPatent.setPatentInfo(novo);
        } else {
            fs.remove(selectedPatent.getPatentInfo().getId());
            Files novo = new Files((ObjectId) gfsFiles.getId());
            selectedPatent.setPatentInfo(novo);
        }

        FacesMessage msg = new FacesMessage("Sucesso", event.getFile().getFileName() + " foi enviado.");
        FacesContext.getCurrentInstance().addMessage(null, msg);
    }

    /**
     * Updates the project's patents list
     */
    public void updateList() {
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletRequest req = (HttpServletRequest) context
                .getExternalContext().getRequest();
        String param = req.getParameter("listType");
        if (param != null) {
            this.type = req.getParameter("listType");
        }
        if (type.contentEquals("complete")) {
            patents.getRepo().setBlacklisted(false);
            patents.getRepo().setCompleted(true);
        } else if (type.contentEquals("incomplete")) {
            patents.getRepo().setBlacklisted(false);
            patents.getRepo().setCompleted(false);
        } else if (type.contentEquals("darklist")) {
            patents.getRepo().setBlacklisted(true);
            patents.getRepo().setCompleted(true);
        } else {
            patents.getRepo().setBlacklisted(false);
            patents.getRepo().setCompleted(true);
        }
    }

    /**
     * Sends a patent to the black list
     */
    public void updateBlackListPatent() {
        patentRepository.sendPatentToBlacklist(patents.getRowData());
        updateList();
    }

    /**
     *
     * @return
     */
    public DataModel<Patent> getPatents() {
        return patents;
    }

    /**
     *
     * @param patents
     */
    public void setPatents(DataModel<Patent> patents) {
        this.patents = (LazyPatentBrazilianDataModel) patents;
    }

    /**
     *
     * @return
     */
    public String[] getFilters() {
        return filters;
    }

    /**
     *
     * @return
     */
    public StreamedContent getDownload() {
        return download;
    }

    /**
     *
     * @param download
     */
    public void setDownload(StreamedContent download) {
        this.download = download;
    }

    /**
     *
     * @return
     */
    public int getTotalCount() {
        return totalCount;
    }

    /**
     *
     * @return
     */
    public int getPartialCount() {
        return patents.getRowCount();
    }

    /**
     *
     * @return
     */
    public String getType() {
        return type;
    }

    /**
     *
     * @return
     */
    public Patent getSelectedPatent() {
        return selectedPatent;
    }

    /**
     *
     * @param selectedPatent
     */
    public void setSelectedPatent(Patent selectedPatent) {
        this.selectedPatent = selectedPatent;
    }

    /**
     *
     * @return
     */
    public Priority getNewPriority() {
        return newPriority;
    }

    /**
     *
     * @param newPriority
     */
    public void setNewPriority(Priority newPriority) {
        this.newPriority = newPriority;
    }

    /**
     *
     * @return
     */
    public List<Country> getCountries() {
        return countries;
    }

    /**
     *
     * @param countries
     */
    public void setCountries(List<Country> countries) {
        this.countries = countries;
    }

    /**
     *
     * @return
     */
    public Applicant getNewApplicant() {
        return newApplicant;
    }

    /**
     *
     * @param newApplicant
     */
    public void setNewApplicant(Applicant newApplicant) {
        this.newApplicant = newApplicant;
    }

    /**
     *
     * @return
     */
    public Inventor getNewInventor() {
        return newInventor;
    }

    /**
     *
     * @param newInventor
     */
    public void setNewInventor(Inventor newInventor) {
        this.newInventor = newInventor;
    }

    /**
     *
     * @return
     */
    public List<Applicant> getApplicants() {
        return applicants;
    }

    /**
     *
     * @param applicants
     */
    public void setApplicants(List<Applicant> applicants) {
        this.applicants = applicants;
    }

    /**
     *
     * @return
     */
    public List<Inventor> getInventors() {
        return inventors;
    }

    /**
     *
     * @param inventors
     */
    public void setInventors(List<Inventor> inventors) {
        this.inventors = inventors;
    }

    /**
     *
     * @return
     */
    public UploadedFile getFile() {
        return file;
    }

    /**
     *
     * @param file
     */
    public void setFile(UploadedFile file) {
        this.file = file;
    }
}
