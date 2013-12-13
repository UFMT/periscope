package br.ufmt.periscope.controller;

import br.ufmt.periscope.model.Project;
import br.ufmt.periscope.qualifier.CurrentProject;
import br.ufmt.periscope.repository.PatentRepository;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.inject.Inject;

@ManagedBean
@ViewScoped
public class SideBarController {
   
    private @Inject PatentRepository repo;
    private @Inject @CurrentProject Project currentProject;
    private Boolean renderer;

    public SideBarController() {
    }
   

    public Boolean getRenderer() {
        
        setRenderer();
        return renderer;
    }

    public void setRenderer() {
        this.renderer = !repo.isEmpty(currentProject);
    }
}
