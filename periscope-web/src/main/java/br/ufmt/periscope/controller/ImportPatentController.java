package br.ufmt.periscope.controller;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import javax.inject.Inject;

import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.UploadedFile;

import br.ufmt.periscope.importer.PatentImporter;
import br.ufmt.periscope.importer.PatentImporterFactory;
import br.ufmt.periscope.importer.PatentRepository;
import br.ufmt.periscope.model.Project;
import br.ufmt.periscope.qualifier.CurrentProject;

import com.github.jmkgreen.morphia.Datastore;

@ManagedBean
@ViewScoped
public class ImportPatentController implements Serializable{
	
	private static final long serialVersionUID = 7744517674295407077L;
	
	
	private @Inject Datastore ds;
	private @Inject @CurrentProject Project currentProject;
	private @Inject PatentRepository patentRepository;
	
	private String fileOrigin;
	private String[] origins= PatentImporterFactory.ORIGINS;
	private UploadedFile fileUploaded = null;
	
	public void importPatents(){
		if(fileUploaded == null){
			FacesMessage msg = new FacesMessage("Erro","Nenhum arquivo foi enviado.");  
	        FacesContext.getCurrentInstance().addMessage(null, msg);
		}else{
			try {
				InputStream is = fileUploaded.getInputstream();
				PatentImporter importer = PatentImporterFactory.getImporter(fileOrigin);
				importer.initWithStream(is);				
					
				patentRepository.savePatentToDatabase(importer,currentProject);				
					
				FacesMessage msg = new FacesMessage("Sucesso","Arquivo importado com sucesso.");  
		        FacesContext.getCurrentInstance().addMessage(null, msg);		        		        		        
				
			} catch (IOException e) {		
				FacesMessage msg = new FacesMessage("Erro","Ocorreu um erro com o arquivo que foi enviado.");  
		        FacesContext.getCurrentInstance().addMessage(null, msg);
				e.printStackTrace();
			}
			
		}
	}
	
	public void handleFileUpload(FileUploadEvent event) { 
							
		fileUploaded = event.getFile();
        FacesMessage msg = new FacesMessage("Sucesso", event.getFile().getFileName() + " foi enviado.");  
        FacesContext.getCurrentInstance().addMessage(null, msg);  
    }

	public UploadedFile getFileUploaded(){
		return fileUploaded;
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
}
