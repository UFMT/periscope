package br.ufmt.periscope.controller;

import br.ufmt.periscope.lazy.LazyApplicantDataModel;
import br.ufmt.periscope.lazy.LazyInventorDataModel;
import br.ufmt.periscope.lazy.LazyPatentDataModel;
import br.ufmt.periscope.model.Applicant;
import br.ufmt.periscope.model.Classification;
import br.ufmt.periscope.model.Country;
import br.ufmt.periscope.model.Files;
import br.ufmt.periscope.model.Inventor;
import br.ufmt.periscope.model.Patent;
import br.ufmt.periscope.model.Priority;
import br.ufmt.periscope.model.Project;
import br.ufmt.periscope.qualifier.CurrentProject;
import br.ufmt.periscope.repository.CountryRepository;
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
import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.context.Flash;
import javax.faces.model.DataModel;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import org.bson.types.ObjectId;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;
import org.primefaces.model.UploadedFile;

@ManagedBean
@ViewScoped
public class PatentController {
    
    private @Inject
    @CurrentProject
    Project currentProject;
    private @Inject
    PatentRepository patentRepository;
    private @Inject
    LazyPatentDataModel patents;
    private String type = "complete";
    private String[] filters = {"complete", "incomplete", "darklist"};
    private int totalCount = 0;
    private Patent selectedPatent;
    private UploadedFile file;
    private Files temporaryPresentationFile;
    private Files temporaryPatentInfo;
    private GridFS fs;
    private @Inject
    CountryRepository countryRepository;
    private List<Country> countries = new ArrayList<Country>();
    private @Inject
    LazyApplicantDataModel applicants;
    private @Inject
    LazyInventorDataModel inventors;
    private @Inject
    Applicant newApplicant;
    private @Inject
    Priority newPriority;
    private @Inject
    Inventor newInventor;
    private StreamedContent download;

    /**
     * Controller constructor<BR/>
     * Loads project's patents, countries, applicants and inventors
     *
     */
    @PostConstruct
    public void init() {
        patents.getRepo().setBlacklisted(false);
        patents.getRepo().setCompleted(false);
        patents.getRepo().setCurrentProject(currentProject);
        totalCount = patents.getRowCount();
        countries = countryRepository.getAll();
        applicants.getApplicantRepository().setCurrentProject(currentProject);
        inventors.getInventorRepository().setCurrentProject(currentProject);
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletRequest req = (HttpServletRequest) context.getExternalContext().getRequest();
        if (req.getParameter("patentId") != null) {
            selectedPatent = patentRepository.getPatentWithId(currentProject, new ObjectId(req.getParameter("patentId"))).get(0);
            temporaryPatentInfo = selectedPatent.getPatentInfo();
            temporaryPresentationFile = selectedPatent.getPresentationFile();
            updateApplicants();
            updateInventors();
        }
        if (req.getParameter("patentAdd") != null) {
            selectedPatent = new Patent();
            selectedPatent.setMainClassification(new Classification());
            selectedPatent.setProject(currentProject);
            updateApplicants();
            updateInventors();
        }
        
        updateList();
    }

    /**
     * Updates the selected patent's applicants list
     */
    public void updateApplicants() {
        applicants.setSelectedApplicants(selectedPatent.getApplicants());
    }

    /**
     * Updates the selected patent's inventors list
     */
    public void updateInventors() {
        inventors.setSelectedInventors(selectedPatent.getInventors());
    }

    /**
     * Save the edited patent
     *
     * @return
     * @throws java.net.UnknownHostException
     */
    public String save() throws UnknownHostException {
        selectedPatent.setApplicants(applicants.getSelectedApplicants());
        selectedPatent.setInventors(inventors.getSelectedInventors());
        openFs();
        if (selectedPatent.getPatentInfo() != null && !temporaryPatentInfo.equals(selectedPatent.getPatentInfo())) {
            fs.remove(selectedPatent.getPatentInfo().getId());
        }
        if (selectedPatent.getPresentationFile() != null && !temporaryPresentationFile.equals(selectedPatent.getPresentationFile())) {
            fs.remove(selectedPatent.getPresentationFile().getId());
        }
        selectedPatent.setPatentInfo(temporaryPatentInfo);
        selectedPatent.setPresentationFile(temporaryPresentationFile);
        patentRepository.savePatent(selectedPatent);
        return "listPatent";
    }

    /**
     * Adds new patent to the current project
     *
     * @return
     * @throws java.net.UnknownHostException
     */
    public String add() throws UnknownHostException {
        selectedPatent.setApplicants(applicants.getSelectedApplicants());
        selectedPatent.setInventors(inventors.getSelectedInventors());
        selectedPatent.setPatentInfo(temporaryPatentInfo);
        selectedPatent.setPresentationFile(temporaryPatentInfo);
        patentRepository.savePatentToDatabase(selectedPatent, currentProject);
        return "listPatent";
    }
    
