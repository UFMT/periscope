package br.ufmt.periscope.controller;

import br.ufmt.periscope.lazy.LazyApplicantDataModel;
import br.ufmt.periscope.lazy.LazyInventorDataModel;
import br.ufmt.periscope.lazy.LazyPatentDataModel;
import br.ufmt.periscope.model.Applicant;
import br.ufmt.periscope.model.Country;
import br.ufmt.periscope.model.Files;
import br.ufmt.periscope.model.Inventor;
import br.ufmt.periscope.model.Patent;
import br.ufmt.periscope.model.Priority;
import br.ufmt.periscope.model.Project;
import br.ufmt.periscope.qualifier.CurrentProject;
import br.ufmt.periscope.repository.CountryRepository;
import br.ufmt.periscope.repository.PatentRepository;
import br.ufmt.periscope.util.ResourcesLazy;
import com.mongodb.gridfs.GridFS;
import com.mongodb.gridfs.GridFSDBFile;
import com.mongodb.gridfs.GridFSInputFile;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
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

/**
 * - @ManagedBean<BR/>
 * - @ViewScoped<BR/>
 * Classe controller responsável por operações relacionadas às patentes
 */
@ManagedBean
@ViewScoped
public class PatentController {

    private @Inject
    @CurrentProject
    Project currentProject;
    private @Inject
    Logger log;
    private @Inject
    PatentRepository patentRepository;
    private @Inject
    LazyPatentDataModel patents;
    private String type = "complete";
    private String[] filters = {"complete", "incomplete", "darklist"};
    private int totalCount = 0;
    private Patent selectedPatent;
    private UploadedFile file;
    private StreamedContent temporaryPresentationFile;
    private StreamedContent temporaryPatentInfo;
    private GridFS fs;
    private @Inject
    ResourcesLazy lazyResources;
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
    private Boolean editing = false;
    private StreamedContent download;

    /**
     * Metodo de pós contrução do Controller<BR/>
     * Carrega as patentes, países, depositantes e inventores do projeto
     * atualmente selecionado
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
            editing = true;
            selectedPatent = patentRepository.getPatentWithId(currentProject, new ObjectId(req.getParameter("patentId"))).get(0);
            updateFilesInformation();
            if (selectedPatent.getMainCPCClassification() == null) {
                selectedPatent.setMainCPCClassification(lazyResources.getClassification());
            }
            if (selectedPatent.getMainClassification() == null) {
                selectedPatent.setMainClassification(lazyResources.getClassification());
            }
            updateApplicants();
            updateInventors();
        } else if (req.getParameter("patentAdd") != null) {
            selectedPatent = lazyResources.getPatent();
            selectedPatent.setMainClassification(lazyResources.getClassification());
            selectedPatent.setMainCPCClassification(lazyResources.getClassification());
            selectedPatent.setProject(currentProject);
            updateApplicants();
            updateInventors();
        } else {
            updateList();
        }
    }

    /**
     * Método chamado durante o pós construct ao editar uma patente<BR/>
     * Verifica a existência de arquivos na patente selecionada e os coloca
     * disponíveis para edição
     */
    private void updateFilesInformation() {
        ExternalContext externalContext = FacesContext.getCurrentInstance().getExternalContext();
        if (selectedPatent.getPatentInfo() != null) {
            try {
                InputStream arquivo = filesToInputStream(selectedPatent.getPatentInfo());
                String name = selectedPatent.getPatentInfo().getFilename();
                temporaryPatentInfo = new DefaultStreamedContent(arquivo, externalContext.getMimeType(name), name);
            } catch (Exception e) {
                log.throwing(PatentController.class.getName(), "updateFilesInformation", e);
            }
        }
        if (selectedPatent.getPresentationFile() != null) {
            try {
                InputStream arquivo = filesToInputStream(selectedPatent.getPresentationFile());
                String name = selectedPatent.getPresentationFile().getFilename();
                temporaryPresentationFile = new DefaultStreamedContent(arquivo, externalContext.getMimeType(name), name);
            } catch (Exception e) {
                log.throwing(PatentController.class.getName(), "updateFilesInformation", e);
            }
        }
    }

    /**
     * Atualiza a lista de depositantes de acordo com os depositantes de uma
     * patente selecionada para edição
     */
    public void updateApplicants() {
        applicants.setSelectedApplicants(selectedPatent.getApplicants());
    }

    /**
     * Atualiza a lista de inventores de acordo com os inventores de uma patente
     * selecionada para edição
     */
    public void updateInventors() {
        inventors.setSelectedInventors(selectedPatent.getInventors());
    }

