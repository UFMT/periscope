package br.ufmt.periscope.controller;

import br.ufmt.periscope.model.Project;
import br.ufmt.periscope.qualifier.CurrentProject;
import br.ufmt.periscope.repository.ProjectRepository;
import javax.faces.bean.ManagedBean;
import javax.inject.Inject;

/**
 * - @ManagedBean<BR/>
 * Classe controller responsável por operações de visualização relacionadas aos menus da barra lateral
 */
@ManagedBean(name = "sidebarController")
public class SidebarController {

    private @Inject
    @CurrentProject
    Project currentProject;
    private @Inject
    ProjectRepository projectRepository;

    /**
     * Método que verifica a existência de patentes no projeto
     * @return Valor booleano se existem patentes no projeto
     */
    public boolean isEmptyPatent() {
        return projectRepository.isEmptyPatent(currentProject);
    }

}
