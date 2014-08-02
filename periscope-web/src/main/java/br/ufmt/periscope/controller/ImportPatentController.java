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

    @PostConstruct
    public void init() {
        origins = importerFactory.getORIGINS();
    }

    public void importPatents() {
        if (uploadAttachment == null || uploadAttachment.isEmpty()) {
            FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro", "Nenhum arquivo foi enviado.");
            FacesContext.getCurrentInstance().addMessage(null, msg);
        } else {
            try {
                //long tempoInicio = System.currentTimeMillis();
                boolean imported = true;
                List<String> errors = new ArrayList<String>();
                for (UploadedFile file : uploadAttachment) {

                    InputStream is = file.getInputstream();
                    PatentImporter importer = importerFactory.getImporter(fileOrigin);
                    if (importer.initWithStream(is)) {
                        patentRepository.savePatentToDatabase(importer, currentProject);

//                        System.out.println(currentProject.getPatents().size());
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
                e.printStackTrace();
            }

        }
    }

    public void handleFileUpload(FileUploadEvent event) {
        uploadAttachment.add(event.getFile());
        FacesMessage msg = new FacesMessage("Sucesso", event.getFile().getFileName() + " foi enviado.");
        FacesContext.getCurrentInstance().addMessage(null, msg);
    }

    public String getFileOrigin() {
        return fileOrigin;
    }

    public void setFileOrigin(String fileOrigin) {
        this.fileOrigin = fileOrigin;
    }

    public String[] getOrigins() {
        return origins;
    }

    public List<UploadedFile> getUploadAttachment() {
        return uploadAttachment;
    }

    public void setUploadAttachment(List<UploadedFile> uploadAttachment) {
        this.uploadAttachment = uploadAttachment;
    }

}