    /**
     * Salva a patente que foi editada
     *
     * @return Página da listagem de patentes
     */
    public String save() {
        selectedPatent.setApplicants(applicants.getSelectedApplicants());
        selectedPatent.setInventors(inventors.getSelectedInventors());
        selectedPatent.setPatentInfo(savePatentInfo());
        selectedPatent.setPresentationFile(savePresentationFile());
        patentRepository.savePatent(selectedPatent);
        return "listPatent";
    }

    /**
     * Adiciona uma nova patente ao projeto
     *
     * @return Página da listagem de patentes
     */
    public String add() {
        selectedPatent.setApplicants(applicants.getSelectedApplicants());
        selectedPatent.setInventors(inventors.getSelectedInventors());
        selectedPatent.setPatentInfo(savePatentInfo());
        selectedPatent.setPresentationFile(savePresentationFile());
        patentRepository.savePatentToDatabase(selectedPatent, currentProject);
        return "listPatent";
    }

    /**
     * Durante edição ou adição de patente se o usuário fizer alterações nos
     * arquivos da patente e cancelar a operação esses arquivos são deletados
     *
     * @param page pagina que chamou o cancelamento das operações
     * @return De acordo com a página de entrada retorna a página de listagem de
     * patentes ou página inicial do projeto
     */
    public String cancelChange(String page) {
//        if (temporaryPatentInfo != null) {
//            if (!temporaryPatentInfo.equals(selectedPatent.getPatentInfo())) {
//                openFs();
//                fs.remove(temporaryPatentInfo.getId());
//            }
//        }
//        if (temporaryPresentationFile != null) {
//            if (!temporaryPresentationFile.equals(selectedPatent.getPresentationFile())) {
//                openFs();
//                fs.remove(temporaryPresentationFile.getId());
//            }
//        }
        if (page.equals("edit")) {
            return "listPatent";
        }
        if (page.equals("add")) {
            return "projectHome";
        }
        return "";
    }

    /**
     * Nova instancia de prioridade criada para adição de prioridades
     */
    public void doPriority() {
        newPriority = lazyResources.getPriority();
    }

    /**
     * Adiciona nova prioridade à lista de prioridades da patente
     *
     * @return <code>""</code>
     */
    public String addPriority() {
        List<Priority> list = selectedPatent.getPriorities();
        newPriority.setCountry(countryRepository.getCountryByAcronym(newPriority.getCountry().getAcronym()));
        list.add(newPriority);
        selectedPatent.setPriorities(list);
        return "";
    }

    /**
     * Retira uma prioridade da lista de prioridades da patente
     *
     * @param priority prioridade a ser removida da lista de prioridades da
     * patente
     * @return <code>""</code>
     */
    public String deletePriority(Priority priority) {
        List<Priority> list = selectedPatent.getPriorities();
        list.remove(priority);
        selectedPatent.setPriorities(list);
        return "";
    }

    /**
     * Nova instancia de depositante criada para adição de depositantes
     */
    public void doApplicant() {
        newApplicant = lazyResources.getApplicant();
    }

    /**
     * Criar e adiciona novo depositante à lista de depositantes da patente
     *
     * @return <code>""</code>
     */
    public String createNewApplicant() {
        newApplicant.setCountry(countryRepository.getCountryByAcronym(newApplicant.getCountry().getAcronym()));
        if (applicants.verify(newApplicant)) {
            if (!applicants.getSelectedApplicants().contains(newApplicant)) {
                applicants.getSelectedApplicants().add(newApplicant);
            }
        }
        return "";
    }

    /**
     * Vincula um novo depositante à lista de depositantes da patente
     *
     * @param applicant depositante a ser vinculado na lista de depositantes da
     * patente
     * @return <code>""</code>
     */
    public String addApplicant(Applicant applicant) {
        applicants.getSelectedApplicants().add(applicant);
        return "";
    }

    /**
     * Retira um depositante da lista de depositantes da patente
     *
     * @param applicant depositante a ser removido da lista de depositantes da
     * patente
     * @return <code>""</code>
     */
    public String deleteApplicant(Applicant applicant) {
        applicants.getSelectedApplicants().remove(applicant);
        return "";
    }

    /**
     * Nova instancia de inventor criada para adição de inventores
     */
    public void doInventor() {
        newInventor = lazyResources.getInventor();
    }

    /**
     * Criar e adiciona novo inventor à lista de inventores da patente
     *
     * @return <code>""</code>
     */
    public String createNewInventor() {
        newInventor.setCountry(countryRepository.getCountryByAcronym(newInventor.getCountry().getAcronym()));
        if (inventors.verify(newInventor)) {
            if (!inventors.getSelectedInventors().contains(newInventor)) {
                inventors.getSelectedInventors().add(newInventor);
            }
        }
        return "";
    }