    /**
     * When adding or editing a patent if the user leaves the patent's file unchanged it will be deleted
     * 
     * @param page
     * @return
     * @throws UnknownHostException
     */
    public String cancelChange(String page) throws UnknownHostException {
        if (temporaryPatentInfo != null) {
            if (!temporaryPatentInfo.equals(selectedPatent.getPatentInfo())) {
                openFs();
                fs.remove(temporaryPatentInfo.getId());
            }
        }
        if (temporaryPresentationFile != null) {
            if (!temporaryPresentationFile.equals(selectedPatent.getPresentationFile())) {
                openFs();
                fs.remove(temporaryPresentationFile.getId());
            }
        }
        if(page.equals("edit")){
            return "listPatent";
        }
        if(page.equals("add")){
            return "projectHome";
        }
        return "";
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
        newApplicant.setCountry(countryRepository.getCountryByAcronym(newApplicant.getCountry().getAcronym()));
        if (applicants.verify(newApplicant)) {
            if (!applicants.getSelectedApplicants().contains(newApplicant)) {
                applicants.getSelectedApplicants().add(newApplicant);
            }
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
        applicants.getSelectedApplicants().add(applicant);
        return "";
    }

    /**
     * Deletes a applicant from the patent's applicants list
     *
     * @param applicant
     * @return
     */
    public String deleteApplicant(Applicant applicant) {
        applicants.getSelectedApplicants().remove(applicant);
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
        newInventor.setCountry(countryRepository.getCountryByAcronym(newInventor.getCountry().getAcronym()));
        if (inventors.verify(newInventor)) {
            if (!inventors.getSelectedInventors().contains(newInventor)) {
                inventors.getSelectedInventors().add(newInventor);
            }
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
        inventors.getSelectedInventors().add(inventor);
        return "";
    }

    /**
     * Deletes a inventor from the patent's inventors list
     *
     * @param inventor
     * @return
     */
    public String deleteInventor(Inventor inventor) {
        inventors.getSelectedInventors().remove(inventor);
        return "";
    }

    /**
     * Pre-loads the patent's presentation file or the presentation file before user can download it
     *
     * @param file
     * @throws UnknownHostException
     * @throws IOException
     */
    public void downloadFile(Files file) throws UnknownHostException, IOException {
        openFs();
        GridFSDBFile gfile = fs.findOne(file.getId());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        long writeTo = gfile.writeTo(out);
        byte[] data = out.toByteArray();
        ByteArrayInputStream istream = new ByteArrayInputStream(data);
        InputStream arquivo = istream;
        ExternalContext externalContext = FacesContext.getCurrentInstance().getExternalContext();
        setDownload(new DefaultStreamedContent(arquivo, externalContext.getMimeType(gfile.getFilename()), gfile.getFilename()));
        
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
        openFs();
        file = event.getFile();
        GridFSInputFile gfsFiles = fs.createFile(file.getInputstream());
        gfsFiles.setFilename(file.getFileName());
        gfsFiles.save();
        if (temporaryPresentationFile == null) {
            temporaryPresentationFile = new Files((ObjectId) gfsFiles.getId());
        }
        FacesMessage msg = new FacesMessage("Sucesso", event.getFile().getFileName() + " foi enviado.");
        FacesContext.getCurrentInstance().addMessage(null, msg);
        
    }

    /**
     * Deletes the selected patent's current Presentation file
     *
     * @return
     * @throws java.net.UnknownHostException
     */
    public String deletePresentationFile() throws UnknownHostException {
        if(!temporaryPresentationFile.equals(selectedPatent.getPresentationFile())){
            openFs();
            fs.remove(temporaryPresentationFile.getId());
        }
        temporaryPresentationFile = null;
        Flash flash = FacesContext.getCurrentInstance().
                getExternalContext().getFlash();
        flash.put("info", "Deletado com Sucesso");
        return "";
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
        openFs();
        file = event.getFile();
        GridFSInputFile gfsFiles = fs.createFile(file.getInputstream());
        gfsFiles.setFilename(file.getFileName());
        gfsFiles.save();
        if (temporaryPatentInfo == null) {
            temporaryPatentInfo = new Files((ObjectId) gfsFiles.getId());
        }
        
        FacesMessage msg = new FacesMessage("Sucesso", event.getFile().getFileName() + " foi enviado.");
        FacesContext.getCurrentInstance().addMessage(null, msg);
    }

    /**
     * Deletes the selected patent's current Presentation file
     *
     * @return
     * @throws java.net.UnknownHostException
     */
    public String deletePatentInfo() throws UnknownHostException {
        if(!temporaryPatentInfo.equals(selectedPatent.getPatentInfo())){
            openFs();
            fs.remove(temporaryPatentInfo.getId());
        }
        temporaryPatentInfo = null;
        Flash flash = FacesContext.getCurrentInstance().
                getExternalContext().getFlash();
        flash.put("info", "Deletado com Sucesso");
        return "";
    }

    /**
     * Opens a conection with the database to acess the patent's files
     */
    private void openFs() throws UnknownHostException {
        if (fs == null) {
            fs = patentRepository.getFs();
        }
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
            patents.getRepo().setCompleted(null);
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
        this.patents = (LazyPatentDataModel) patents;
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
    public LazyApplicantDataModel getApplicants() {
        return applicants;
    }

    /**
     *
     * @param applicants
     */
    public void setApplicants(LazyApplicantDataModel applicants) {
        this.applicants = applicants;
    }

    /**
     *
     * @return
     */
    public LazyInventorDataModel getInventors() {
        return inventors;
    }

    /**
     *
     * @param inventors
     */
    public void setInventors(LazyInventorDataModel inventors) {
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
    
    /**
     *
     * @return
     */
    public Files getTemporaryPresentationFile() {
        return temporaryPresentationFile;
    }
    
    /**
     *
     * @param temporaryPresentationFile
     */
    public void setTemporaryPresentationFile(Files temporaryPresentationFile) {
        this.temporaryPresentationFile = temporaryPresentationFile;
    }
    
    /**
     *
     * @return
     */
    public Files getTemporaryPatentInfo() {
        return temporaryPatentInfo;
    }
    
    /**
     *
     * @param temporaryPatentInfo
     */
    public void setTemporaryPatentInfo(Files temporaryPatentInfo) {
        this.temporaryPatentInfo = temporaryPatentInfo;
    }
   
}
