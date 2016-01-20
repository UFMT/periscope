package br.ufmt.periscope.controller;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import javax.inject.Inject;

import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.UploadedFile;

import br.ufmt.periscope.importer.PatentImporter;
import br.ufmt.periscope.importer.PatentImporterFactory;
import br.ufmt.periscope.model.Project;
import br.ufmt.periscope.qualifier.CurrentProject;
import br.ufmt.periscope.repository.PatentRepository;

import com.github.jmkgreen.morphia.Datastore;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.faces.event.ValueChangeEvent;

/**
 * - @ManagedBean<BR/>
 * - @ViewScoped<BR/>
 * Classe controller responsável por operações relacionadas à importação
 */
@ManagedBean
@ViewScoped
public class ImportPatentController implements Serializable {

    private static final long serialVersionUID = 7744517674295407077L;
    private @Inject
    Datastore ds;
    private @Inject
    @CurrentProject
    Project currentProject;
    private @Inject
    PatentRepository patentRepository;
    private @Inject
    PatentImporterFactory importerFactory;
    private String fileOrigin;
    private String[] origins = null;
    private List<UploadedFile> uploadAttachment = new ArrayList<UploadedFile>();
    private boolean stepOne = true;
    private boolean stepTwo = false;
    private boolean stepThree = false;
    
    
    /**
     * Método pós construtor que atualiza os importadores existentes
     */
    @PostConstruct
    public void init() {
        origins = importerFactory.getORIGINS();
    }

    /**
     * Método chamado para efetivamente fazer a importação das patente a partir
     * dos arquivos selecionados
     */
    public void importPatents() {
        nextStep();
        
        if (uploadAttachment == null || uploadAttachment.isEmpty()) {
            FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro", "Nenhum arquivo foi enviado.");
            FacesContext.getCurrentInstance().addMessage(null, msg);
        } else {
            try {
                boolean imported = true;
                List<String> errors = new ArrayList<String>();
                for (UploadedFile file : uploadAttachment) {
                    InputStream is = file.getInputstream();
                    PatentImporter importer = importerFactory.getImporter(fileOrigin);
                    if (!file.getFileName().contains("csv") || !importer.provider().equals("PATENSCOPE")
                            && (!file.getFileName().contains("xls") || !importer.provider().equals("DPMA"))) {
                        if (importer.initWithStream(is)) {
                            patentRepository.savePatentToDatabase(importer, currentProject);
                        } else {
                            imported = false;
                            errors.add(file.getFileName());
                        }
                    } else {
                        imported = false;
                        errors.add(file.getFileName());
                    }
                }
                if (imported) {
                    FacesMessage msg = new FacesMessage("Sucesso", "Arquivo importado com sucesso.");
                    FacesContext.getCurrentInstance().addMessage(null, msg);
                } else {
                    FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro", "Ocorreu um erro com o(s) arquivo(s) enviado(s) " + Arrays.toString(errors.toArray()) + ".");
                    FacesContext.getCurrentInstance().addMessage(null, msg);
                }

            } catch (IOException e) {
                FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro", "Ocorreu um erro com o arquivo que foi enviado.");
                FacesContext.getCurrentInstance().addMessage(null, msg);
                System.out.println("Catch");
            }

        }
    }

    /**
     * Método responsável por lidar com o upload dos arquivos
     *
     * @param event
     */
    public void handleFileUpload(FileUploadEvent event) {
        uploadAttachment.add(event.getFile());
        FacesMessage msg = new FacesMessage("Sucesso", event.getFile().getFileName() + " foi enviado.");
        FacesContext.getCurrentInstance().addMessage(null, msg);
        stepTwo = false;
        stepThree = true;
    }
       

    /**
     *
     * @return
     */
    public String getFileOrigin() {
        return fileOrigin;
    }

    /**
     *
     * @param fileOrigin
     */
    public void setFileOrigin(String fileOrigin) {
        this.fileOrigin = fileOrigin;
    }

    /**
     *
     * @return
     */
    public String[] getOrigins() {
        return origins;
    }

    /**
     *
     * @return
     */
    public List<UploadedFile> getUploadAttachment() {
        return uploadAttachment;
    }

    /**
     *
     * @param uploadAttachment
     */
    public void setUploadAttachment(List<UploadedFile> uploadAttachment) {
        this.uploadAttachment = uploadAttachment;
    }

    public boolean isStepOne() {
        return stepOne;
    }

    public void setStepOne(boolean stepOne) {
        this.stepOne = stepOne;
    }
    
    public void nextStep(){
        this.stepThree = !stepOne && stepTwo;
        this.stepTwo = !stepThree && stepOne; 
        this.stepOne = !stepThree && !stepOne;
        
    }

    public boolean isStepTwo() {
        return stepTwo;
    }

    public void setStepTwo(boolean stepTwo) {
        this.stepTwo = stepTwo;
    }

    public boolean isStepThree() {
        return stepThree;
    }

    public void setStepThree(boolean stepThree) {
        this.stepThree = stepThree;
    }

    
}