    /**
     * Vincula um novo inventor à lista de inventores da patente
     *
     * @param inventor inventor a ser vinculado na lista de inventores da
     * patente
     * @return <code>""</code>
     */
    public String addInventor(Inventor inventor) {
        inventors.getSelectedInventors().add(inventor);
        return "";
    }

    /**
     * Retira um inventor da lista de inventores da patente
     *
     * @param inventor inventor a ser removido da lista de inventores da patente
     * @return <code>""</code>
     */
    public String deleteInventor(Inventor inventor) {
        inventors.getSelectedInventors().remove(inventor);
        return "";
    }

    /**
     * Cria um DefaultStreamedContent utilizado para fazer download dos arquivos
     * da patente
     *
     * @param stream InputStream com os dados do arquivo
     * @param name Nome do arquivo
     * @return StreamedContent - pode ser utilizado para download
     */
    private StreamedContent gerarStream(InputStream stream, String name) {
        ExternalContext externalContext = FacesContext.getCurrentInstance().getExternalContext();
        return new DefaultStreamedContent(stream, externalContext.getMimeType(name), name);
    }

    /**
     * Carrega previamente o arquivo da folha de rosto de uma patente antes que
     * o usuário possa baixá-la
     *
     * @param file Arquivo que será preparado para download
     * @throws IOException
     */
    public void downloadFile(Files file) throws IOException {
        openFs();
        GridFSDBFile gfile = fs.findOne(file.getId());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        long writeTo = gfile.writeTo(out);
        byte[] data = out.toByteArray();
        ByteArrayInputStream istream = new ByteArrayInputStream(data);
        InputStream arquivo = istream;
        closeFs();
        setDownload(gerarStream(arquivo, gfile.getFilename()));
    }

    /**
     * Carrega previamente o arquivo da folha de rosto de uma patente antes que
     * o usuário possa baixá-la
     *
     * @param file Arquivo que será preparado para download
     * @throws IOException
     */
    public void downloadFile(DefaultStreamedContent file) throws IOException {
        setDownload(file);
//        openFs();
//        GridFSDBFile gfile = fs.findOne(file.getId());
//        ByteArrayOutputStream out = new ByteArrayOutputStream();
//        long writeTo = gfile.writeTo(out);
//        byte[] data = out.toByteArray();
//        ByteArrayInputStream istream = new ByteArrayInputStream(data);
//        InputStream arquivo = istream;
//        closeFs();
//        ExternalContext externalContext = FacesContext.getCurrentInstance().getExternalContext();
//        setDownload(new DefaultStreamedContent(arquivo, externalContext.getMimeType(gfile.getFilename()), gfile.getFilename()));
    }

    /**
     * Transforma um arquivo do tipo Files para uma InputStream
     *
     * @param file arquivo do tipo Files
     * @return InpuStream equivalente ao arquivo
     * @throws IOException
     */
    public InputStream filesToInputStream(Files file) throws IOException {
        openFs();
        GridFSDBFile gfile = fs.findOne(file.getId());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        long writeTo = gfile.writeTo(out);
        byte[] data = out.toByteArray();
        ByteArrayInputStream istream = new ByteArrayInputStream(data);
        InputStream arquivo = istream;
        closeFs();
        return arquivo;
    }

    /**
     * Método responsável pelo upload do arquivo de folha de rosto da
     * patente<BR/>
     * Caso a patente já tenha uma folha do rosto o método deleta o arquivo do
     * banco de dados antes de fazer upload do novo arquivo
     *
     * @param event
     * @throws IOException
     */
    public void uploadPresentationFile(FileUploadEvent event) throws IOException {
        file = event.getFile();
        temporaryPresentationFile = null;
        temporaryPresentationFile = gerarStream(file.getInputstream(), file.getFileName());
        FacesMessage msg = new FacesMessage("Sucesso", event.getFile().getFileName() + " foi enviado.");
        FacesContext.getCurrentInstance().addMessage(null, msg);
    }

    /**
     * Deleta o arquivo de folha de rosto atual da patente
     *
     * @return <code>""</code>
     */
    public String deletePresentationFile() {
//        if (!temporaryPresentationFile.equals(selectedPatent.getPresentationFile())) {
//            openFs();
//            fs.remove(temporaryPresentationFile.getId());
//        }
        temporaryPresentationFile = null;
//        closeFs();
        Flash flash = FacesContext.getCurrentInstance().
                getExternalContext().getFlash();
        flash.put("info", "Deletado com Sucesso");
        return "";
    }

