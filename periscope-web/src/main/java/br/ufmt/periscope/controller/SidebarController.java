package br.ufmt.periscope.controller;

import br.ufmt.periscope.model.Project;
import br.ufmt.periscope.qualifier.CurrentProject;
import br.ufmt.periscope.repository.ProjectRepository;
import javax.faces.bean.ManagedBean;
import javax.inject.Inject;

@ManagedBean(name = "sidebarController")
public class SidebarController {

    private @Inject
    @CurrentProject
    Project currentProject;
    private @Inject
    ProjectRepository projectRepository;

    public boolean isEmptyPatent() {
        return projectRepository.isEmptyPatent(currentProject);
    }

}
