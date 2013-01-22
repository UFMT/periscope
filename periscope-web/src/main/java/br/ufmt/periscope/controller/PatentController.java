package br.ufmt.periscope.controller;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import javax.faces.model.DataModel;
import javax.faces.model.ListDataModel;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

import br.ufmt.periscope.model.Patent;
import br.ufmt.periscope.model.Project;
import br.ufmt.periscope.qualifier.CurrentProject;
import br.ufmt.periscope.repository.PatentRepository;

@ManagedBean
@ViewScoped
public class PatentController {		

	private @Inject @CurrentProject Project currentProject;
	private @Inject PatentRepository patentRepository;
	private DataModel<Patent> patents = null;
	private String type = "complete";
	private String[] filters = {"complete","incomplete","darklist"};
	private int totalCount = 0;
	
	@PostConstruct
	public void init(){
		patents = new ListDataModel<Patent>(patentRepository.getAllPatents(currentProject));
		totalCount = patents.getRowCount();
		updateList();
	}
	
	public void updateList(){
		FacesContext context = FacesContext.getCurrentInstance();
		HttpServletRequest req = (HttpServletRequest) context
				.getExternalContext().getRequest();
		String param = req.getParameter("listType");
		if(param != null){
			type = req.getParameter("listType");
		}				
		if(type.contentEquals("complete")){
			patents = new ListDataModel<Patent>(patentRepository.getPatentsComplete(currentProject,true));
		}else if(type.contentEquals("incomplete")){
			patents = new ListDataModel<Patent>(patentRepository.getPatentsComplete(currentProject,false));
		}else if(type.contentEquals("darklist")){
			patents = new ListDataModel<Patent>(patentRepository.getPatentsDarklist(currentProject, true));
		}else{
			patents = new ListDataModel<Patent>(patentRepository.getPatentsDarklist(currentProject, false));
		}
	}
	
	public void updateBlackListPatent(){
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
	
	public int getPartialCount(){
		return patents.getRowCount();
	}

	public String getType() {
		return type;
	}
}