    /**
     * Método chamado ao se editar ou criar uma patente<BR/>
     * Salva um arquivo de fha de roso de uma patente no banco de dados<BR/>
     * Caso a patente originalmente tenha tido um arquivo e ao editar ele foi
     * excluído apenas exclui o arquivo original do banco e retorna o valor nulo
     *
     * @return Files - arquivo que foi adicionado ao banco
     */
    private Files savePresentationFile() {
        openFs();
        if (temporaryPresentationFile == null && selectedPatent.getPresentationFile() != null) {
            fs.remove(selectedPatent.getPresentationFile().getId());
            closeFs();
            return null;
        } else if (selectedPatent.getPresentationFile() != null) {
            fs.remove(selectedPatent.getPresentationFile().getId());
        }
        GridFSInputFile gfsFiles = fs.createFile(temporaryPresentationFile.getStream());
        gfsFiles.setFilename(temporaryPresentationFile.getName());
        gfsFiles.setContentType(temporaryPresentationFile.getContentType());
        gfsFiles.save();

        Files f = lazyResources.getFiles();
        f.setId((ObjectId) gfsFiles.getId());
        closeFs();
        return f;
    }

    /**
     * Método responsável pelo upload do arquivo completo da patente<BR/>
     * Caso a patente já tenha um arquivo da patente o método deleta o arquivo
     * do banco de dados antes de fazer upload do novo arquivo
     *
     * @param event
     * @throws IOException
     */
    public void uploadPatentInfo(FileUploadEvent event) throws IOException {
        file = event.getFile();
//        temporaryPatentInfo = null;
        temporaryPatentInfo = gerarStream(file.getInputstream(), file.getFileName());
        FacesMessage msg = new FacesMessage("Sucesso", event.getFile().getFileName() + " foi enviado.");
        FacesContext.getCurrentInstance().addMessage(null, msg);
    }

    /**
     * Deleta o arquivo completo atual da patente
     *
     * @return <code>""</code>
     */
    public String deletePatentInfo() {
//        if (!temporaryPatentInfo.equals(selectedPatent.getPatentInfo())) {
//            openFs();
//            fs.remove(temporaryPatentInfo.getId());
//        }
        temporaryPatentInfo = null;
//        closeFs();
        Flash flash = FacesContext.getCurrentInstance().
                getExternalContext().getFlash();
        flash.put("info", "Deletado com Sucesso");
        return "";
    }

    /**
     * Método chamado ao se editar ou criar uma patente<BR/>
     * Salva um arquivo completo de patente no banco de dados<BR/>
     * Caso a patente originalmente tenha tido um arquivo e ao editar ele foi
     * excluído apenas exclui o arquivo original do banco e retorna o valor nulo
     *
     * @return Files - arquivo que foi adicionado ao banco
     */
    private Files savePatentInfo() {
        openFs();
        if (temporaryPatentInfo == null && selectedPatent.getPatentInfo() != null) {
            fs.remove(selectedPatent.getPatentInfo().getId());
            closeFs();
            return null;
        } else if (selectedPatent.getPatentInfo() != null) {
            fs.remove(selectedPatent.getPatentInfo().getId());
        }
        GridFSInputFile gfsFiles = fs.createFile(temporaryPatentInfo.getStream());
        gfsFiles.setFilename(temporaryPatentInfo.getName());
        gfsFiles.setContentType(temporaryPatentInfo.getContentType());
        gfsFiles.save();

        Files f = lazyResources.getFiles();
        f.setId((ObjectId) gfsFiles.getId());
        closeFs();
        return f;
    }

    /**
     * Abre uma conexão com o banco de dados para tornar acessível os arquivos
     * da patente
     */
    private void openFs() {
        if (fs == null) {
            System.out.println("Openning FS");
            fs = lazyResources.getFS();
        }
    }

    /**
     * Fecha uma conexão com o banco de dados que torna acessível os arquivos da
     * patente
     */
    private void closeFs() {
        if (fs != null) {
            System.out.println("Closing FS");
            fs = null;
        }
    }

    /**
     * Atualiza a lista de patentes do projeto
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
     * Envia uma patente para a lista negra
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
    public StreamedContent getTemporaryPresentationFile() {
        return temporaryPresentationFile;
    }

    /**
     *
     * @param temporaryPresentationFile
     */
    public void setTemporaryPresentationFile(StreamedContent temporaryPresentationFile) {
        this.temporaryPresentationFile = temporaryPresentationFile;
    }

    /**
     *
     * @return
     */
    public StreamedContent getTemporaryPatentInfo() {
        return temporaryPatentInfo;
    }

    /**
     *
     * @param temporaryPatentInfo
     */
    public void setTemporaryPatentInfo(StreamedContent temporaryPatentInfo) {
        this.temporaryPatentInfo = temporaryPatentInfo;
    }

    /**
     *
     * @return
     */
    public Boolean getEditing() {
        return editing;
    }

    /**
     *
     * @param editing
     */
    public void setEditing(Boolean editing) {
        this.editing = editing;
    }
}
